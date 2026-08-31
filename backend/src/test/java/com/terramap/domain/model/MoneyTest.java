package com.terramap.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    @Test
    void createsValidMoney() {
        Money money = new Money(new BigDecimal("250000.00"), "brl");

        assertThat(money.getAmount()).isEqualByComparingTo("250000.00");
        assertThat(money.getCurrency()).isEqualTo("BRL"); // normalized to uppercase
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "-100.50"})
    void rejectsNonPositiveAmount(String amount) {
        assertThatThrownBy(() -> new Money(new BigDecimal(amount), "BRL"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
    }

    @Test
    void rejectsNullAmount() {
        assertThatThrownBy(() -> new Money(null, "BRL"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsNullCurrency() {
        assertThatThrownBy(() -> new Money(BigDecimal.TEN, null))
                .isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "US", "USDD", "  "})
    void rejectsCurrencyThatIsNotThreeLetters(String currency) {
        assertThatThrownBy(() -> new Money(BigDecimal.TEN, currency))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void equalityIgnoresTrailingZerosAndCurrencyCase() {
        Money a = new Money(new BigDecimal("100.00"), "brl");
        Money b = new Money(new BigDecimal("100"), "BRL");

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void differentAmountsAreNotEqual() {
        Money a = new Money(BigDecimal.TEN, "BRL");
        Money b = new Money(BigDecimal.ONE, "BRL");

        assertThat(a).isNotEqualTo(b);
    }
}
