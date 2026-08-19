package com.meroddi.javaegitimi.m02oop.ders10;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * DERS 10 - Immutability ve record.
 *
 * Money bir "value object"tir: kimligi yoktur, DEGERI onu tanimlar.
 * 100 TL ile 100 TL birbirinin ayni seydir - tipki 5 sayisi gibi.
 *
 * record ne yapar?
 *  - Alanlari private final yapar
 *  - Constructor'i yazar
 *  - equals / hashCode / toString'i yazar
 *  - Getter'lari amount() / currency() adiyla uretir
 * Yani asagida yazmadigimiz ~60 satirlik kaliplasmis kodu derleyici uretir.
 *
 * Neden immutable? Cunku bir nesneyi kimse arkandan degistiremezse:
 *  - coklu thread'de guvenlidir (Yil 2'de anlayacagin en pahali konu)
 *  - hata ayiklamasi kolaydir ("bu deger nerede degisti?" sorusu ortadan kalkar)
 *  - Map'te anahtar olarak guvenle kullanilir
 */
public record Money(BigDecimal amount, String currency) {

    /**
     * Compact constructor: dogrulama ve normalizasyon yeri.
     * Alan atamasi otomatik yapilir, biz sadece degeri duzeltiriz/kontrol ederiz.
     */
    public Money {
        if (amount == null) {
            throw new IllegalArgumentException("Tutar null olamaz");
        }
        if (currency == null || currency.length() != 3) {
            throw new IllegalArgumentException("Para birimi 3 harf olmali (TRY, USD, EUR)");
        }
        amount = amount.setScale(2, RoundingMode.HALF_UP); // her zaman 2 hane
        currency = currency.toUpperCase();
    }

    /** Fabrika metotlari: new Money(new BigDecimal("10.00"), "TRY") yerine Money.tryOf("10.00") */
    public static Money tryOf(String amount) {
        return new Money(new BigDecimal(amount), "TRY");
    }

    public static Money zeroTry() {
        return new Money(BigDecimal.ZERO, "TRY");
    }

    /** DIKKAT: nesneyi degistirmez, YENI nesne dondurur. */
    public Money add(Money other) {
        requireSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }

    public Money subtract(Money other) {
        requireSameCurrency(other);
        return new Money(this.amount.subtract(other.amount), this.currency);
    }

    public Money multiply(int quantity) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(quantity)), this.currency);
    }

    public Money percentage(String rate) {
        return new Money(this.amount.multiply(new BigDecimal(rate)), this.currency);
    }

    public boolean isGreaterThan(Money other) {
        requireSameCurrency(other);
        return this.amount.compareTo(other.amount) > 0;
    }

    public boolean isZero() {
        return this.amount.signum() == 0;
    }

    /** Farkli para birimlerini toplamak sessizce yapilirsa muhasebe patlar. */
    private void requireSameCurrency(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                    "Farkli para birimleri toplanamaz: " + this.currency + " + " + other.currency);
        }
    }

    /** record'un otomatik toString'ini ezebiliriz. */
    @Override
    public String toString() {
        return amount + " " + currency;
    }
}
