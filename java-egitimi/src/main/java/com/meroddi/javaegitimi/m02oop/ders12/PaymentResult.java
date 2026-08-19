package com.meroddi.javaegitimi.m02oop.ders12;

import com.meroddi.javaegitimi.m02oop.ders10.Money;

/**
 * DERS 12 - Odeme sonucu (immutable sonuc nesnesi).
 *
 * Metotlar "basarisiz oldu" bilgisini ya exception ile ya da boyle bir
 * sonuc nesnesi ile tasir. Beklenen is kurali hatalari icin (kart limiti
 * yetersiz gibi) sonuc nesnesi daha uygundur: exception, BEKLENMEYEN durumlar icindir.
 */
public record PaymentResult(
        boolean successful,
        String orderNo,
        String provider,
        Money chargedAmount,
        Money commission,
        String reference,
        int settlementDays,
        String errorMessage) {

    public static PaymentResult success(String orderNo, String provider, Money charged,
                                        Money commission, String reference, int settlementDays) {
        return new PaymentResult(true, orderNo, provider, charged, commission, reference, settlementDays, null);
    }

    public static PaymentResult failed(String orderNo, String provider, String errorMessage) {
        return new PaymentResult(false, orderNo, provider, null, null, null, 0, errorMessage);
    }

    @Override
    public String toString() {
        if (successful) {
            return "[OK]   %s | %-16s | tahsil: %-14s | komisyon: %-10s | ref: %s | valor: %d gun"
                    .formatted(orderNo, provider, chargedAmount, commission, reference, settlementDays);
        }
        return "[HATA] %s | %-16s | %s".formatted(orderNo, provider, errorMessage);
    }
}
