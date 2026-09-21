package com.schoolsaas.schoolfees;

/**
 * MOBILE_MONEY couvre deux cas : la saisie manuelle d'un règlement reçu hors ligne, et
 * l'encaissement automatique par le module {@code mobilemoney}. Un paiement automatique se
 * reconnaît à son {@code recordedByUserId} vide, personne ne l'ayant saisi.
 */
public enum FeePaymentMethod {
    CASH,
    BANK_TRANSFER,
    MOBILE_MONEY,
    OTHER
}
