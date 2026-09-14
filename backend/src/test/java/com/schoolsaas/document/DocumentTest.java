package com.schoolsaas.document;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

/** Bibliothèque de documents (ROADMAP.md 2.2) : upload, droits de consultation, archivage, isolation. */
class DocumentTest extends AbstractIntegrationTest {

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
    private DocumentRepository documentRepository;

    @Test
    void uploadsListsDownloadsAndArchivesADocument() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Documents");
        String token = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "admin-doc@ecole.example", Role.ADMIN);
        SchoolClass schoolClass = schoolClassRepository.save(TestAuthSupport.withTenant(new SchoolClass("6ème A", null), tenant.getId()));

        MockMultipartFile file = new MockMultipartFile("file", "cours.txt", "text/plain", "Contenu du cours".getBytes(StandardCharsets.UTF_8));

        String uploadResponse = mockMvc.perform(multipart("/api/v1/documents")
                        .file(file)
                        .param("title", "Cours de maths")
                        .param("scope", "CLASS")
                        .param("schoolClassId", schoolClass.getId().toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("Cours de maths"))
                .andExpect(jsonPath("$.data.archived").value(false))
                .andReturn().getResponse().getContentAsString();
        Long documentId = objectMapper.readTree(uploadResponse).get("data").get("id").asLong();

        mockMvc.perform(get("/api/v1/documents").param("schoolClassId", schoolClass.getId().toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));

        mockMvc.perform(get("/api/v1/documents/" + documentId + "/download").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(result -> org.assertj.core.api.Assertions.assertThat(result.getResponse().getContentAsString())
                        .isEqualTo("Contenu du cours"));

        mockMvc.perform(delete("/api/v1/documents/" + documentId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/documents").param("schoolClassId", schoolClass.getId().toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void restrictsVisibilityToTheConfiguredRoles() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Documents Visibilité");
        String adminToken = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "admin-doc-vis@ecole.example", Role.ADMIN);
        String teacherToken = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "teacher-doc-vis@ecole.example", Role.TEACHER);

        MockMultipartFile file = new MockMultipartFile("file", "confidentiel.txt", "text/plain", "Secret".getBytes(StandardCharsets.UTF_8));
        String uploadResponse = mockMvc.perform(multipart("/api/v1/documents")
                        .file(file)
                        .param("title", "Réservé direction")
                        .param("scope", "SERVICE")
                        .param("serviceLabel", "Direction")
                        .param("visibleRoles", "ADMIN")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long documentId = objectMapper.readTree(uploadResponse).get("data").get("id").asLong();

        mockMvc.perform(get("/api/v1/documents/" + documentId).header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("DOCUMENT_NOT_VISIBLE"));

        mockMvc.perform(get("/api/v1/documents").param("serviceLabel", "Direction").header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void aTenantCanNeverSeeDocumentsOfAnotherTenant() throws Exception {
        Tenant tenantA = TestAuthSupport.createActiveTenant(tenantRepository, "École Documents A");
        Tenant tenantB = TestAuthSupport.createActiveTenant(tenantRepository, "École Documents B");
        Document documentB = documentRepository.save(TestAuthSupport.withTenant(
                new Document("Secret B", DocumentScope.SERVICE, null, null, "RH", "f.txt", "text/plain", 5, "unused-key"),
                tenantB.getId()));

        String tokenA = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenantA, "admin-a-doc@ecole.example", Role.ADMIN);

        mockMvc.perform(get("/api/v1/documents/" + documentB.getId()).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());
    }
}
