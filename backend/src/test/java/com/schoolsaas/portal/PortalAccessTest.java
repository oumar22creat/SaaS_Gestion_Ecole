package com.schoolsaas.portal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schoolsaas.AbstractIntegrationTest;
import com.schoolsaas.auth.Role;
import com.schoolsaas.auth.User;
import com.schoolsaas.auth.UserRepository;
import com.schoolsaas.auth.dto.TenantLoginRequest;
import com.schoolsaas.parent.Parent;
import com.schoolsaas.parent.ParentRepository;
import com.schoolsaas.parent.StudentParent;
import com.schoolsaas.parent.StudentParentRepository;
import com.schoolsaas.student.Student;
import com.schoolsaas.student.StudentRepository;
import com.schoolsaas.tenant.Tenant;
import com.schoolsaas.tenant.TenantRepository;
import com.schoolsaas.tenant.TenantResolver;
import com.schoolsaas.tenant.TenantStatus;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Portail parent/élève. Le risque propre à ce portail n'est pas le cloisonnement entre
 * établissements (déjà assuré par RLS) mais le cloisonnement <b>entre familles d'un même
 * établissement</b> : deux parents partagent le même tenant, donc seul le contrôle applicatif
 * empêche l'un de lire le dossier de l'enfant de l'autre.
 */
class PortalAccessTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private ParentRepository parentRepository;
    @Autowired private StudentParentRepository studentParentRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private ObjectMapper objectMapper;

    private Tenant tenant;

    private Tenant tenant() {
        if (tenant == null) {
            tenant = tenantRepository.save(
                    new Tenant("École Test", "ecole-" + UUID.randomUUID(), TenantStatus.ACTIVE));
        }
        return tenant;
    }

    private User account(String email, Role role, String firstName) {
        User user = new User(email, passwordEncoder.encode("Secret123!"), firstName, "Famille", role);
        user.setSchoolId(tenant().getId());
        return userRepository.save(user);
    }

    private Student student(String firstName, User account) {
        Student student = new Student(
                "M-" + UUID.randomUUID().toString().substring(0, 8),
                firstName,
                "Famille",
                java.time.LocalDate.of(2012, 4, 3),
                "F",
                null);
        student.setSchoolId(tenant().getId());
        if (account != null) {
            student.setUserId(account.getId());
        }
        return studentRepository.save(student);
    }

    private Parent parentOf(Student child, User account, String firstName) {
        Parent parent = new Parent(firstName, "Famille", firstName.toLowerCase() + "@famille.example", "70000000");
        parent.setSchoolId(tenant().getId());
        parent.setUserId(account.getId());
        parent = parentRepository.save(parent);

        StudentParent link = new StudentParent(child.getId(), parent.getId(), "MERE", true);
        link.setSchoolId(tenant().getId());
        studentParentRepository.save(link);
        return parent;
    }

    private String loginAs(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .header(TenantResolver.TENANT_HEADER, tenant().getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TenantLoginRequest(tenant().getSubdomain(), email, "Secret123!"))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("data").get("accessToken").asText();
    }

    @Test
    void parentSeesOnlyTheirOwnChildren() throws Exception {
        User parentAccount = account("maman-a@ecole.example", Role.PARENT, "Awa");
        Student myChild = student("Fatou", null);
        parentOf(myChild, parentAccount, "Awa");

        User otherParentAccount = account("maman-b@ecole.example", Role.PARENT, "Mariam");
        Student otherChild = student("Sekou", null);
        parentOf(otherChild, otherParentAccount, "Mariam");

        String token = loginAs("maman-a@ecole.example");
        mockMvc.perform(get("/api/v1/portal/children")
                        .header("Authorization", "Bearer " + token)
                        .header(TenantResolver.TENANT_HEADER, tenant().getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].firstName").value("Fatou"));

        // Le cœur du portail : deviner l'identifiant de l'enfant d'une autre famille ne suffit
        // pas. La réponse est "introuvable", pour ne même pas confirmer que cet élève existe.
        mockMvc.perform(get("/api/v1/portal/students/" + otherChild.getId() + "/grades")
                        .header("Authorization", "Bearer " + token)
                        .header(TenantResolver.TENANT_HEADER, tenant().getId()))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/portal/students/" + otherChild.getId() + "/attendance")
                        .param("from", "2026-09-01")
                        .param("to", "2026-09-30")
                        .header("Authorization", "Bearer " + token)
                        .header(TenantResolver.TENANT_HEADER, tenant().getId()))
                .andExpect(status().isNotFound());

        // L'emploi du temps est soumis au même contrôle : il révèle la classe de l'enfant.
        mockMvc.perform(get("/api/v1/portal/students/" + otherChild.getId() + "/timetable")
                        .header("Authorization", "Bearer " + token)
                        .header(TenantResolver.TENANT_HEADER, tenant().getId()))
                .andExpect(status().isNotFound());
    }

    /** Un élève sans classe affectée obtient une liste vide, pas une erreur. */
    @Test
    void timetableIsEmptyWhenTheStudentHasNoClass() throws Exception {
        User studentAccount = account("sans-classe@ecole.example", Role.STUDENT, "Ibrahim");
        Student me = student("Ibrahim", studentAccount);

        String token = loginAs("sans-classe@ecole.example");
        mockMvc.perform(get("/api/v1/portal/students/" + me.getId() + "/timetable")
                        .header("Authorization", "Bearer " + token)
                        .header(TenantResolver.TENANT_HEADER, tenant().getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void studentSeesOnlyTheirOwnRecord() throws Exception {
        User studentAccount = account("eleve@ecole.example", Role.STUDENT, "Oumar");
        Student me = student("Oumar", studentAccount);
        Student classmate = student("Aminata", null);

        String token = loginAs("eleve@ecole.example");
        mockMvc.perform(get("/api/v1/portal/children")
                        .header("Authorization", "Bearer " + token)
                        .header(TenantResolver.TENANT_HEADER, tenant().getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(me.getId()));

        mockMvc.perform(get("/api/v1/portal/students/" + classmate.getId() + "/grades")
                        .header("Authorization", "Bearer " + token)
                        .header(TenantResolver.TENANT_HEADER, tenant().getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void staffCannotUseTheFamilyPortal() throws Exception {
        account("prof@ecole.example", Role.TEACHER, "Modibo");
        String token = loginAs("prof@ecole.example");

        mockMvc.perform(get("/api/v1/portal/children")
                        .header("Authorization", "Bearer " + token)
                        .header(TenantResolver.TENANT_HEADER, tenant().getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    void openingAccessTwiceIsRejected() throws Exception {
        account("admin@ecole.example", Role.ADMIN, "Direction");
        Student child = student("Kadia", null);
        String adminToken = loginAs("admin@ecole.example");

        mockMvc.perform(post("/api/v1/students/" + child.getId() + "/account")
                        .header("Authorization", "Bearer " + adminToken)
                        .header(TenantResolver.TENANT_HEADER, tenant().getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new FamilyAccountController.OpenAccessRequest(
                                        "kadia@ecole.example", "Secret123!"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.role").value("STUDENT"));

        mockMvc.perform(post("/api/v1/students/" + child.getId() + "/account")
                        .header("Authorization", "Bearer " + adminToken)
                        .header(TenantResolver.TENANT_HEADER, tenant().getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new FamilyAccountController.OpenAccessRequest(
                                        "autre@ecole.example", "Secret123!"))))
                .andExpect(status().isConflict());
    }
}
