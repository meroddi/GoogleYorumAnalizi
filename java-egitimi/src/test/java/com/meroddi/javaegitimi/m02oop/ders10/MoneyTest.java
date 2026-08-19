package com.meroddi.javaegitimi.m02oop.ders10;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DERS 15 - Money icin birim testler.
 *
 * Test yazmanin amaci "kod calisiyor mu" degil, "kural hala gecerli mi"dir.
 * Yarin biri yuvarlama mantigini bozarsa, bu testler kirmizi yanar.
 */
class MoneyTest {

    @Test
    @DisplayName("Tutar her zaman 2 haneye yuvarlanir")
    void amountIsAlwaysScaledToTwoDigits() {
        Money money = Money.tryOf("100.005");
        assertEquals(new BigDecimal("100.01"), money.amount());
    }

    @Test
    @DisplayName("Toplama yeni nesne uretir, orijinali degistirmez (immutability)")
    void addDoesNotMutateOriginal() {
        Money original = Money.tryOf("100.00");
        Money result = original.add(Money.tryOf("50.00"));

        assertEquals(Money.tryOf("100.00"), original);
        assertEquals(Money.tryOf("150.00"), result);
    }

    @Test
    @DisplayName("Ayni deger, farkli nesne -> equals true")
    void valueEquality() {
        assertEquals(Money.tryOf("250.00"), Money.tryOf("250.00"));
        assertEquals(Money.tryOf("250.00").hashCode(), Money.tryOf("250.00").hashCode());
    }

    @Test
    @DisplayName("Carpma: birim fiyat x adet")
    void multiply() {
        assertEquals(Money.tryOf("867.00"), Money.tryOf("289.00").multiply(3));
    }

    @Test
    @DisplayName("Farkli para birimleri toplanamaz")
    void differentCurrenciesCannotBeAdded() {
        Money tryMoney = Money.tryOf("100.00");
        Money usdMoney = new Money(new BigDecimal("100.00"), "USD");

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> tryMoney.add(usdMoney));
        assertTrue(exception.getMessage().contains("TRY"));
    }

    @Test
    @DisplayName("Gecersiz para birimi kabul edilmez")
    void invalidCurrencyRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Money(BigDecimal.ONE, "TL"));
    }

    @Test
    @DisplayName("Karsilastirma ve sifir kontrolu")
    void comparison() {
        assertTrue(Money.tryOf("100.00").isGreaterThan(Money.tryOf("99.99")));
        assertFalse(Money.tryOf("100.00").isGreaterThan(Money.tryOf("100.00")));
        assertTrue(Money.zeroTry().isZero());
    }
}
