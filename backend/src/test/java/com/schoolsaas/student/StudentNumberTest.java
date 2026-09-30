package com.schoolsaas.student;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schoolsaas.AbstractIntegrationTest;
import com.schoolsaas.TestAuthSupport;
import com.schoolsaas.auth.Role;
import com.schoolsaas.auth.UserRepository;
import com.schoolsaas.tenant.Tenant;
import com.schoolsaas.tenant.TenantContext;
import com.schoolsaas.tenant.TenantRepository;
import java.time.Year;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

/** Attribution automatique des matricules d'élèves. */
class StudentNumberTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private StudentNumberGenerator generator;
    @Autowired private StudentRepository studentRepository;

    private String inscrire(Tenant tenant, String token, String prenom, String matricule) throws Exception {
        String champ = matricule == null ? "" : "\"studentNumber\":\"" + matricule + "\",";
        return objectMapper.readTree(mockMvc.perform(post("/api/v1/students")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" + champ + "\"firstName\":\"" + prenom + "\",\"lastName\":\"Traoré\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString())
                .path("data").path("studentNumber").asText();
    }

    @Test
    void assignsANumberWhenNoneIsProvided() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Matricule");
        String token = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "sec-mat@ecole.example", Role.SECRETARY);

        String premier = inscrire(tenant, token, "Aminata", null);
        String second = inscrire(tenant, token, "Modibo", null);

        // Format AAAA-NNNN : l'année situe la promotion, le rang repart à 1 chaque année.
        assertThat(premier).matches(Year.now().getValue() + "-\\d{4}");
        assertThat(second).matches(Year.now().getValue() + "-\\d{4}");
        assertThat(premier).isNotEqualTo(second);
    }

    /**
     * Deux établissements numérotent chacun de leur côté : le matricule d'une école ne doit
     * dépendre en rien du rythme d'inscription d'une autre.
     */
    @Test
    void numbersAreIndependentBetweenEstablishments() throws Exception {
        Tenant a = TestAuthSupport.createActiveTenant(tenantRepository, "École Numérotation A");
        Tenant b = TestAuthSupport.createActiveTenant(tenantRepository, "École Numérotation B");
        String tokenA = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, a, "sec-a@ecole.example", Role.SECRETARY);
        String tokenB = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, b, "sec-b@ecole.example", Role.SECRETARY);

        String a1 = inscrire(a, tokenA, "Awa", null);
        String b1 = inscrire(b, tokenB, "Sékou", null);
        String a2 = inscrire(a, tokenA, "Fatoumata", null);
        assertThat(a1).as("premier de A").endsWith("-0001");
        assertThat(b1).as("premier de B").endsWith("-0001");
        assertThat(a2).as("second de A").endsWith("-0002");
    }

    /**
     * Le cas qui justifie la table de compteurs. Un MAX(matricule)+1 lu puis réécrit par
     * l'application donnerait ici plusieurs fois le même numéro.
     */
    @Test
    void neverProducesTheSameNumberTwiceUnderConcurrency() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Concurrence");
        int demandes = 30;

        ExecutorService pool = Executors.newFixedThreadPool(10);
        List<Callable<String>> travaux = IntStream.range(0, demandes)
                .<Callable<String>>mapToObj(i -> () -> {
                    TenantContext.set(tenant.getId());
                    try {
                        return generator.next();
                    } finally {
                        TenantContext.clear();
                    }
                })
                .toList();

        List<Future<String>> resultats = pool.invokeAll(travaux);
        pool.shutdown();

        Set<String> matricules = resultats.stream().map(f -> {
            try {
                return f.get();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }).collect(Collectors.toSet());

        assertThat(matricules).hasSize(demandes);
    }

    /**
     * La liste doit fonctionner pour un établissement dont aucun élève n'a de compte famille —
     * c'est-à-dire tout établissement qui vient d'ouvrir. La carte des adresses de connexion
     * était alors immuable, et l'interroger avec un identifiant nul levait une
     * NullPointerException : erreur 500 sur le premier écran que voit une école.
     */
    @Test
    void listsStudentsWhoHaveNoFamilyAccount() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Sans Portail");
        String token = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "sec-portail@ecole.example", Role.SECRETARY);
        inscrire(tenant, token, "Kadiatou", null);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/v1/students").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].portalEmail").doesNotExist());
    }

    /** Un établissement qui reprend un effectif existant garde sa propre numérotation. */
    @Test
    void keepsAnExplicitlyProvidedNumber() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Reprise");
        String token = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "sec-reprise@ecole.example", Role.SECRETARY);

        assertThat(inscrire(tenant, token, "Oumar", "ANCIEN-042")).isEqualTo("ANCIEN-042");
    }

    @Test
    void refusesADuplicateProvidedNumber() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Doublon");
        String token = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "sec-doublon@ecole.example", Role.SECRETARY);
        inscrire(tenant, token, "Mariam", "DOUBLON-1");

        mockMvc.perform(post("/api/v1/students")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentNumber\":\"DOUBLON-1\",\"firstName\":\"Autre\",\"lastName\":\"Traoré\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("STUDENT_NUMBER_ALREADY_USED"));
    }
}
