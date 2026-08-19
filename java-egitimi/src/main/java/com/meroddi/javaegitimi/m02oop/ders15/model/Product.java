package com.meroddi.javaegitimi.m02oop.ders15.model;

import com.meroddi.javaegitimi.m02oop.ders10.Money;

/**
 * DERS 15 - MODEL KATMANI: Urun.
 *
 * Model (domain) katmani, isin KURALLARINI tasir. Veri tabanini, HTTP'yi,
 * ekrani bilmez. Bu ayrimi bozan projeler 2 yil sonra test edilemez hale gelir.
 */
public class Product {

    private final String sku;
    private final String name;
    private Money price;
    private int stock;

    public Product(String sku, String name, Money price, int stock) {
        if (sku == null || sku.isBlank()) {
            throw new IllegalArgumentException("SKU zorunlu");
        }
        if (price == null || price.isZero()) {
            throw new IllegalArgumentException("Fiyat sifir olamaz: " + sku);
        }
        if (stock < 0) {
            throw new IllegalArgumentException("Stok negatif olamaz: " + sku);
        }
        this.sku = sku;
        this.name = name;
        this.price = price;
        this.stock = stock;
    }

    public boolean hasStock(int quantity) {
        return stock >= quantity;
    }

    public void decreaseStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Adet pozitif olmali");
        }
        if (!hasStock(quantity)) {
            throw new IllegalStateException(
                    "Yetersiz stok [%s]: istenen %d, mevcut %d".formatted(sku, quantity, stock));
        }
        this.stock -= quantity;
    }

    /** Siparis iptalinde stok geri verilir. */
    public void restock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Adet pozitif olmali");
        }
        this.stock += quantity;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public Money getPrice() {
        return price;
    }

    public int getStock() {
        return stock;
    }

    @Override
    public String toString() {
        return "%s (%s) %s - %d adet".formatted(name, sku, price, stock);
    }
}
