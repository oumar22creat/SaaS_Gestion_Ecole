package com.schoolsaas.mobilemoney;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * Implémentation par défaut, sans appel réseau : elle journalise la demande et s'arrête là.
 * Même parti pris que {@code LoggingNotificationGateway} pour FCM et que le stockage local
 * pour S3 — le reste de l'application se développe et se teste sans attendre les accès
 * marchands, et brancher l'opérateur réel se fera en fournissant un autre bean.
 *
 * <p>Conséquence à connaître : avec cette implémentation, aucune facture ne sera jamais
 * créditée automatiquement, puisque personne n'appellera le callback.
 */
@Component
@ConditionalOnMissingBean(ignored = LoggingMobileMoneyGateway.class, value = MobileMoneyGateway.class)
public class LoggingMobileMoneyGateway implements MobileMoneyGateway {

    private static final Logger log = LoggerFactory.getLogger(LoggingMobileMoneyGateway.class);

    @Override
    public String providerName() {
        return "LOGGING";
    }

    @Override
    public InitiationResult initiate(
            String reference, long amountCents, String payerMsisdn, String description) {
        log.warn(
                "Mobile money non branché : demande simulée reference={} montant={} payeur={} objet={}",
                reference,
                amountCents,
                payerMsisdn,
                description);
        return new InitiationResult(null, null);
    }
}
