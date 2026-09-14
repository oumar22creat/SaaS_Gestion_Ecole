package com.schoolsaas.homework;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schoolsaas.AbstractIntegrationTest;
import com.schoolsaas.TestAuthSupport;
import com.schoolsaas.auth.Role;
import com.schoolsaas.auth.UserRepository;
import com.schoolsaas.notification.NotificationGateway;
import com.schoolsaas.notification.NotificationType;
import com.schoolsaas.schoolclass.SchoolClass;
import com.schoolsaas.schoolclass.SchoolClassRepository;
import com.schoolsaas.subject.Subject;
import com.schoolsaas.subject.SubjectRepository;
import com.schoolsaas.tenant.Tenant;
import com.schoolsaas.tenant.TenantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

/** Cahier de textes et devoirs (ROADMAP.md 2.3) : saisie, notification, isolation. */
class LessonTest extends AbstractIntegrationTest {

    @MockBean
    private NotificationGateway notificationGateway;

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
    private SubjectRepository subjectRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Test
    void createsALessonAndNotifiesOnlyWhenHomeworkIsSet() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Cahier");
        String token = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "teacher-lesson@ecole.example", Role.TEACHER);
        SchoolClass schoolClass = schoolClassRepository.save(TestAuthSupport.withTenant(new SchoolClass("6ème A", null), tenant.getId()));
        Subject subject = subjectRepository.save(TestAuthSupport.withTenant(new Subject("Maths", "MATH-HW", 3), tenant.getId()));

        mockMvc.perform(post("/api/v1/lessons")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"schoolClassId\":" + schoolClass.getId() + ",\"subjectId\":" + subject.getId()
                                + ",\"sessionDate\":\"2026-10-01\",\"content\":\"Chapitre 3\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.homework").doesNotExist());
        verifyNoInteractions(notificationGateway);

        mockMvc.perform(post("/api/v1/lessons")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"schoolClassId\":" + schoolClass.getId() + ",\"subjectId\":" + subject.getId()
                                + ",\"sessionDate\":\"2026-10-02\",\"content\":\"Chapitre 4\","
                                + "\"homework\":\"Exercices p.42\",\"homeworkDueDate\":\"2026-10-08\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.homework").value("Exercices p.42"));
        verify(notificationGateway).send(argThat(event ->
                event.type() == NotificationType.NEW_HOMEWORK && event.body().contains(schoolClass.getId().toString())));
    }

    @Test
    void listsLessonsForAClassWithinADateRange() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Cahier Liste");
        String token = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "teacher-lesson-list@ecole.example", Role.TEACHER);
        SchoolClass schoolClass = schoolClassRepository.save(TestAuthSupport.withTenant(new SchoolClass("6ème A", null), tenant.getId()));
        Subject subject = subjectRepository.save(TestAuthSupport.withTenant(new Subject("Maths", "MATH-HW2", 3), tenant.getId()));
        lessonRepository.save(TestAuthSupport.withTenant(
                new Lesson(schoolClass.getId(), subject.getId(), java.time.LocalDate.of(2026, 10, 1), "Cours 1"), tenant.getId()));
        lessonRepository.save(TestAuthSupport.withTenant(
                new Lesson(schoolClass.getId(), subject.getId(), java.time.LocalDate.of(2026, 11, 15), "Cours hors période"), tenant.getId()));

        mockMvc.perform(get("/api/v1/lessons")
                        .param("schoolClassId", schoolClass.getId().toString())
                        .param("from", "2026-09-01")
                        .param("to", "2026-10-31")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].content").value("Cours 1"));
    }

    @Test
    void aTenantCanNeverSeeLessonsOfAnotherTenant() throws Exception {
        Tenant tenantA = TestAuthSupport.createActiveTenant(tenantRepository, "École Cahier A");
        Tenant tenantB = TestAuthSupport.createActiveTenant(tenantRepository, "École Cahier B");
        SchoolClass classB = schoolClassRepository.save(TestAuthSupport.withTenant(new SchoolClass("Secrète", null), tenantB.getId()));
        Subject subjectB = subjectRepository.save(TestAuthSupport.withTenant(new Subject("Secret", "SEC-HW", 1), tenantB.getId()));
        Lesson lessonB = lessonRepository.save(TestAuthSupport.withTenant(
                new Lesson(classB.getId(), subjectB.getId(), java.time.LocalDate.of(2026, 10, 1), "Secret"), tenantB.getId()));

        String tokenA = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenantA, "teacher-a-lesson@ecole.example", Role.TEACHER);

        mockMvc.perform(get("/api/v1/lessons/" + lessonB.getId()).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());
    }
}
