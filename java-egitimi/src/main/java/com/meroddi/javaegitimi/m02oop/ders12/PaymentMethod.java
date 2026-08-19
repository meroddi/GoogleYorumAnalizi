package com.meroddi.javaegitimi.m02oop.ders12;

import com.meroddi.javaegitimi.m02oop.ders10.Money;

/**
 * DERS 12 - Abstract class ve polymorphism.
 *
 * ABSTRACT SINIF: ortak iskeleti tasir ama tek basina nesnesi uretilemez.
 * "Odeme yontemi" diye somut bir sey yoktur; kredi karti, havale, kapida odeme vardir.
 *
 * Buradaki kalip gercek odeme sistemlerinde birebir boyledir:
 *   dogrula -> komisyonu hesapla -> odemeyi al -> sonucu dondur
 * Adimlarin SIRASI sabittir (ust sinif), adimlarin ICERIGI degisir (alt siniflar).
 */
public abstract class PaymentMethod {

    protected final String providerName;

    protected PaymentMethod(String providerName) {
        this.providerName = providerName;
    }

    /**
     * Sabit akis - template method.
     * final: alt siniflar akisi bozamasin, sadece adimlari degistirsin.
     */
    public final PaymentResult pay(Money amount, String orderNo) {
        String validationError = validate(amount);
        if (validationError != null) {
            return PaymentResult.failed(orderNo, providerName, validationError);
        }
        Money commission = commission(amount);
        Money charged = amount.add(commission);
        String reference = execute(charged, orderNo);
        return PaymentResult.success(orderNo, providerName, charged, commission, reference, settlementDays());
    }

    // --- Alt siniflarin DOLDURMAK ZORUNDA oldugu bosluklar (abstract) ---

    /** Odeme saglayicisina gore komisyon. */
    public abstract Money commission(Money amount);

    /** Gercek tahsilat adimi; gercek hayatta banka/POS API cagrisi olur. */
    protected abstract String execute(Money amount, String orderNo);

    /** Ekranda gosterilecek ad. */
    public abstract String displayName();

    // --- Varsayilan davranislar: alt sinif isterse ezer, istemezse kullanir ---

    /** Dogrulama basarisizsa hata mesaji, basariliysa null doner. */
    protected String validate(Money amount) {
        if (amount == null || amount.isZero()) {
            return "Tutar sifir olamaz";
        }
        return null;
    }

    /** Paranin saticinin hesabina gecme suresi (gun). */
    public int settlementDays() {
        return 1;
    }

    public String getProviderName() {
        return providerName;
    }
}
