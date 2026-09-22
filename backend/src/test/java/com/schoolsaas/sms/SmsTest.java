package com.schoolsaas.sms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schoolsaas.AbstractIntegrationTest;
import com.schoolsaas.TestAuthSupport;
import com.schoolsaas.auth.Role;
import com.schoolsaas.auth.UserRepository;
import com.schoolsaas.notification.NotificationType;
import com.schoolsaas.parent.Parent;
import com.schoolsaas.parent.ParentRepository;
import com.schoolsaas.parent.StudentParent;
import com.schoolsaas.parent.StudentParentRepository;
import com.schoolsaas.schoolclass.SchoolClass;
import com.schoolsaas.schoolclass.SchoolClassRepository;
import com.schoolsaas.student.Student;
import com.schoolsaas.student.StudentRepository;
import com.schoolsaas.tenant.Tenant;
import com.schoolsaas.tenant.TenantContext;
import com.schoolsaas.tenant.TenantRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

/**
 * SMS aux familles (cahier-des-charges.md §16). Les notifications n'étaient jamais envoyées :
 * la passerelle par défaut écrivait dans les logs. Or au Mali c'est le SMS qui atteint
 * réellement les parents.
 */
class SmsTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SchoolClassRepository schoolClassRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private ParentRepository parentRepository;

    @Autowired
    private StudentParentRepository studentParentRepository;

    @Autowired
    private SmsService smsService;

    @Autowired
    private SmsMessageRepository smsMessageRepository;

    /**
     * Les numéros sont saisis au guichet comme on les dicte. Sans normalisation, la moitié
     * des messages seraient refusés par l'opérateur pour un numéro pourtant valide.
     */
    @Test
    void normalisesLocallyTypedPhoneNumbersToInternationalFormat() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École SMS Numéros");
        TenantContext.set(tenant.getId());

        Student student = studentRepository.save(
                TestAuthSupport.withTenant(new Student("SMS-1", "Awa", "Traoré", null, null, null), tenant.getId()));
        Parent avecEspaces = parentRepository.save(
                TestAuthSupport.withTenant(new Parent("Fanta", "Traoré", null, "76 00 00 01"), tenant.getId()));
        Parent avecZeroZero = parentRepository.save(
                TestAuthSupport.withTenant(new Parent("Moussa", "Traoré", null, "0022376000002"), tenant.getId()));
        studentParentRepository.save(TestAuthSupport.withTenant(
                new StudentParent(student.getId(), avecEspaces.getId(), "MERE", true), tenant.getId()));
        studentParentRepository.save(TestAuthSupport.withTenant(
                new StudentParent(student.getId(), avecZeroZero.getId(), "PERE", false), tenant.getId()));

        List<String> numbers = smsService.guardianNumbers(student.getId());

        // Le contact principal en tête : sur une fratrie, cela évite d'écrire à tout le monde.
        assertThat(numbers).containsExactly("+22376000001", "+22376000002");
    }

    /** Un élève sans responsable joignable ne produit ni envoi ni trace : il n'y a rien à dire. */
    @Test
    void sendsNothingWhenNoGuardianHasAPhoneNumber() {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École SMS Sans Numéro");
        TenantContext.set(tenant.getId());

        Student student = studentRepository.save(
                TestAuthSupport.withTenant(new Student("SMS-2", "Salif", "Koné", null, null, null), tenant.getId()));
        Parent sansTelephone = parentRepository.save(
                TestAuthSupport.withTenant(new Parent("Aïcha", "Koné", "a@ecole.example", null), tenant.getId()));
        studentParentRepository.save(TestAuthSupport.withTenant(
                new StudentParent(student.getId(), sansTelephone.getId(), "MERE", true), tenant.getId()));

        long before = smsMessageRepository.count();
        smsService.notifyGuardians(student.getId(), NotificationType.ABSENCE, "Test");

        assertThat(smsMessageRepository.count()).isEqualTo(before);
    }

    /**
     * Chaque envoi est tracé même sans opérateur branché : le journal montre alors le
     * fournisseur « log ». Un établissement qui croirait ses parents prévenus alors que rien
     * n'est parti serait plus mal loti que s'il savait devoir téléphoner.
     */
    @Test
    void recordsEverySmsIncludingWhenNoProviderIsConfigured() {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École SMS Journal");
        TenantContext.set(tenant.getId());

        SmsMessage message = smsService.send("+22376000009", "Message de test", NotificationType.ANNOUNCEMENT);

        assertThat(message.getRecipient()).isEqualTo("+22376000009");
        assertThat(message.getBody()).isEqualTo("Message de test");
        assertThat(message.getStatus()).isEqualTo(SmsStatus.SENT);
        assertThat(smsMessageRepository.findById(message.getId())).isPresent();
    }

    /**
     * Une absence déclenche un SMS nommant l'élève. Le message portait son identifiant interne
     * (« Élève 7 — ABSENT ») : illisible pour un parent, et inutilisable pour une famille de
     * plusieurs enfants dans l'établissement.
     */
    @Test
    void anAbsenceTextsTheGuardiansWithTheStudentName() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École SMS Absence");
        String token = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "prof-sms@ecole.example", Role.TEACHER);
        TenantContext.set(tenant.getId());

        SchoolClass schoolClass = schoolClassRepository.save(
                TestAuthSupport.withTenant(new SchoolClass("6ème A", null), tenant.getId()));
        Student student = studentRepository.save(TestAuthSupport.withTenant(
                new Student("SMS-3", "Fatoumata", "Sidibé", null, null, schoolClass.getId()), tenant.getId()));
        Parent parent = parentRepository.save(
                TestAuthSupport.withTenant(new Parent("Aminata", "Sidibé", null, "76000003"), tenant.getId()));
        studentParentRepository.save(TestAuthSupport.withTenant(
                new StudentParent(student.getId(), parent.getId(), "MERE", true), tenant.getId()));

        mockMvc.perform(post("/api/v1/attendance/roll-call")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"schoolClassId\":" + schoolClass.getId() + ",\"date\":\"" + LocalDate.now()
                                + "\",\"entries\":[{\"studentId\":" + student.getId()
                                + ",\"status\":\"ABSENT\",\"justified\":false}]}"))
                .andExpect(status().isCreated());

        List<SmsMessage> sent = smsMessageRepository.findAll().stream()
                .filter(sms -> sms.getRecipient().equals("+22376000003"))
                .toList();
        assertThat(sent).hasSize(1);
        assertThat(sent.getFirst().getBody())
                .contains("Fatoumata Sidibé")
                .contains("absence signalée")
                // Le message est daté en clair, pas en ISO : il est lu sur un téléphone.
                .contains(LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
    }

    /**
     * La relance des impayés est déclenchée par le comptable, jamais automatiquement : elle
     * engage l'établissement auprès des familles. Un seul message par élève, même s'il cumule
     * plusieurs échéances — trois SMS le même jour se lisent comme du harcèlement.
     */
    @Test
    void remindsEachLateFamilyOnceRegardlessOfTheNumberOfOverdueInstalments() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École SMS Relance");
        String token = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "compta-sms@ecole.example", Role.ACCOUNTANT);
        TenantContext.set(tenant.getId());

        SchoolClass schoolClass = schoolClassRepository.save(
                TestAuthSupport.withTenant(new SchoolClass("6ème A", null), tenant.getId()));
        Student student = studentRepository.save(TestAuthSupport.withTenant(
                new Student("SMS-4", "Ibrahim", "Cissé", null, null, schoolClass.getId()), tenant.getId()));
        Parent parent = parentRepository.save(
                TestAuthSupport.withTenant(new Parent("Sekou", "Cissé", null, "76000004"), tenant.getId()));
        studentParentRepository.save(TestAuthSupport.withTenant(
                new StudentParent(student.getId(), parent.getId(), "PERE", true), tenant.getId()));

        // Deux échéances dépassées pour le même élève.
        for (String label : List.of("Trimestre 1", "Trimestre 2")) {
            String response = mockMvc.perform(post("/api/v1/school-fees/schedules")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"schoolClassId\":" + schoolClass.getId() + ",\"label\":\"" + label
                                    + "\",\"amountCents\":50000,\"dueDate\":\"" + LocalDate.now().minusDays(20) + "\"}"))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();
            Long scheduleId = objectMapper.readTree(response).get("data").get("id").asLong();
            mockMvc.perform(post("/api/v1/school-fees/schedules/" + scheduleId + "/generate-invoices")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(post("/api/v1/school-fees/reminders").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(1));

        List<SmsMessage> sent = smsMessageRepository.findAll().stream()
                .filter(sms -> sms.getRecipient().equals("+22376000004"))
                .toList();
        assertThat(sent).hasSize(1);
        assertThat(sent.getFirst().getBody()).contains("Ibrahim Cissé").contains("2 échéances").contains("100 000 XOF");
    }
}
