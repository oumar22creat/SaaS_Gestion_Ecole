package com.schoolsaas.messaging;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schoolsaas.AbstractIntegrationTest;
import com.schoolsaas.TestAuthSupport;
import com.schoolsaas.auth.Role;
import com.schoolsaas.auth.User;
import com.schoolsaas.auth.UserRepository;
import com.schoolsaas.tenant.Tenant;
import com.schoolsaas.tenant.TenantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

/** Messagerie interne (ROADMAP.md 2.4) : conversations, messages, confirmation de lecture, annonces, isolation. */
class MessagingTest extends AbstractIntegrationTest {

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
    private ConversationRepository conversationRepository;

    @Test
    void sendsAMessageAndTracksReadConfirmation() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Messagerie");
        User teacher = TestAuthSupport.createUser(userRepository, passwordEncoder, tenant, "teacher-msg@ecole.example", Role.TEACHER);
        String adminToken = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "admin-msg@ecole.example", Role.ADMIN);
        String teacherToken = TestAuthSupport.login(mockMvc, objectMapper, tenant, "teacher-msg@ecole.example");

        String createResponse = mockMvc.perform(post("/api/v1/conversations")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Réunion\",\"announcement\":false,\"participantUserIds\":[" + teacher.getId() + "]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.participantUserIds.length()").value(2))
                .andReturn().getResponse().getContentAsString();
        Long conversationId = objectMapper.readTree(createResponse).get("data").get("id").asLong();

        mockMvc.perform(post("/api/v1/conversations/" + conversationId + "/messages")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Bonjour à tous\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/conversations/" + conversationId).header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(1));

        mockMvc.perform(post("/api/v1/conversations/" + conversationId + "/read").header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/conversations/" + conversationId).header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(0));

        mockMvc.perform(get("/api/v1/conversations/" + conversationId + "/messages").param("search", "Bonjour")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void rejectsCreatingAConversationWithoutAnyParticipant() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Messagerie Vide");
        String adminToken = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "admin-msg-empty@ecole.example", Role.ADMIN);

        mockMvc.perform(post("/api/v1/conversations")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Privé\",\"announcement\":false,\"participantUserIds\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("PARTICIPANTS_REQUIRED"));
    }

    @Test
    void nonParticipantsCannotAccessAConversation() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Messagerie Accès");
        User teacher = TestAuthSupport.createUser(userRepository, passwordEncoder, tenant, "teacher-msg-access@ecole.example", Role.TEACHER);
        String adminToken = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "admin-msg-access@ecole.example", Role.ADMIN);
        String outsiderToken = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "outsider-msg@ecole.example", Role.TEACHER);

        String createResponse = mockMvc.perform(post("/api/v1/conversations")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Privé\",\"announcement\":false,\"participantUserIds\":[" + teacher.getId() + "]}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long conversationId = objectMapper.readTree(createResponse).get("data").get("id").asLong();

        mockMvc.perform(get("/api/v1/conversations/" + conversationId).header("Authorization", "Bearer " + outsiderToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("NOT_A_PARTICIPANT"));
    }

    @Test
    void teacherCannotCreateAnAnnouncement() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Annonce RBAC");
        String teacherToken = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "teacher-announce@ecole.example", Role.TEACHER);

        mockMvc.perform(post("/api/v1/conversations")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Annonce\",\"announcement\":true}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ANNOUNCEMENT_NOT_ALLOWED"));
    }

    @Test
    void anAnnouncementReachesEveryActiveUser() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Annonce");
        String adminToken = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "admin-announce@ecole.example", Role.ADMIN);
        TestAuthSupport.createUser(userRepository, passwordEncoder, tenant, "teacher-announce2@ecole.example", Role.TEACHER);

        mockMvc.perform(post("/api/v1/conversations")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Rentrée\",\"announcement\":true}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.participantUserIds.length()").value(2));
    }

    @Test
    void aTenantCanNeverSeeConversationsOfAnotherTenant() throws Exception {
        Tenant tenantA = TestAuthSupport.createActiveTenant(tenantRepository, "École Messagerie A");
        Tenant tenantB = TestAuthSupport.createActiveTenant(tenantRepository, "École Messagerie B");
        User adminB = TestAuthSupport.createUser(userRepository, passwordEncoder, tenantB, "admin-b-msg@ecole.example", Role.ADMIN);
        Conversation conversationB = conversationRepository.save(
                TestAuthSupport.withTenant(new Conversation("Secret", false, adminB.getId()), tenantB.getId()));

        String tokenA = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenantA, "admin-a-msg@ecole.example", Role.ADMIN);

        mockMvc.perform(get("/api/v1/conversations/" + conversationB.getId()).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());
    }
}
