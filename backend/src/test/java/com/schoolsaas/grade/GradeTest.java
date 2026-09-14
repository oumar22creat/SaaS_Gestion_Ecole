package com.schoolsaas.grade;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schoolsaas.AbstractIntegrationTest;
import com.schoolsaas.TestAuthSupport;
import com.schoolsaas.auth.Role;
import com.schoolsaas.auth.UserRepository;
import com.schoolsaas.schoolclass.SchoolClass;
import com.schoolsaas.schoolclass.SchoolClassRepository;
import com.schoolsaas.student.Student;
import com.schoolsaas.student.StudentRepository;
import com.schoolsaas.subject.Subject;
import com.schoolsaas.subject.SubjectRepository;
import com.schoolsaas.tenant.Tenant;
import com.schoolsaas.tenant.TenantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

/** CRUD évaluations/notes + calcul automatique des moyennes (ROADMAP.md 1.8). */
class GradeTest extends AbstractIntegrationTest {

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
    private StudentRepository studentRepository;

    @Autowired
    private ExamRepository examRepository;

    @Test
    void computesExamStatisticsIgnoringAbsentStudents() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Notes");
        String token = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "teacher-grade@ecole.example", Role.TEACHER);
        SchoolClass schoolClass = schoolClassRepository.save(TestAuthSupport.withTenant(new SchoolClass("6ème A", null), tenant.getId()));
        Subject subject = subjectRepository.save(TestAuthSupport.withTenant(new Subject("Maths", "MATH", 2), tenant.getId()));
        Student s1 = studentRepository.save(TestAuthSupport.withTenant(new Student("G1", "Ada", "L", null, null, schoolClass.getId()), tenant.getId()));
        Student s2 = studentRepository.save(TestAuthSupport.withTenant(new Student("G2", "Alan", "T", null, null, schoolClass.getId()), tenant.getId()));
        Student s3 = studentRepository.save(TestAuthSupport.withTenant(new Student("G3", "Grace", "H", null, null, schoolClass.getId()), tenant.getId()));

        String examBody = "{\"schoolClassId\":" + schoolClass.getId() + ",\"subjectId\":" + subject.getId()
                + ",\"label\":\"Contrôle 1\",\"maxScore\":20,\"coefficient\":2,\"examDate\":\"2026-09-20\"}";
        String examResp = mockMvc.perform(post("/api/v1/exams")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(examBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long examId = objectMapper.readTree(examResp).get("data").get("id").asLong();

        String gradesBody = "{\"entries\":["
                + "{\"studentId\":" + s1.getId() + ",\"score\":16,\"absent\":false},"
                + "{\"studentId\":" + s2.getId() + ",\"score\":8,\"absent\":false},"
                + "{\"studentId\":" + s3.getId() + ",\"score\":null,\"absent\":true}"
                + "]}";
        mockMvc.perform(post("/api/v1/exams/" + examId + "/grades")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gradesBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.length()").value(3));

        mockMvc.perform(get("/api/v1/exams/" + examId + "/statistics").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.gradedCount").value(2))
                .andExpect(jsonPath("$.data.average").value(12.0))
                .andExpect(jsonPath("$.data.min").value(8.0))
                .andExpect(jsonPath("$.data.max").value(16.0));

        mockMvc.perform(get("/api/v1/students/" + s1.getId() + "/subjects/" + subject.getId() + "/average")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.average").value(16.0));

        mockMvc.perform(get("/api/v1/classes/" + schoolClass.getId() + "/subjects/" + subject.getId() + "/average")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.average").value(12.0))
                .andExpect(jsonPath("$.data.studentCount").value(2));
    }

    @Test
    void weightsTheStudentAverageByExamCoefficient() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Notes Coeff");
        String token = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "teacher-grade2@ecole.example", Role.TEACHER);
        SchoolClass schoolClass = schoolClassRepository.save(TestAuthSupport.withTenant(new SchoolClass("6ème A", null), tenant.getId()));
        Subject subject = subjectRepository.save(TestAuthSupport.withTenant(new Subject("Maths", "MATH2", 2), tenant.getId()));
        Student student = studentRepository.save(TestAuthSupport.withTenant(new Student("G4", "Ada", "L", null, null, null), tenant.getId()));

        Exam exam1 = examRepository.save(TestAuthSupport.withTenant(
                new Exam(schoolClass.getId(), subject.getId(), "Devoir 1", 20, 1, java.time.LocalDate.of(2026, 9, 1)), tenant.getId()));
        Exam exam2 = examRepository.save(TestAuthSupport.withTenant(
                new Exam(schoolClass.getId(), subject.getId(), "Examen final", 20, 3, java.time.LocalDate.of(2026, 9, 30)), tenant.getId()));

        mockMvc.perform(post("/api/v1/exams/" + exam1.getId() + "/grades")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"entries\":[{\"studentId\":" + student.getId() + ",\"score\":10,\"absent\":false}]}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/exams/" + exam2.getId() + "/grades")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"entries\":[{\"studentId\":" + student.getId() + ",\"score\":18,\"absent\":false}]}"))
                .andExpect(status().isCreated());

        // (10*1 + 18*3) / (1+3) = 16
        mockMvc.perform(get("/api/v1/students/" + student.getId() + "/subjects/" + subject.getId() + "/average")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.average").value(16.0));
    }

    @Test
    void aTenantCanNeverSeeExamsOfAnotherTenant() throws Exception {
        Tenant tenantA = TestAuthSupport.createActiveTenant(tenantRepository, "École Notes A");
        Tenant tenantB = TestAuthSupport.createActiveTenant(tenantRepository, "École Notes B");
        SchoolClass classB = schoolClassRepository.save(TestAuthSupport.withTenant(new SchoolClass("Secrète", null), tenantB.getId()));
        Subject subjectB = subjectRepository.save(TestAuthSupport.withTenant(new Subject("Secret", "SEC", 1), tenantB.getId()));
        Exam examB = examRepository.save(TestAuthSupport.withTenant(
                new Exam(classB.getId(), subjectB.getId(), "Secret", 20, 1, java.time.LocalDate.of(2026, 9, 1)), tenantB.getId()));

        String tokenA = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenantA, "teacher-a-grade@ecole.example", Role.TEACHER);

        mockMvc.perform(get("/api/v1/exams/" + examB.getId()).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());
    }
}
