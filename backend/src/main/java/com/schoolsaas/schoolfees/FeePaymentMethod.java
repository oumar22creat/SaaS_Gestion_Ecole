package com.schoolsaas.schoolfees;

/**
 * MOBILE_MONEY trace un paiement reçu hors-ligne via ce canal (saisie manuelle) — aucun
 * fournisseur n'est intégré pour déclencher un paiement en ligne réel, voir ADR-024.
 */
public enum FeePaymentMethod {
    CASH,
    BANK_TRANSFER,
    MOBILE_MONEY,
    OTHER
}
