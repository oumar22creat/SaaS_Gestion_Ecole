package com.schoolsaas.student;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schoolsaas.AbstractIntegrationTest;
import com.schoolsaas.TestAuthSupport;
import com.schoolsaas.auth.Role;
import com.schoolsaas.auth.UserRepository;
import com.schoolsaas.schoolclass.SchoolClass;
import com.schoolsaas.schoolclass.SchoolClassRepository;
import com.schoolsaas.tenant.Tenant;
import com.schoolsaas.tenant.TenantRepository;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

/** CRUD élèves + import CSV (ROADMAP.md 1.5) + isolation cross-tenant. */
class StudentTest extends AbstractIntegrationTest {

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
    private StudentRepository studentRepository;

    @Autowired
    private SchoolClassRepository schoolClassRepository;

    @Test
    void createsAStudentAndRejectsADuplicateStudentNumber() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Élève");
        String token = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "admin-student@ecole.example", Role.ADMIN);

        mockMvc.perform(post("/api/v1/students")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentNumber\":\"E001\",\"firstName\":\"Léa\",\"lastName\":\"Martin\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.studentNumber").value("E001"));

        mockMvc.perform(post("/api/v1/students")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentNumber\":\"E001\",\"firstName\":\"Autre\",\"lastName\":\"Élève\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("STUDENT_NUMBER_ALREADY_USED"));
    }

    @Test
    void importsStudentsFromCsvAndAssignsExistingClassByName() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Import");
        String token = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "admin-import@ecole.example", Role.ADMIN);
        SchoolClass schoolClass =
                schoolClassRepository.save(TestAuthSupport.withTenant(new SchoolClass("6ème A", null), tenant.getId()));

        String csv = "studentNumber,firstName,lastName,birthDate,gender,className\n"
                + "IMP001,Ada,Lovelace,2012-05-01,F,6ème A\n"
                + "IMP002,Alan,Turing,,M,\n"
                + "IMP001,Doublon,Doublon,,,\n";
        MockMultipartFile file = new MockMultipartFile("file", "students.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/v1/students/import").file(file).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.imported").value(2))
                .andExpect(jsonPath("$.data.errors.length()").value(1))
                .andExpect(jsonPath("$.data.errors[0].line").value(4));

        Student imported = studentRepository.findAll().stream()
                .filter(s -> "IMP001".equals(s.getStudentNumber()))
                .findFirst().orElseThrow();
        org.assertj.core.api.Assertions.assertThat(imported.getSchoolClassId()).isEqualTo(schoolClass.getId());
    }

    /**
     * L'import inscrivait l'élève sans le rattacher à l'année active, contrairement à la
     * création unitaire. Le manque ne levait aucune erreur : une école qui démarre en
     * important son effectif — le cas normal — voyait son année scolaire rester vide, donc
     * aucun passage de classe possible en fin d'année.
     */
    @Test
    void anImportedStudentIsEnrolledInTheActiveSchoolYearLikeAnyOther() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Import Année");
        String token = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "admin-annee@ecole.example", Role.ADMIN);
        SchoolClass schoolClass =
                schoolClassRepository.save(TestAuthSupport.withTenant(new SchoolClass("7ème A", null), tenant.getId()));

        String yearResponse = mockMvc.perform(post("/api/v1/school-years")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"label\":\"2026-2027\",\"startDate\":\"2026-09-15\",\"endDate\":\"2027-07-05\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long yearId = objectMapper.readTree(yearResponse).path("data").path("id").asLong();
        mockMvc.perform(put("/api/v1/school-years/" + yearId + "/activate").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        String csv = "studentNumber,firstName,lastName,birthDate,gender,className\n"
                + "ANN001,Fanta,Sidibé,2013-02-11,F,7ème A\n";
        MockMultipartFile file = new MockMultipartFile("file", "students.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(multipart("/api/v1/students/import").file(file).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.imported").value(1));

        Student imported = studentRepository.findAll().stream()
                .filter(s -> "ANN001".equals(s.getStudentNumber()))
                .findFirst().orElseThrow();

        mockMvc.perform(get("/api/v1/school-years/enrollments")
                        .param("studentId", String.valueOf(imported.getId()))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].schoolYearId").value(yearId))
                .andExpect(jsonPath("$.data[0].schoolClassId").value(schoolClass.getId()))
                .andExpect(jsonPath("$.data[0].status").value("ENROLLED"));
    }

    @Test
    void aTenantCanNeverSeeStudentsOfAnotherTenant() throws Exception {
        Tenant tenantA = TestAuthSupport.createActiveTenant(tenantRepository, "École Élève A");
        Tenant tenantB = TestAuthSupport.createActiveTenant(tenantRepository, "École Élève B");
        Student studentB = studentRepository.save(TestAuthSupport.withTenant(
                new Student("SECRET", "Isolé", "TenantB", null, null, null), tenantB.getId()));

        String tokenA = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenantA, "admin-a-student@ecole.example", Role.ADMIN);

        mockMvc.perform(get("/api/v1/students/" + studentB.getId()).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/students").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(result -> org.assertj.core.api.Assertions.assertThat(result.getResponse().getContentAsString())
                        .doesNotContain("SECRET"));
    }
}
