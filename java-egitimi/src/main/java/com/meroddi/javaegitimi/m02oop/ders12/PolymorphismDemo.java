package com.meroddi.javaegitimi.m02oop.ders12;

import com.meroddi.javaegitimi.m02oop.ders10.Money;

/**
 * DERS 12 - Polymorphism (cok bicimlilik).
 *
 * Ana fikir: kasa kodu hangi odeme yontemiyle calistigini BILMEZ.
 * Yeni bir odeme yontemi eklendiginde bu dosyada tek satir degismez.
 * (SOLID'in "O"su - Open/Closed: gelistirmeye acik, degistirmeye kapali.)
 *
 * Calistirma:
 *   mvn -q exec:java -Dexec.mainClass=com.meroddi.javaegitimi.m02oop.ders12.PolymorphismDemo
 */
public class PolymorphismDemo {

    public static void main(String[] args) {

        // Hepsi PaymentMethod tipinde tutuluyor - gercek siniflari farkli.
        PaymentMethod[] methods = {
                new CreditCardPayment("Garanti", "4506 3474 1234 5678", 1, Money.tryOf("20000.00")),
                new CreditCardPayment("Yapi Kredi", "5528 7900 8765 4321", 6, Money.tryOf("20000.00")),
                new BankTransferPayment("Ziraat", "TR330006100519786457841326"),
                new CashOnDeliveryPayment("Aras Kargo")
        };

        Money cartTotal = Money.tryOf("3450.00");
        String orderNo = "ORD-00042";

        // ---------------------------------------------------------------
        // 1) AYNI CAGRI, FARKLI DAVRANIS
        // ---------------------------------------------------------------
        System.out.println("--- Odeme secenekleri (sepet: " + cartTotal + ") ---");
        for (PaymentMethod method : methods) {
            System.out.printf("%-38s komisyon: %-12s valor: %d gun%n",
                    method.displayName(), method.commission(cartTotal), method.settlementDays());
        }
        System.out.println();

        // ---------------------------------------------------------------
        // 2) TAHSILAT - kasa kodu tek satir; hangi yontem oldugu onemsiz
        // ---------------------------------------------------------------
        System.out.println("--- Tahsilat ---");
        for (PaymentMethod method : methods) {
            PaymentResult result = method.pay(cartTotal, orderNo);
            System.out.println(result);
        }
        System.out.println();

        // ---------------------------------------------------------------
        // 3) HATA DURUMLARI - her yontemin kendi kurali devrede
        // ---------------------------------------------------------------
        System.out.println("--- Kural ihlalleri ---");
        System.out.println(new CashOnDeliveryPayment("MNG Kargo").pay(Money.tryOf("9000.00"), "ORD-00043"));
        System.out.println(new CreditCardPayment("Akbank", "4111 1111 1111 1111", 1, Money.tryOf("1000.00"))
                .pay(Money.tryOf("3450.00"), "ORD-00044"));
        System.out.println(new BankTransferPayment("Vakifbank", "TR33-BOZUK")
                .pay(Money.tryOf("100.00"), "ORD-00045"));
        System.out.println();

        // ---------------------------------------------------------------
        // 4) EN UCUZ YONTEMI SECMEK - polymorphism ile is mantigi
        // ---------------------------------------------------------------
        PaymentMethod cheapest = methods[0];
        for (PaymentMethod method : methods) {
            if (cheapest.commission(cartTotal).isGreaterThan(method.commission(cartTotal))) {
                cheapest = method;
            }
        }
        System.out.println("--- Secim ---");
        System.out.println("En dusuk komisyonlu yontem : " + cheapest.displayName()
                + " (" + cheapest.commission(cartTotal) + ")");
        System.out.println();

        // ---------------------------------------------------------------
        // 5) ABSTRACT SINIF NESNESI URETILEMEZ
        // ---------------------------------------------------------------
        System.out.println("--- Abstract sinif ---");
        System.out.println("new PaymentMethod(...) yazilamaz: derleyici izin vermez.");
        System.out.println("Cunku 'genel odeme yontemi' diye somut bir sey yoktur.");
        System.out.println();

        System.out.println("--- Ozet ---");
        System.out.println("Yeni yontem (orn. dijital cuzdan) eklemek icin:");
        System.out.println("  1. PaymentMethod'u genisleten yeni bir sinif yaz");
        System.out.println("  2. Diziye ekle");
        System.out.println("Bu dosyadaki dongulerin HICBIRI degismez. Iyi tasarimin olcusu budur.");
    }
}
