package com.meroddi.javaegitimi.m01temeller.ders06;

import java.util.Arrays;

/**
 * DERS 06 - Diziler (array), cok boyutlu diziler, Arrays yardimcilari.
 *
 * Gercek hayat baglami: depo stok sayimi ve fiyat analizi. Dizi, Java'daki en
 * ilkel veri yapisidir; ArrayList dahil bircok koleksiyon iceride dizi kullanir.
 * Bu yuzden "kalici cekirdek" tarafinda dizinin nasil calistigini bilmek sarttir.
 *
 * Calistirma:
 *   mvn -q exec:java -Dexec.mainClass=com.meroddi.javaegitimi.m01temeller.ders06.StockArrayDemo
 */
public class StockArrayDemo {

    public static void main(String[] args) {

        // ---------------------------------------------------------------
        // 1) DIZI OLUSTURMA - boyut sabittir, sonradan buyutulemez
        // ---------------------------------------------------------------
        int[] emptyStock = new int[5];              // hepsi 0 ile dolar
        String[] skus = {"KLV-001", "MNT-014", "MSE-220", "KLK-077", "WEB-450"};
        int[] stockCounts = {42, 7, 0, 130, 15};
        double[] prices = {749.90, 4299.00, 289.50, 1150.00, 899.00};

        System.out.println("--- Dizi temelleri ---");
        System.out.println("Varsayilan degerler : " + Arrays.toString(emptyStock));
        System.out.println("Dizi boyutu         : " + skus.length + "  (metot degil, alan: .length)");
        System.out.println("Ilk eleman  skus[0] : " + skus[0]);
        System.out.println("Son eleman          : " + skus[skus.length - 1]);
        System.out.println();

        // ---------------------------------------------------------------
        // 2) SINIR HATASI - uretimdeki en sik gorulen exception'lardan
        // ---------------------------------------------------------------
        System.out.println("--- Sinir disi erisim ---");
        try {
            System.out.println(skus[5]); // gecerli indeksler 0..4
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("Hata yakalandi -> " + e.getMessage());
            System.out.println("Kural: indeks her zaman 0 ile length-1 arasindadir.");
        }
        System.out.println();

        // ---------------------------------------------------------------
        // 3) STOK RAPORU - dizileri paralel gezmek
        // ---------------------------------------------------------------
        System.out.println("--- Depo stok raporu ---");
        System.out.printf("%-10s %8s %10s %14s %s%n", "SKU", "ADET", "FIYAT", "STOK DEGERI", "DURUM");

        double totalValue = 0.0;
        int outOfStock = 0;
        int criticalStock = 0;

        for (int i = 0; i < skus.length; i++) {
            double lineValue = stockCounts[i] * prices[i];
            totalValue += lineValue;

            String status;
            if (stockCounts[i] == 0) {
                status = "TUKENDI";
                outOfStock++;
            } else if (stockCounts[i] < 10) {
                status = "KRITIK";
                criticalStock++;
            } else {
                status = "NORMAL";
            }

            System.out.printf("%-10s %8d %10.2f %14.2f %s%n",
                    skus[i], stockCounts[i], prices[i], lineValue, status);
        }
        System.out.printf("%nToplam stok degeri : %.2f TL%n", totalValue);
        System.out.println("Tukenen urun       : " + outOfStock);
        System.out.println("Kritik seviye      : " + criticalStock);
        System.out.println();

        // ---------------------------------------------------------------
        // 4) MIN / MAX / ORTALAMA - tek gecisle (single pass)
        // ---------------------------------------------------------------
        int maxIndex = 0;
        int minIndex = 0;
        int sum = 0;
        for (int i = 0; i < stockCounts.length; i++) {
            if (stockCounts[i] > stockCounts[maxIndex]) {
                maxIndex = i;
            }
            if (stockCounts[i] < stockCounts[minIndex]) {
                minIndex = i;
            }
            sum += stockCounts[i];
        }
        System.out.println("--- Tek gecisli analiz ---");
        System.out.println("En cok stok  : " + skus[maxIndex] + " (" + stockCounts[maxIndex] + " adet)");
        System.out.println("En az stok   : " + skus[minIndex] + " (" + stockCounts[minIndex] + " adet)");
        System.out.printf("Ortalama     : %.2f adet%n", (double) sum / stockCounts.length);
        System.out.println();

        // ---------------------------------------------------------------
        // 5) ARRAYS SINIFI - kopyalama, siralama, arama, doldurma
        //    Dikkat: sort ORIJINAL diziyi degistirir. Once kopyala.
        // ---------------------------------------------------------------
        int[] sorted = Arrays.copyOf(stockCounts, stockCounts.length);
        Arrays.sort(sorted);

        System.out.println("--- Arrays yardimcilari ---");
        System.out.println("Orijinal : " + Arrays.toString(stockCounts));
        System.out.println("Sirali   : " + Arrays.toString(sorted));
        // binarySearch SADECE sirali dizide dogru calisir - sirasiz dizide cop deger doner
        System.out.println("15 degerinin sirali dizideki yeri : " + Arrays.binarySearch(sorted, 15));
        System.out.println("Ilk 3 eleman : " + Arrays.toString(Arrays.copyOfRange(stockCounts, 0, 3)));

        int[] reserved = new int[5];
        Arrays.fill(reserved, 2);   // her urunden 2 adet rezerve
        System.out.println("Rezerve  : " + Arrays.toString(reserved));
        System.out.println();

        // ---------------------------------------------------------------
        // 6) DIZI KOPYALAMA TUZAGI - '=' kopyalamaz, ayni diziyi paylasir
        // ---------------------------------------------------------------
        int[] alias = stockCounts;                                   // AYNI dizi
        int[] copy = Arrays.copyOf(stockCounts, stockCounts.length); // GERCEK kopya
        alias[0] = 999;

        System.out.println("--- Referans vs kopya ---");
        System.out.println("stockCounts[0] : " + stockCounts[0] + "  <-- alias uzerinden degisti");
        System.out.println("copy[0]        : " + copy[0] + "   <-- kopya etkilenmedi");
        stockCounts[0] = 42; // geri al
        System.out.println();

        // ---------------------------------------------------------------
        // 7) COK BOYUTLU DIZI - "dizilerin dizisi"
        //    Ornek: 3 depo x 5 urun stok matrisi
        // ---------------------------------------------------------------
        String[] warehouses = {"Istanbul", "Ankara", "Izmir"};
        int[][] matrix = {
                {20, 3, 0, 60, 5},
                {12, 2, 0, 40, 7},
                {10, 2, 0, 30, 3}
        };

        System.out.println("--- Depo x Urun stok matrisi ---");
        System.out.printf("%-10s", "DEPO");
        for (String sku : skus) {
            System.out.printf("%10s", sku);
        }
        System.out.printf("%9s%n", "TOPLAM");

        int[] perProductTotal = new int[skus.length];
        for (int w = 0; w < warehouses.length; w++) {
            int rowTotal = 0;
            System.out.printf("%-10s", warehouses[w]);
            for (int p = 0; p < skus.length; p++) {
                System.out.printf("%10d", matrix[w][p]);
                rowTotal += matrix[w][p];
                perProductTotal[p] += matrix[w][p];
            }
            System.out.printf("%9d%n", rowTotal);
        }
        System.out.printf("%-10s", "TOPLAM");
        for (int total : perProductTotal) {
            System.out.printf("%10d", total);
        }
        System.out.println();
        System.out.println();

        System.out.println("--- Not ---");
        System.out.println("Dizinin boyutu sabittir. Eleman ekleyip cikarabilmen gerekiyorsa");
        System.out.println("ArrayList / HashMap kullanilir - Collections konusu (Modul 3).");
    }
}
