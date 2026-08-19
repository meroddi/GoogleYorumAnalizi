package com.meroddi.javaegitimi.m02oop.ders12;

import com.meroddi.javaegitimi.m02oop.ders10.Money;

/**
 * DERS 12 - Havale / EFT ile odeme.
 */
public class BankTransferPayment extends PaymentMethod {

    private final String iban;

    public BankTransferPayment(String bankName, String iban) {
        super(bankName);
        this.iban = iban;
    }

    @Override
    public Money commission(Money amount) {
        return Money.tryOf("0.00");   // havalede komisyon yok
    }

    @Override
    protected String validate(Money amount) {
        String base = super.validate(amount);
        if (base != null) {
            return base;
        }
        if (iban == null || iban.length() != 26 || !iban.startsWith("TR")) {
            return "Gecersiz IBAN";
        }
        return null;
    }

    @Override
    protected String execute(Money amount, String orderNo) {
        return "EFT-" + orderNo.replace("ORD-", "");
    }

    @Override
    public String displayName() {
        return "Havale/EFT (" + iban.substring(0, 6) + "..." + iban.substring(iban.length() - 4) + ")";
    }

    @Override
    public int settlementDays() {
        return 0;   // para dogrudan hesaba gecer
    }
}
