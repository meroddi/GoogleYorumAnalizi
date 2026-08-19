package com.meroddi.javaegitimi.m02oop.ders11;

import com.meroddi.javaegitimi.m02oop.ders10.Money;

import java.util.Objects;

/**
 * DERS 11 - Inheritance (kalitim) - UST SINIF.
 *
 * Ortak olan ne varsa burada: hesap numarasi, sahibi, bakiye, para yatirma/cekme.
 * Farkli olan (cekim kurallari, faiz, ucret) alt siniflarda ozellestirilir.
 *
 * Kalitim "kod tekrarini onlemek icin" degil, "IS-A (bir turudur) iliskisi
 * gercekten varsa" kullanilir. VadesizHesap BIR Hesap'tir -> dogru.
 * Musteri BIR Hesap degildir -> kalitim yanlis olurdu.
 */
public class Account {

    // protected: bu sinif + alt siniflar gorur, disari gormez.
    protected final String accountNo;
    protected final String ownerName;
    protected Money balance;

    public Account(String accountNo, String ownerName, Money initialBalance) {
        this.accountNo = Objects.requireNonNull(accountNo, "Hesap no zorunlu");
        this.ownerName = ownerName;
        this.balance = initialBalance;
    }

    public void deposit(Money amount) {
        if (amount == null || amount.isZero()) {
            throw new IllegalArgumentException("Yatirilacak tutar pozitif olmali");
        }
        this.balance = balance.add(amount);
    }

    /**
     * Para cekme akisi ust sinifta SABIT, kural alt sinifta DEGISKEN.
     * Bu kalip "template method" olarak bilinir.
     */
    public final void withdraw(Money amount) {
        if (amount == null || amount.isZero()) {
            throw new IllegalArgumentException("Cekilecek tutar pozitif olmali");
        }
        Money totalCost = amount.add(withdrawalFee(amount));
        if (!canWithdraw(totalCost)) {
            throw new IllegalStateException(
                    "Cekim reddedildi. Bakiye: " + balance + ", istenen (ucret dahil): " + totalCost);
        }
        this.balance = balance.subtract(totalCost);
    }

    /** Alt siniflar bu kurali degistirebilir (override). */
    protected boolean canWithdraw(Money amount) {
        return !amount.isGreaterThan(balance);
    }

    /** Varsayilan: ucret yok. */
    protected Money withdrawalFee(Money amount) {
        return Money.zeroTry();
    }

    /** Hesap tipinin adi - alt siniflar ezer. */
    public String accountType() {
        return "GENEL HESAP";
    }

    public Money getBalance() {
        return balance;
    }

    public String getAccountNo() {
        return accountNo;
    }

    public String getOwnerName() {
        return ownerName;
    }

    // ==================================================================
    //  Object sinifindan gelen metotlar - Java'da HER SINIF Object'ten turer
    // ==================================================================

    /** Log ve hata ayiklamanin temeli. Ezmezsen "Account@6d06d69c" gorursun. */
    @Override
    public String toString() {
        return "%s[%s, %s, bakiye=%s]".formatted(accountType(), accountNo, ownerName, balance);
    }

    /**
     * Iki hesap ne zaman "ayni"dir? Hesap numarasi ayniysa.
     * KURAL: equals'i ezersen hashCode'u da ezmek ZORUNDASIN.
     * Yoksa HashMap/HashSet icinde nesneni bir daha bulamazsin.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Account other = (Account) o;
        return accountNo.equals(other.accountNo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountNo);
    }
}
