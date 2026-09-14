package com.schoolsaas.parent;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schoolsaas.AbstractIntegrationTest;
import com.schoolsaas.TestAuthSupport;
import com.schoolsaas.auth.Role;
import com.schoolsaas.auth.UserRepository;
import com.schoolsaas.student.Student;
import com.schoolsaas.student.StudentRepository;
import com.schoolsaas.tenant.Tenant;
import com.schoolsaas.tenant.TenantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

/** CRUD parents/tuteurs + association aux élèves (ROADMAP.md 1.5) + isolation cross-tenant. */
class ParentTest extends AbstractIntegrationTest {

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
    private ParentRepository parentRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Test
    void linksAParentToAStudentAndListsTheAssociation() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Parent");
        String token = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "admin-parent@ecole.example", Role.ADMIN);

        String parentBody = mockMvc.perform(post("/api/v1/parents")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Claire\",\"lastName\":\"Martin\",\"email\":\"claire@ecole.example\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long parentId = objectMapper.readTree(parentBody).get("data").get("id").asLong();

        Student student = studentRepository.save(
                TestAuthSupport.withTenant(new Student("P001", "Léa", "Martin", null, null, null), tenant.getId()));

        mockMvc.perform(post("/api/v1/students/" + student.getId() + "/parents")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"parentId\":" + parentId + ",\"relationship\":\"MERE\",\"primaryContact\":true}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.relationship").value("MERE"));

        mockMvc.perform(get("/api/v1/students/" + student.getId() + "/parents").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));

        mockMvc.perform(get("/api/v1/parents/" + parentId + "/students").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0]").value(student.getId()));
    }

    @Test
    void rejectsLinkingTheSameParentTwiceToTheSameStudent() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Parent Dup");
        String token = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "admin-parent-dup@ecole.example", Role.ADMIN);
        Parent parent = parentRepository.save(TestAuthSupport.withTenant(new Parent("Claire", "Martin", null, null), tenant.getId()));
        Student student = studentRepository.save(
                TestAuthSupport.withTenant(new Student("P002", "Léa", "Martin", null, null, null), tenant.getId()));

        String body = "{\"parentId\":" + parent.getId() + ",\"relationship\":\"MERE\",\"primaryContact\":true}";
        mockMvc.perform(post("/api/v1/students/" + student.getId() + "/parents")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/students/" + student.getId() + "/parents")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void aTenantCanNeverSeeParentsOfAnotherTenant() throws Exception {
        Tenant tenantA = TestAuthSupport.createActiveTenant(tenantRepository, "École Parent A");
        Tenant tenantB = TestAuthSupport.createActiveTenant(tenantRepository, "École Parent B");
        Parent parentB = parentRepository.save(
                TestAuthSupport.withTenant(new Parent("Isolé", "TenantB", null, null), tenantB.getId()));

        String tokenA = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenantA, "admin-a-parent@ecole.example", Role.ADMIN);

        mockMvc.perform(get("/api/v1/parents/" + parentB.getId()).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());
    }
}
