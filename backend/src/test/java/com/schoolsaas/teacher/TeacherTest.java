package com.schoolsaas.teacher;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schoolsaas.AbstractIntegrationTest;
import com.schoolsaas.TestAuthSupport;
import com.schoolsaas.auth.Role;
import com.schoolsaas.auth.UserRepository;
import com.schoolsaas.tenant.Tenant;
import com.schoolsaas.tenant.TenantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

/** CRUD enseignants (ROADMAP.md 1.5) + isolation cross-tenant (CLAUDE.md). */
class TeacherTest extends AbstractIntegrationTest {

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
    private TeacherRepository teacherRepository;

    @Test
    void adminCanCreateAndReadATeacher() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Prof");
        String token = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "admin-teacher@ecole.example", Role.ADMIN);

        mockMvc.perform(post("/api/v1/teachers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Marie\",\"lastName\":\"Curie\",\"email\":\"marie@ecole.example\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.firstName").value("Marie"))
                .andExpect(jsonPath("$.data.active").value(true));
    }

    @Test
    void teacherRoleCannotManageTeachers() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Prof RBAC");
        String token = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "teacher-rbac@ecole.example", Role.TEACHER);

        mockMvc.perform(get("/api/v1/teachers").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void aTenantCanNeverSeeTeachersOfAnotherTenant() throws Exception {
        Tenant tenantA = TestAuthSupport.createActiveTenant(tenantRepository, "École Prof A");
        Tenant tenantB = TestAuthSupport.createActiveTenant(tenantRepository, "École Prof B");
        Teacher teacherB = TestAuthSupport.withTenant(new Teacher("Isolée", "TenantB", null, null), tenantB.getId());
        teacherRepository.save(teacherB);

        String tokenA = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenantA, "admin-a-teacher@ecole.example", Role.ADMIN);

        mockMvc.perform(get("/api/v1/teachers/" + teacherB.getId()).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/teachers").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(result -> org.assertj.core.api.Assertions.assertThat(result.getResponse().getContentAsString())
                        .doesNotContain("TenantB"));
    }
}
