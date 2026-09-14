package com.schoolsaas.schoolfees;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

/** Comptabilité et frais scolaires (ROADMAP.md 3.3) : grille tarifaire, génération de factures, paiements, reporting, RBAC, isolation. */
class SchoolFeesTest extends AbstractIntegrationTest {

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
    private FeeScheduleRepository feeScheduleRepository;

    @Test
    void generatesInvoicesForActiveStudentsAndTracksPaymentsToFullSettlement() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Frais Scolaires");
        SchoolClass schoolClass = schoolClassRepository.save(TestAuthSupport.withTenant(new SchoolClass("6ème A", null), tenant.getId()));
        studentRepository.save(TestAuthSupport.withTenant(new Student("F1", "Ada", "L", null, null, schoolClass.getId()), tenant.getId()));
        studentRepository.save(TestAuthSupport.withTenant(new Student("F2", "Alan", "T", null, null, schoolClass.getId()), tenant.getId()));
        Student inactive = studentRepository.save(
                TestAuthSupport.withTenant(new Student("F3", "Grace", "H", null, null, schoolClass.getId()), tenant.getId()));
        inactive.setActive(false);
        studentRepository.save(inactive);

        String accountantToken = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "accountant-fees@ecole.example", Role.ACCOUNTANT);
        String teacherToken = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "teacher-fees@ecole.example", Role.TEACHER);

        mockMvc.perform(post("/api/v1/school-fees/schedules")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"schoolClassId\":" + schoolClass.getId() + ",\"label\":\"Scolarité 2026\",\"amountCents\":100000,"
                                + "\"dueDate\":\"2026-10-01\"}"))
                .andExpect(status().isForbidden());

        String scheduleResp = mockMvc.perform(post("/api/v1/school-fees/schedules")
                        .header("Authorization", "Bearer " + accountantToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"schoolClassId\":" + schoolClass.getId() + ",\"label\":\"Scolarité 2026\",\"amountCents\":100000,"
                                + "\"dueDate\":\"2026-10-01\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long scheduleId = objectMapper.readTree(scheduleResp).get("data").get("id").asLong();

        String invoicesResp = mockMvc.perform(post("/api/v1/school-fees/schedules/" + scheduleId + "/generate-invoices")
                        .header("Authorization", "Bearer " + accountantToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andReturn().getResponse().getContentAsString();
        Long invoiceId = objectMapper.readTree(invoicesResp).get("data").get(0).get("id").asLong();

        // Régénérer est idempotent : toujours 2 factures, pas de doublon pour l'élève inactif.
        mockMvc.perform(post("/api/v1/school-fees/schedules/" + scheduleId + "/generate-invoices")
                        .header("Authorization", "Bearer " + accountantToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.length()").value(2));

        mockMvc.perform(post("/api/v1/school-fees/invoices/" + invoiceId + "/payments")
                        .header("Authorization", "Bearer " + accountantToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amountCents\":40000,\"method\":\"CASH\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/school-fees/invoices/" + invoiceId).header("Authorization", "Bearer " + accountantToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PARTIALLY_PAID"))
                .andExpect(jsonPath("$.data.amountPaidCents").value(40000));

        mockMvc.perform(post("/api/v1/school-fees/invoices/" + invoiceId + "/payments")
                        .header("Authorization", "Bearer " + accountantToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amountCents\":60000,\"method\":\"MOBILE_MONEY\",\"reference\":\"OM-12345\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/school-fees/invoices/" + invoiceId).header("Authorization", "Bearer " + accountantToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PAID"))
                .andExpect(jsonPath("$.data.amountPaidCents").value(100000));

        mockMvc.perform(get("/api/v1/school-fees/reporting")
                        .param("schoolClassId", schoolClass.getId().toString())
                        .param("from", "2026-09-01")
                        .param("to", "2026-10-31")
                        .header("Authorization", "Bearer " + accountantToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalDueCents").value(200000))
                .andExpect(jsonPath("$.data.totalPaidCents").value(100000))
                .andExpect(jsonPath("$.data.totalOutstandingCents").value(100000))
                .andExpect(jsonPath("$.data.unpaidInvoices.length()").value(1));
    }

    @Test
    void aTenantCanNeverSeeFeeSchedulesOfAnotherTenant() throws Exception {
        Tenant tenantA = TestAuthSupport.createActiveTenant(tenantRepository, "École Frais A");
        Tenant tenantB = TestAuthSupport.createActiveTenant(tenantRepository, "École Frais B");
        SchoolClass classB = schoolClassRepository.save(TestAuthSupport.withTenant(new SchoolClass("Secrète", null), tenantB.getId()));
        FeeSchedule scheduleB = feeScheduleRepository.save(TestAuthSupport.withTenant(
                new FeeSchedule(classB.getId(), "Secret", 50000, "XOF", java.time.LocalDate.of(2026, 10, 1)), tenantB.getId()));

        String tokenA = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenantA, "accountant-fees-a@ecole.example", Role.ACCOUNTANT);

        mockMvc.perform(post("/api/v1/school-fees/schedules/" + scheduleB.getId() + "/generate-invoices")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());
    }
}
