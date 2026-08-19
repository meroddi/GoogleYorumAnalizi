package com.meroddi.javaegitimi.m02oop.ders09;

import java.time.LocalDate;

/**
 * DERS 09 - Sinif ve nesne.
 *
 * Bir SINIF, kaliptir (blueprint). Bellekte yer kaplamaz.
 * Bir NESNE, o kaliptan uretilmis somut ornektir ve heap'te yasar.
 *
 * "Customer" bir kavramdir; "Ayse Yilmaz, musteri no C-1001" bir nesnedir.
 */
public class Customer {

    // --- ALANLAR (field / instance variable): her nesnenin KENDI kopyasi olur ---
    private String customerNo;
    private String fullName;
    private String email;
    private LocalDate registeredAt;
    private boolean active;

    /**
     * CONSTRUCTOR (yapici metot):
     *  - Sinifla ayni isimde olur
     *  - Donus tipi YAZILMAZ
     *  - Nesne dogarken calisir ve nesneyi GECERLI bir hale getirir
     */
    public Customer(String customerNo, String fullName, String email) {
        // Savunmaci kontrol: gecersiz nesnenin dogmasina izin verme.
        // Bozuk nesneyi sonradan yakalamak, hic dogmasina izin vermemekten cok daha pahalidir.
        if (customerNo == null || customerNo.isBlank()) {
            throw new IllegalArgumentException("Musteri no bos olamaz");
        }
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("Gecersiz e-posta: " + email);
        }
        // 'this' = "su anda olusturulan nesne".
        // Parametre adiyla alan adi ayni oldugunda ayirt etmek icin zorunludur.
        this.customerNo = customerNo;
        this.fullName = fullName;
        this.email = email;
        this.registeredAt = LocalDate.now();
        this.active = true;
    }

    /**
     * CONSTRUCTOR OVERLOADING: kayit tarihi disaridan verilebilsin (veri tabanindan okurken).
     * this(...) ile diger constructor'i cagirmak, dogrulama kodunu tekrarlamaz.
     */
    public Customer(String customerNo, String fullName, String email, LocalDate registeredAt) {
        this(customerNo, fullName, email);
        this.registeredAt = registeredAt;
    }

    // --- DAVRANISLAR (metotlar): nesnenin yapabildikleri ---

    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }

    /** Musterinin ne kadar suredir kayitli oldugu (gun). */
    public long membershipDays() {
        return java.time.temporal.ChronoUnit.DAYS.between(registeredAt, LocalDate.now());
    }

    /** Veriyi disari verirken maskele (Ders 03). */
    public String maskedEmail() {
        int at = email.indexOf('@');
        String name = email.substring(0, at);
        String visible = name.length() <= 2 ? name : name.substring(0, 2);
        return visible + "***" + email.substring(at);
    }

    // --- GETTER'lar: alanlar private, disari kontrollu acilir (Ders 10'da derinlesecek) ---

    public String getCustomerNo() {
        return customerNo;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public LocalDate getRegisteredAt() {
        return registeredAt;
    }

    public boolean isActive() {
        return active;
    }
}
