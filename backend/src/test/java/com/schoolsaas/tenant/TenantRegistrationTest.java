package com.schoolsaas.tenant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schoolsaas.AbstractIntegrationTest;
import com.schoolsaas.tenant.dto.TenantRegistrationRequest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

class TenantRegistrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TenantRepository tenantRepository;

    @Test
    void registersAnEstablishmentAndLogsInTheInitialAdmin() throws Exception {
        String subdomain = "ecole-" + UUID.randomUUID().toString().substring(0, 8);
        var request = new TenantRegistrationRequest(
                "École Nouvelle", subdomain, "admin@ecole-nouvelle.example", "Sup3rSecret!", "Ada", "Lovelace");

        MvcResult result = mockMvc.perform(post("/api/v1/tenants/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.subdomain").value(subdomain))
                .andExpect(jsonPath("$.data.tokens.accessToken").isNotEmpty())
                .andReturn();

        String accessToken = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("data").get("tokens").get("accessToken").asText();

        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("admin@ecole-nouvelle.example"))
                .andExpect(jsonPath("$.data.role").value("ADMIN"));

        assertTenantStatusIsTrial(subdomain);
    }

    @Test
    void rejectsADuplicateSubdomain() throws Exception {
        String subdomain = "ecole-dup-" + UUID.randomUUID().toString().substring(0, 8);
        tenantRepository.save(new Tenant("École Existante", subdomain, TenantStatus.ACTIVE));

        var request = new TenantRegistrationRequest(
                "Autre École", subdomain, "autre@ecole.example", "Sup3rSecret!", "Ada", "Lovelace");

        mockMvc.perform(post("/api/v1/tenants/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("SUBDOMAIN_ALREADY_TAKEN"));
    }

    @Test
    void rejectsAWeakPassword() throws Exception {
        var request = new TenantRegistrationRequest(
                "École Faible", "ecole-faible-" + UUID.randomUUID().toString().substring(0, 8),
                "admin@ecole-faible.example", "short", "Ada", "Lovelace");

        mockMvc.perform(post("/api/v1/tenants/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    private void assertTenantStatusIsTrial(String subdomain) {
        Tenant tenant = tenantRepository.findBySubdomain(subdomain).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(tenant.getStatus()).isEqualTo(TenantStatus.TRIAL);
    }
}
