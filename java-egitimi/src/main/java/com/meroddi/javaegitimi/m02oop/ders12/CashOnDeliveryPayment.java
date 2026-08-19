package com.meroddi.javaegitimi.m02oop.ders12;

import com.meroddi.javaegitimi.m02oop.ders10.Money;

/**
 * DERS 12 - Kapida odeme.
 */
public class CashOnDeliveryPayment extends PaymentMethod {

    private static final Money MAX_AMOUNT = Money.tryOf("5000.00");

    private final String courierCompany;

    public CashOnDeliveryPayment(String courierCompany) {
        super(courierCompany);
        this.courierCompany = courierCompany;
    }

    @Override
    public Money commission(Money amount) {
        return Money.tryOf("29.90");   // sabit hizmet bedeli
    }

    @Override
    protected String validate(Money amount) {
        String base = super.validate(amount);
        if (base != null) {
            return base;
        }
        if (amount.isGreaterThan(MAX_AMOUNT)) {
            return "Kapida odeme siniri asildi (en fazla " + MAX_AMOUNT + ")";
        }
        return null;
    }

    @Override
    protected String execute(Money amount, String orderNo) {
        return "COD-" + courierCompany.substring(0, 3).toUpperCase() + "-" + orderNo;
    }

    @Override
    public String displayName() {
        return "Kapida Odeme (" + courierCompany + ")";
    }

    @Override
    public int settlementDays() {
        return 7;   // kurye tahsilati sonrasi
    }
}
