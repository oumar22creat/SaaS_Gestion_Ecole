package com.schoolsaas.canteen;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schoolsaas.AbstractIntegrationTest;
import com.schoolsaas.TestAuthSupport;
import com.schoolsaas.auth.Role;
import com.schoolsaas.auth.UserRepository;
import com.schoolsaas.billing.PlanRepository;
import com.schoolsaas.billing.SubscriptionRepository;
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

/** Cantine (ROADMAP.md 3.4) : menus, réservations (côté staff), facturation à la consommation réelle, impayés, RBAC, isolation. */
class CanteenTest extends AbstractIntegrationTest {

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
    private MenuRepository menuRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private PlanRepository planRepository;

    @Test
    void reservesMealsBillsConsumptionAndTracksPaymentToFullSettlement() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Cantine");
        TestAuthSupport.grantAllPlanFeatures(subscriptionRepository, planRepository, tenant);
        SchoolClass schoolClass = schoolClassRepository.save(TestAuthSupport.withTenant(new SchoolClass("6ème A", null), tenant.getId()));
        Student student = studentRepository.save(
                TestAuthSupport.withTenant(new Student("C1", "Ada", "L", null, null, schoolClass.getId()), tenant.getId()));
        String secretaryToken = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "secretary-canteen@ecole.example", Role.SECRETARY);
        String accountantToken = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "accountant-canteen@ecole.example", Role.ACCOUNTANT);

        mockMvc.perform(post("/api/v1/canteen/menus")
                        .header("Authorization", "Bearer " + secretaryToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"date\":\"2026-10-01\",\"mainDescription\":\"Riz au poulet\",\"specialDietDescription\":\"Riz aux légumes (végétarien)\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.specialDietDescription").value("Riz aux légumes (végétarien)"));

        mockMvc.perform(post("/api/v1/canteen/reservations")
                        .header("Authorization", "Bearer " + accountantToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentId\":" + student.getId() + ",\"date\":\"2026-10-01\",\"specialDiet\":false}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/canteen/reservations")
                        .header("Authorization", "Bearer " + secretaryToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentId\":" + student.getId() + ",\"date\":\"2026-10-01\",\"specialDiet\":false}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/canteen/reservations")
                        .header("Authorization", "Bearer " + secretaryToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentId\":" + student.getId() + ",\"date\":\"2026-10-02\",\"specialDiet\":true}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/canteen/students/" + student.getId() + "/reservations")
                        .param("from", "2026-10-01").param("to", "2026-10-31")
                        .header("Authorization", "Bearer " + secretaryToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));

        String invoiceResp = mockMvc.perform(post("/api/v1/canteen/invoices/generate")
                        .header("Authorization", "Bearer " + accountantToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentId\":" + student.getId() + ",\"periodFrom\":\"2026-10-01\",\"periodTo\":\"2026-10-31\","
                                + "\"pricePerMealCents\":1000}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.mealCount").value(2))
                .andExpect(jsonPath("$.data.amountDueCents").value(2000))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString();
        Long invoiceId = objectMapper.readTree(invoiceResp).get("data").get("id").asLong();

        mockMvc.perform(get("/api/v1/canteen/invoices/unpaid").header("Authorization", "Bearer " + accountantToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));

        mockMvc.perform(post("/api/v1/canteen/invoices/" + invoiceId + "/payments")
                        .header("Authorization", "Bearer " + accountantToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amountCents\":2000,\"method\":\"CASH\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/canteen/invoices/" + invoiceId).header("Authorization", "Bearer " + accountantToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PAID"))
                .andExpect(jsonPath("$.data.amountPaidCents").value(2000));

        mockMvc.perform(get("/api/v1/canteen/invoices/unpaid").header("Authorization", "Bearer " + accountantToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void aTenantCanNeverSeeMenusOfAnotherTenant() throws Exception {
        Tenant tenantA = TestAuthSupport.createActiveTenant(tenantRepository, "École Cantine A");
        TestAuthSupport.grantAllPlanFeatures(subscriptionRepository, planRepository, tenantA);
        Tenant tenantB = TestAuthSupport.createActiveTenant(tenantRepository, "École Cantine B");
        menuRepository.save(TestAuthSupport.withTenant(
                new Menu(java.time.LocalDate.of(2026, 10, 1), "Secret", null), tenantB.getId()));

        String tokenA = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenantA, "secretary-canteen-a@ecole.example", Role.SECRETARY);

        mockMvc.perform(get("/api/v1/canteen/menus")
                        .param("from", "2026-09-01").param("to", "2026-10-31")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }
}
