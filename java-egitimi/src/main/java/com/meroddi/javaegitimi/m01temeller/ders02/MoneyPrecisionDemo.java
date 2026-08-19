package com.meroddi.javaegitimi.m01temeller.ders02;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * DERS 02 - Degiskenler, veri tipleri, stack/heap, ve paranin dogru tutulmasi.
 *
 * Gercek hayat baglami: odeme sistemi. Bu dersteki hata (para'yi double tutmak)
 * bankacilik ve e-ticaret projelerinde gercekten yasanmis, mutabakat farklarina
 * ve incident'lara sebep olmus bir hatadir.
 *
 * Calistirma:
 *   mvn -q exec:java -Dexec.mainClass=com.meroddi.javaegitimi.m01temeller.ders02.MoneyPrecisionDemo
 */
public class MoneyPrecisionDemo {

    public static void main(String[] args) {

        // ---------------------------------------------------------------
        // 1) PRIMITIVE TIPLER - deger dogrudan degiskenin icinde durur (stack)
        // ---------------------------------------------------------------
        int orderCount = 1_250;              // tam sayi, ~ +/- 2.1 milyar
        long transactionId = 9_000_000_123L; // int'e sigmaz -> long, sonuna L
        double ratio = 0.19;                 // ondalikli (KDV orani gibi)
        boolean isPaid = true;               // true / false
        char currencySymbol = 'T';           // tek karakter, tek tirnak
        byte retryCount = 3;                 // -128..127, bellek kritikse

        System.out.println("--- Primitive tipler ---");
        System.out.println("orderCount     = " + orderCount);
        System.out.println("transactionId  = " + transactionId);
        System.out.println("ratio          = " + ratio);
        System.out.println("isPaid         = " + isPaid);
        System.out.println("currencySymbol = " + currencySymbol);
        System.out.println("retryCount     = " + retryCount);
        System.out.println("int araligi    : " + Integer.MIN_VALUE + " .. " + Integer.MAX_VALUE);
        System.out.println();

        // ---------------------------------------------------------------
        // 2) TASMA (overflow) - int sinirini asarsa Java hata vermez, sessizce doner.
        //    Uretimde "negatif bakiye" seklinde ortaya cikan klasik bug.
        // ---------------------------------------------------------------
        int max = Integer.MAX_VALUE;
        System.out.println("--- Tasma ---");
        System.out.println("Integer.MAX_VALUE + 1 = " + (max + 1) + "  <-- sessizce negatife dondu!");
        System.out.println("Cozum: long kullan   -> " + ((long) max + 1));
        System.out.println();

        // ---------------------------------------------------------------
        // 3) DOUBLE ILE PARA = FELAKET
        //    double ikilik (binary) tabanda calisir; 0.1 gibi sayilar tam tutulamaz.
        // ---------------------------------------------------------------
        double priceDouble = 0.1;
        double sumDouble = 0.0;
        for (int i = 0; i < 10; i++) {
            sumDouble += priceDouble; // 10 adet 0.10 TL'lik urun
        }
        System.out.println("--- double ile para ---");
        System.out.println("10 x 0.10 TL = " + sumDouble + "   (beklenen 1.0)");
        System.out.println("0.1 + 0.2    = " + (0.1 + 0.2) + " (beklenen 0.3)");
        System.out.println("Bu fark 1 milyon islemde mutabakat farkina donusur.");
        System.out.println();

        // ---------------------------------------------------------------
        // 4) DOGRU YOL: BigDecimal (referans tip, nesne olarak heap'te durur)
        //    Dikkat: BigDecimal'i String ile olustur, double ile DEGIL.
        // ---------------------------------------------------------------
        BigDecimal unitPrice = new BigDecimal("0.10");
        BigDecimal totalPrice = BigDecimal.ZERO;
        for (int i = 0; i < 10; i++) {
            totalPrice = totalPrice.add(unitPrice); // BigDecimal immutable: sonucu geri atamak zorunlu
        }
        System.out.println("--- BigDecimal ile para ---");
        System.out.println("10 x 0.10 TL = " + totalPrice + "   (tam dogru)");
        System.out.println("new BigDecimal(0.1)      -> " + new BigDecimal(0.1) + "  <-- double'dan gelen kir");
        System.out.println("new BigDecimal(\"0.1\")    -> " + new BigDecimal("0.1") + "  <-- dogrusu");
        System.out.println();

        // ---------------------------------------------------------------
        // 5) GERCEK SENARYO: sepet tutari uzerinden KDV ve toplam hesabi
        // ---------------------------------------------------------------
        BigDecimal cartAmount = new BigDecimal("1499.99");
        BigDecimal vatRate = new BigDecimal("0.20");
        BigDecimal vat = cartAmount.multiply(vatRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal grandTotal = cartAmount.add(vat);

        System.out.println("--- Fatura hesabi ---");
        System.out.println("Ara toplam : " + cartAmount + " TL");
        System.out.println("KDV %20    : " + vat + " TL");
        System.out.println("Genel toplam: " + grandTotal + " TL");
        System.out.println();

        // ---------------------------------------------------------------
        // 6) REFERANS TIP vs PRIMITIVE - degisken neyi tutuyor?
        // ---------------------------------------------------------------
        int a = 5;
        int b = a;   // degerin KOPYASI
        b = 10;      // a etkilenmez

        int[] x = {1, 2, 3};
        int[] y = x; // ADRESIN kopyasi - ayni dizi
        y[0] = 99;   // x de degisti!

        System.out.println("--- Primitive kopya vs referans kopya ---");
        System.out.println("a = " + a + ", b = " + b + "   (birbirinden bagimsiz)");
        System.out.println("x[0] = " + x[0] + ", y[0] = " + y[0] + " (ayni nesneyi gosteriyorlar)");
        System.out.println();

        // ---------------------------------------------------------------
        // 7) var - tip cikarimi. Tip kaybolmaz, sadece yazmiyoruz.
        // ---------------------------------------------------------------
        var customerName = "Ayse Yilmaz";       // derleyici String oldugunu anlar
        var invoiceTotal = new BigDecimal("250.00");
        System.out.println("--- var ---");
        System.out.println(customerName + " -> " + invoiceTotal + " TL");
        System.out.println("customerName gercek tipi : " + ((Object) customerName).getClass().getSimpleName());
        System.out.println("invoiceTotal gercek tipi : " + invoiceTotal.getClass().getSimpleName());
    }
}
