package com.meroddi.javaegitimi.m02oop.ders11;

import com.meroddi.javaegitimi.m02oop.ders10.Money;

/**
 * DERS 11 - Vadeli hesap: faiz isler, vadesinden once cekimde ucret alinir.
 */
public class SavingsAccount extends Account {

    private final String annualRate;   // ornek: "0.35" -> %35
    private int withdrawalCount = 0;

    public SavingsAccount(String accountNo, String ownerName, Money initialBalance, String annualRate) {
        super(accountNo, ownerName, initialBalance);
        this.annualRate = annualRate;
    }

    /** Vadeli hesapta erken cekim ucreti: cekilen tutarin %1'i. */
    @Override
    protected Money withdrawalFee(Money amount) {
        return amount.percentage("0.01");
    }

    /** Ust sinifin metodunu ezip, uzerine kendi isini ekleyen tipik ornek. */
    @Override
    protected boolean canWithdraw(Money amount) {
        if (withdrawalCount >= 3) {
            return false;   // ayda en fazla 3 cekim
        }
        // super.canWithdraw: ust siniftaki kurali cagirip uzerine kural eklemek
        boolean allowed = super.canWithdraw(amount);
        if (allowed) {
            withdrawalCount++;
        }
        return allowed;
    }

    /** Bu sinifa OZEL davranis: faiz isletme. */
    public Money applyMonthlyInterest() {
        java.math.BigDecimal monthlyRate = new java.math.BigDecimal(annualRate)
                .divide(java.math.BigDecimal.valueOf(12), 6, java.math.RoundingMode.HALF_UP);
        Money interest = new Money(balance.amount().multiply(monthlyRate), balance.currency());
        this.balance = balance.add(interest);
        return interest;
    }

    @Override
    public String accountType() {
        return "VADELI";
    }

    public int getWithdrawalCount() {
        return withdrawalCount;
    }
}
