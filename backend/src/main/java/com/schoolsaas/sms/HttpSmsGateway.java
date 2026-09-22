package com.schoolsaas.sms;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Envoi par API HTTP, pilotée par la configuration (voir {@link SmsProperties}).
 *
 * <p>Générique à dessein : les opérateurs maliens et les agrégateurs régionaux exposent tous
 * une API REST qui prend un destinataire et un texte, sans jamais partager le même format.
 * Décrire la requête en configuration permet de brancher celui qui sera retenu sans écrire de
 * code, et évite de maintenir des adaptateurs pour des fournisseurs qu'on n'utilisera pas.
 */
@Component
@ConditionalOnProperty(prefix = "app.sms", name = "enabled", havingValue = "true")
public class HttpSmsGateway implements SmsGateway {

    private static final Logger log = LoggerFactory.getLogger(HttpSmsGateway.class);

    /** Un SMS qui met plus de dix secondes à partir ne doit pas bloquer la feuille d'appel. */
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final SmsProperties properties;
    private final HttpClient httpClient;

    public HttpSmsGateway(SmsProperties properties) {
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    }

    @Override
    public String send(String recipient, String body) {
        String payload = properties.bodyTemplate()
                .replace("{to}", escapeJson(recipient))
                .replace("{text}", escapeJson(body))
                .replace("{from}", escapeJson(properties.senderName() == null ? "" : properties.senderName()));

        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(properties.url()))
                .timeout(TIMEOUT)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload));
        if (properties.authHeader() != null && !properties.authHeader().isBlank()) {
            request.header(properties.authHeader(), properties.authValue());
        }

        try {
            HttpResponse<String> response = httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return response.body();
            }
            // Le corps de la réponse porte la raison du refus (crédit épuisé, numéro invalide,
            // expéditeur non autorisé) : il est conservé tel quel dans le journal.
            throw new SmsDeliveryException(
                    "Refus du fournisseur (HTTP " + response.statusCode() + ") : " + response.body());
        } catch (java.io.IOException e) {
            throw new SmsDeliveryException("Fournisseur SMS injoignable", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SmsDeliveryException("Envoi SMS interrompu", e);
        }
    }

    @Override
    public String providerName() {
        return properties.provider() == null ? "http" : properties.provider();
    }

    /** Le message peut contenir des guillemets ou des accents : il doit rester du JSON valide. */
    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }
}
