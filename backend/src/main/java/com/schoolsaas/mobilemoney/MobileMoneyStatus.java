package com.schoolsaas.mobilemoney;

/** Cycle de vie d'une tentative de paiement mobile money. */
public enum MobileMoneyStatus {
    /** Demande envoyée au fournisseur, en attente de la validation du payeur sur son téléphone. */
    PENDING,
    SUCCEEDED,
    FAILED
}
