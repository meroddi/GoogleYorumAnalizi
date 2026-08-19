package com.meroddi.javaegitimi.m02oop.ders11;

import com.meroddi.javaegitimi.m02oop.ders10.Money;

import java.util.HashSet;
import java.util.Set;

/**
 * DERS 11 - Kalitim, super, override, Object metotlari.
 *
 * Calistirma:
 *   mvn -q exec:java -Dexec.mainClass=com.meroddi.javaegitimi.m02oop.ders11.InheritanceDemo
 */
public class InheritanceDemo {

    public static void main(String[] args) {

        CheckingAccount checking = new CheckingAccount(
                "TR-1001", "Ayse Yilmaz", Money.tryOf("1000.00"), Money.tryOf("5000.00"));
        SavingsAccount savings = new SavingsAccount(
                "TR-2002", "Mehmet Demir", Money.tryOf("50000.00"), "0.35");

        // ---------------------------------------------------------------
        // 1) toString ezildi -> log'lar okunabilir
        // ---------------------------------------------------------------
        System.out.println("--- Hesaplar (toString ezildi) ---");
        System.out.println(checking);
        System.out.println(savings);
        System.out.println();

        // ---------------------------------------------------------------
        // 2) AYNI METOT, FARKLI KURAL (override)
        // ---------------------------------------------------------------
        System.out.println("--- Vadesiz hesap: overdraft limiti ---");
        System.out.println("Baslangic bakiye : " + checking.getBalance());
        checking.withdraw(Money.tryOf("3000.00"));    // bakiyeden fazla ama limit icinde
        System.out.println("3000 cekildi     : " + checking.getBalance() + "  (eksiye dustu, limit icinde)");
        try {
            checking.withdraw(Money.tryOf("10000.00"));
        } catch (IllegalStateException e) {
            System.out.println("Engellendi -> " + e.getMessage());
        }
        System.out.println();

        System.out.println("--- Vadeli hesap: cekim ucreti + cekim limiti ---");
        System.out.println("Baslangic bakiye : " + savings.getBalance());
        savings.withdraw(Money.tryOf("10000.00"));
        System.out.println("10000 cekildi    : " + savings.getBalance() + "  (100 TL ucret dusuldu)");
        savings.withdraw(Money.tryOf("1000.00"));
        savings.withdraw(Money.tryOf("1000.00"));
        System.out.println("Cekim sayisi     : " + savings.getWithdrawalCount());
        try {
            savings.withdraw(Money.tryOf("100.00"));
        } catch (IllegalStateException e) {
            System.out.println("Engellendi -> aylik cekim hakki bitti");
        }
        System.out.println();

        // ---------------------------------------------------------------
        // 3) ALT SINIFA OZEL DAVRANIS
        // ---------------------------------------------------------------
        Money interest = savings.applyMonthlyInterest();
        System.out.println("--- Faiz (sadece vadeli hesapta var) ---");
        System.out.println("Islenen faiz : " + interest);
        System.out.println("Yeni bakiye  : " + savings.getBalance());
        System.out.println();

        // ---------------------------------------------------------------
        // 4) UPCASTING - alt sinif nesnesi ust sinif tipinde tutulur
        //    Java, calisma aninda GERCEK tipin metodunu cagirir (dynamic dispatch).
        // ---------------------------------------------------------------
        Account[] portfolio = {checking, savings,
                new Account("TR-3003", "Zeynep Kaya", Money.tryOf("250.00"))};

        System.out.println("--- Portfoy (ust sinif tipinde dizi) ---");
        Money total = Money.zeroTry();
        for (Account account : portfolio) {
            // accountType() cagrisi derleme aninda Account'a bakar,
            // calisma aninda nesnenin GERCEK sinifina gore calisir.
            System.out.printf("%-12s %-10s %-14s %14s%n",
                    account.accountType(), account.getAccountNo(), account.getOwnerName(), account.getBalance());
            total = total.add(account.getBalance());
        }
        System.out.println("Toplam varlik: " + total);
        System.out.println();

        // ---------------------------------------------------------------
        // 5) instanceof + pattern matching (Java 16+)
        // ---------------------------------------------------------------
        System.out.println("--- Tip kontrolu ---");
        for (Account account : portfolio) {
            if (account instanceof SavingsAccount s) {      // hem kontrol hem donusum
                System.out.println(s.getAccountNo() + " vadeli, kalan cekim hakki: "
                        + (3 - s.getWithdrawalCount()));
            } else if (account instanceof CheckingAccount c) {
                System.out.println(c.getAccountNo() + " vadesiz, ek hesap limiti: " + c.getOverdraftLimit());
            } else {
                System.out.println(account.getAccountNo() + " genel hesap");
            }
        }
        System.out.println("Not: kod her yerde instanceof ile dolduysa, tasarim yanlistir.");
        System.out.println("     Dogrusu polymorphism'dir -> Ders 12.");
        System.out.println();

        // ---------------------------------------------------------------
        // 6) equals / hashCode: neden ikisi birlikte ezilir?
        // ---------------------------------------------------------------
        Account copy = new Account("TR-3003", "Zeynep Kaya", Money.tryOf("999.00"));
        Account original = portfolio[2];

        System.out.println("--- equals / hashCode ---");
        System.out.println("Ayni hesap no, farkli bakiye -> equals : " + original.equals(copy));
        System.out.println("hashCode'lar esit mi                   : " + (original.hashCode() == copy.hashCode()));

        Set<Account> accountSet = new HashSet<>();
        accountSet.add(original);
        accountSet.add(copy);       // ayni hesap sayilir, tekrar eklenmez
        System.out.println("HashSet boyutu (2 nesne eklendi)       : " + accountSet.size());
        System.out.println("hashCode ezilmeseydi boyut 2 olurdu ve mukerrer kayit olusurdu.");
    }
}
