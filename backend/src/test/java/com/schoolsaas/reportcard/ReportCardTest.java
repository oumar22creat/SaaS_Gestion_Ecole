package com.schoolsaas.reportcard;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

/** Génération automatique des bulletins + export PDF (ROADMAP.md 2.1). */
class ReportCardTest extends AbstractIntegrationTest {

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
    private SubjectRepository subjectRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private ClassSubjectAssignmentRepository assignmentRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private GradeRepository gradeRepository;

    @Autowired
    private AttendanceRecordRepository attendanceRecordRepository;

    @Autowired
    private ReportCardRepository reportCardRepository;

    @Test
    void generatesAReportCardFromExistingGradesAndAttendance() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Bulletin");
        String token = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "admin-reportcard@ecole.example", Role.ADMIN);

        SchoolClass schoolClass = schoolClassRepository.save(TestAuthSupport.withTenant(new SchoolClass("6ème A", null), tenant.getId()));
        Subject maths = subjectRepository.save(TestAuthSupport.withTenant(new Subject("Maths", "MATH-RC", 3), tenant.getId()));
        Subject french = subjectRepository.save(TestAuthSupport.withTenant(new Subject("Français", "FR-RC", 2), tenant.getId()));
        Teacher teacher = teacherRepository.save(TestAuthSupport.withTenant(new Teacher("Jean", "Dupont", null, null), tenant.getId()));
        assignmentRepository.save(TestAuthSupport.withTenant(new ClassSubjectAssignment(schoolClass.getId(), maths.getId(), teacher.getId()), tenant.getId()));
        assignmentRepository.save(TestAuthSupport.withTenant(new ClassSubjectAssignment(schoolClass.getId(), french.getId(), teacher.getId()), tenant.getId()));

        Student student = studentRepository.save(TestAuthSupport.withTenant(
                new Student("RC1", "Ada", "Lovelace", null, null, schoolClass.getId()), tenant.getId()));

        Exam mathsExam = examRepository.save(TestAuthSupport.withTenant(
                new Exam(schoolClass.getId(), maths.getId(), "Contrôle", 20, 1, LocalDate.of(2026, 10, 1)), tenant.getId()));
        gradeRepository.save(TestAuthSupport.withTenant(new Grade(mathsExam.getId(), student.getId(), 16.0, false, null), tenant.getId()));
        Exam frenchExam = examRepository.save(TestAuthSupport.withTenant(
                new Exam(schoolClass.getId(), french.getId(), "Dictée", 20, 1, LocalDate.of(2026, 10, 2)), tenant.getId()));
        gradeRepository.save(TestAuthSupport.withTenant(new Grade(frenchExam.getId(), student.getId(), 10.0, false, null), tenant.getId()));

        attendanceRecordRepository.save(TestAuthSupport.withTenant(
                new AttendanceRecord(student.getId(), schoolClass.getId(), LocalDate.of(2026, 10, 3), AttendanceStatus.ABSENT, null, false, null),
                tenant.getId()));
        attendanceRecordRepository.save(TestAuthSupport.withTenant(
                new AttendanceRecord(student.getId(), schoolClass.getId(), LocalDate.of(2026, 10, 4), AttendanceStatus.LATE, null, false, null),
                tenant.getId()));

        String generateBody = "{\"schoolClassId\":" + schoolClass.getId() + ",\"periodLabel\":\"Trimestre 1\","
                + "\"periodFrom\":\"2026-09-01\",\"periodTo\":\"2026-10-31\"}";
        String response = mockMvc.perform(post("/api/v1/report-cards/generate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(generateBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.length()").value(1))
                // (16*3 + 10*2) / (3+2) = 13.6
                .andExpect(jsonPath("$.data[0].generalAverage").value(13.6))
                .andExpect(jsonPath("$.data[0].absenceCount").value(1))
                .andExpect(jsonPath("$.data[0].lateCount").value(1))
                .andExpect(jsonPath("$.data[0].entries.length()").value(2))
                .andReturn().getResponse().getContentAsString();
        Long reportCardId = objectMapper.readTree(response).get("data").get(0).get("id").asLong();

        mockMvc.perform(put("/api/v1/report-cards/" + reportCardId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"generalComment\":\"Bon trimestre\",\"councilDecision\":\"Passage\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.generalComment").value("Bon trimestre"));

        mockMvc.perform(put("/api/v1/report-cards/" + reportCardId + "/entries/" + maths.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"teacherComment\":\"Excellent travail\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.teacherComment").value("Excellent travail"));

        mockMvc.perform(get("/api/v1/report-cards/" + reportCardId + "/pdf").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    byte[] body = result.getResponse().getContentAsByteArray();
                    org.assertj.core.api.Assertions.assertThat(body.length).isGreaterThan(100);
                    org.assertj.core.api.Assertions.assertThat(new String(body, 0, 4)).isEqualTo("%PDF");
                });
    }

    @Test
    void regeneratingReplacesTheSubjectEntriesRatherThanDuplicatingThem() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Bulletin Regen");
        String token = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "admin-regen@ecole.example", Role.ADMIN);
        SchoolClass schoolClass = schoolClassRepository.save(TestAuthSupport.withTenant(new SchoolClass("6ème B", null), tenant.getId()));
        Subject subject = subjectRepository.save(TestAuthSupport.withTenant(new Subject("SVT", "SVT-RC", 1), tenant.getId()));
        Teacher teacher = teacherRepository.save(TestAuthSupport.withTenant(new Teacher("Jean", "Dupont", null, null), tenant.getId()));
        assignmentRepository.save(TestAuthSupport.withTenant(new ClassSubjectAssignment(schoolClass.getId(), subject.getId(), teacher.getId()), tenant.getId()));
        studentRepository.save(TestAuthSupport.withTenant(
                new Student("RC2", "Alan", "Turing", null, null, schoolClass.getId()), tenant.getId()));

        String generateBody = "{\"schoolClassId\":" + schoolClass.getId() + ",\"periodLabel\":\"Trimestre 1\","
                + "\"periodFrom\":\"2026-09-01\",\"periodTo\":\"2026-10-31\"}";

        mockMvc.perform(post("/api/v1/report-cards/generate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(generateBody))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/report-cards/generate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(generateBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].entries.length()").value(1));
    }

    @Test
    void aTenantCanNeverSeeReportCardsOfAnotherTenant() throws Exception {
        Tenant tenantA = TestAuthSupport.createActiveTenant(tenantRepository, "École Bulletin A");
        Tenant tenantB = TestAuthSupport.createActiveTenant(tenantRepository, "École Bulletin B");
        SchoolClass classB = schoolClassRepository.save(TestAuthSupport.withTenant(new SchoolClass("Secrète", null), tenantB.getId()));
        Student studentB = studentRepository.save(TestAuthSupport.withTenant(
                new Student("SEC", "Isolé", "TenantB", null, null, classB.getId()), tenantB.getId()));
        ReportCard reportCardB = reportCardRepository.save(TestAuthSupport.withTenant(
                new ReportCard(studentB.getId(), classB.getId(), "Trimestre 1", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 31)),
                tenantB.getId()));

        String tokenA = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenantA, "admin-a-reportcard@ecole.example", Role.ADMIN);

        mockMvc.perform(get("/api/v1/report-cards/" + reportCardB.getId()).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());
    }

    /**
     * Le rang manquait alors qu'au Mali c'est la première ligne que lit un parent, avant même
     * la moyenne. Deux élèves à égalité partagent le même rang et le suivant est décalé
     * d'autant — c'est la convention des bulletins, et elle garde le rang cohérent avec
     * l'effectif. Un élève sans aucune note n'est pas classé : lui donner le dernier rang le
     * sanctionnerait pour une absence d'évaluation, pas pour ses résultats.
     */
    @Test
    void ranksStudentsByGeneralAverageWithSharedRanksOnTies() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Rang");
        String token = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "admin-rang@ecole.example", Role.ADMIN);

        SchoolClass schoolClass = schoolClassRepository.save(
                TestAuthSupport.withTenant(new SchoolClass("6ème A", null), tenant.getId()));
        Subject maths = subjectRepository.save(TestAuthSupport.withTenant(new Subject("Maths", "MATH-RANK", 1), tenant.getId()));
        Teacher teacher = teacherRepository.save(TestAuthSupport.withTenant(new Teacher("Jean", "Dupont", null, null), tenant.getId()));
        assignmentRepository.save(TestAuthSupport.withTenant(
                new ClassSubjectAssignment(schoolClass.getId(), maths.getId(), teacher.getId()), tenant.getId()));
        Exam exam = examRepository.save(TestAuthSupport.withTenant(
                new Exam(schoolClass.getId(), maths.getId(), "Contrôle", 20, 1, LocalDate.of(2026, 10, 1)), tenant.getId()));

        Student premier = student(tenant, schoolClass, "RK1", "Awa");
        Student exAequo = student(tenant, schoolClass, "RK2", "Boubacar");
        Student dernier = student(tenant, schoolClass, "RK3", "Fanta");
        Student sansNote = student(tenant, schoolClass, "RK4", "Salif");

        grade(tenant, exam, premier, 18.0);
        grade(tenant, exam, exAequo, 18.0);
        grade(tenant, exam, dernier, 9.0);

        String response = mockMvc.perform(post("/api/v1/report-cards/generate")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"schoolClassId\":" + schoolClass.getId() + ",\"periodLabel\":\"Trimestre Rang\","
                                + "\"periodFrom\":\"2026-09-01\",\"periodTo\":\"2026-10-31\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        var cards = objectMapper.readTree(response).get("data");
        java.util.Map<Long, Integer> rankByStudent = new java.util.HashMap<>();
        java.util.Map<Long, Integer> sizeByStudent = new java.util.HashMap<>();
        cards.forEach(card -> {
            Long studentId = card.get("studentId").asLong();
            rankByStudent.put(studentId, card.get("rankInClass").isNull() ? null : card.get("rankInClass").asInt());
            sizeByStudent.put(studentId, card.get("classSize").asInt());
        });

        org.assertj.core.api.Assertions.assertThat(rankByStudent.get(premier.getId())).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(rankByStudent.get(exAequo.getId())).isEqualTo(1);
        // Deux premiers ex aequo : le suivant est 3e, pas 2e.
        org.assertj.core.api.Assertions.assertThat(rankByStudent.get(dernier.getId())).isEqualTo(3);
        org.assertj.core.api.Assertions.assertThat(rankByStudent.get(sansNote.getId())).isNull();
        // L'effectif affiché est celui de la classe, y compris l'élève non classé.
        org.assertj.core.api.Assertions.assertThat(sizeByStudent.values()).containsOnly(4);
    }

    private Student student(Tenant tenant, SchoolClass schoolClass, String number, String firstName) {
        return studentRepository.save(TestAuthSupport.withTenant(
                new Student(number, firstName, "Test", null, null, schoolClass.getId()), tenant.getId()));
    }

    private void grade(Tenant tenant, Exam exam, Student student, double score) {
        gradeRepository.save(TestAuthSupport.withTenant(
                new Grade(exam.getId(), student.getId(), score, false, null), tenant.getId()));
    }
}
