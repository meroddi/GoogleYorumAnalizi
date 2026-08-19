package com.meroddi.javaegitimi.m02oop.ders14;

/**
 * DERS 14 - Kendi exception sinifin.
 *
 * RuntimeException'dan turedigi icin "unchecked"tir: cagiran taraf yakalamak
 * zorunda degildir. Is kurali ihlalleri genelde boyle modellenir; cunku
 * cagiran katman cogu zaman bu hatayi anlamli sekilde ELE ALAMAZ, sadece
 * kullaniciya bildirir.
 *
 * Iyi exception, mesajinda SORUNU COZECEK bilgiyi tasir:
 * "gecersiz durum" degil, "ORD-42: SHIPPED -> CREATED gecisi yapilamaz" der.
 */
public class InvalidStatusTransitionException extends RuntimeException {

    private final String orderNo;
    private final OrderStatus from;
    private final OrderStatus to;

    public InvalidStatusTransitionException(String orderNo, OrderStatus from, OrderStatus to) {
        super("%s: %s -> %s gecisi yapilamaz. Izin verilenler: %s"
                .formatted(orderNo, from, to, from.allowedTransitions()));
        this.orderNo = orderNo;
        this.from = from;
        this.to = to;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public OrderStatus getFrom() {
        return from;
    }

    public OrderStatus getTo() {
        return to;
    }
}
