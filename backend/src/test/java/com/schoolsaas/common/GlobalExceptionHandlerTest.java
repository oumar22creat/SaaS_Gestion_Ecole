package com.schoolsaas.common;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schoolsaas.AbstractIntegrationTest;
import com.schoolsaas.TestAuthSupport;
import com.schoolsaas.auth.Role;
import com.schoolsaas.auth.UserRepository;
import com.schoolsaas.tenant.Tenant;
import com.schoolsaas.tenant.TenantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Un endpoint qui n'existe pas doit renvoyer 404, pas 500 — bug découvert en testant le
 * frontend contre un vrai backend authentifié (voir docs/SESSION_LOG.md) :
 * {@code NoResourceFoundException} (repli ressources statiques de Spring quand aucun
 * {@code @RequestMapping} ne correspond) tombait dans le handler générique et renvoyait à
 * tort "INTERNAL_ERROR".
 */
class GlobalExceptionHandlerTest extends AbstractIntegrationTest {

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

    @Test
    void anUnmappedApiPathReturnsNotFoundEvenWhenAuthenticated() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École 404");
        String token = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "admin-404@ecole.example", Role.ADMIN);

        mockMvc.perform(get("/api/v1/this-endpoint-does-not-exist").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void anUnmappedApiPathReturnsUnauthenticatedWithoutAToken() throws Exception {
        mockMvc.perform(get("/api/v1/this-endpoint-does-not-exist-either"))
                .andExpect(status().isUnauthorized());
    }
}
