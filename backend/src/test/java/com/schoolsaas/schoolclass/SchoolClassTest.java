package com.schoolsaas.schoolclass;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schoolsaas.AbstractIntegrationTest;
import com.schoolsaas.TestAuthSupport;
import com.schoolsaas.auth.Role;
import com.schoolsaas.auth.UserRepository;
import com.schoolsaas.subject.Subject;
import com.schoolsaas.subject.SubjectRepository;
import com.schoolsaas.teacher.Teacher;
import com.schoolsaas.teacher.TeacherRepository;
import com.schoolsaas.tenant.Tenant;
import com.schoolsaas.tenant.TenantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

/** CRUD classes + affectation enseignant/classe/matière (ROADMAP.md 1.5). */
class SchoolClassTest extends AbstractIntegrationTest {

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
    private TeacherRepository teacherRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Test
    void createsAClassAndAssignsATeacherToASubject() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Classe");
        String token = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "admin-class@ecole.example", Role.ADMIN);

        String classId = mockMvc.perform(post("/api/v1/classes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"6ème A\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long id = objectMapper.readTree(classId).get("data").get("id").asLong();

        Teacher teacher = TestAuthSupport.withTenant(new Teacher("Jean", "Dupont", null, null), tenant.getId());
        teacherRepository.save(teacher);
        Subject subject = TestAuthSupport.withTenant(new Subject("Mathématiques", "MATH", 3), tenant.getId());
        subjectRepository.save(subject);

        mockMvc.perform(post("/api/v1/classes/" + id + "/subjects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subjectId\":" + subject.getId() + ",\"teacherId\":" + teacher.getId() + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.teacherId").value(teacher.getId()));

        mockMvc.perform(get("/api/v1/classes/" + id + "/subjects").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void aTenantCanNeverSeeClassesOfAnotherTenant() throws Exception {
        Tenant tenantA = TestAuthSupport.createActiveTenant(tenantRepository, "École Classe A");
        Tenant tenantB = TestAuthSupport.createActiveTenant(tenantRepository, "École Classe B");
        SchoolClass classB = TestAuthSupport.withTenant(new SchoolClass("Classe Secrète B", null), tenantB.getId());
        schoolClassRepository.save(classB);

        String tokenA = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenantA, "admin-a-class@ecole.example", Role.ADMIN);

        mockMvc.perform(get("/api/v1/classes/" + classB.getId()).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());
    }

}
