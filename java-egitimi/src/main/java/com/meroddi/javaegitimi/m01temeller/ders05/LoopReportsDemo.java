package com.meroddi.javaegitimi.m01temeller.ders05;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * DERS 05 - Donguler: for, while, do-while, for-each, break/continue.
 *
 * Gercek hayat baglami: raporlama. Backend'de yazdigin kodun buyuk kismi
 * "bir listeyi bastan sona gez, bir seyler hesapla, sonucu dondur" bicimindedir.
 * Odeme taksitlendirme, gunluk ciro raporu, toplu e-posta gonderimi...
 *
 * Calistirma:
 *   mvn -q exec:java -Dexec.mainClass=com.meroddi.javaegitimi.m01temeller.ders05.LoopReportsDemo
 */
public class LoopReportsDemo {

    public static void main(String[] args) {

        // ---------------------------------------------------------------
        // 1) FOR - sayaci sen yonetirsin: baslangic; sart; adim
        // ---------------------------------------------------------------
        System.out.println("--- Taksit tablosu (for) ---");
        BigDecimal amount = new BigDecimal("12000.00");
        int installments = 6;
        BigDecimal monthly = amount.divide(BigDecimal.valueOf(installments), 2, RoundingMode.HALF_UP);
        BigDecimal remaining = amount;

        System.out.printf("%-8s %12s %14s%n", "TAKSIT", "TUTAR", "KALAN BORC");
        for (int i = 1; i <= installments; i++) {
            // Son taksit, yuvarlamadan kalan kurusu ustlenir (gercek hayatta boyle yapilir)
            BigDecimal current = (i == installments) ? remaining : monthly;
            remaining = remaining.subtract(current);
            System.out.printf("%-8d %12s %14s%n", i, current, remaining);
        }
        System.out.println();

        // ---------------------------------------------------------------
        // 2) FOR-EACH - indeksle isin yoksa her zaman bunu tercih et
        // ---------------------------------------------------------------
        String[] productNames = {"Klavye", "Monitor", "Mouse", "Kulaklik"};
        int[] quantities = {2, 1, 3, 1};
        double[] unitPrices = {749.90, 4299.00, 289.50, 1150.00};

        System.out.println("--- Sepet (for-each) ---");
        for (String name : productNames) {
            System.out.println("  urun: " + name);
        }
        System.out.println();

        // ---------------------------------------------------------------
        // 3) BIRIKTIRME (accumulator) - toplama/sayma isinin standart kalibi
        // ---------------------------------------------------------------
        double cartTotal = 0.0;
        int itemCount = 0;
        for (int i = 0; i < productNames.length; i++) {
            double lineTotal = quantities[i] * unitPrices[i];
            cartTotal += lineTotal;
            itemCount += quantities[i];
            System.out.printf("%-10s %2d x %8.2f = %10.2f%n",
                    productNames[i], quantities[i], unitPrices[i], lineTotal);
        }
        System.out.printf("%-10s %2s   %8s = %10.2f  (%d parca)%n", "TOPLAM", "", "", cartTotal, itemCount);
        System.out.println();

        // ---------------------------------------------------------------
        // 4) WHILE - kac kez donecegini bilmiyorsan
        //    Ornek: sayfa sayfa veri cekme (pagination) - gercek API entegrasyonlarinin kalibi
        // ---------------------------------------------------------------
        System.out.println("--- Sayfalama (while) ---");
        int totalRecords = 47;
        int pageSize = 10;
        int page = 0;
        int processed = 0;
        while (processed < totalRecords) {
            int batch = Math.min(pageSize, totalRecords - processed);
            page++;
            processed += batch;
            System.out.printf("Sayfa %d cekildi: %2d kayit (toplam %2d/%d)%n",
                    page, batch, processed, totalRecords);
        }
        System.out.println();

        // ---------------------------------------------------------------
        // 5) DO-WHILE - govde EN AZ BIR KEZ calisir
        //    Ornek: yeniden deneme (retry) mantigi
        // ---------------------------------------------------------------
        System.out.println("--- Retry (do-while) ---");
        int attempt = 0;
        boolean success = false;
        do {
            attempt++;
            success = attempt >= 3;                     // 3. denemede basarili oluyor gibi davran
            long backoffMs = (long) Math.pow(2, attempt) * 100; // 200, 400, 800 ms
            System.out.printf("Deneme %d -> %s (bir sonraki bekleme: %d ms)%n",
                    attempt, success ? "BASARILI" : "HATA", backoffMs);
        } while (!success && attempt < 5);
        System.out.println();

        // ---------------------------------------------------------------
        // 6) BREAK ve CONTINUE
        // ---------------------------------------------------------------
        System.out.println("--- break / continue ---");
        int[] paymentAmounts = {150, -20, 890, 0, 4500, 12000, 300};
        int validCount = 0;
        int sum = 0;
        for (int value : paymentAmounts) {
            if (value <= 0) {
                continue;                                // gecersiz kaydi ATLA, donguye devam
            }
            if (sum + value > 10000) {
                System.out.println("Gunluk limit asilacakti, islem durduruldu: " + value);
                break;                                   // donguyu tamamen BITIR
            }
            sum += value;
            validCount++;
        }
        System.out.println("Islenen odeme sayisi: " + validCount + ", toplam: " + sum + " TL");
        System.out.println();

        // ---------------------------------------------------------------
        // 7) IC ICE DONGU - matris/rapor uretimi
        // ---------------------------------------------------------------
        System.out.println("--- Sube x Gun ciro matrisi (ic ice for) ---");
        String[] branches = {"Kadikoy", "Besiktas", "Uskudar"};
        int[][] dailySales = {
                {1200, 1500, 900, 1750, 2100},
                {800, 950, 1100, 1050, 1600},
                {600, 720, 640, 810, 990}
        };
        String[] days = {"Pzt", "Sal", "Car", "Per", "Cum"};

        System.out.printf("%-10s", "SUBE");
        for (String day : days) {
            System.out.printf("%7s", day);
        }
        System.out.printf("%9s%n", "TOPLAM");

        int grandTotal = 0;
        for (int b = 0; b < branches.length; b++) {
            int branchTotal = 0;
            System.out.printf("%-10s", branches[b]);
            for (int d = 0; d < days.length; d++) {
                System.out.printf("%7d", dailySales[b][d]);
                branchTotal += dailySales[b][d];
            }
            grandTotal += branchTotal;
            System.out.printf("%9d%n", branchTotal);
        }
        System.out.printf("%-10s%35s %8d%n", "GENEL", "", grandTotal);
        System.out.println();

        // ---------------------------------------------------------------
        // 8) TUZAKLAR
        // ---------------------------------------------------------------
        System.out.println("--- Tuzaklar ---");
        System.out.println("1) Sonsuz dongu: sayaci artirmayi unutmak (while(i<10) icinde i++ yoksa).");
        System.out.println("2) Off-by-one: i <= dizi.length yazmak -> ArrayIndexOutOfBoundsException.");
        System.out.println("3) Dongu icinde veritabanina sorgu atmak (N+1 problemi) - Yil 2'nin en sik incident'i.");
        System.out.println("4) Dongu icinde String'i + ile birlestirmek (Ders 03).");
    }
}
