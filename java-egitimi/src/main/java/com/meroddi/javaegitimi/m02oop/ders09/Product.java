package com.meroddi.javaegitimi.m02oop.ders09;

import java.math.BigDecimal;

/**
 * DERS 09 - Ikinci ornek sinif: Urun.
 *
 * Dikkat: bu sinif sadece "veri tasiyan bir kutu" degil. Stok azaltma kurali
 * urun nesnesinin KENDI icinde. Kurali disariya birakirsan (once stok kontrolu
 * yapan 5 farkli servis yazilir ve biri unutur) veri tutarsiz hale gelir.
 */
public class Product {

    private final String sku;          // final = bir kez atanir, bir daha degismez
    private String name;
    private BigDecimal price;
    private int stockQuantity;

    public Product(String sku, String name, BigDecimal price, int stockQuantity) {
        if (price == null || price.signum() < 0) {
            throw new IllegalArgumentException("Fiyat negatif olamaz: " + price);
        }
        if (stockQuantity < 0) {
            throw new IllegalArgumentException("Stok negatif olamaz: " + stockQuantity);
        }
        this.sku = sku;
        this.name = name;
        this.price = price;
        this.stockQuantity = stockQuantity;
    }

    /** Is kurali nesnenin icinde: stoktan dusme. */
    public void decreaseStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Adet pozitif olmali");
        }
        if (quantity > stockQuantity) {
            throw new IllegalStateException(
                    "Yetersiz stok. Istenen: " + quantity + ", mevcut: " + stockQuantity);
        }
        this.stockQuantity -= quantity;
    }

    public void increaseStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Adet pozitif olmali");
        }
        this.stockQuantity += quantity;
    }

    public boolean isInStock() {
        return stockQuantity > 0;
    }

    /** Fiyat degisikligi kontrollu yapilir: %50'den fazla artis muhtemelen hatadir. */
    public void changePrice(BigDecimal newPrice) {
        if (newPrice == null || newPrice.signum() <= 0) {
            throw new IllegalArgumentException("Fiyat pozitif olmali");
        }
        BigDecimal limit = price.multiply(new BigDecimal("1.5"));
        if (newPrice.compareTo(limit) > 0) {
            throw new IllegalArgumentException(
                    "Fiyat tek seferde %50'den fazla artirilamaz. Mevcut: " + price + ", istenen: " + newPrice);
        }
        this.price = newPrice;
    }

    public BigDecimal totalStockValue() {
        return price.multiply(BigDecimal.valueOf(stockQuantity));
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }
}
