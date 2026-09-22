package com.schoolsaas.sms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Passerelle par défaut : journalise au lieu d'envoyer. Active tant qu'aucun opérateur n'est
 * configuré ({@code app.sms.enabled=false}).
 *
 * <p>Elle ne prétend jamais avoir envoyé : l'appel réussit, mais le journal des SMS montre
 * que le fournisseur est « log ». Un établissement qui croirait ses parents prévenus alors
 * que rien n'est parti serait dans une situation pire que s'il savait devoir téléphoner.
 */
@Component
@ConditionalOnProperty(prefix = "app.sms", name = "enabled", havingValue = "false", matchIfMissing = true)
public class LoggingSmsGateway implements SmsGateway {

    private static final Logger log = LoggerFactory.getLogger(LoggingSmsGateway.class);

    @Override
    public String send(String recipient, String body) {
        log.info("SMS non envoyé (aucun opérateur configuré) — destinataire {}, message \"{}\"", recipient, body);
        return null;
    }

    @Override
    public String providerName() {
        return "log";
    }
}
