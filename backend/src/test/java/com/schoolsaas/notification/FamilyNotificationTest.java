package com.schoolsaas.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schoolsaas.AbstractIntegrationTest;
import com.schoolsaas.TestAuthSupport;
import com.schoolsaas.auth.Role;
import com.schoolsaas.auth.User;
import com.schoolsaas.auth.UserRepository;
import com.schoolsaas.grade.Exam;
import com.schoolsaas.grade.ExamRepository;
import com.schoolsaas.parent.Parent;
import com.schoolsaas.parent.ParentRepository;
import com.schoolsaas.parent.StudentParent;
import com.schoolsaas.parent.StudentParentRepository;
import com.schoolsaas.schoolclass.SchoolClass;
import com.schoolsaas.schoolclass.SchoolClassRepository;
import com.schoolsaas.student.Student;
import com.schoolsaas.student.StudentRepository;
import com.schoolsaas.subject.Subject;
import com.schoolsaas.subject.SubjectRepository;
import com.schoolsaas.tenant.Tenant;
import com.schoolsaas.tenant.TenantRepository;
import com.schoolsaas.tenant.TenantResolver;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

/**
 * À qui les notifications sont réellement adressées.
 *
 * <p>Les absences partaient avec une liste de destinataires vide : le chemin avait été écrit
 * pour le SMS, qui vise les tuteurs par numéro de téléphone et n'a jamais eu besoin de
 * comptes. Rien ne le signalait — le registre journalisait un envoi sans destinataire.
 * {@code NEW_GRADE}, lui, n'était jamais déclenché du tout.
 */
class FamilyNotificationTest extends AbstractIntegrationTest {

    @MockBean
    private NotificationGateway notificationGateway;

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private StudentRepository studentRepository;
    @Autowired private ParentRepository parentRepository;
    @Autowired private StudentParentRepository studentParentRepository;
    @Autowired private SchoolClassRepository schoolClassRepository;
    @Autowired private ExamRepository examRepository;
    @Autowired private SubjectRepository subjectRepository;

    private Tenant tenant;
    private String jetonAdmin;
    private SchoolClass classe;
    private Student eleve;
    private User compteEleve;
    private User compteParent;

    @BeforeEach
    void preparerUneFamille() throws Exception {
        tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Famille");
        jetonAdmin = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "admin-famille@ecole.example", Role.ADMIN);

        classe = TestAuthSupport.withTenant(new SchoolClass("6e Famille", null), tenant.getId());
        classe = schoolClassRepository.save(classe);

        compteEleve = compte("eleve-famille@ecole.example", Role.STUDENT);
        compteParent = compte("parent-famille@ecole.example", Role.PARENT);

        eleve = TestAuthSupport.withTenant(
                new Student("M-" + UUID.randomUUID().toString().substring(0, 8), "Fatoumata", "Diallo",
                        LocalDate.of(2013, 5, 2), "F", classe.getId()),
                tenant.getId());
        eleve.setUserId(compteEleve.getId());
        eleve = studentRepository.save(eleve);

        Parent parent = TestAuthSupport.withTenant(
                new Parent("Oumou", "Diallo", "oumou@famille.example", "70000000"), tenant.getId());
        parent.setUserId(compteParent.getId());
        parent = parentRepository.save(parent);
        studentParentRepository.save(
                TestAuthSupport.withTenant(new StudentParent(eleve.getId(), parent.getId(), "MERE", true), tenant.getId()));
    }

    private User compte(String email, Role role) {
        User user = new User(email, passwordEncoder.encode("Secret123!"), "Compte", "Famille", role);
        user.setSchoolId(tenant.getId());
        return userRepository.save(user);
    }

    /**
     * Le message d'absence est rédigé à l'intention d'un tuteur : l'élève n'y est pas adjoint,
     * cela lui enverrait un texte qui ne lui est pas adressé.
     */
    @Test
    void sendsAnAbsenceToTheGuardiansOnly() throws Exception {
        mockMvc.perform(post("/api/v1/attendance/roll-call")
                        .header("Authorization", "Bearer " + jetonAdmin)
                        .header(TenantResolver.TENANT_HEADER, tenant.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"schoolClassId":%d,"date":"2026-10-02",
                                 "entries":[{"studentId":%d,"status":"ABSENT"}]}"""
                                .formatted(classe.getId(), eleve.getId())))
                .andExpect(status().isCreated());

        verify(notificationGateway).send(argThat(event ->
                event.type() == NotificationType.ABSENCE
                        && event.recipientUserIds().contains(compteParent.getId())
                        && !event.recipientUserIds().contains(compteEleve.getId())));
        verifyNoMoreInteractions(notificationGateway);
    }

    /** Une note concerne l'élève autant que sa famille. */
    @Test
    void sendsANewGradeToTheWholeFamily() throws Exception {
        Exam evaluation = saisirUneNote("12");

        verify(notificationGateway).send(argThat(event ->
                event.type() == NotificationType.NEW_GRADE
                        && event.recipientUserIds().contains(compteParent.getId())
                        && event.recipientUserIds().contains(compteEleve.getId())
                        // La note elle-même n'y figure pas : une notification s'affiche sur un
                        // écran verrouillé, que lit quiconque passe à côté.
                        && !event.body().contains("12")
                        && event.body().contains("Fatoumata")));
        assertThat(evaluation.getId()).isNotNull();
        verifyNoMoreInteractions(notificationGateway);
    }

    /** Une saisie se corrige plusieurs fois : autant d'alertes apprendraient à les ignorer. */
    @Test
    void staysSilentWhenTheGradeDoesNotChange() throws Exception {
        Exam evaluation = saisirUneNote("12");
        verify(notificationGateway).send(argThat(event -> event.type() == NotificationType.NEW_GRADE));

        mockMvc.perform(post("/api/v1/exams/{id}/grades", evaluation.getId())
                        .header("Authorization", "Bearer " + jetonAdmin)
                        .header(TenantResolver.TENANT_HEADER, tenant.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"entries":[{"studentId":%d,"score":12,"absent":false}]}"""
                                .formatted(eleve.getId())))
                .andExpect(status().isCreated());

        verifyNoMoreInteractions(notificationGateway);
    }

    private Exam saisirUneNote(String note) throws Exception {
        Subject matiere = subjectRepository.save(TestAuthSupport.withTenant(
                new Subject("Mathématiques", "MATH-" + UUID.randomUUID().toString().substring(0, 4), 4), tenant.getId()));
        Exam evaluation = examRepository.save(TestAuthSupport.withTenant(
                new Exam(classe.getId(), matiere.getId(), "Composition 1er trimestre", 20, 2, LocalDate.of(2026, 11, 20)),
                tenant.getId()));
        mockMvc.perform(post("/api/v1/exams/{id}/grades", evaluation.getId())
                        .header("Authorization", "Bearer " + jetonAdmin)
                        .header(TenantResolver.TENANT_HEADER, tenant.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"entries":[{"studentId":%d,"score":%s,"absent":false}]}"""
                                .formatted(eleve.getId(), note)))
                .andExpect(status().isCreated());
        return evaluation;
    }
}
