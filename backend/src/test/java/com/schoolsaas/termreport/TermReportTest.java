package com.schoolsaas.termreport;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schoolsaas.AbstractIntegrationTest;
import com.schoolsaas.TestAuthSupport;
import com.schoolsaas.attendance.AttendanceRecord;
import com.schoolsaas.attendance.AttendanceRecordRepository;
import com.schoolsaas.attendance.AttendanceStatus;
import com.schoolsaas.auth.Role;
import com.schoolsaas.auth.UserRepository;
import com.schoolsaas.grade.Exam;
import com.schoolsaas.grade.ExamRepository;
import com.schoolsaas.grade.ExamType;
import com.schoolsaas.grade.Grade;
import com.schoolsaas.grade.GradeRepository;
import com.schoolsaas.schoolclass.ClassSubjectAssignment;
import com.schoolsaas.schoolclass.ClassSubjectAssignmentRepository;
import com.schoolsaas.schoolclass.SchoolClass;
import com.schoolsaas.schoolclass.SchoolClassRepository;
import com.schoolsaas.student.Student;
import com.schoolsaas.student.StudentRepository;
import com.schoolsaas.subject.Subject;
import com.schoolsaas.subject.SubjectRepository;
import com.schoolsaas.teacher.Teacher;
import com.schoolsaas.teacher.TeacherRepository;
import com.schoolsaas.tenant.Tenant;
import com.schoolsaas.tenant.TenantRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

/** Rapport trimestriel adressé à la tutelle. */
class TermReportTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private SchoolClassRepository schoolClassRepository;
    @Autowired private SubjectRepository subjectRepository;
    @Autowired private TeacherRepository teacherRepository;
    @Autowired private ClassSubjectAssignmentRepository assignmentRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private ExamRepository examRepository;
    @Autowired private GradeRepository gradeRepository;
    @Autowired private AttendanceRecordRepository attendanceRepository;

    /**
     * Le cœur du rapport : des agrégats, pas des noms. Et surtout, un élève sans note ne
     * doit pas peser sur le taux de réussite — le compter comme un échec ferait baisser le
     * chiffre adressé au ministère pour une évaluation qui n'a pas eu lieu.
     */
    @Test
    void theReportAggregatesTheClassAndLeavesUnassessedStudentsOutOfThePassRate() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Rapport");
        tenant.setDirectorName("Mme Fatoumata Sidibé");
        tenant.setOfficialAuthority("RÉPUBLIQUE DU MALI");
        tenant.setHeadOfficeCity("Bamako");
        tenantRepository.save(tenant);

        String token = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "direction@ecole.example",
                Role.DIRECTION);

        Teacher titulaire = teacherRepository.save(
                TestAuthSupport.withTenant(new Teacher("Modibo", "Keïta", null, null), tenant.getId()));
        SchoolClass classe = schoolClassRepository.save(TestAuthSupport.withTenant(
                new SchoolClass("6ème année A", titulaire.getId()), tenant.getId()));
        Subject maths = subjectRepository.save(
                TestAuthSupport.withTenant(new Subject("Mathématiques", "MATH-RAP", 4), tenant.getId()));
        assignmentRepository.save(TestAuthSupport.withTenant(
                new ClassSubjectAssignment(classe.getId(), maths.getId(), titulaire.getId()), tenant.getId()));

        // Trois élèves notés : 16, 12 et 6. Deux atteignent la moyenne, soit 66,7 %.
        double[] notes = {16, 12, 6};
        String[] sexes = {"F", "M", "F"};
        for (int i = 0; i < notes.length; i++) {
            Student eleve = studentRepository.save(TestAuthSupport.withTenant(
                    new Student("RAP-" + i, "Élève" + i, "Nom" + i, LocalDate.of(2014, 1, 1), sexes[i],
                            classe.getId()), tenant.getId()));
            Exam composition = examRepository.save(TestAuthSupport.withTenant(
                    new Exam(classe.getId(), maths.getId(), "Composition " + i, 20, 2,
                            LocalDate.of(2026, 11, 10), ExamType.COMPOSITION), tenant.getId()));
            gradeRepository.save(TestAuthSupport.withTenant(
                    new Grade(composition.getId(), eleve.getId(), notes[i], false, null), tenant.getId()));
            attendanceRepository.save(TestAuthSupport.withTenant(
                    new AttendanceRecord(eleve.getId(), classe.getId(), LocalDate.of(2026, 11, 12),
                            i == 0 ? AttendanceStatus.ABSENT : AttendanceStatus.PRESENT, null, i == 0, null),
                    tenant.getId()));
        }
        // Un quatrième élève, inscrit mais jamais évalué.
        studentRepository.save(TestAuthSupport.withTenant(
                new Student("RAP-X", "Sans", "Note", LocalDate.of(2014, 5, 5), "M", classe.getId()),
                tenant.getId()));

        String corps = "{\"schoolClassId\":" + classe.getId() + ",\"periodLabel\":\"1er trimestre\","
                + "\"periodFrom\":\"2026-09-15\",\"periodTo\":\"2026-12-20\"}";
        mockMvc.perform(post("/api/v1/report-cards/generate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(corps))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/term-reports/classes/" + classe.getId())
                        .param("periodLabel", "1er trimestre")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.className").value("6ème année A"))
                .andExpect(jsonPath("$.data.headTeacherName").value("Modibo Keïta"))
                .andExpect(jsonPath("$.data.effectif.total").value(4))
                .andExpect(jsonPath("$.data.effectif.filles").value(2))
                .andExpect(jsonPath("$.data.effectif.garcons").value(2))
                .andExpect(jsonPath("$.data.effectif.evalues").value(3))
                // (16 + 12 + 6) / 3 = 11,33
                .andExpect(jsonPath("$.data.resultats.moyenneClasse").value(11.33))
                .andExpect(jsonPath("$.data.resultats.meilleureMoyenne").value(16.0))
                .andExpect(jsonPath("$.data.resultats.plusFaibleMoyenne").value(6.0))
                .andExpect(jsonPath("$.data.resultats.admis").value(2))
                // 2 sur 3 évalués, et non 2 sur 4 inscrits.
                .andExpect(jsonPath("$.data.resultats.tauxReussite").value(66.67))
                .andExpect(jsonPath("$.data.resultats.evaluesSansNote").value(1))
                .andExpect(jsonPath("$.data.matieres[0].matiere").value("Mathématiques"))
                .andExpect(jsonPath("$.data.matieres[0].atteignentLaMoyenne").value(2))
                .andExpect(jsonPath("$.data.assiduite.absences").value(1))
                .andExpect(jsonPath("$.data.assiduite.absencesJustifiees").value(1))
                // 2 présents sur 3 appels.
                .andExpect(jsonPath("$.data.assiduite.tauxPresence").value(66.67));

        byte[] pdf = mockMvc.perform(get("/api/v1/term-reports/classes/" + classe.getId() + "/pdf")
                        .param("periodLabel", "1er trimestre")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        try (org.apache.pdfbox.pdmodel.PDDocument document = org.apache.pdfbox.Loader.loadPDF(pdf)) {
            String texte = new org.apache.pdfbox.text.PDFTextStripper().getText(document);
            org.assertj.core.api.Assertions.assertThat(texte)
                    .contains("RAPPORT TRIMESTRIEL")
                    .contains("RÉPUBLIQUE DU MALI")
                    .contains("6ème année A")
                    // Le même réglage porte le même nom partout : l'écran de classe dit
                    // « Professeur principal », le rapport le disait « TITULAIRE », et
                    // personne ne faisait le rapprochement.
                    .contains("PROFESSEUR PRINCIPAL")
                    .doesNotContain("TITULAIRE")
                    .contains("RÉSULTATS PAR DISCIPLINE")
                    .contains("RÉPARTITION DES MOYENNES")
                    // La signature porte le nom du directeur, pas un intitulé seul.
                    .contains("LE DIRECTEUR")
                    .contains("Mme Fatoumata Sidibé")
                    .contains("Bamako")
                    // Les caractères hors WinAnsi doivent être convertis, pas remplacés par
                    // un « ? » : « MOYENNE ≥ 10 » en plein intitulé de colonne faisait
                    // douter du document entier.
                    .contains("MOYENNE >= 10")
                    .doesNotContain("? 10");
        }
    }

    /** Sans bulletins générés, le rapport refuse plutôt que de rendre un document vide. */
    @Test
    void theReportRefusesWhenNoReportCardHasBeenGeneratedForThePeriod() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Rapport Vide");
        String token = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "dir-vide@ecole.example",
                Role.DIRECTION);
        SchoolClass classe = schoolClassRepository.save(
                TestAuthSupport.withTenant(new SchoolClass("5ème année A", null), tenant.getId()));

        mockMvc.perform(get("/api/v1/term-reports/classes/" + classe.getId())
                        .param("periodLabel", "1er trimestre")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("NO_REPORT_CARD_FOR_PERIOD"));
    }

    /** Un enseignant n'adresse pas de rapport au ministère : ce document engage l'école. */
    @Test
    void aTeacherCannotProduceTheMinistryReport() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Rapport Rôle");
        String token = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "prof-rapport@ecole.example",
                Role.TEACHER);
        SchoolClass classe = schoolClassRepository.save(
                TestAuthSupport.withTenant(new SchoolClass("4ème année A", null), tenant.getId()));

        mockMvc.perform(get("/api/v1/term-reports/classes/" + classe.getId())
                        .param("periodLabel", "1er trimestre")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
