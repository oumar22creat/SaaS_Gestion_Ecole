package com.schoolsaas.statistics;

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

/** Statistiques avancées (ROADMAP.md 3.2) : évolution des résultats dans le temps, export CSV, isolation. */
class AdvancedStatisticsTest extends AbstractIntegrationTest {

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

    @Test
    void computesResultsEvolutionByCalendarMonthAndExportsCsv() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Statistiques Avancées");
        String token = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "admin-advstats@ecole.example", Role.ADMIN);
        SchoolClass schoolClass = schoolClassRepository.save(TestAuthSupport.withTenant(new SchoolClass("6ème A", null), tenant.getId()));
        Subject subject = subjectRepository.save(TestAuthSupport.withTenant(new Subject("Maths", "MATH-ADV", 2), tenant.getId()));
        Student student = studentRepository.save(
                TestAuthSupport.withTenant(new Student("AS1", "Ada", "L", null, null, schoolClass.getId()), tenant.getId()));

        createExamAndGrade(token, schoolClass.getId(), subject.getId(), "2026-09-15", student.getId(), 10);
        createExamAndGrade(token, schoolClass.getId(), subject.getId(), "2026-10-10", student.getId(), 16);

        mockMvc.perform(get("/api/v1/statistics/advanced/results-evolution")
                        .param("schoolClassId", schoolClass.getId().toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.points.length()").value(2))
                .andExpect(jsonPath("$.data.points[0].period").value("2026-09"))
                .andExpect(jsonPath("$.data.points[0].average").value(10.0))
                .andExpect(jsonPath("$.data.points[1].period").value("2026-10"))
                .andExpect(jsonPath("$.data.points[1].average").value(16.0));

        String csv = mockMvc.perform(get("/api/v1/statistics/advanced/results-evolution.csv")
                        .param("schoolClassId", schoolClass.getId().toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        org.assertj.core.api.Assertions.assertThat(csv)
                .startsWith("period,average,gradeCount\n")
                .contains("2026-09,10.0,1")
                .contains("2026-10,16.0,1");
    }

    @Test
    void aClassIdFromAnotherTenantYieldsNoDataInsteadOfLeaking() throws Exception {
        Tenant tenantA = TestAuthSupport.createActiveTenant(tenantRepository, "École Stats A");
        Tenant tenantB = TestAuthSupport.createActiveTenant(tenantRepository, "École Stats B");
        SchoolClass classB = schoolClassRepository.save(TestAuthSupport.withTenant(new SchoolClass("Secrète", null), tenantB.getId()));
        String tokenA = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenantA, "admin-advstats-a@ecole.example", Role.ADMIN);

        mockMvc.perform(get("/api/v1/statistics/advanced/results-evolution")
                        .param("schoolClassId", classB.getId().toString())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.points.length()").value(0));
    }

    private Long createExamAndGrade(String token, Long schoolClassId, Long subjectId, String examDate, Long studentId, double score)
            throws Exception {
        String examBody = "{\"schoolClassId\":" + schoolClassId + ",\"subjectId\":" + subjectId
                + ",\"label\":\"Contrôle\",\"maxScore\":20,\"coefficient\":1,\"examDate\":\"" + examDate + "\"}";
        String examResp = mockMvc.perform(post("/api/v1/exams")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(examBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long examId = objectMapper.readTree(examResp).get("data").get("id").asLong();

        mockMvc.perform(post("/api/v1/exams/" + examId + "/grades")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"entries\":[{\"studentId\":" + studentId + ",\"score\":" + score + ",\"absent\":false}]}"))
                .andExpect(status().isCreated());
        return examId;
    }
}
