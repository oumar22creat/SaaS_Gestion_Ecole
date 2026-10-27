package com.schoolsaas.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MoneyFormatTest {

    @Test
    void displaysXofAsFcfaWithAPlainThousandsSeparator() {
        assertThat(MoneyFormat.fcfa(25000)).isEqualTo("25 000 FCFA");
        assertThat(MoneyFormat.format(1250000, "XOF")).isEqualTo("1 250 000 FCFA");
        assertThat(MoneyFormat.format(500, "xof")).isEqualTo("500 FCFA");
    }

    @Test
    void fallsBackToFcfaWhenCurrencyIsMissing() {
        assertThat(MoneyFormat.format(1000, null)).isEqualTo("1 000 FCFA");
        assertThat(MoneyFormat.displayCurrency(" ")).isEqualTo("FCFA");
    }

    @Test
    void keepsOtherCurrencyCodesUnchanged() {
        assertThat(MoneyFormat.format(4900, "EUR")).isEqualTo("4 900 EUR");
    }
}
