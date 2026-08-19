package com.meroddi.javaegitimi.m01temeller.ders07;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * DERS 07 - Metotlar: parametre, donus degeri, overloading, varargs,
 *           kapsam (scope), pass-by-value gercegi, ozyineleme (recursion).
 *
 * Gercek hayat baglami: fiyatlandirma servisi. Bir e-ticaret sisteminde
 * "urunun son fiyati" hesabi 10+ kuraldan olusur. Bunu tek bir dev metotta
 * yazan kisi test edemez; kucuk ve tek isli metotlara bolen kisi test eder.
 *
 * Calistirma:
 *   mvn -q exec:java -Dexec.mainClass=com.meroddi.javaegitimi.m01temeller.ders07.PricingMethodsDemo
 */
public class PricingMethodsDemo {

    /** Sabitler metot icinde degil, sinif seviyesinde tanimlanir. */
    private static final BigDecimal VAT_RATE = new BigDecimal("0.20");
    private static final int MONEY_SCALE = 2;

    public static void main(String[] args) {

        // ---------------------------------------------------------------
        // 1) BASIT METOT CAGRISI
        // ---------------------------------------------------------------
        BigDecimal listPrice = new BigDecimal("2500.00");
        System.out.println("--- Fiyat hesabi ---");
        System.out.println("Liste fiyati        : " + listPrice + " TL");
        System.out.println("KDV                 : " + calculateVat(listPrice) + " TL");
        System.out.println("KDV dahil           : " + addVat(listPrice) + " TL");
        System.out.println("%15 indirimli       : " + applyDiscount(listPrice, 15) + " TL");
        System.out.println();

        // ---------------------------------------------------------------
        // 2) OVERLOADING (asiri yukleme)
        //    Ayni isim, farkli parametre imzasi. Cagiran taraf icin okunakli API.
        // ---------------------------------------------------------------
        System.out.println("--- Overloading: ayni isim, farkli imza ---");
        System.out.println("applyDiscount(2500, 15)          -> " + applyDiscount(listPrice, 15));
        System.out.println("applyDiscount(2500, \"WELCOME10\") -> " + applyDiscount(listPrice, "WELCOME10"));
        System.out.println("applyDiscount(2500, 15, 300)     -> " + applyDiscount(listPrice, 15, new BigDecimal("300")));
        System.out.println("(son ornek: indirim ust siniri 300 TL ile kisitlandi)");
        System.out.println();

        // ---------------------------------------------------------------
        // 3) VARARGS - degisken sayida parametre
        // ---------------------------------------------------------------
        System.out.println("--- Varargs ---");
        System.out.println("Sepet toplami (3 urun) : " + sumAll(
                new BigDecimal("749.90"), new BigDecimal("4299.00"), new BigDecimal("289.50")));
        System.out.println("Sepet toplami (0 urun) : " + sumAll());
        System.out.println();

        // ---------------------------------------------------------------
        // 4) METOTLARI BIRLESTIRMEK - gercek fiyat akisi
        // ---------------------------------------------------------------
        System.out.println("--- Kasa fisi ---");
        printReceipt("Mekanik Klavye", new BigDecimal("2500.00"), 2, "WELCOME10");
        printReceipt("Monitor 27\"", new BigDecimal("4299.00"), 1, null);
        System.out.println();

        // ---------------------------------------------------------------
        // 5) PASS-BY-VALUE - Java'da HER SEY deger ile gecer
        //    Primitive'de degerin kopyasi, nesnede ADRESIN kopyasi gecer.
        // ---------------------------------------------------------------
        int quantity = 5;
        tryToChangePrimitive(quantity);

        int[] basket = {1, 2, 3};
        tryToChangeArrayContent(basket);   // icerik degisir
        tryToReassignArray(basket);        // yeniden atama disariyi ETKILEMEZ

        System.out.println("--- Pass-by-value ---");
        System.out.println("quantity  : " + quantity + "        (metot degistiremedi)");
        System.out.println("basket[0] : " + basket[0] + "        (icerik degisti)");
        System.out.println("basket    : uzunluk " + basket.length + " (yeniden atama disari yansimadi)");
        System.out.println();

        // ---------------------------------------------------------------
        // 6) SCOPE (kapsam) - degisken tanimlandigi blokta yasar
        // ---------------------------------------------------------------
        System.out.println("--- Kapsam ---");
        int outer = 10;
        {
            int inner = 20;                 // sadece bu blokta gecerli
            System.out.println("Ic blok: outer=" + outer + ", inner=" + inner);
        }
        // System.out.println(inner); // <-- derlenmez: inner burada yok
        System.out.println("Dis blok: outer=" + outer);
        System.out.println();

        // ---------------------------------------------------------------
        // 7) RECURSION (ozyineleme) - metot kendini cagirir
        //    Ornek: kategori agacinda toplam urun sayisi / birlesik faiz
        // ---------------------------------------------------------------
        System.out.println("--- Ozyineleme: birlesik faiz ---");
        BigDecimal principal = new BigDecimal("10000.00");
        for (int year = 1; year <= 5; year++) {
            System.out.printf("%d. yil sonu: %s TL%n", year,
                    compoundInterest(principal, new BigDecimal("0.35"), year));
        }
        System.out.println("Not: her ozyinelemede DURDURMA KOSULU olmali, yoksa StackOverflowError.");
    }

    // ==================================================================
    //  Fiyat hesaplama metotlari
    //  Kural: bir metot TEK is yapar ve adi ne yaptigini soyler.
    // ==================================================================

    /** KDV tutarini hesaplar. */
    private static BigDecimal calculateVat(BigDecimal amount) {
        return amount.multiply(VAT_RATE).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    /** KDV dahil tutari dondurur. */
    private static BigDecimal addVat(BigDecimal amount) {
        return amount.add(calculateVat(amount));  // metot metodu cagirir
    }

    /** Yuzde indirim uygular. */
    private static BigDecimal applyDiscount(BigDecimal amount, int percent) {
        if (percent <= 0) {
            return amount;
        }
        BigDecimal discount = amount
                .multiply(BigDecimal.valueOf(percent))
                .divide(BigDecimal.valueOf(100), MONEY_SCALE, RoundingMode.HALF_UP);
        return amount.subtract(discount);
    }

    /** Ayni isim, farkli imza: kupon kodu ile indirim. */
    private static BigDecimal applyDiscount(BigDecimal amount, String couponCode) {
        int percent = switch (couponCode == null ? "" : couponCode) {
            case "WELCOME10" -> 10;
            case "BLACKFRIDAY" -> 40;
            case "STUDENT5" -> 5;
            default -> 0;
        };
        return applyDiscount(amount, percent);
    }

    /** Ayni isim, ucuncu imza: indirim tutarina ust sinir. */
    private static BigDecimal applyDiscount(BigDecimal amount, int percent, BigDecimal maxDiscount) {
        BigDecimal discounted = applyDiscount(amount, percent);
        BigDecimal discount = amount.subtract(discounted);
        if (discount.compareTo(maxDiscount) > 0) {
            discount = maxDiscount;
        }
        return amount.subtract(discount);
    }

    /** Varargs: sifir veya daha fazla tutar toplanir. */
    private static BigDecimal sumAll(BigDecimal... amounts) {
        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal amount : amounts) {   // amounts aslinda bir dizidir
            total = total.add(amount);
        }
        return total.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    /** void metot: deger dondurmez, is yapar (ekrana yazar). */
    private static void printReceipt(String productName, BigDecimal unitPrice, int quantity, String coupon) {
        BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
        BigDecimal afterCoupon = (coupon == null) ? lineTotal : applyDiscount(lineTotal, coupon);
        BigDecimal vat = calculateVat(afterCoupon);
        BigDecimal payable = afterCoupon.add(vat);

        System.out.printf("%-16s %d x %9s = %10s | kupon: %-12s | KDV: %8s | ODENECEK: %10s%n",
                productName, quantity, unitPrice, lineTotal,
                coupon == null ? "-" : coupon, vat, payable);
    }

    // ==================================================================
    //  Pass-by-value gosterimi
    // ==================================================================

    private static void tryToChangePrimitive(int value) {
        value = 999;              // sadece yerel kopya degisti
    }

    private static void tryToChangeArrayContent(int[] array) {
        array[0] = 99;            // ayni nesneye yaziyor -> disaridan gorulur
    }

    private static void tryToReassignArray(int[] array) {
        array = new int[]{7, 7};  // yerel degisken artik baska nesneyi gosteriyor
        array[0] = 100;           // disaridaki diziyi etkilemez
    }

    // ==================================================================
    //  Ozyineleme
    // ==================================================================

    /** Birlesik faiz: f(anapara, oran, yil) = f(anapara, oran, yil-1) * (1 + oran) */
    private static BigDecimal compoundInterest(BigDecimal principal, BigDecimal rate, int years) {
        if (years == 0) {                       // DURDURMA KOSULU (base case)
            return principal.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        }
        BigDecimal previous = compoundInterest(principal, rate, years - 1);
        return previous.multiply(BigDecimal.ONE.add(rate)).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
