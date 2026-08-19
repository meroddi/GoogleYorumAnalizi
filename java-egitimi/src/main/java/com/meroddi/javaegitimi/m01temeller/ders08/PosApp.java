package com.meroddi.javaegitimi.m01temeller.ders08;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Scanner;

/**
 * DERS 08 - MODUL 1 MINI PROJESI: Konsol POS (kasa) uygulamasi.
 *
 * Modul 1'de ogrenilen her sey burada bir arada:
 *   degiskenler ve tipler (Ders 02), String islemleri (Ders 03),
 *   if/switch (Ders 04), donguler (Ders 05), diziler (Ders 06), metotlar (Ders 07).
 *
 * Yol haritasinin kurali: "bitmis proje -> %60 kalicilik". Bu yuzden modul
 * calisan bir programla bitiyor.
 *
 * Calistirma (etkilesimli):
 *   mvn -q exec:java -Dexec.mainClass=com.meroddi.javaegitimi.m01temeller.ders08.PosApp
 *
 * Calistirma (girdiyi borudan vererek, otomatik test gibi):
 *   printf "1\n1\n2\n1\n3\n1\n4\n5\n" | mvn -q exec:java \
 *     -Dexec.mainClass=com.meroddi.javaegitimi.m01temeller.ders08.PosApp
 */
public class PosApp {

    // --- Urun katalogu: paralel diziler (Ders 06) ---
    private static final String[] PRODUCT_CODES = {"KLV-001", "MNT-014", "MSE-220", "KLK-077"};
    private static final String[] PRODUCT_NAMES = {"Mekanik Klavye", "Monitor 27\"", "Kablosuz Mouse", "Kulaklik"};
    private static final BigDecimal[] PRODUCT_PRICES = {
            new BigDecimal("2500.00"), new BigDecimal("4299.00"),
            new BigDecimal("289.50"), new BigDecimal("1150.00")
    };
    private static final int[] STOCK = {10, 4, 25, 8};

    private static final BigDecimal VAT_RATE = new BigDecimal("0.20");
    private static final int MAX_LINES = 20;

    // --- Acik fisin satirlari ---
    private static final int[] cartProductIndex = new int[MAX_LINES];
    private static final int[] cartQuantity = new int[MAX_LINES];
    private static int cartLineCount = 0;

    // --- Gun sonu ozeti ---
    private static int receiptCount = 0;
    private static BigDecimal dailyTotal = BigDecimal.ZERO;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        printHeader();

        boolean running = true;
        while (running) {
            printMenu();
            String choice = readLine(scanner);

            if (choice == null) {            // girdi bitti (boru ile calistirildi)
                System.out.println("\nGirdi akisi bitti, kasa kapatiliyor.");
                break;
            }

            switch (choice.trim()) {
                case "1" -> listProducts();
                case "2" -> addToCart(scanner);
                case "3" -> showCart();
                case "4" -> checkout();
                case "5" -> {
                    printDailySummary();
                    running = false;
                }
                case "" -> { /* bos satiri yok say */ }
                default -> System.out.println(">> Gecersiz secim: " + choice);
            }
        }
        scanner.close();
    }

    // ==================================================================
    //  Ekran
    // ==================================================================

    private static void printHeader() {
        System.out.println("""
                ==================================================
                            MERODDI POS - Kasa Ekrani
                ==================================================""");
    }

    private static void printMenu() {
        System.out.println();
        System.out.println("1) Urunleri listele");
        System.out.println("2) Fise urun ekle");
        System.out.println("3) Acik fisi goster");
        System.out.println("4) Fisi kapat (odeme)");
        System.out.println("5) Gun sonu ve cikis");
        System.out.print("Secim: ");
    }

    private static void listProducts() {
        System.out.println();
        System.out.printf("%-4s %-10s %-18s %10s %8s%n", "NO", "KOD", "URUN", "FIYAT", "STOK");
        for (int i = 0; i < PRODUCT_CODES.length; i++) {
            System.out.printf("%-4d %-10s %-18s %10s %8d%n",
                    i + 1, PRODUCT_CODES[i], PRODUCT_NAMES[i], PRODUCT_PRICES[i], STOCK[i]);
        }
    }

    // ==================================================================
    //  Fis islemleri
    // ==================================================================

    private static void addToCart(Scanner scanner) {
        listProducts();
        System.out.print("Urun no: ");
        Integer productNo = readInt(scanner);
        if (productNo == null) {
            return;
        }
        int index = productNo - 1;
        if (index < 0 || index >= PRODUCT_CODES.length) {   // Ders 06: sinir kontrolu
            System.out.println(">> Boyle bir urun yok.");
            return;
        }

        System.out.print("Adet: ");
        Integer quantity = readInt(scanner);
        if (quantity == null) {
            return;
        }
        if (quantity <= 0) {
            System.out.println(">> Adet 0'dan buyuk olmali.");
            return;
        }
        if (quantity > STOCK[index]) {
            System.out.println(">> Yetersiz stok. Mevcut: " + STOCK[index]);
            return;
        }
        if (cartLineCount >= MAX_LINES) {
            System.out.println(">> Fis satir siniri doldu, once fisi kapatin.");
            return;
        }

        cartProductIndex[cartLineCount] = index;
        cartQuantity[cartLineCount] = quantity;
        cartLineCount++;
        STOCK[index] -= quantity;                            // stok rezerve edildi

        System.out.printf(">> Eklendi: %s x %d%n", PRODUCT_NAMES[index], quantity);
    }

    private static void showCart() {
        System.out.println();
        if (cartLineCount == 0) {
            System.out.println("Fis bos.");
            return;
        }
        System.out.printf("%-18s %6s %12s %12s%n", "URUN", "ADET", "BIRIM", "TUTAR");
        for (int i = 0; i < cartLineCount; i++) {
            int p = cartProductIndex[i];
            BigDecimal lineTotal = lineTotal(i);
            System.out.printf("%-18s %6d %12s %12s%n",
                    PRODUCT_NAMES[p], cartQuantity[i], PRODUCT_PRICES[p], lineTotal);
        }
        BigDecimal subtotal = subtotal();
        System.out.printf("%-18s %6s %12s %12s%n", "ARA TOPLAM", "", "", subtotal);
    }

    private static void checkout() {
        if (cartLineCount == 0) {
            System.out.println(">> Bos fis kapatilamaz.");
            return;
        }

        BigDecimal subtotal = subtotal();
        BigDecimal discount = calculateDiscount(subtotal);
        BigDecimal afterDiscount = subtotal.subtract(discount);
        BigDecimal vat = afterDiscount.multiply(VAT_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal payable = afterDiscount.add(vat);

        System.out.println();
        System.out.println("--------------- FIS ---------------");
        for (int i = 0; i < cartLineCount; i++) {
            int p = cartProductIndex[i];
            System.out.printf("%-18s %3d x %9s = %10s%n",
                    PRODUCT_NAMES[p], cartQuantity[i], PRODUCT_PRICES[p], lineTotal(i));
        }
        System.out.println("-----------------------------------");
        System.out.printf("%-24s %12s%n", "Ara toplam", subtotal);
        System.out.printf("%-24s %12s  (%s)%n", "Indirim", discount, discountReason(subtotal));
        System.out.printf("%-24s %12s%n", "KDV %20", vat);
        System.out.printf("%-24s %12s%n", "ODENECEK", payable);
        System.out.println("-----------------------------------");

        receiptCount++;
        dailyTotal = dailyTotal.add(payable);
        cartLineCount = 0;                  // fisi sifirla
        System.out.println(">> Odeme alindi, fis kapatildi.");
    }

    // ==================================================================
    //  Hesaplama metotlari (Ders 07: tek isli, test edilebilir metotlar)
    // ==================================================================

    private static BigDecimal lineTotal(int line) {
        int p = cartProductIndex[line];
        return PRODUCT_PRICES[p].multiply(BigDecimal.valueOf(cartQuantity[line]));
    }

    private static BigDecimal subtotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < cartLineCount; i++) {
            total = total.add(lineTotal(i));
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    /** Kademeli indirim kurali (Ders 04). */
    private static BigDecimal calculateDiscount(BigDecimal subtotal) {
        BigDecimal rate;
        if (subtotal.compareTo(new BigDecimal("10000")) >= 0) {
            rate = new BigDecimal("0.10");
        } else if (subtotal.compareTo(new BigDecimal("5000")) >= 0) {
            rate = new BigDecimal("0.05");
        } else {
            rate = BigDecimal.ZERO;
        }
        return subtotal.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }

    private static String discountReason(BigDecimal subtotal) {
        if (subtotal.compareTo(new BigDecimal("10000")) >= 0) {
            return "10000 TL ustu %10";
        } else if (subtotal.compareTo(new BigDecimal("5000")) >= 0) {
            return "5000 TL ustu %5";
        }
        return "indirim yok";
    }

    private static void printDailySummary() {
        System.out.println();
        System.out.println("============ GUN SONU ============");
        System.out.println("Kapatilan fis sayisi : " + receiptCount);
        System.out.println("Toplam ciro          : " + dailyTotal.setScale(2, RoundingMode.HALF_UP) + " TL");
        if (receiptCount > 0) {
            BigDecimal average = dailyTotal.divide(BigDecimal.valueOf(receiptCount), 2, RoundingMode.HALF_UP);
            System.out.println("Ortalama sepet       : " + average + " TL");
        }
        System.out.println("Kalan stoklar:");
        for (int i = 0; i < PRODUCT_CODES.length; i++) {
            System.out.printf("  %-10s %-18s %4d adet%n", PRODUCT_CODES[i], PRODUCT_NAMES[i], STOCK[i]);
        }
        System.out.println("==================================");
    }

    // ==================================================================
    //  Girdi okuma - savunmaci programlama
    //  Kullanicidan gelen her sey yanlis olabilir; program cokmemeli.
    // ==================================================================

    /** Bir satir okur; girdi bittiyse null doner. */
    private static String readLine(Scanner scanner) {
        if (!scanner.hasNextLine()) {
            return null;
        }
        return scanner.nextLine();
    }

    /** Sayi okur; gecersizse uyarir ve null doner (program cokmez). */
    private static Integer readInt(Scanner scanner) {
        String line = readLine(scanner);
        if (line == null) {
            return null;
        }
        try {
            return Integer.parseInt(line.trim());
        } catch (NumberFormatException e) {
            System.out.println(">> Sayi bekleniyordu, '" + line.trim() + "' girildi.");
            return null;
        }
    }
}
