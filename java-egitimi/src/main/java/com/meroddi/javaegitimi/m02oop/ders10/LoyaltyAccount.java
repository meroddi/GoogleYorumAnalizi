package com.meroddi.javaegitimi.m02oop.ders10;

import java.util.Arrays;

/**
 * DERS 10 - Encapsulation (kapsulleme).
 *
 * Kural: alanlar private, disariya sadece KONTROLLU kapi acilir.
 * Amac "getter/setter yazmak" degil; nesnenin gecersiz duruma dusmesini
 * imkansiz kilmaktir. Bir nesnenin her an dogru olmasi gereken kurallarina
 * "invariant" denir. Burada invariant: puan asla negatif olamaz.
 */
public class LoyaltyAccount {

    private final String customerNo;
    private int points;
    private String[] usedCampaignCodes;   // degistirilebilir (mutable) alan - dikkat!

    public LoyaltyAccount(String customerNo, int initialPoints, String[] usedCampaignCodes) {
        if (initialPoints < 0) {
            throw new IllegalArgumentException("Puan negatif olamaz");
        }
        this.customerNo = customerNo;
        this.points = initialPoints;
        // SAVUNMACI KOPYA: disaridan gelen diziyi dogrudan saklamiyoruz.
        // Saklasaydik, cagiran taraf diziyi degistirerek nesnemizi bozabilirdi.
        this.usedCampaignCodes = Arrays.copyOf(usedCampaignCodes, usedCampaignCodes.length);
    }

    /** Harcama tutarindan puan kazandirir: her 100 TL = 1 puan */
    public void earnFromPurchase(Money amount) {
        if (amount == null || amount.isZero()) {
            return;
        }
        int earned = amount.amount().intValue() / 100;
        this.points += earned;
    }

    /**
     * Puan harcar. Setter yerine ANLAMLI metot: "setPoints(-50)" bir is kurali degildir,
     * "spend(50)" is kuralidir ve kontrol edilebilir.
     */
    public void spend(int amountToSpend) {
        if (amountToSpend <= 0) {
            throw new IllegalArgumentException("Harcanacak puan pozitif olmali");
        }
        if (amountToSpend > points) {
            throw new IllegalStateException(
                    "Yetersiz puan. Istenen: " + amountToSpend + ", mevcut: " + points);
        }
        this.points -= amountToSpend;   // invariant korunur: points asla negatife dusemez
    }

    public String tier() {
        if (points >= 1000) {
            return "PLATIN";
        } else if (points >= 500) {
            return "ALTIN";
        } else if (points >= 100) {
            return "GUMUS";
        }
        return "STANDART";
    }

    public String getCustomerNo() {
        return customerNo;
    }

    public int getPoints() {
        return points;
    }

    /**
     * TEHLIKELI getter: ic diziyi dogrudan verir.
     * Cagiran taraf icerigi degistirirse nesnenin durumu disaridan bozulur.
     * (Ogretmek icin bilerek birakildi.)
     */
    public String[] getUsedCampaignCodesLeaky() {
        return usedCampaignCodes;
    }

    /** DOGRU getter: kopyasini verir, ic durum korunur. */
    public String[] getUsedCampaignCodes() {
        return Arrays.copyOf(usedCampaignCodes, usedCampaignCodes.length);
    }
}
