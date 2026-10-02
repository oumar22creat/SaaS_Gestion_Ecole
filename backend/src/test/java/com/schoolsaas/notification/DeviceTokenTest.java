package com.schoolsaas.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schoolsaas.AbstractIntegrationTest;
import com.schoolsaas.TestAuthSupport;
import com.schoolsaas.auth.Role;
import com.schoolsaas.tenant.Tenant;
import com.schoolsaas.tenant.TenantRepository;
import com.schoolsaas.tenant.TenantResolver;
import com.schoolsaas.auth.UserRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Enregistrement des appareils joignables par notification (docs/NOTIFICATIONS-PUSH.md).
 *
 * <p>Le cas qui justifie cette table est celui du téléphone partagé : celui d'une école, celui
 * d'un foyer. Un enseignant s'y connecte, s'en va, un parent prend la suite — et les absences
 * des anciennes classes ne doivent pas continuer d'arriver sur cet écran. C'est pourquoi un
 * jeton n'appartient qu'à un compte à la fois.
 */
class DeviceTokenTest extends AbstractIntegrationTest {

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
    private DeviceTokenRepository deviceTokenRepository;


    private static final String CORPS = """
            {"token":"%s","platform":"%s"}""";

    /** Enregistre un appareil pour le compte authentifié par ce jeton. */
    private void enregistrer(Tenant tenant, String jwt, String token, String plateforme) throws Exception {
        mockMvc.perform(post("/api/v1/device-tokens")
                        .header("Authorization", "Bearer " + jwt)
                        .header(TenantResolver.TENANT_HEADER, tenant.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CORPS.formatted(token, plateforme)))
                .andExpect(status().isNoContent());
    }

    /**
     * Les appareils rattachés à cet établissement.
     *
     * <p>Le filtrage est fait ici plutôt que confié au filtre Hibernate : hors requête HTTP
     * celui-ci n'est pas activé, et ces tests partagent une base avec les autres. L'isolation
     * elle-même est couverte par {@code TenantIsolationTest}, qui reproduit le contexte de
     * production ; ce qui s'observe ici, c'est que chaque appareil porte bien le bon
     * établissement.
     */
    private List<DeviceToken> appareilsDe(Tenant tenant) {
        return deviceTokenRepository.findAll().stream()
                .filter(appareil -> tenant.getId().equals(appareil.getSchoolId()))
                .toList();
    }

    @Test
    void registersADeviceForTheAuthenticatedAccount() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Appareils");
        String jwt = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "prof-appareil@ecole.example", Role.TEACHER);

        enregistrer(tenant, jwt, "jeton-telephone-prof", "ANDROID");

        assertThat(appareilsDe(tenant))
                .singleElement()
                .satisfies(appareil -> {
                    assertThat(appareil.getToken()).isEqualTo("jeton-telephone-prof");
                    assertThat(appareil.getPlatform()).isEqualTo(DevicePlatform.ANDROID);
                });
    }

    /**
     * L'application rejoue l'enregistrement à chaque lancement : FCM renouvelle les jetons sans
     * prévenir, et un client qui n'enregistrerait qu'une fois deviendrait injoignable.
     */
    @Test
    void registeringTwiceDoesNotDuplicateTheDevice() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Rejeu");
        String jwt = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "prof-rejeu@ecole.example", Role.TEACHER);

        enregistrer(tenant, jwt, "jeton-rejoue", "ANDROID");
        enregistrer(tenant, jwt, "jeton-rejoue", "ANDROID");

        assertThat(appareilsDe(tenant)).hasSize(1);
    }

    /** Le cas du téléphone partagé : l'appareil change de main, pas de destinataire. */
    @Test
    void handsTheDeviceOverToTheAccountThatLogsInNext() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Partagée");
        String jwtEnseignant = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "prof-partage@ecole.example", Role.TEACHER);
        String jwtParent = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "parent-partage@ecole.example", Role.PARENT);

        enregistrer(tenant, jwtEnseignant, "jeton-telephone-partage", "ANDROID");
        Long enseignant = appareilsDe(tenant).getFirst().getUserId();

        enregistrer(tenant, jwtParent, "jeton-telephone-partage", "ANDROID");

        List<DeviceToken> appareils = appareilsDe(tenant);
        assertThat(appareils).hasSize(1);
        assertThat(appareils.getFirst().getUserId())
                .as("l'appareil doit suivre le compte connecté, pas s'ajouter au précédent")
                .isNotEqualTo(enseignant);
    }

    @Test
    void forgetsTheDeviceOnLogout() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Sortie");
        String jwt = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "prof-sortie@ecole.example", Role.TEACHER);
        enregistrer(tenant, jwt, "jeton-a-retirer", "IOS");

        mockMvc.perform(delete("/api/v1/device-tokens/{token}", "jeton-a-retirer")
                        .header("Authorization", "Bearer " + jwt)
                        .header(TenantResolver.TENANT_HEADER, tenant.getId()))
                .andExpect(status().isNoContent());

        assertThat(appareilsDe(tenant)).isEmpty();
    }

    /** Un appareil appartient à son établissement, comme toute donnée métier (ADR-001). */
    @Test
    void stampsTheDeviceWithItsOwnEstablishment() throws Exception {
        Tenant premiere = TestAuthSupport.createActiveTenant(tenantRepository, "École Une");
        Tenant seconde = TestAuthSupport.createActiveTenant(tenantRepository, "École Deux");
        String jwt = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, premiere, "prof-une@ecole.example", Role.TEACHER);

        enregistrer(premiere, jwt, "jeton-ecole-une", "ANDROID");

        assertThat(appareilsDe(seconde)).isEmpty();
    }

    @Test
    void rejectsAnUnauthenticatedRegistration() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Anonyme");

        mockMvc.perform(post("/api/v1/device-tokens")
                        .header(TenantResolver.TENANT_HEADER, tenant.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CORPS.formatted("jeton-anonyme", "ANDROID")))
                .andExpect(status().isUnauthorized());
    }
}
