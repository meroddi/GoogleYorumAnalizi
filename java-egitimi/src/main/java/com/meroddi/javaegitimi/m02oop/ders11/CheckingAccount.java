package com.meroddi.javaegitimi.m02oop.ders11;

import com.meroddi.javaegitimi.m02oop.ders10.Money;

/**
 * DERS 11 - Vadesiz hesap: ek hesap (overdraft) limiti vardir.
 *
 * Bakiye sifirin altina inebilir; sinir "kredili mevduat" limitidir.
 */
public class CheckingAccount extends Account {

    private final Money overdraftLimit;

    public CheckingAccount(String accountNo, String ownerName, Money initialBalance, Money overdraftLimit) {
        // super(...) : ust sinifin constructor'i. Ilk satirda olmak ZORUNDA.
        super(accountNo, ownerName, initialBalance);
        this.overdraftLimit = overdraftLimit;
    }

    /** Kural degisti: bakiye + limit kadar cekilebilir. */
    @Override
    protected boolean canWithdraw(Money amount) {
        Money available = balance.add(overdraftLimit);
        return !amount.isGreaterThan(available);
    }

    @Override
    public String accountType() {
        return "VADESIZ";
    }

    public Money getOverdraftLimit() {
        return overdraftLimit;
    }
}
