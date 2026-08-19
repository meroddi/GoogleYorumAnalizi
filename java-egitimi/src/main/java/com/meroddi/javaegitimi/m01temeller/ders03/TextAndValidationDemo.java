package com.meroddi.javaegitimi.m01temeller.ders03;

/**
 * DERS 03 - Operatorler, String'ler ve metin dogrulama.
 *
 * Gercek hayat baglami: bir odeme servisine gelen IBAN'i kabul etmeden once
 * dogrulamak, ve islem sonucunu log satirina yazmak. Ikisi de her backend
 * gelistiricisinin ilk hafta yaptigi islerdendir.
 *
 * Calistirma:
 *   mvn -q exec:java -Dexec.mainClass=com.meroddi.javaegitimi.m01temeller.ders03.TextAndValidationDemo
 */
public class TextAndValidationDemo {

    public static void main(String[] args) {

        // ---------------------------------------------------------------
        // 1) OPERATORLER
        // ---------------------------------------------------------------
        int totalItems = 17;
        int boxCapacity = 5;
        System.out.println("--- Aritmetik ---");
        System.out.println("Dolu kutu sayisi : " + (totalItems / boxCapacity));   // 3 (tam bolme)
        System.out.println("Artan urun       : " + (totalItems % boxCapacity));   // 2 (kalan)
        System.out.println("Gereken kutu     : " + ((totalItems + boxCapacity - 1) / boxCapacity)); // yukari yuvarlama hilesi
        System.out.println();

        int stock = 10;
        stock -= 3;   // stock = stock - 3
        stock += 1;
        System.out.println("--- Atama + karsilastirma + mantik ---");
        System.out.println("Kalan stok : " + stock);

        boolean inStock = stock > 0;
        boolean isActive = true;
        // && ve || "kisa devre" yapar: soldaki sonucu belirlerse sag taraf HIC calismaz.
        // Bu, null kontrolunun temel silahidir: (x != null && x.length() > 0)
        System.out.println("Satilabilir mi : " + (inStock && isActive));
        System.out.println();

        // ---------------------------------------------------------------
        // 2) == vs equals  (Java'daki 1 numarali baslangic hatasi)
        // ---------------------------------------------------------------
        String a = "TR33";
        String b = "TR33";                 // ayni havuzdaki (string pool) nesne
        String c = new String("TR33");     // heap'te YENI nesne

        System.out.println("--- == vs equals ---");
        System.out.println("a == b        : " + (a == b) + "   (ayni adres, havuzdan geldi)");
        System.out.println("a == c        : " + (a == c) + "  (farkli adres!)");
        System.out.println("a.equals(c)   : " + a.equals(c) + "   (icerik esit -> dogru kontrol bu)");
        System.out.println("Kural: nesnelerde HER ZAMAN equals, sadece primitive'lerde ==");
        System.out.println();

        // ---------------------------------------------------------------
        // 3) SIK KULLANILAN String METOTLARI
        // ---------------------------------------------------------------
        String rawInput = "  tr33 0006 1005 1978 6457 8413 26  ";
        String iban = rawInput.trim().replace(" ", "").toUpperCase();

        System.out.println("--- Girdi temizleme ---");
        System.out.println("Ham girdi   : '" + rawInput + "'");
        System.out.println("Temizlenmis : '" + iban + "'  (uzunluk: " + iban.length() + ")");
        System.out.println("Ulke kodu   : " + iban.substring(0, 2));
        System.out.println("Son 4 hane  : " + iban.substring(iban.length() - 4));
        System.out.println("TR ile mi basliyor : " + iban.startsWith("TR"));
        System.out.println();

        // ---------------------------------------------------------------
        // 4) GERCEK DOGRULAMA: IBAN mod-97 kontrolu (ISO 13616)
        // ---------------------------------------------------------------
        System.out.println("--- IBAN dogrulama ---");
        printIbanCheck(iban);
        printIbanCheck("TR330006100519786457841327"); // son hane bozuk -> gecersiz
        printIbanCheck("TR3300061005");               // uzunluk hatali
        System.out.println();

        // ---------------------------------------------------------------
        // 5) MASKELEME - log'a tam IBAN/kart numarasi yazmak KVKK ihlalidir
        // ---------------------------------------------------------------
        System.out.println("--- Maskeleme ---");
        System.out.println("Log'a yazilacak hali : " + mask(iban));
        System.out.println();

        // ---------------------------------------------------------------
        // 6) String IMMUTABLE'dir + StringBuilder
        //    Her "+" yeni bir String nesnesi uretir. Dongude bu, cop toplayiciyi
        //    bogar. Buyuk metin uretimi her zaman StringBuilder ile yapilir.
        // ---------------------------------------------------------------
        int loopCount = 30_000;

        long t1 = System.nanoTime();
        String slow = "";
        for (int i = 0; i < loopCount; i++) {
            slow += "x"; // her adimda YENI String
        }
        long slowMs = (System.nanoTime() - t1) / 1_000_000;

        long t2 = System.nanoTime();
        StringBuilder fast = new StringBuilder();
        for (int i = 0; i < loopCount; i++) {
            fast.append("x"); // ayni tampon uzerinde yazar
        }
        long fastMs = (System.nanoTime() - t2) / 1_000_000;

        System.out.println("--- String + vs StringBuilder (" + loopCount + " adim) ---");
        System.out.println("String  + : " + slowMs + " ms (uzunluk " + slow.length() + ")");
        System.out.println("Builder   : " + fastMs + " ms (uzunluk " + fast.length() + ")");
        System.out.println();

        // ---------------------------------------------------------------
        // 7) BICIMLENDIRME - log satiri ve fis ciktisi
        // ---------------------------------------------------------------
        System.out.println("--- Bicimlendirilmis cikti ---");
        // %-12s : sola yasli 12 karakter, %8.2f : 8 genislik 2 ondalik
        System.out.printf("%-12s %8s %10s%n", "URUN", "ADET", "TUTAR");
        System.out.printf("%-12s %8d %10.2f%n", "Klavye", 2, 749.90);
        System.out.printf("%-12s %8d %10.2f%n", "Monitor", 1, 4299.00);

        String logLine = String.format("[%s] level=%s user=%s iban=%s result=%s",
                "2026-08-19T10:15:30", "INFO", "u-1042", mask(iban), "VALID");
        System.out.println(logLine);

        // Text block (Java 15+): cok satirli metni okunakli yazmak icin
        String mail = """
                Sayin musterimiz,
                %s numarali hesabiniz dogrulanmistir.
                Iyi gunler dileriz.""".formatted(mask(iban));
        System.out.println();
        System.out.println(mail);
    }

    /** IBAN'i dogrular ve sonucu ekrana yazar. */
    private static void printIbanCheck(String iban) {
        boolean valid = isValidTurkishIban(iban);
        System.out.println(mask(iban) + " -> " + (valid ? "GECERLI" : "GECERSIZ"));
    }

    /**
     * TR IBAN dogrulama:
     * 1) 26 karakter ve "TR" ile baslamali
     * 2) Ilk 4 karakter sona tasinir
     * 3) Harfler sayiya cevrilir (A=10, B=11, ... Z=35)
     * 4) Olusan dev sayinin 97'ye bolumunden kalan 1 olmali
     */
    private static boolean isValidTurkishIban(String iban) {
        if (iban == null || iban.length() != 26 || !iban.startsWith("TR")) {
            return false;
        }
        String rearranged = iban.substring(4) + iban.substring(0, 4);

        // Sayi 26 haneden uzun oldugu icin long'a sigmaz.
        // Kalani adim adim tasiyarak hesapliyoruz (modular aritmetik).
        long remainder = 0;
        for (int i = 0; i < rearranged.length(); i++) {
            char ch = rearranged.charAt(i);
            int value;
            if (ch >= '0' && ch <= '9') {
                value = ch - '0';                 // karakterden sayiya
                remainder = (remainder * 10 + value) % 97;
            } else if (ch >= 'A' && ch <= 'Z') {
                value = ch - 'A' + 10;            // A=10 ... Z=35 (iki basamak)
                remainder = (remainder * 100 + value) % 97;
            } else {
                return false;                     // gecersiz karakter
            }
        }
        return remainder == 1;
    }

    /** Hassas veriyi log'a yazmadan once maskeler: TR33 **** **** 3126 */
    private static String mask(String value) {
        if (value == null || value.length() < 8) {
            return "****";
        }
        return value.substring(0, 4) + " **** **** " + value.substring(value.length() - 4);
    }
}
