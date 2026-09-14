package com.schoolsaas.discipline;

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
import com.schoolsaas.schoolclass.SchoolClass;
import com.schoolsaas.schoolclass.SchoolClassRepository;
import com.schoolsaas.student.Student;
import com.schoolsaas.student.StudentRepository;
import com.schoolsaas.tenant.Tenant;
import com.schoolsaas.tenant.TenantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

/** Vie scolaire (ROADMAP.md 3.1) : incidents, sanctions, convocations, observations, historique, statistiques, RBAC, isolation. */
class DisciplineTest extends AbstractIntegrationTest {

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
    private IncidentRepository incidentRepository;

    @Test
    void reportsAnIncidentDecidesASanctionAndFeedsHistoryAndStatistics() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Vie Scolaire");
        SchoolClass schoolClass = schoolClassRepository.save(TestAuthSupport.withTenant(new SchoolClass("6ème A", null), tenant.getId()));
        Student student = studentRepository.save(TestAuthSupport.withTenant(new Student("D1", "Tom", "Dupont", null, null, null), tenant.getId()));
        String teacherToken = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "teacher-discipline@ecole.example", Role.TEACHER);
        String vieScolaireToken = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "vs-discipline@ecole.example", Role.VIE_SCOLAIRE);

        String incidentBody = "{\"schoolClassId\":" + schoolClass.getId() + ",\"occurredAt\":\"2026-10-01\","
                + "\"severity\":\"MAJOR\",\"description\":\"Bagarre en cours\",\"studentIds\":[" + student.getId() + "]}";
        String created = mockMvc.perform(post("/api/v1/discipline/incidents")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(incidentBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.studentIds.length()").value(1))
                .andReturn().getResponse().getContentAsString();
        Long incidentId = objectMapper.readTree(created).get("data").get("id").asLong();

        mockMvc.perform(post("/api/v1/discipline/incidents/" + incidentId + "/sanctions")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentId\":" + student.getId() + ",\"type\":\"DETENTION\",\"durationDays\":1}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/discipline/incidents/" + incidentId + "/sanctions")
                        .header("Authorization", "Bearer " + vieScolaireToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentId\":" + student.getId() + ",\"type\":\"DETENTION\",\"durationDays\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.type").value("DETENTION"));

        mockMvc.perform(post("/api/v1/discipline/observations")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentId\":" + student.getId() + ",\"positive\":false,\"description\":\"Bavardage répété\"}"))
                .andExpect(status().isCreated());

        String convocationBody = "{\"studentId\":" + student.getId()
                + ",\"convokeParent\":true,\"reason\":\"Suite à l'incident\",\"scheduledAt\":\"2026-10-05T09:00:00Z\"}";
        String convocationCreated = mockMvc.perform(post("/api/v1/discipline/convocations")
                        .header("Authorization", "Bearer " + vieScolaireToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(convocationBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("SCHEDULED"))
                .andReturn().getResponse().getContentAsString();
        Long convocationId = objectMapper.readTree(convocationCreated).get("data").get("id").asLong();

        mockMvc.perform(put("/api/v1/discipline/convocations/" + convocationId + "/status")
                        .header("Authorization", "Bearer " + vieScolaireToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DONE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DONE"));

        mockMvc.perform(get("/api/v1/discipline/students/" + student.getId() + "/history")
                        .header("Authorization", "Bearer " + vieScolaireToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sanctions.length()").value(1))
                .andExpect(jsonPath("$.data.observations.length()").value(1))
                .andExpect(jsonPath("$.data.convocations.length()").value(1));

        mockMvc.perform(get("/api/v1/discipline/statistics")
                        .param("schoolClassId", schoolClass.getId().toString())
                        .param("from", "2026-09-01")
                        .param("to", "2026-10-31")
                        .header("Authorization", "Bearer " + vieScolaireToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.incidentCount").value(1))
                .andExpect(jsonPath("$.data.incidentsBySeverity.MAJOR").value(1))
                .andExpect(jsonPath("$.data.sanctionsByType.DETENTION").value(1));
    }

    @Test
    void rejectsASanctionForAStudentNotDeclaredInTheIncident() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Vie Scolaire Rejet");
        SchoolClass schoolClass = schoolClassRepository.save(TestAuthSupport.withTenant(new SchoolClass("5ème B", null), tenant.getId()));
        Student inIncident = studentRepository.save(TestAuthSupport.withTenant(new Student("D2", "Léa", "Martin", null, null, null), tenant.getId()));
        Student outsider = studentRepository.save(TestAuthSupport.withTenant(new Student("D3", "Zoé", "Bernard", null, null, null), tenant.getId()));
        String vieScolaireToken = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "vs-discipline-2@ecole.example", Role.VIE_SCOLAIRE);

        String incidentBody = "{\"schoolClassId\":" + schoolClass.getId() + ",\"occurredAt\":\"2026-10-02\","
                + "\"severity\":\"MINOR\",\"description\":\"Retard\",\"studentIds\":[" + inIncident.getId() + "]}";
        String created = mockMvc.perform(post("/api/v1/discipline/incidents")
                        .header("Authorization", "Bearer " + vieScolaireToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(incidentBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long incidentId = objectMapper.readTree(created).get("data").get("id").asLong();

        mockMvc.perform(post("/api/v1/discipline/incidents/" + incidentId + "/sanctions")
                        .header("Authorization", "Bearer " + vieScolaireToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentId\":" + outsider.getId() + ",\"type\":\"WARNING\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("STUDENT_NOT_IN_INCIDENT"));
    }

    @Test
    void aTenantCanNeverSeeIncidentsOfAnotherTenant() throws Exception {
        Tenant tenantA = TestAuthSupport.createActiveTenant(tenantRepository, "École Vie Scolaire A");
        Tenant tenantB = TestAuthSupport.createActiveTenant(tenantRepository, "École Vie Scolaire B");
        SchoolClass classB = schoolClassRepository.save(TestAuthSupport.withTenant(new SchoolClass("Secrète", null), tenantB.getId()));
        Incident incidentB = incidentRepository.save(TestAuthSupport.withTenant(
                new Incident(classB.getId(), java.time.LocalDate.of(2026, 10, 3), IncidentSeverity.SEVERE, "Secret", 1L), tenantB.getId()));

        String tokenA = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenantA, "vs-discipline-a@ecole.example", Role.VIE_SCOLAIRE);

        mockMvc.perform(get("/api/v1/discipline/incidents/" + incidentB.getId()).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());
    }
}
