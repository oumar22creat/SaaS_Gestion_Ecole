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

    // ---------------------------------------------------------------- Rôles (faille corrigée)

    @Test
    void parentsAndStudentsCannotCreateConversationsOrAnnouncements() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Messagerie Familles");
        User admin = TestAuthSupport.createUser(userRepository, passwordEncoder, tenant, "admin-fam-msg@ecole.example", Role.ADMIN);
        String parentToken = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "parent-msg@ecole.example", Role.PARENT);
        String studentToken = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "eleve-msg@ecole.example", Role.STUDENT);
        long before = conversationRepository.count();

        for (String token : new String[] {parentToken, studentToken}) {
            mockMvc.perform(post("/api/v1/conversations")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"Question\",\"announcement\":false,\"participantUserIds\":[" + admin.getId() + "]}"))
                    .andExpect(status().isForbidden());
            mockMvc.perform(post("/api/v1/conversations")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"Annonce pirate\",\"announcement\":true}"))
                    .andExpect(status().isForbidden());
        }
        org.assertj.core.api.Assertions.assertThat(conversationRepository.count()).isEqualTo(before);
    }

    @Test
    void aTeacherCanOpenAConversationButCannotWriteInAnAnnouncement() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Messagerie Prof");
        User secretary = TestAuthSupport.createUser(userRepository, passwordEncoder, tenant, "secr-msg@ecole.example", Role.SECRETARY);
        String teacherToken = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "prof-msg-rbac@ecole.example", Role.TEACHER);
        String directionToken = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "dir-msg-rbac@ecole.example", Role.DIRECTION);

        mockMvc.perform(post("/api/v1/conversations")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Salle 12\",\"announcement\":false,\"participantUserIds\":[" + secretary.getId() + "]}"))
                .andExpect(status().isCreated());

        Long announcementId = createAnnouncement(directionToken);
        // Le professeur est destinataire de l'annonce (utilisateur actif), mais y répondre
        // reviendrait à écrire à tout l'établissement.
        mockMvc.perform(post("/api/v1/conversations/" + announcementId + "/messages")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Répondre à tous\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ANNOUNCEMENT_NOT_ALLOWED"));
        mockMvc.perform(post("/api/v1/conversations/" + announcementId + "/messages")
                        .header("Authorization", "Bearer " + directionToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Rentrée le 1er octobre\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void parentsCanReadTheAnnouncementsTheyReceiveButNotReply() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Annonce Familles");
        String directionToken = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "dir-annonce-fam@ecole.example", Role.DIRECTION);
        String parentToken = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "parent-annonce@ecole.example", Role.PARENT);

        Long announcementId = createAnnouncement(directionToken);
        mockMvc.perform(post("/api/v1/conversations/" + announcementId + "/messages")
                        .header("Authorization", "Bearer " + directionToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Réunion des parents jeudi\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/conversations").header("Authorization", "Bearer " + parentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].announcement").value(true));
        mockMvc.perform(get("/api/v1/conversations/" + announcementId + "/messages").header("Authorization", "Bearer " + parentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
        mockMvc.perform(post("/api/v1/conversations/" + announcementId + "/messages")
                        .header("Authorization", "Bearer " + parentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Réponse à tous\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void anAnnouncementNeverReachesAnotherTenant() throws Exception {
        Tenant tenantA = TestAuthSupport.createActiveTenant(tenantRepository, "École Annonce Iso A");
        Tenant tenantB = TestAuthSupport.createActiveTenant(tenantRepository, "École Annonce Iso B");
        String directionA = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenantA, "dir-iso-a@ecole.example", Role.DIRECTION);
        String parentB = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenantB, "parent-iso-b@ecole.example", Role.PARENT);
        String directionB = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenantB, "dir-iso-b@ecole.example", Role.DIRECTION);

        Long announcementId = createAnnouncement(directionA);

        mockMvc.perform(get("/api/v1/conversations/" + announcementId).header("Authorization", "Bearer " + parentB))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/conversations").header("Authorization", "Bearer " + parentB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
        mockMvc.perform(post("/api/v1/conversations/" + announcementId + "/messages")
                        .header("Authorization", "Bearer " + directionB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Intrusion\"}"))
                .andExpect(status().isNotFound());
    }

    /** Annonce créée par la direction ; renvoie son identifiant. */
    private Long createAnnouncement(String directionToken) throws Exception {
        String body = mockMvc.perform(post("/api/v1/conversations")
                        .header("Authorization", "Bearer " + directionToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Annonce\",\"announcement\":true}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("data").get("id").asLong();
    }
}
