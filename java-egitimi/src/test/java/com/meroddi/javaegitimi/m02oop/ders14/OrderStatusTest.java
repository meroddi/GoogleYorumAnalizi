package com.meroddi.javaegitimi.m02oop.ders14;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DERS 15 - Durum makinesi testleri.
 *
 * Durum gecis kurallari is acisindan kritiktir: "odenmemis siparis kargolanabilir mi?"
 * sorusunun cevabi bir testte yaziliysa, kimse yanlislikla degistiremez.
 */
class OrderStatusTest {

    @Test
    @DisplayName("Yeni siparis odenebilir veya iptal edilebilir")
    void createdTransitions() {
        assertTrue(OrderStatus.CREATED.canTransitionTo(OrderStatus.PAID));
        assertTrue(OrderStatus.CREATED.canTransitionTo(OrderStatus.CANCELLED));
    }

    @Test
    @DisplayName("Odeme yapilmadan kargoya verilemez")
    void cannotShipUnpaidOrder() {
        assertFalse(OrderStatus.CREATED.canTransitionTo(OrderStatus.SHIPPED));
    }

    @Test
    @DisplayName("Iptal edilmis siparis yeniden acilamaz")
    void cancelledIsTerminal() {
        assertTrue(OrderStatus.CANCELLED.isTerminal());
        assertTrue(OrderStatus.CANCELLED.allowedTransitions().isEmpty());
    }

    @Test
    @DisplayName("Teslim edilen siparis iade edilebilir")
    void deliveredCanBeRefunded() {
        assertTrue(OrderStatus.DELIVERED.canTransitionTo(OrderStatus.REFUNDED));
        assertFalse(OrderStatus.DELIVERED.canTransitionTo(OrderStatus.SHIPPED));
    }

    @Test
    @DisplayName("Ilerleme yuzdeleri musteriye dogru gosterilir")
    void progressPercent() {
        assertTrue(OrderStatus.CREATED.progressPercent() < OrderStatus.PAID.progressPercent());
        assertTrue(OrderStatus.PAID.progressPercent() < OrderStatus.SHIPPED.progressPercent());
        assertTrue(OrderStatus.DELIVERED.progressPercent() == 100);
    }
}
