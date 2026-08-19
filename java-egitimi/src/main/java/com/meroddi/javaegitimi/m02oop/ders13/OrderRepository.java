package com.meroddi.javaegitimi.m02oop.ders13;

import java.util.List;
import java.util.Optional;

/**
 * DERS 13 - Interface (arayuz): SOZLESME.
 *
 * "Siparis nasil saklanir" sorusunun CEVABINI degil, SORUSUNU tanimlar.
 * Yarin veri tabani PostgreSQL olur, bugun bellekte tutulur, testte sahte
 * bir uygulama kullanilir - servis kodu hicbirini bilmez.
 *
 * Bu, Spring Framework'un temel calisma bicimidir (Yil 1'in son ceyregi).
 */
public interface OrderRepository {

    /** Siparisi kaydeder. */
    void save(OrderSummary order);

    /**
     * Siparis numarasina gore arar.
     * Optional: "olmayabilir" bilgisini tip seviyesinde tasir -> null donmekten iyidir.
     */
    Optional<OrderSummary> findByOrderNo(String orderNo);

    /** Tum siparisler. */
    List<OrderSummary> findAll();

    /**
     * DEFAULT METOT (Java 8+): arayuzde govdeli metot.
     * Var olan arayuze, tum uygulamalari bozmadan yeni yetenek eklemenin yolu.
     */
    default int count() {
        return findAll().size();
    }

    default boolean exists(String orderNo) {
        return findByOrderNo(orderNo).isPresent();
    }
}
