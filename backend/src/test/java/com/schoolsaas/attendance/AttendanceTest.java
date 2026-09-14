package com.schoolsaas.attendance;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import com.schoolsaas.student.Student;
import com.schoolsaas.student.StudentRepository;
import com.schoolsaas.tenant.Tenant;
import com.schoolsaas.tenant.TenantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

/** Feuille d'appel, motifs/justificatifs/historique, notification parent (ROADMAP.md 1.7). */
class AttendanceTest extends AbstractIntegrationTest {

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
    private StudentRepository studentRepository;

    @Autowired
    private AttendanceRecordRepository attendanceRecordRepository;

    @Test
    void submitsARollCallAndNotifiesTheParentForAnAbsentStudent() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Appel");
        String token = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "teacher-attendance@ecole.example", Role.TEACHER);
        SchoolClass schoolClass = schoolClassRepository.save(TestAuthSupport.withTenant(new SchoolClass("6ème A", null), tenant.getId()));
        Student present = studentRepository.save(TestAuthSupport.withTenant(new Student("A1", "Léa", "Martin", null, null, null), tenant.getId()));
        Student absent = studentRepository.save(TestAuthSupport.withTenant(new Student("A2", "Tom", "Dupont", null, null, null), tenant.getId()));

        String body = "{\"schoolClassId\":" + schoolClass.getId() + ",\"date\":\"2026-09-15\",\"entries\":["
                + "{\"studentId\":" + present.getId() + ",\"status\":\"PRESENT\",\"justified\":false},"
                + "{\"studentId\":" + absent.getId() + ",\"status\":\"ABSENT\",\"reason\":\"Maladie\",\"justified\":true}"
                + "]}";

        mockMvc.perform(post("/api/v1/attendance/roll-call")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.length()").value(2));

        verify(notificationGateway).send(argThat(event ->
                event.type() == NotificationType.ABSENCE && event.body().contains(absent.getId().toString())));
        verifyNoMoreInteractions(notificationGateway);

        mockMvc.perform(get("/api/v1/attendance")
                        .header("Authorization", "Bearer " + token)
                        .param("schoolClassId", schoolClass.getId().toString())
                        .param("date", "2026-09-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void updatingARecordKeepsAHistoryOfThePreviousState() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Historique");
        String token = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "teacher-history@ecole.example", Role.TEACHER);
        SchoolClass schoolClass = schoolClassRepository.save(TestAuthSupport.withTenant(new SchoolClass("6ème A", null), tenant.getId()));
        Student student = studentRepository.save(TestAuthSupport.withTenant(new Student("H1", "Léa", "Martin", null, null, null), tenant.getId()));

        String rollCallBody = "{\"schoolClassId\":" + schoolClass.getId() + ",\"date\":\"2026-09-16\",\"entries\":["
                + "{\"studentId\":" + student.getId() + ",\"status\":\"LATE\",\"reason\":\"Bus en retard\",\"justified\":false}]}";
        String created = mockMvc.perform(post("/api/v1/attendance/roll-call")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rollCallBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long recordId = objectMapper.readTree(created).get("data").get(0).get("id").asLong();

        mockMvc.perform(put("/api/v1/attendance/" + recordId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ABSENT\",\"reason\":\"En réalité absent\",\"justified\":true,\"comment\":\"Corrigé\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ABSENT"));

        mockMvc.perform(get("/api/v1/attendance/" + recordId + "/history").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].previousStatus").value("LATE"))
                .andExpect(jsonPath("$.data[0].previousReason").value("Bus en retard"));
    }

    @Test
    void aTenantCanNeverSeeAttendanceRecordsOfAnotherTenant() throws Exception {
        Tenant tenantA = TestAuthSupport.createActiveTenant(tenantRepository, "École Absence A");
        Tenant tenantB = TestAuthSupport.createActiveTenant(tenantRepository, "École Absence B");
        SchoolClass classB = schoolClassRepository.save(TestAuthSupport.withTenant(new SchoolClass("Secrète", null), tenantB.getId()));
        Student studentB = studentRepository.save(TestAuthSupport.withTenant(new Student("B1", "Isolé", "TenantB", null, null, null), tenantB.getId()));
        AttendanceRecord recordB = attendanceRecordRepository.save(TestAuthSupport.withTenant(
                new AttendanceRecord(studentB.getId(), classB.getId(), java.time.LocalDate.of(2026, 9, 17), AttendanceStatus.ABSENT, null, false, null),
                tenantB.getId()));

        String tokenA = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenantA, "teacher-a-attendance@ecole.example", Role.TEACHER);

        mockMvc.perform(get("/api/v1/attendance/" + recordB.getId()).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());
    }
}
