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

    @Autowired private com.schoolsaas.teacher.TeacherRepository teacherRepository;
    @Autowired private com.schoolsaas.schoolclass.SchoolClassRepository schoolClassRepository;
    @Autowired private com.schoolsaas.subject.SubjectRepository subjectRepository;
    @Autowired private com.schoolsaas.schoolclass.ClassSubjectAssignmentRepository assignmentRepository;
    @Autowired private com.schoolsaas.student.StudentRepository studentRepository;
    @Autowired private com.schoolsaas.parent.ParentRepository parentRepository;
    @Autowired private com.schoolsaas.parent.StudentParentRepository studentParentRepository;

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

    // ---------------------------------------------------------------- Destinataires (annonce déguisée)

    @Test
    void recipientsAreDeduplicatedCappedAndMustBeActiveExistingAccounts() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Destinataires");
        String secretaryToken = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "secr-dest@ecole.example", Role.SECRETARY);
        User colleague = TestAuthSupport.createUser(userRepository, passwordEncoder, tenant, "coll-dest@ecole.example", Role.TEACHER);
        User former = TestAuthSupport.createUser(userRepository, passwordEncoder, tenant, "former-dest@ecole.example", Role.TEACHER);
        former.setActive(false);
        userRepository.save(former);

        // Doublons : plus d'erreur 500 sur la contrainte UNIQUE, un seul participant par compte.
        mockMvc.perform(post("/api/v1/conversations")
                        .header("Authorization", "Bearer " + secretaryToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(conversation(colleague.getId() + "," + colleague.getId() + "," + colleague.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.participantUserIds.length()").value(2));

        StringBuilder many = new StringBuilder();
        for (int i = 0; i <= MessagingService.MAX_RECIPIENTS; i++) {
            User u = TestAuthSupport.createUser(userRepository, passwordEncoder, tenant, "masse-" + i + "@ecole.example", Role.PARENT);
            many.append(i == 0 ? "" : ",").append(u.getId());
        }
        long before = conversationRepository.count();
        mockMvc.perform(post("/api/v1/conversations")
                        .header("Authorization", "Bearer " + secretaryToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(conversation(many.toString())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("TOO_MANY_RECIPIENTS"));
        mockMvc.perform(post("/api/v1/conversations")
                        .header("Authorization", "Bearer " + secretaryToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(conversation(String.valueOf(former.getId()))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("USER_INACTIVE"));
        mockMvc.perform(post("/api/v1/conversations")
                        .header("Authorization", "Bearer " + secretaryToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(conversation(colleague.getId() + ",99999999")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("USER_NOT_FOUND"));
        org.assertj.core.api.Assertions.assertThat(conversationRepository.count()).isEqualTo(before);
    }

    @Test
    void aTeacherOnlyWritesToFamiliesOfHisClasses() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Familles Prof");
        Long t = tenant.getId();
        com.schoolsaas.teacher.Teacher fiche = teacherRepository.save(TestAuthSupport.withTenant(
                new com.schoolsaas.teacher.Teacher("Ina", "Prof", "prof-fam@ecole.example", null), t));
        com.schoolsaas.schoolclass.SchoolClass mine = schoolClassRepository.save(
                TestAuthSupport.withTenant(new com.schoolsaas.schoolclass.SchoolClass("5ème A", null), t));
        com.schoolsaas.schoolclass.SchoolClass other = schoolClassRepository.save(
                TestAuthSupport.withTenant(new com.schoolsaas.schoolclass.SchoolClass("5ème B", null), t));
        com.schoolsaas.subject.Subject maths = subjectRepository.save(
                TestAuthSupport.withTenant(new com.schoolsaas.subject.Subject("Maths", "MFAM", 1), t));
        assignmentRepository.save(TestAuthSupport.withTenant(
                new com.schoolsaas.schoolclass.ClassSubjectAssignment(mine.getId(), maths.getId(), fiche.getId()), t));

        User parentMine = familyParent(tenant, mine.getId(), "parent-mine@ecole.example", "FAM1");
        User parentOther = familyParent(tenant, other.getId(), "parent-other@ecole.example", "FAM2");
        User studentOther = TestAuthSupport.createUser(userRepository, passwordEncoder, tenant, "eleve-other@ecole.example", Role.STUDENT);
        com.schoolsaas.student.Student fiche2 = new com.schoolsaas.student.Student("FAM3", "Eli", "Autre", null, null, other.getId());
        fiche2.setUserId(studentOther.getId());
        studentRepository.save(TestAuthSupport.withTenant(fiche2, t));
        User colleague = TestAuthSupport.createUser(userRepository, passwordEncoder, tenant, "coll-fam@ecole.example", Role.VIE_SCOLAIRE);

        String teacherToken = TestAuthSupport.login(mockMvc, objectMapper, tenant,
                TestAuthSupport.createUser(userRepository, passwordEncoder, tenant, "prof-fam@ecole.example", Role.TEACHER).getEmail());
        String directionToken = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "dir-fam@ecole.example", Role.DIRECTION);

        mockMvc.perform(post("/api/v1/conversations").header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON).content(conversation(parentMine.getId() + "," + colleague.getId())))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/conversations").header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON).content(conversation(parentMine.getId() + "," + parentOther.getId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value(com.schoolsaas.teacher.TeacherScope.DENIED_CODE));
        mockMvc.perform(post("/api/v1/conversations").header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON).content(conversation(String.valueOf(studentOther.getId()))))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/conversations").header("Authorization", "Bearer " + directionToken)
                        .contentType(MediaType.APPLICATION_JSON).content(conversation(parentOther.getId() + "," + studentOther.getId())))
                .andExpect(status().isCreated());
    }

    @Test
    void recipientsOfAnotherTenantAreNotFound() throws Exception {
        Tenant tenantA = TestAuthSupport.createActiveTenant(tenantRepository, "École Dest Iso A");
        Tenant tenantB = TestAuthSupport.createActiveTenant(tenantRepository, "École Dest Iso B");
        User userB = TestAuthSupport.createUser(userRepository, passwordEncoder, tenantB, "cible-b@ecole.example", Role.PARENT);
        String directionA = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenantA, "dir-dest-a@ecole.example", Role.DIRECTION);

        mockMvc.perform(post("/api/v1/conversations").header("Authorization", "Bearer " + directionA)
                        .contentType(MediaType.APPLICATION_JSON).content(conversation(String.valueOf(userB.getId()))))
                .andExpect(status().isNotFound());
    }

    /** Compte PARENT rattaché à une fiche parent, elle-même liée à un élève de la classe donnée. */
    private User familyParent(Tenant tenant, Long classId, String email, String studentNumber) {
        Long t = tenant.getId();
        User account = TestAuthSupport.createUser(userRepository, passwordEncoder, tenant, email, Role.PARENT);
        com.schoolsaas.student.Student child = studentRepository.save(TestAuthSupport.withTenant(
                new com.schoolsaas.student.Student(studentNumber, "Enfant", studentNumber, null, null, classId), t));
        com.schoolsaas.parent.Parent parent = new com.schoolsaas.parent.Parent("Parent", studentNumber, null, null);
        parent.setUserId(account.getId());
        parent = parentRepository.save(TestAuthSupport.withTenant(parent, t));
        studentParentRepository.save(TestAuthSupport.withTenant(
                new com.schoolsaas.parent.StudentParent(child.getId(), parent.getId(), "MERE", true), t));
        return account;
    }

    private static String conversation(String participantIds) {
        return "{\"title\":\"Sujet\",\"announcement\":false,\"participantUserIds\":[" + participantIds + "]}";
    }
}
