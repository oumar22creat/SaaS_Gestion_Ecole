package com.schoolsaas.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.schoolsaas.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Le CORS a coupé toutes les écoles en production : la configuration n'autorisait que le
 * domaine nu, alors que chaque établissement vit sur son sous-domaine.
 *
 * <p>Le piège n'est pas évident. On suppose volontiers qu'une page et son API servies sur le
 * même domaine échappent au CORS ; c'est faux, les navigateurs joignent un en-tête
 * {@code Origin} aux POST même en même-origine, et Spring traite alors la requête comme une
 * requête CORS. Une origine absente de la liste donne 403 « Invalid CORS request », sans
 * qu'aucune trace n'apparaisse côté applicatif — la requête est refusée avant.
 */
@TestPropertySource(properties = "app.cors.allowed-origins=https://exemple.test,https://*.exemple.test")
class CorsTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String INSCRIPTION = """
            {"schoolName":"École CORS","subdomain":"ecole-cors-%s","adminEmail":"cors-%s@exemple.test",
             "adminPassword":"Sup3rSecret!","adminFirstName":"Ada","adminLastName":"Lovelace"}""";

    /** Le cas qui cassait la production : une école appelant l'API depuis son sous-domaine. */
    @Test
    void acceptsARequestFromAnEstablishmentSubdomain() throws Exception {
        String suffixe = "sub" + System.nanoTime();
        mockMvc.perform(post("/api/v1/tenants/register")
                        .header("Origin", "https://une-ecole.exemple.test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(INSCRIPTION.formatted(suffixe, suffixe)))
                .andExpect(status().isCreated());
    }

    @Test
    void acceptsARequestFromTheBareDomain() throws Exception {
        String suffixe = "nu" + System.nanoTime();
        mockMvc.perform(post("/api/v1/tenants/register")
                        .header("Origin", "https://exemple.test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(INSCRIPTION.formatted(suffixe, suffixe)))
                .andExpect(status().isCreated());
    }

    /** L'ouverture aux sous-domaines ne doit pas ouvrir à n'importe qui. */
    @Test
    void refusesARequestFromAForeignOrigin() throws Exception {
        String suffixe = "etranger" + System.nanoTime();
        mockMvc.perform(post("/api/v1/tenants/register")
                        .header("Origin", "https://site-malveillant.example")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(INSCRIPTION.formatted(suffixe, suffixe)))
                .andExpect(status().isForbidden());
    }

    /**
     * L'application mobile n'appelle pas l'API depuis le domaine de l'école mais depuis
     * l'origine locale de son WebView. Cette origine n'apparaît dans aucune configuration de
     * déploiement : si le backend ne l'accepte pas d'office, l'application compilée échoue en
     * 403 dès l'écran de connexion, alors que le site web fonctionne.
     */
    @ParameterizedTest
    @ValueSource(strings = {"https://localhost", "capacitor://localhost", "ionic://localhost"})
    void acceptsARequestFromTheMobileWebView(String origine) throws Exception {
        String suffixe = "mobile" + System.nanoTime();
        mockMvc.perform(post("/api/v1/tenants/register")
                        .header("Origin", origine)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(INSCRIPTION.formatted(suffixe, suffixe)))
                .andExpect(status().isCreated());
    }
}
