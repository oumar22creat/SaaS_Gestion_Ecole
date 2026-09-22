package com.schoolsaas.schoolyear;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import com.schoolsaas.student.Student;
import com.schoolsaas.student.StudentRepository;
import com.schoolsaas.tenant.Tenant;
import com.schoolsaas.tenant.TenantRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Années scolaires et rentrée (cahier-des-charges.md §7). Le produit n'avait aucune notion
 * d'année : la classe d'un élève tenait dans une colonne qui s'écrasait, et la rentrée se
 * faisait fiche par fiche.
 */
class SchoolYearTest extends AbstractIntegrationTest {

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
    private EnrollmentRepository enrollmentRepository;

    /**
     * Sans cet invariant, « l'année en cours » devient ambigu et chaque écran choisit la
     * sienne. L'index unique en base le garantit, le service doit donc clôturer la
     * précédente au lieu d'échouer.
     */
    @Test
    void activatingAYearClosesThePreviousOne() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Années");
        String token = adminToken(tenant, "admin-years@ecole.example");

        Long first = createYear(token, "2025-2026", "2025-10-01", "2026-07-31");
        Long second = createYear(token, "2026-2027", "2026-10-01", "2027-07-31");

        mockMvc.perform(put("/api/v1/school-years/" + first + "/activate").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        mockMvc.perform(put("/api/v1/school-years/" + second + "/activate").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        mockMvc.perform(get("/api/v1/school-years/active").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.label").value("2026-2027"));

        // La première est archivée, pas supprimée : ses bulletins et ses factures restent.
        mockMvc.perform(get("/api/v1/school-years").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data[?(@.label=='2025-2026')].status").value("CLOSED"));
    }

    /** Créer un élève l'inscrit sur l'année active : sa classe cesse d'être une donnée sans passé. */
    @Test
    void creatingAStudentRecordsAnEnrollmentOnTheActiveYear() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Inscription");
        String token = adminToken(tenant, "admin-enroll@ecole.example");
        SchoolClass sixieme = schoolClassRepository.save(
                TestAuthSupport.withTenant(new SchoolClass("6ème A", null), tenant.getId()));

        Long yearId = createYear(token, "2026-2027", "2026-10-01", "2027-07-31");
        mockMvc.perform(put("/api/v1/school-years/" + yearId + "/activate").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        String response = mockMvc.perform(post("/api/v1/students")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentNumber\":\"E-1\",\"firstName\":\"Awa\",\"lastName\":\"Traoré\","
                                + "\"schoolClassId\":" + sixieme.getId() + "}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long studentId = objectMapper.readTree(response).get("data").get("id").asLong();

        mockMvc.perform(get("/api/v1/school-years/enrollments")
                        .param("studentId", studentId.toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].schoolYearLabel").value("2026-2027"))
                .andExpect(jsonPath("$.data[0].className").value("6ème A"))
                .andExpect(jsonPath("$.data[0].status").value("ENROLLED"));
    }

    /**
     * La rentrée : une promotion entière réinscrite en une opération, là où il fallait
     * modifier chaque fiche à la main. Un redoublant reprend sa classe, un partant n'est pas
     * réinscrit — et son passé reste consultable.
     */
    @Test
    void promotesAWholeClassIntoTheNextYear() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Rentrée");
        String token = adminToken(tenant, "admin-promo@ecole.example");
        SchoolClass sixieme = schoolClassRepository.save(
                TestAuthSupport.withTenant(new SchoolClass("6ème A", null), tenant.getId()));
        SchoolClass cinquieme = schoolClassRepository.save(
                TestAuthSupport.withTenant(new SchoolClass("5ème A", null), tenant.getId()));

        Long y1 = createYear(token, "2025-2026", "2025-10-01", "2026-07-31");
        Long y2 = createYear(token, "2026-2027", "2026-10-01", "2027-07-31");
        mockMvc.perform(put("/api/v1/school-years/" + y1 + "/activate").header("Authorization", "Bearer " + token));

        Long passe = createStudent(token, "P-1", "Awa", sixieme.getId());
        Long redouble = createStudent(token, "P-2", "Boubacar", sixieme.getId());
        Long part = createStudent(token, "P-3", "Fanta", sixieme.getId());

        mockMvc.perform(post("/api/v1/school-years/" + y2 + "/promotions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourceYearId\":" + y1 + ",\"decisions\":["
                                + "{\"studentId\":" + passe + ",\"outcome\":\"PROMOTED\",\"targetClassId\":" + cinquieme.getId() + "},"
                                + "{\"studentId\":" + redouble + ",\"outcome\":\"REPEATING\"},"
                                + "{\"studentId\":" + part + ",\"outcome\":\"TRANSFERRED\"}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.promotedCount").value(1))
                .andExpect(jsonPath("$.data.repeatingCount").value(1))
                .andExpect(jsonPath("$.data.leftCount").value(1))
                .andExpect(jsonPath("$.data.skipped.length()").value(0));

        // Deux élèves réinscrits sur la nouvelle année, celui qui est parti ne l'est pas.
        mockMvc.perform(get("/api/v1/school-years").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data[?(@.label=='2026-2027')].enrolledCount").value(2));

        // L'élève qui passe garde la trace de sa 6ème ET a sa 5ème.
        mockMvc.perform(get("/api/v1/school-years/enrollments")
                        .param("studentId", passe.toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].className").value("5ème A"))
                .andExpect(jsonPath("$.data[1].className").value("6ème A"))
                .andExpect(jsonPath("$.data[1].status").value("PROMOTED"));

        // Celui qui est parti conserve son année, marquée comme un départ.
        mockMvc.perform(get("/api/v1/school-years/enrollments")
                        .param("studentId", part.toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].status").value("TRANSFERRED"));
    }

    /**
     * Une rentrée relancée — deux onglets, un clic de trop — ne doit pas dupliquer les
     * inscriptions. L'index unique en base le garantit, ce test vérifie que le service ne
     * plante pas pour autant.
     */
    @Test
    void replayingAPromotionDoesNotDuplicateEnrollments() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Rejeu");
        String token = adminToken(tenant, "admin-replay@ecole.example");
        SchoolClass sixieme = schoolClassRepository.save(
                TestAuthSupport.withTenant(new SchoolClass("6ème A", null), tenant.getId()));
        SchoolClass cinquieme = schoolClassRepository.save(
                TestAuthSupport.withTenant(new SchoolClass("5ème A", null), tenant.getId()));

        Long y1 = createYear(token, "2025-2026", "2025-10-01", "2026-07-31");
        Long y2 = createYear(token, "2026-2027", "2026-10-01", "2027-07-31");
        mockMvc.perform(put("/api/v1/school-years/" + y1 + "/activate").header("Authorization", "Bearer " + token));
        Long studentId = createStudent(token, "R-1", "Salif", sixieme.getId());

        String body = "{\"sourceYearId\":" + y1 + ",\"decisions\":[{\"studentId\":" + studentId
                + ",\"outcome\":\"PROMOTED\",\"targetClassId\":" + cinquieme.getId() + "}]}";
        for (int attempt = 0; attempt < 2; attempt++) {
            mockMvc.perform(post("/api/v1/school-years/" + y2 + "/promotions")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk());
        }

        assertThat(enrollmentRepository.findAll().stream()
                .filter(e -> e.getStudentId().equals(studentId))
                .count())
                .isEqualTo(2);
    }

    /** Un élève dont la décision n'a pas de classe d'arrivée est rapporté, pas perdu en silence. */
    @Test
    void reportsStudentsThatCouldNotBeReenrolled() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Oubli");
        String token = adminToken(tenant, "admin-skip@ecole.example");
        SchoolClass sixieme = schoolClassRepository.save(
                TestAuthSupport.withTenant(new SchoolClass("6ème A", null), tenant.getId()));

        Long y1 = createYear(token, "2025-2026", "2025-10-01", "2026-07-31");
        Long y2 = createYear(token, "2026-2027", "2026-10-01", "2027-07-31");
        mockMvc.perform(put("/api/v1/school-years/" + y1 + "/activate").header("Authorization", "Bearer " + token));
        Long studentId = createStudent(token, "S-1", "Moussa", sixieme.getId());

        mockMvc.perform(post("/api/v1/school-years/" + y2 + "/promotions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourceYearId\":" + y1 + ",\"decisions\":[{\"studentId\":" + studentId
                                + ",\"outcome\":\"PROMOTED\"}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.promotedCount").value(0))
                .andExpect(jsonPath("$.data.skipped.length()").value(1));
    }

    /** Une année scolaire engage l'établissement : le secrétariat la lit, la direction la décide. */
    @Test
    void onlyDirectionCanCreateOrActivateASchoolYear() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Droits Années");
        String secretaryToken = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "sec-years@ecole.example", Role.SECRETARY);

        mockMvc.perform(post("/api/v1/school-years")
                        .header("Authorization", "Bearer " + secretaryToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"label\":\"2026-2027\",\"startDate\":\"2026-10-01\",\"endDate\":\"2027-07-31\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/school-years").header("Authorization", "Bearer " + secretaryToken))
                .andExpect(status().isOk());
    }

    private String adminToken(Tenant tenant, String email) throws Exception {
        return TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, email, Role.ADMIN);
    }

    private Long createYear(String token, String label, String from, String to) throws Exception {
        String response = mockMvc.perform(post("/api/v1/school-years")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"label\":\"" + label + "\",\"startDate\":\"" + from + "\",\"endDate\":\"" + to + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("data").get("id").asLong();
    }

    private Long createStudent(String token, String number, String firstName, Long classId) throws Exception {
        String response = mockMvc.perform(post("/api/v1/students")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentNumber\":\"" + number + "\",\"firstName\":\"" + firstName
                                + "\",\"lastName\":\"Test\",\"schoolClassId\":" + classId + "}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("data").get("id").asLong();
    }
}
