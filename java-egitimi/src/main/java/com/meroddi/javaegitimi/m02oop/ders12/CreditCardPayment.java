package com.meroddi.javaegitimi.m02oop.ders12;

import com.meroddi.javaegitimi.m02oop.ders10.Money;

/**
 * DERS 12 - Kredi karti ile odeme.
 */
public class CreditCardPayment extends PaymentMethod {

    private final String maskedCardNo;
    private final int installments;
    private final Money limit;

    public CreditCardPayment(String bankName, String cardNo, int installments, Money limit) {
        super(bankName);
        this.maskedCardNo = mask(cardNo);
        this.installments = installments;
        this.limit = limit;
    }

    @Override
    public Money commission(Money amount) {
        // Taksit arttikca komisyon artar: %2.3 + her ek taksit icin %0.4
        // Oran hesabinda da double kullanmiyoruz (Ders 02).
        java.math.BigDecimal rate = new java.math.BigDecimal("0.023")
                .add(new java.math.BigDecimal("0.004").multiply(java.math.BigDecimal.valueOf(installments - 1L)));
        return new Money(amount.amount().multiply(rate), amount.currency());
    }

    @Override
    protected String validate(Money amount) {
        String base = super.validate(amount);     // ust siniftaki genel kontrol
        if (base != null) {
            return base;
        }
        if (amount.isGreaterThan(limit)) {
            return "Kart limiti yetersiz (limit: " + limit + ")";
        }
        if (installments < 1 || installments > 12) {
            return "Taksit sayisi 1-12 arasinda olmali";
        }
        return null;
    }

    @Override
    protected String execute(Money amount, String orderNo) {
        // Gercek hayatta: banka POS API'sine HTTP cagrisi.
        return "AUTH-" + Math.abs(orderNo.hashCode() % 1_000_000);
    }

    @Override
    public String displayName() {
        return "Kredi Karti " + maskedCardNo + (installments > 1 ? " (" + installments + " taksit)" : " (tek cekim)");
    }

    @Override
    public int settlementDays() {
        return installments > 1 ? 30 : 2;   // taksitli satista para gec gelir
    }

    private static String mask(String cardNo) {
        String digits = cardNo.replace(" ", "");
        if (digits.length() < 8) {
            return "****";
        }
        return digits.substring(0, 4) + " **** **** " + digits.substring(digits.length() - 4);
    }
}
