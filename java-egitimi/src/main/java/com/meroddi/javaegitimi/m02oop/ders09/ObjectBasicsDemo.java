package com.meroddi.javaegitimi.m02oop.ders09;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DERS 09 - Sinif, nesne, constructor, this, null.
 *
 * Calistirma:
 *   mvn -q exec:java -Dexec.mainClass=com.meroddi.javaegitimi.m02oop.ders09.ObjectBasicsDemo
 */
public class ObjectBasicsDemo {

    public static void main(String[] args) {

        // ---------------------------------------------------------------
        // 1) NESNE URETMEK - 'new' heap'te yer ayirir, constructor'i calistirir
        // ---------------------------------------------------------------
        Customer ayse = new Customer("C-1001", "Ayse Yilmaz", "ayse.yilmaz@example.com");
        Customer mehmet = new Customer("C-1002", "Mehmet Demir", "mehmet@example.com",
                LocalDate.of(2023, 3, 14));

        System.out.println("--- Iki farkli nesne, ayni kalip ---");
        System.out.println(ayse.getCustomerNo() + " | " + ayse.getFullName()
                + " | " + ayse.maskedEmail() + " | uyelik: " + ayse.membershipDays() + " gun");
        System.out.println(mehmet.getCustomerNo() + " | " + mehmet.getFullName()
                + " | " + mehmet.maskedEmail() + " | uyelik: " + mehmet.membershipDays() + " gun");
        System.out.println();

        // ---------------------------------------------------------------
        // 2) HER NESNENIN KENDI DURUMU VAR
        // ---------------------------------------------------------------
        ayse.deactivate();
        System.out.println("--- Bagimsiz durum ---");
        System.out.println("Ayse aktif mi   : " + ayse.isActive());
        System.out.println("Mehmet aktif mi : " + mehmet.isActive() + "  (etkilenmedi)");
        System.out.println();

        // ---------------------------------------------------------------
        // 3) CONSTRUCTOR DOGRULAMASI - gecersiz nesne dogamaz
        // ---------------------------------------------------------------
        System.out.println("--- Gecersiz nesne uretme denemesi ---");
        try {
            new Customer("C-1003", "Hatali Kayit", "eposta-degil");
        } catch (IllegalArgumentException e) {
            System.out.println("Engellendi -> " + e.getMessage());
        }
        System.out.println();

        // ---------------------------------------------------------------
        // 4) IS KURALI NESNENIN ICINDE - Product ornegi
        // ---------------------------------------------------------------
        Product keyboard = new Product("KLV-001", "Mekanik Klavye", new BigDecimal("2500.00"), 10);
        System.out.println("--- Stok islemleri ---");
        System.out.println("Baslangic stogu : " + keyboard.getStockQuantity());
        keyboard.decreaseStock(3);
        System.out.println("3 satildi       : " + keyboard.getStockQuantity());
        keyboard.increaseStock(5);
        System.out.println("5 mal girisi    : " + keyboard.getStockQuantity());
        System.out.println("Stok degeri     : " + keyboard.totalStockValue() + " TL");

        try {
            keyboard.decreaseStock(1000);
        } catch (IllegalStateException e) {
            System.out.println("Engellendi -> " + e.getMessage());
        }
        try {
            keyboard.changePrice(new BigDecimal("9999.00"));
        } catch (IllegalArgumentException e) {
            System.out.println("Engellendi -> " + e.getMessage());
        }
        System.out.println();

        // ---------------------------------------------------------------
        // 5) REFERANS: iki degisken ayni nesneyi gosterebilir
        // ---------------------------------------------------------------
        Product sameKeyboard = keyboard;    // KOPYA DEGIL, ayni nesne
        sameKeyboard.decreaseStock(2);
        System.out.println("--- Referans paylasimi ---");
        System.out.println("keyboard stok     : " + keyboard.getStockQuantity());
        System.out.println("sameKeyboard stok : " + sameKeyboard.getStockQuantity() + " (ayni nesne)");
        System.out.println("keyboard == sameKeyboard : " + (keyboard == sameKeyboard));
        System.out.println();

        // ---------------------------------------------------------------
        // 6) NULL - "hicbir nesneyi gostermiyor". Java'nin en pahali hatasi.
        //    (Tony Hoare buna "benim milyar dolarlik hatam" der.)
        // ---------------------------------------------------------------
        Product missing = null;
        System.out.println("--- null ---");
        try {
            System.out.println(missing.getName());
        } catch (NullPointerException e) {
            System.out.println("NullPointerException olustu: nesne yokken metodu cagrilamaz.");
        }
        System.out.println("Korunma yolu: if (missing != null) { ... }  ya da Optional (Modul 3).");
        System.out.println();

        // ---------------------------------------------------------------
        // 7) NESNE DIZISI - bir katalog
        // ---------------------------------------------------------------
        Product[] catalog = {
                keyboard,
                new Product("MNT-014", "Monitor 27\"", new BigDecimal("4299.00"), 4),
                new Product("MSE-220", "Kablosuz Mouse", new BigDecimal("289.50"), 0)
        };

        System.out.println("--- Katalog ---");
        BigDecimal total = BigDecimal.ZERO;
        for (Product p : catalog) {
            System.out.printf("%-10s %-16s %10s %5d adet %s%n",
                    p.getSku(), p.getName(), p.getPrice(), p.getStockQuantity(),
                    p.isInStock() ? "" : "(TUKENDI)");
            total = total.add(p.totalStockValue());
        }
        System.out.println("Toplam envanter degeri: " + total + " TL");
    }
}
