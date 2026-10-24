package com.schoolsaas.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schoolsaas.AbstractIntegrationTest;
import com.schoolsaas.TestAuthSupport;
import com.schoolsaas.auth.Role;
import com.schoolsaas.auth.User;
import com.schoolsaas.auth.UserRepository;
import com.schoolsaas.parent.Parent;
import com.schoolsaas.parent.ParentRepository;
import com.schoolsaas.parent.StudentParent;
import com.schoolsaas.parent.StudentParentRepository;
import com.schoolsaas.schoolclass.ClassSubjectAssignment;
import com.schoolsaas.schoolclass.ClassSubjectAssignmentRepository;
import com.schoolsaas.schoolclass.SchoolClass;
import com.schoolsaas.schoolclass.SchoolClassRepository;
import com.schoolsaas.student.Student;
import com.schoolsaas.student.StudentRepository;
import com.schoolsaas.subject.Subject;
import com.schoolsaas.subject.SubjectRepository;
import com.schoolsaas.teacher.Teacher;
import com.schoolsaas.teacher.TeacherRepository;
import com.schoolsaas.tenant.Tenant;
import com.schoolsaas.tenant.TenantRepository;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Annuaire de la messagerie — {@code GET /api/v1/conversations/recipients} (ADR-019) :
 * liste par rôle, aucun e-mail/téléphone, familles refusées, comptes inactifs et appelant
 * exclus, isolation entre établissements, recherche et pagination.
 */
class MessagingRecipientsTest extends AbstractIntegrationTest {

    private static final String URL = "/api/v1/conversations/recipients";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private TeacherRepository teacherRepository;
    @Autowired private SchoolClassRepository schoolClassRepository;
    @Autowired private SubjectRepository subjectRepository;
    @Autowired private ClassSubjectAssignmentRepository assignmentRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private ParentRepository parentRepository;
    @Autowired private StudentParentRepository studentParentRepository;

    private Tenant tenant;
    private User admin;
    private User direction;
    private User teacher;
    private User secretary;
    private User vieScolaire;
    private User accountant;
    private User inactiveStaff;
    private User parentMine;
    private User studentMine;
    private User parentOther;
    private User studentOther;
    private User otherTenantStaff;

    @BeforeEach
    void setUp() {
        String k = Long.toString(System.nanoTime(), 36);
        tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Annuaire " + k);
        Long t = tenant.getId();
        admin = user("admin-" + k, Role.ADMIN, "Alice", "Admin");
        direction = user("dir-" + k, Role.DIRECTION, "Denis", "Direction");
        teacher = user("prof-" + k, Role.TEACHER, "Paul", "Prof");
        secretary = user("sec-" + k, Role.SECRETARY, "Sarah", "Secretaire");
        vieScolaire = user("vs-" + k, Role.VIE_SCOLAIRE, "Victor", "Viesco");
        accountant = user("compta-" + k, Role.ACCOUNTANT, "Chloe", "Compta");
        inactiveStaff = user("ancien-" + k, Role.TEACHER, "Igor", "Inactif");
        inactiveStaff.setActive(false);
        userRepository.save(inactiveStaff);

        Teacher fiche = teacherRepository.save(TestAuthSupport.withTenant(
                new Teacher("Paul", "Prof", teacher.getEmail(), null), t));
        SchoolClass mine = schoolClassRepository.save(TestAuthSupport.withTenant(new SchoolClass("6A-" + k, null), t));
        SchoolClass other = schoolClassRepository.save(TestAuthSupport.withTenant(new SchoolClass("6B-" + k, null), t));
        Subject maths = subjectRepository.save(TestAuthSupport.withTenant(new Subject("Maths", "M" + k, 1), t));
        assignmentRepository.save(TestAuthSupport.withTenant(
                new ClassSubjectAssignment(mine.getId(), maths.getId(), fiche.getId()), t));

        parentMine = familyParent(mine.getId(), "pm-" + k, "PM" + k);
        studentMine = familyStudent(mine.getId(), "em-" + k, "EM" + k);
        parentOther = familyParent(other.getId(), "po-" + k, "PO" + k);
        studentOther = familyStudent(other.getId(), "eo-" + k, "EO" + k);

        Tenant otherTenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Annuaire Autre " + k);
        otherTenantStaff = TestAuthSupport.createUser(
                userRepository, passwordEncoder, otherTenant, "ailleurs-" + k + "@ecole.example", Role.TEACHER);
    }

    @Test
    void adminAndDirectionSeeAllActiveAccountsButThemselves() throws Exception {
        Set<Long> expected = ids(direction, teacher, secretary, vieScolaire, accountant,
                parentMine, studentMine, parentOther, studentOther);
        assertThat(recipientIds(admin)).isEqualTo(expected);

        Set<Long> forDirection = new HashSet<>(expected);
        forDirection.remove(direction.getId());
        forDirection.add(admin.getId());
        assertThat(recipientIds(direction)).isEqualTo(forDirection);
    }

    @Test
    void aTeacherSeesStaffAndOnlyTheFamiliesOfHisClasses() throws Exception {
        assertThat(recipientIds(teacher)).isEqualTo(ids(
                admin, direction, secretary, vieScolaire, accountant, parentMine, studentMine));
    }

    @Test
    void aTeacherWithoutAnyClassSeesStaffOnly() throws Exception {
        User newcomer = user("nouveau-" + System.nanoTime(), Role.TEACHER, "Nina", "Nouvelle");
        assertThat(recipientIds(newcomer)).isEqualTo(ids(
                admin, direction, teacher, secretary, vieScolaire, accountant));
    }

    @Test
    void secretaryVieScolaireAndAccountantSeeStaffOnly() throws Exception {
        assertThat(recipientIds(secretary)).isEqualTo(ids(admin, direction, teacher, vieScolaire, accountant));
        assertThat(recipientIds(vieScolaire)).isEqualTo(ids(admin, direction, teacher, secretary, accountant));
        assertThat(recipientIds(accountant)).isEqualTo(ids(admin, direction, teacher, secretary, vieScolaire));
    }

    @Test
    void neverExposesEmailOrPhone() throws Exception {
        String body = mockMvc.perform(get(URL).header("Authorization", "Bearer " + token(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").exists())
                .andExpect(jsonPath("$.data[0].displayName").exists())
                .andExpect(jsonPath("$.data[0].role").exists())
                .andReturn().getResponse().getContentAsString();
        for (JsonNode entry : objectMapper.readTree(body).get("data")) {
            assertThat(entry.has("email")).isFalse();
            assertThat(entry.has("phone")).isFalse();
        }
        assertThat(body).doesNotContain("@ecole.example");
    }

    @Test
    void familiesAreForbidden() throws Exception {
        mockMvc.perform(get(URL).header("Authorization", "Bearer " + token(parentMine)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(URL).header("Authorization", "Bearer " + token(studentMine)))
                .andExpect(status().isForbidden());
    }

    @Test
    void neverListsAccountsOfAnotherTenant() throws Exception {
        assertThat(recipientIds(admin)).doesNotContain(otherTenantStaff.getId());
        String otherToken = TestAuthSupport.login(mockMvc, objectMapper,
                tenantRepository.findById(otherTenantStaff.getSchoolId()).orElseThrow(), otherTenantStaff.getEmail());
        mockMvc.perform(get(URL).header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.total").value(0));
    }

    @Test
    void inactiveAccountsAreExcluded() throws Exception {
        assertThat(recipientIds(admin)).doesNotContain(inactiveStaff.getId());
        assertThat(recipientIds(secretary)).doesNotContain(inactiveStaff.getId());
    }

    @Test
    void searchesByNameAndPaginates() throws Exception {
        mockMvc.perform(get(URL).param("search", "viesco").header("Authorization", "Bearer " + token(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(vieScolaire.getId()))
                .andExpect(jsonPath("$.data[0].displayName").value("Victor Viesco"));
        mockMvc.perform(get(URL).param("search", "SARAH secr").header("Authorization", "Bearer " + token(admin)))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(secretary.getId()));

        mockMvc.perform(get(URL).param("size", "2").param("page", "1").header("Authorization", "Bearer " + token(secretary)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.meta.page").value(2))
                .andExpect(jsonPath("$.meta.pageSize").value(2))
                .andExpect(jsonPath("$.meta.total").value(5));
    }

    // --- outils -------------------------------------------------------------------------

    private Set<Long> recipientIds(User caller) throws Exception {
        String body = mockMvc.perform(get(URL).param("size", "200").header("Authorization", "Bearer " + token(caller)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Set<Long> result = new HashSet<>();
        objectMapper.readTree(body).get("data").forEach(n -> result.add(n.get("id").asLong()));
        return result;
    }

    private String token(User user) throws Exception {
        return TestAuthSupport.login(mockMvc, objectMapper, tenant, user.getEmail());
    }

    private static Set<Long> ids(User... users) {
        Set<Long> result = new HashSet<>();
        for (User u : users) {
            result.add(u.getId());
        }
        return result;
    }

    private User user(String localPart, Role role, String firstName, String lastName) {
        User user = new User(localPart + "@ecole.example", passwordEncoder.encode("Sup3rSecret!"), firstName, lastName, role);
        user.setSchoolId(tenant.getId());
        return userRepository.save(user);
    }

    /** Compte PARENT rattaché à une fiche parent liée à un élève de la classe donnée. */
    private User familyParent(Long classId, String localPart, String studentNumber) {
        Long t = tenant.getId();
        User account = user(localPart, Role.PARENT, "Parent", studentNumber);
        Student child = studentRepository.save(TestAuthSupport.withTenant(
                new Student(studentNumber, "Enfant", studentNumber, null, null, classId), t));
        Parent parent = new Parent("Parent", studentNumber, null, null);
        parent.setUserId(account.getId());
        parent = parentRepository.save(TestAuthSupport.withTenant(parent, t));
        studentParentRepository.save(TestAuthSupport.withTenant(
                new StudentParent(child.getId(), parent.getId(), "MERE", true), t));
        return account;
    }

    /** Compte ÉLÈVE rattaché à une fiche élève de la classe donnée. */
    private User familyStudent(Long classId, String localPart, String studentNumber) {
        User account = user(localPart, Role.STUDENT, "Eleve", studentNumber);
        Student fiche = new Student(studentNumber, "Eleve", studentNumber, null, null, classId);
        fiche.setUserId(account.getId());
        studentRepository.save(TestAuthSupport.withTenant(fiche, tenant.getId()));
        return account;
    }
}
