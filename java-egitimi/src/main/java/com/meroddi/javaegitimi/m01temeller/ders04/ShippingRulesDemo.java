package com.meroddi.javaegitimi.m01temeller.ders04;

import java.math.BigDecimal;

/**
 * DERS 04 - Karar yapilari: if / else if / else, ternary, modern switch.
 *
 * Gercek hayat baglami: her e-ticaret sisteminde bir "kural motoru" vardir.
 * Kargo ucreti, indirim, risk skoru... Bunlarin tamami if/switch ile baslar.
 * Kodun okunabilirligi burada belirlenir: ic ice 5 if yazan kisi 6 ay sonra
 * kendi kodunu anlamaz.
 *
 * Calistirma:
 *   mvn -q exec:java -Dexec.mainClass=com.meroddi.javaegitimi.m01temeller.ders04.ShippingRulesDemo
 */
public class ShippingRulesDemo {

    public static void main(String[] args) {

        // ---------------------------------------------------------------
        // 1) KARGO UCRETI - klasik if / else if / else zinciri
        // ---------------------------------------------------------------
        System.out.println("--- Kargo ucreti kurallari ---");
        printShipping(new BigDecimal("120.00"), "STANDART", 1.2);
        printShipping(new BigDecimal("450.00"), "STANDART", 1.2);
        printShipping(new BigDecimal("450.00"), "EXPRESS", 1.2);
        printShipping(new BigDecimal("900.00"), "STANDART", 18.0);
        System.out.println();

        // ---------------------------------------------------------------
        // 2) TERNARY - tek satirlik if/else. Sadece BASIT durumlarda kullanilir.
        // ---------------------------------------------------------------
        int stock = 0;
        String label = stock > 0 ? "Stokta" : "Tukendi";
        System.out.println("--- Ternary ---");
        System.out.println("Stok durumu : " + label);
        System.out.println("Ic ice ternary yazma; okunmuyor. 2 dalda kalmiyorsa if kullan.");
        System.out.println();

        // ---------------------------------------------------------------
        // 3) MODERN SWITCH (Java 14+) - "->" bicimi.
        //    Eski switch'te break unutmak = fall-through bug'i. Yeni bicimde yok.
        // ---------------------------------------------------------------
        System.out.println("--- Siparis durumu -> musteriye gosterilecek metin ---");
        for (String status : new String[]{"CREATED", "PAID", "SHIPPED", "CANCELLED", "UNKNOWN"}) {
            System.out.printf("%-10s : %s%n", status, describeStatus(status));
        }
        System.out.println();

        // ---------------------------------------------------------------
        // 4) SWITCH ILE DEGER URETME (switch expression)
        // ---------------------------------------------------------------
        System.out.println("--- Odeme yontemine gore komisyon orani ---");
        for (String method : new String[]{"CREDIT_CARD", "BANK_TRANSFER", "CASH_ON_DELIVERY"}) {
            System.out.printf("%-18s : %%%s%n", method, commissionRate(method));
        }
        System.out.println();

        // ---------------------------------------------------------------
        // 5) RISK SKORU - kurallari "erken cikis" (guard clause) ile yazmak
        //    Ic ice if yerine, sartlar saglanmiyorsa erkenden don. Kod duz kalir.
        // ---------------------------------------------------------------
        System.out.println("--- Odeme risk skoru ---");
        printRisk(new BigDecimal("50.00"), 12, false, "TR");
        printRisk(new BigDecimal("25000.00"), 1, false, "TR");
        printRisk(new BigDecimal("8000.00"), 0, true, "RU");
        System.out.println();

        // ---------------------------------------------------------------
        // 6) TUZAK: kayan noktali sayilari == ile karsilastirmak
        // ---------------------------------------------------------------
        double computed = 0.1 + 0.2;
        System.out.println("--- Kayan nokta karsilastirmasi ---");
        System.out.println("(0.1 + 0.2) == 0.3          : " + (computed == 0.3) + "  <-- guvenilmez");
        System.out.println("Math.abs(fark) < 0.000001   : " + (Math.abs(computed - 0.3) < 0.000001));
        System.out.println("Parada zaten BigDecimal.compareTo kullanilir (Ders 02).");
    }

    /**
     * Kargo ucreti kurali:
     *  - 15 kg uzeri  -> agir kargo, sabit 149.90
     *  - EXPRESS      -> 79.90
     *  - 300 TL uzeri -> ucretsiz
     *  - diger        -> 39.90
     * Sira onemlidir: ilk eslesen kural kazanir.
     */
    private static BigDecimal shippingFee(BigDecimal cartTotal, String shippingType, double weightKg) {
        if (weightKg > 15.0) {
            return new BigDecimal("149.90");
        } else if ("EXPRESS".equals(shippingType)) {   // sabit deger solda: null gelse bile patlamaz
            return new BigDecimal("79.90");
        } else if (cartTotal.compareTo(new BigDecimal("300.00")) >= 0) {
            return BigDecimal.ZERO;
        } else {
            return new BigDecimal("39.90");
        }
    }

    private static void printShipping(BigDecimal cartTotal, String type, double weightKg) {
        BigDecimal fee = shippingFee(cartTotal, type, weightKg);
        String feeText = fee.compareTo(BigDecimal.ZERO) == 0 ? "UCRETSIZ" : fee + " TL";
        System.out.printf("Sepet %8s TL | %-8s | %5.1f kg -> kargo: %s%n",
                cartTotal, type, weightKg, feeText);
    }

    /** Modern switch: her dal bir deger dondurur, break gerekmez. */
    private static String describeStatus(String status) {
        return switch (status) {
            case "CREATED" -> "Siparisiniz alindi";
            case "PAID" -> "Odemeniz onaylandi";
            case "SHIPPED", "IN_TRANSIT" -> "Kargoya verildi";   // birden fazla etiket tek dalda
            case "DELIVERED" -> "Teslim edildi";
            case "CANCELLED" -> "Siparis iptal edildi";
            default -> "Bilinmeyen durum";                        // default olmazsa derlenmez
        };
    }

    private static BigDecimal commissionRate(String paymentMethod) {
        return switch (paymentMethod) {
            case "CREDIT_CARD" -> new BigDecimal("2.30");
            case "BANK_TRANSFER" -> new BigDecimal("0.50");
            case "CASH_ON_DELIVERY" -> new BigDecimal("3.75");
            default -> BigDecimal.ZERO;
        };
    }

    /**
     * Risk skoru: 0-100. Kurallar birikimli.
     * Guard clause ornegi: gecersiz girdi en basta elenir.
     */
    private static int riskScore(BigDecimal amount, int pastOrderCount, boolean newDevice, String country) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return 100; // anlamsiz tutar -> en yuksek risk
        }

        int score = 0;
        if (amount.compareTo(new BigDecimal("10000")) > 0) {
            score += 40;
        } else if (amount.compareTo(new BigDecimal("2000")) > 0) {
            score += 15;
        }
        if (pastOrderCount == 0) {
            score += 25;
        } else if (pastOrderCount < 3) {
            score += 10;
        }
        if (newDevice) {
            score += 20;
        }
        if (!"TR".equals(country)) {
            score += 15;
        }
        return Math.min(score, 100); // ust siniri asma
    }

    private static void printRisk(BigDecimal amount, int pastOrders, boolean newDevice, String country) {
        int score = riskScore(amount, pastOrders, newDevice, country);
        String decision = switch (score / 25) {      // 0-24 / 25-49 / 50-74 / 75-100
            case 0 -> "OTOMATIK ONAY";
            case 1 -> "ONAY (izlemede)";
            case 2 -> "MANUEL INCELEME";
            default -> "RED";
        };
        System.out.printf("%9s TL | gecmis siparis: %2d | yeni cihaz: %-5b | ulke: %s -> skor %3d -> %s%n",
                amount, pastOrders, newDevice, country, score, decision);
    }
}
