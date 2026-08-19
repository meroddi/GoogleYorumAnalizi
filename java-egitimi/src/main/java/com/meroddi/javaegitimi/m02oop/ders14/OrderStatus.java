package com.meroddi.javaegitimi.m02oop.ders14;

import java.util.EnumSet;
import java.util.Set;

/**
 * DERS 14 - Enum: sabit bir kumeyi tip guvenli sekilde ifade etmek.
 *
 * Neden String yerine enum?
 *   String status = "PAİD";   // yazim hatasi, derleyici goremez, uretimde patlar
 *   OrderStatus.PAID          // yanlis yazarsan DERLENMEZ
 *
 * Ustelik enum sadece etiket degildir: davranis tasiyabilir. Durum gecis
 * kurallarini (state machine) burada tutmak, kurallarin kodun 10 farkli
 * yerine dagilmasini onler.
 */
public enum OrderStatus {

    // Her sabit, enum sinifinin bir NESNESIDIR. Parantez icindekiler constructor'a gider.
    CREATED("Siparis alindi", false),
    PAID("Odeme tamamlandi", false),
    SHIPPED("Kargoya verildi", false),
    DELIVERED("Teslim edildi", true),
    CANCELLED("Iptal edildi", true),
    REFUNDED("Iade edildi", true);

    private final String label;
    private final boolean terminal;   // bu durumdan cikis var mi?

    /** Enum constructor'i her zaman private'dir; disaridan yeni sabit uretilemez. */
    OrderStatus(String label, boolean terminal) {
        this.label = label;
        this.terminal = terminal;
    }

    public String getLabel() {
        return label;
    }

    public boolean isTerminal() {
        return terminal;
    }

    /**
     * Bu durumdan gecilebilecek durumlar.
     * EnumSet: enum'lar icin ozel, cok hizli ve az bellek kullanan kume.
     */
    public Set<OrderStatus> allowedTransitions() {
        return switch (this) {
            case CREATED -> EnumSet.of(PAID, CANCELLED);
            case PAID -> EnumSet.of(SHIPPED, REFUNDED, CANCELLED);
            case SHIPPED -> EnumSet.of(DELIVERED);
            case DELIVERED -> EnumSet.of(REFUNDED);
            case CANCELLED, REFUNDED -> EnumSet.noneOf(OrderStatus.class);
        };
        // Not: yeni bir durum eklersen derleyici bu switch'te eksik dal oldugunu soyler.
        // String kullansaydin hicbir uyari almazdin. Enum'un asil degeri budur.
    }

    public boolean canTransitionTo(OrderStatus target) {
        return allowedTransitions().contains(target);
    }

    /** Musteriye gosterilecek ilerleme yuzdesi. */
    public int progressPercent() {
        return switch (this) {
            case CREATED -> 10;
            case PAID -> 40;
            case SHIPPED -> 75;
            case DELIVERED -> 100;
            case CANCELLED, REFUNDED -> 0;
        };
    }
}
