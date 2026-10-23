package com.schoolsaas.teacher;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schoolsaas.AbstractIntegrationTest;
import com.schoolsaas.TestAuthSupport;
import com.schoolsaas.auth.Role;
import com.schoolsaas.auth.UserRepository;
import com.schoolsaas.schoolclass.ClassSubjectAssignment;
import com.schoolsaas.schoolclass.ClassSubjectAssignmentRepository;
import com.schoolsaas.schoolclass.SchoolClass;
import com.schoolsaas.schoolclass.SchoolClassRepository;
import com.schoolsaas.subject.Subject;
import com.schoolsaas.subject.SubjectRepository;
import com.schoolsaas.tenant.Tenant;
import com.schoolsaas.tenant.TenantRepository;
import com.schoolsaas.timetable.TimetableEntry;
import com.schoolsaas.timetable.TimetableEntryRepository;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * {@code GET /api/v1/me/teaching-scope} (ADR-030) : l'enseignant connecté lit SON périmètre,
 * et seulement le sien ; les autres rôles du personnel reçoivent « non restreint » ; les
 * comptes famille sont refusés ; un établissement voisin n'y entre jamais.
 *
 * <p>Fixture : classes C1 et C2, matières S1 et S2.
 * « in » enseigne (C1, S1) par affectation ; « out » a un créneau (C2, S2) ; « head » est
 * professeur principal de C1 sans y enseigner.
 */
class TeachingScopeControllerTest extends AbstractIntegrationTest {

    private static final String URL = "/api/v1/me/teaching-scope";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private TeacherRepository teacherRepository;
    @Autowired private SchoolClassRepository schoolClassRepository;
    @Autowired private SubjectRepository subjectRepository;
    @Autowired private ClassSubjectAssignmentRepository assignmentRepository;
    @Autowired private TimetableEntryRepository timetableEntryRepository;

    private String suffix;
    private Tenant tenant;
    private String inEmail;
    private SchoolClass c1;
    private SchoolClass c2;
    private Subject s1;
    private Subject s2;
    private String tokenIn;
    private String tokenOut;
    private String tokenHead;

    @BeforeEach
    void setUp() throws Exception {
        suffix = UUID.randomUUID().toString().substring(0, 8);
        tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Scope " + suffix);
        Long t = tenant.getId();

        inEmail = "scope-in-" + suffix + "@ecole.example";
        String outEmail = "scope-out-" + suffix + "@ecole.example";
        String headEmail = "scope-head-" + suffix + "@ecole.example";
        Teacher in = teacherRepository.save(TestAuthSupport.withTenant(new Teacher("Ina", "Dedans", inEmail, null), t));
        Teacher out = teacherRepository.save(TestAuthSupport.withTenant(new Teacher("Otto", "Dehors", outEmail, null), t));
        Teacher head = teacherRepository.save(TestAuthSupport.withTenant(new Teacher("Hélène", "Principale", headEmail, null), t));

        c1 = schoolClassRepository.save(TestAuthSupport.withTenant(new SchoolClass("5e A " + suffix, head.getId()), t));
        c2 = schoolClassRepository.save(TestAuthSupport.withTenant(new SchoolClass("5e B " + suffix, null), t));
        s1 = subjectRepository.save(TestAuthSupport.withTenant(new Subject("Maths", "M" + suffix, 2), t));
        s2 = subjectRepository.save(TestAuthSupport.withTenant(new Subject("Français", "F" + suffix, 2), t));

        assignmentRepository.save(TestAuthSupport.withTenant(new ClassSubjectAssignment(c1.getId(), s1.getId(), in.getId()), t));
        timetableEntryRepository.save(TestAuthSupport.withTenant(new TimetableEntry(
                c2.getId(), s2.getId(), out.getId(), null, DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(9, 0)), t));

        tokenIn = TestAuthSupport.createUserAndLogin(mockMvc, objectMapper, userRepository, passwordEncoder, tenant, inEmail, Role.TEACHER);
        tokenOut = TestAuthSupport.createUserAndLogin(mockMvc, objectMapper, userRepository, passwordEncoder, tenant, outEmail, Role.TEACHER);
        tokenHead = TestAuthSupport.createUserAndLogin(mockMvc, objectMapper, userRepository, passwordEncoder, tenant, headEmail, Role.TEACHER);
    }

    @Test
    void aTeacherReadsHisOwnClassSubjectPairs() throws Exception {
        perform(tokenIn)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.restricted").value(true))
                .andExpect(jsonPath("$.data.classIds.length()").value(1))
                .andExpect(jsonPath("$.data.classIds[0]").value(c1.getId()))
                .andExpect(jsonPath("$.data.headClassIds.length()").value(0))
                .andExpect(jsonPath("$.data.classSubjects.length()").value(1))
                .andExpect(jsonPath("$.data.classSubjects[0].classId").value(c1.getId()))
                .andExpect(jsonPath("$.data.classSubjects[0].subjectId").value(s1.getId()));

        // Un créneau d'emploi du temps vaut affectation.
        perform(tokenOut)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.classSubjects.length()").value(1))
                .andExpect(jsonPath("$.data.classSubjects[0].classId").value(c2.getId()))
                .andExpect(jsonPath("$.data.classSubjects[0].subjectId").value(s2.getId()));
    }

    @Test
    void theHeadTeacherSeesHisClassWithoutItsSubjects() throws Exception {
        perform(tokenHead)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.restricted").value(true))
                .andExpect(jsonPath("$.data.headClassIds.length()").value(1))
                .andExpect(jsonPath("$.data.headClassIds[0]").value(c1.getId()))
                .andExpect(jsonPath("$.data.classIds[0]").value(c1.getId()))
                .andExpect(jsonPath("$.data.classSubjects.length()").value(0));
    }

    @Test
    void theResponseNeverDisclosesAnotherTeacher() throws Exception {
        String body = perform(tokenIn).andReturn().getResponse().getContentAsString();
        org.assertj.core.api.Assertions.assertThat(body)
                .doesNotContain("Otto").doesNotContain("Dehors").doesNotContain("@ecole.example")
                .doesNotContain("teacherId");
    }

    @Test
    void otherStaffRolesAreUnrestrictedAndFamiliesAreRefused() throws Exception {
        for (Role role : new Role[] {Role.ADMIN, Role.DIRECTION, Role.SECRETARY, Role.VIE_SCOLAIRE, Role.ACCOUNTANT}) {
            String token = TestAuthSupport.createUserAndLogin(mockMvc, objectMapper, userRepository, passwordEncoder,
                    tenant, role.name().toLowerCase() + "-" + suffix + "@ecole.example", role);
            perform(token)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.restricted").value(false))
                    .andExpect(jsonPath("$.data.classSubjects.length()").value(0));
        }
        for (Role role : new Role[] {Role.PARENT, Role.STUDENT}) {
            String token = TestAuthSupport.createUserAndLogin(mockMvc, objectMapper, userRepository, passwordEncoder,
                    tenant, role.name().toLowerCase() + "-" + suffix + "@ecole.example", role);
            perform(token).andExpect(status().isForbidden());
        }
        mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
    }

    @Test
    void aNeighbouringSchoolNeverLeaksIntoTheScope() throws Exception {
        // Établissement B : une fiche à la MÊME adresse que « in », affectée à une classe de B.
        Tenant other = TestAuthSupport.createActiveTenant(tenantRepository, "École Voisine " + suffix);
        Long b = other.getId();
        Teacher twin = teacherRepository.save(TestAuthSupport.withTenant(new Teacher("Ina", "Jumelle", inEmail, null), b));
        SchoolClass classB = schoolClassRepository.save(TestAuthSupport.withTenant(new SchoolClass("CM2 " + suffix, twin.getId()), b));
        Subject subjectB = subjectRepository.save(TestAuthSupport.withTenant(new Subject("SVT", "S" + suffix, 1), b));
        assignmentRepository.save(TestAuthSupport.withTenant(new ClassSubjectAssignment(classB.getId(), subjectB.getId(), twin.getId()), b));

        // Le compte de A ne voit que A (pas d'union avec la fiche homonyme de B)…
        perform(tokenIn)
                .andExpect(jsonPath("$.data.classIds.length()").value(1))
                .andExpect(jsonPath("$.data.classIds[0]").value(c1.getId()))
                .andExpect(jsonPath("$.data.headClassIds.length()").value(0))
                .andExpect(jsonPath("$.data.classSubjects.length()").value(1));

        // …et le compte homonyme de B ne voit que B.
        String tokenB = TestAuthSupport.createUserAndLogin(mockMvc, objectMapper, userRepository, passwordEncoder, other, inEmail, Role.TEACHER);
        perform(tokenB)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.classIds.length()").value(1))
                .andExpect(jsonPath("$.data.classIds[0]").value(classB.getId()))
                .andExpect(jsonPath("$.data.headClassIds[0]").value(classB.getId()))
                .andExpect(jsonPath("$.data.classSubjects.length()").value(1))
                .andExpect(jsonPath("$.data.classSubjects[0].subjectId").value(subjectB.getId()));
    }

    private ResultActions perform(String token) throws Exception {
        return mockMvc.perform(get(URL).header("Authorization", "Bearer " + token));
    }
}
