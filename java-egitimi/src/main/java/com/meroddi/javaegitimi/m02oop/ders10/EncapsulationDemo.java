package com.meroddi.javaegitimi.m02oop.ders10;

import java.util.Arrays;

/**
 * DERS 10 - Encapsulation, immutability, record, static.
 *
 * Calistirma:
 *   mvn -q exec:java -Dexec.mainClass=com.meroddi.javaegitimi.m02oop.ders10.EncapsulationDemo
 */
public class EncapsulationDemo {

    public static void main(String[] args) {

        // ---------------------------------------------------------------
        // 1) IMMUTABLE VALUE OBJECT: Money
        // ---------------------------------------------------------------
        Money price = Money.tryOf("1499.99");
        Money vat = price.percentage("0.20");
        Money total = price.add(vat);

        System.out.println("--- Money (record, immutable) ---");
        System.out.println("Fiyat  : " + price);
        System.out.println("KDV    : " + vat);
        System.out.println("Toplam : " + total);
        System.out.println("Orijinal fiyat degisti mi? -> " + price + "  (hayir, yeni nesne uretildi)");
        System.out.println();

        // ---------------------------------------------------------------
        // 2) record ne kazandirdi: equals / hashCode / toString bedava
        // ---------------------------------------------------------------
        Money a = Money.tryOf("100.00");
        Money b = Money.tryOf("100.00");
        Money c = Money.tryOf("100.005");   // 2 haneye yuvarlanir -> 100.01

        System.out.println("--- Deger esitligi ---");
        System.out.println("a == b        : " + (a == b) + "  (farkli nesneler)");
        System.out.println("a.equals(b)   : " + a.equals(b) + "   (degerleri ayni -> esit)");
        System.out.println("a.hashCode()==b.hashCode() : " + (a.hashCode() == b.hashCode()));
        System.out.println("c (100.005)   : " + c + "  (compact constructor yuvarladi)");
        System.out.println("a.amount()    : " + a.amount() + "   (record getter'i)");
        System.out.println();

        // ---------------------------------------------------------------
        // 3) Kurallar nesnenin icinde
        // ---------------------------------------------------------------
        System.out.println("--- Is kurali korumasi ---");
        try {
            Money usd = new Money(new java.math.BigDecimal("50"), "USD");
            a.add(usd);
        } catch (IllegalArgumentException e) {
            System.out.println("Engellendi -> " + e.getMessage());
        }
        System.out.println();

        // ---------------------------------------------------------------
        // 4) ENCAPSULATION: invariant korumasi
        // ---------------------------------------------------------------
        LoyaltyAccount account = new LoyaltyAccount("C-1001", 120, new String[]{"WELCOME10"});
        System.out.println("--- Sadakat hesabi ---");
        System.out.println("Baslangic  : " + account.getPoints() + " puan (" + account.tier() + ")");

        account.earnFromPurchase(Money.tryOf("45000.00"));   // 450 puan
        System.out.println("Alisveris sonrasi : " + account.getPoints() + " puan (" + account.tier() + ")");

        account.spend(200);
        System.out.println("200 puan harcandi : " + account.getPoints() + " puan (" + account.tier() + ")");

        try {
            account.spend(999_999);
        } catch (IllegalStateException e) {
            System.out.println("Engellendi -> " + e.getMessage());
        }
        System.out.println("Puan hicbir yolla negatif yapilamaz: setter yok, sadece anlamli metot var.");
        System.out.println();

        // ---------------------------------------------------------------
        // 5) SIZDIRAN GETTER vs SAVUNMACI KOPYA
        // ---------------------------------------------------------------
        System.out.println("--- Sizdiran getter ---");
        System.out.println("Baslangic ic durum : " + Arrays.toString(account.getUsedCampaignCodes()));

        String[] leaked = account.getUsedCampaignCodesLeaky();
        leaked[0] = "HACKED";                                  // ic diziye dogrudan yaziyoruz
        System.out.println("Sizdiran getter ile disaridan yazildi -> ic durum : "
                + Arrays.toString(account.getUsedCampaignCodes()) + "  <-- BOZULDU");

        String[] safeCopy = account.getUsedCampaignCodes();
        safeCopy[0] = "TEKRAR-DENEME";                         // sadece kopyayi degistirir
        System.out.println("Guvenli getter kopyasi degistirildi   -> ic durum : "
                + Arrays.toString(account.getUsedCampaignCodes()) + "  <-- KORUNDU");
        System.out.println();

        // ---------------------------------------------------------------
        // 6) STATIC vs INSTANCE
        // ---------------------------------------------------------------
        System.out.println("--- static vs instance ---");
        System.out.println("Siparis no 1 : " + OrderIdGenerator.nextId());   // nesne yok, sinif uzerinden
        System.out.println("Siparis no 2 : " + OrderIdGenerator.nextId());
        System.out.println("Siparis no 3 : " + OrderIdGenerator.nextId());
        System.out.println("Uretilen toplam (static sayac): " + OrderIdGenerator.generatedCount());

        OrderIdGenerator gen1 = new OrderIdGenerator();
        OrderIdGenerator gen2 = new OrderIdGenerator();
        gen1.nextLocal();
        gen1.nextLocal();
        gen2.nextLocal();
        System.out.println("gen1 yerel sayac : " + gen1.getLocalCounter() + " (kendi kopyasi)");
        System.out.println("gen2 yerel sayac : " + gen2.getLocalCounter() + " (kendi kopyasi)");
        System.out.println("static sayac hala: " + OrderIdGenerator.generatedCount() + " (paylasilan)");
    }
}
