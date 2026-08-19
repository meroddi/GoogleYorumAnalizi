package com.meroddi.javaegitimi.m02oop.ders15.model;

import com.meroddi.javaegitimi.m02oop.ders10.Money;

/**
 * DERS 15 - Siparis satiri (immutable).
 *
 * Dikkat: urunun fiyatini burada KOPYALIYORUZ. Cunku urunun fiyati yarin
 * degisirse, dun verilen siparisin tutari degismemelidir. Bu, gercek
 * sistemlerdeki en sik gorulen veri modelleme hatalarindan biridir.
 */
public record OrderLine(String sku, String productName, Money unitPrice, int quantity) {

    public OrderLine {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Adet pozitif olmali: " + sku);
        }
    }

    public Money lineTotal() {
        return unitPrice.multiply(quantity);
    }

    @Override
    public String toString() {
        return "%-18s %3d x %-12s = %s".formatted(productName, quantity, unitPrice, lineTotal());
    }
}
