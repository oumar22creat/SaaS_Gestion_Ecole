package com.schoolsaas.transport;

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

/** Transport scolaire (ROADMAP.md 3.6) : lignes/arrêts, affectation, facturation, paiements, RBAC, isolation. */
class TransportTest extends AbstractIntegrationTest {

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
    private StudentRepository studentRepository;

    @Autowired
    private BusRouteRepository busRouteRepository;

    @Test
    void assignsAStudentToACircuitAndBillsTheServiceToFullSettlement() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Transport");
        Student student = studentRepository.save(TestAuthSupport.withTenant(new Student("T1", "Ada", "L", null, null, null), tenant.getId()));
        String secretaryToken = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "secretary-transport@ecole.example", Role.SECRETARY);
        String accountantToken = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "accountant-transport@ecole.example", Role.ACCOUNTANT);

        String routeResp = mockMvc.perform(post("/api/v1/transport/routes")
                        .header("Authorization", "Bearer " + secretaryToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"label\":\"Ligne 1 - Centre-ville\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long routeId = objectMapper.readTree(routeResp).get("data").get("id").asLong();

        mockMvc.perform(post("/api/v1/transport/routes/" + routeId + "/stops")
                        .header("Authorization", "Bearer " + accountantToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Place centrale\",\"sequenceOrder\":0}"))
                .andExpect(status().isForbidden());

        String stopResp = mockMvc.perform(post("/api/v1/transport/routes/" + routeId + "/stops")
                        .header("Authorization", "Bearer " + secretaryToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Place centrale\",\"sequenceOrder\":0}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long stopId = objectMapper.readTree(stopResp).get("data").get("id").asLong();

        // Sans affectation, générer une facture doit échouer.
        mockMvc.perform(post("/api/v1/transport/invoices/generate")
                        .header("Authorization", "Bearer " + accountantToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentId\":" + student.getId() + ",\"periodFrom\":\"2026-10-01\",\"periodTo\":\"2026-10-31\","
                                + "\"amountDueCents\":15000}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ASSIGNMENT_NOT_FOUND"));

        mockMvc.perform(post("/api/v1/transport/assignments")
                        .header("Authorization", "Bearer " + secretaryToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentId\":" + student.getId() + ",\"busRouteId\":" + routeId + ",\"busStopId\":" + stopId + "}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/transport/students/" + student.getId() + "/assignment")
                        .header("Authorization", "Bearer " + secretaryToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.busRouteId").value(routeId));

        String invoiceResp = mockMvc.perform(post("/api/v1/transport/invoices/generate")
                        .header("Authorization", "Bearer " + accountantToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentId\":" + student.getId() + ",\"periodFrom\":\"2026-10-01\",\"periodTo\":\"2026-10-31\","
                                + "\"amountDueCents\":15000}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString();
        Long invoiceId = objectMapper.readTree(invoiceResp).get("data").get("id").asLong();

        mockMvc.perform(post("/api/v1/transport/invoices/" + invoiceId + "/payments")
                        .header("Authorization", "Bearer " + accountantToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amountCents\":15000,\"method\":\"BANK_TRANSFER\",\"reference\":\"VIR-1\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/transport/invoices/" + invoiceId).header("Authorization", "Bearer " + accountantToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PAID"))
                .andExpect(jsonPath("$.data.amountPaidCents").value(15000));
    }

    @Test
    void aTenantCanNeverSeeBusRoutesOfAnotherTenant() throws Exception {
        Tenant tenantA = TestAuthSupport.createActiveTenant(tenantRepository, "École Transport A");
        Tenant tenantB = TestAuthSupport.createActiveTenant(tenantRepository, "École Transport B");
        busRouteRepository.save(TestAuthSupport.withTenant(new BusRoute("Secrète"), tenantB.getId()));

        String tokenA = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenantA, "secretary-transport-a@ecole.example", Role.SECRETARY);

        mockMvc.perform(get("/api/v1/transport/routes").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(result -> org.assertj.core.api.Assertions.assertThat(result.getResponse().getContentAsString())
                        .doesNotContain("Secrète"));
    }
}
