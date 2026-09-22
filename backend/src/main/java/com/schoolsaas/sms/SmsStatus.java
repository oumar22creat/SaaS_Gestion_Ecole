package com.schoolsaas.sms;

public enum SmsStatus {
    /** L'appel au fournisseur n'a pas abouti à une réponse exploitable : état indéterminé. */
    PENDING,
    SENT,
    FAILED
}
