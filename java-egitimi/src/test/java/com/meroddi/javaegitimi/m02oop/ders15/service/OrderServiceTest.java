package com.meroddi.javaegitimi.m02oop.ders15.service;

import com.meroddi.javaegitimi.m02oop.ders10.Money;
import com.meroddi.javaegitimi.m02oop.ders12.CashOnDeliveryPayment;
import com.meroddi.javaegitimi.m02oop.ders12.CreditCardPayment;
import com.meroddi.javaegitimi.m02oop.ders12.PaymentResult;
import com.meroddi.javaegitimi.m02oop.ders13.NotificationSender;
import com.meroddi.javaegitimi.m02oop.ders14.InvalidStatusTransitionException;
import com.meroddi.javaegitimi.m02oop.ders14.OrderStatus;
import com.meroddi.javaegitimi.m02oop.ders15.model.Order;
import com.meroddi.javaegitimi.m02oop.ders15.model.Product;
import com.meroddi.javaegitimi.m02oop.ders15.repository.InMemoryOrderRepository;
import com.meroddi.javaegitimi.m02oop.ders15.repository.InMemoryProductRepository;
import com.meroddi.javaegitimi.m02oop.ders15.repository.OrderRepository;
import com.meroddi.javaegitimi.m02oop.ders15.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DERS 15 - Servis testleri.
 *
 * Dikkat: veri tabani yok, SMTP yok, POS cihazi yok. Hepsi arayuz oldugu icin
 * testte sahte uygulamalar kullaniliyor ve test milisaniyeler icinde bitiyor.
 * "Test edilebilir tasarim"in somut karsiligi budur (Ders 13).
 */
class OrderServiceTest {

    private ProductRepository productRepository;
    private OrderRepository orderRepository;
    private RecordingSender notificationSender;
    private OrderService orderService;

    /** Her testten once TEMIZ bir dunya kurulur - testler birbirini etkilemez. */
    @BeforeEach
    void setUp() {
        productRepository = new InMemoryProductRepository();
        orderRepository = new InMemoryOrderRepository();
        notificationSender = new RecordingSender();
        orderService = new OrderService(productRepository, orderRepository, notificationSender);

        productRepository.save(new Product("KLV-001", "Mekanik Klavye", Money.tryOf("2500.00"), 10));
        productRepository.save(new Product("MSE-220", "Kablosuz Mouse", Money.tryOf("289.50"), 25));
        productRepository.save(new Product("MNT-014", "Monitor 27\"", Money.tryOf("4299.00"), 2));
    }

    @Test
    @DisplayName("Siparis olusturulur, tutar dogru hesaplanir ve stok duser")
    void createOrderCalculatesTotalAndDecreasesStock() {
        Map<String, Integer> basket = new LinkedHashMap<>();
        basket.put("KLV-001", 1);
        basket.put("MSE-220", 2);

        Order order = orderService.createOrder("C-1001", basket);

        // 2500.00 + (2 x 289.50) = 3079.00
        assertEquals(Money.tryOf("3079.00"), order.total());
        assertEquals(3, order.totalItemCount());
        assertEquals(OrderStatus.CREATED, order.getStatus());
        assertEquals(9, productRepository.findBySku("KLV-001").orElseThrow().getStock());
        assertEquals(23, productRepository.findBySku("MSE-220").orElseThrow().getStock());
        assertEquals(1, notificationSender.getSentCount());
    }

    @Test
    @DisplayName("Yetersiz stokta siparis olusmaz ve HICBIR urunun stogu dusmez")
    void insufficientStockLeavesNothingChanged() {
        Map<String, Integer> basket = new LinkedHashMap<>();
        basket.put("KLV-001", 1);      // stok yeterli
        basket.put("MNT-014", 99);     // stok yetersiz

        assertThrows(IllegalStateException.class, () -> orderService.createOrder("C-1001", basket));

        // Once tum satirlar dogrulandigi icin ilk urunun stogu da dusmemis olmali
        assertEquals(10, productRepository.findBySku("KLV-001").orElseThrow().getStock());
        assertEquals(0, orderRepository.count());
    }

    @Test
    @DisplayName("Olmayan urun icin NotFoundException firlatilir")
    void unknownProductThrows() {
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> orderService.createOrder("C-1001", Map.of("YOK-000", 1)));
        assertTrue(exception.getMessage().contains("YOK-000"));
    }

    @Test
    @DisplayName("Basarili odeme siparisi PAID durumuna gecirir")
    void successfulPaymentMarksOrderPaid() {
        Order order = orderService.createOrder("C-1001", Map.of("KLV-001", 1));

        PaymentResult result = orderService.pay(order.getOrderNo(),
                new CreditCardPayment("Garanti", "4506 3474 1234 5678", 1, Money.tryOf("50000.00")));

        assertTrue(result.successful());
        assertEquals(OrderStatus.PAID, order.getStatus());
        assertEquals(result.reference(), order.getPaymentReference());
        // Komisyon: 2500.00 x %2.3 = 57.50 -> tahsil edilen 2557.50
        assertEquals(Money.tryOf("2557.50"), result.chargedAmount());
    }

    @Test
    @DisplayName("Basarisiz odeme siparis durumunu degistirmez")
    void failedPaymentDoesNotChangeStatus() {
        Order order = orderService.createOrder("C-1001", Map.of("MNT-014", 2)); // 8598.00 TL

        PaymentResult result = orderService.pay(order.getOrderNo(),
                new CashOnDeliveryPayment("Aras Kargo"));   // kapida odeme siniri 5000

        assertFalse(result.successful());
        assertEquals(OrderStatus.CREATED, order.getStatus());
    }

    @Test
    @DisplayName("Odenmemis siparis kargoya verilemez")
    void cannotShipUnpaidOrder() {
        Order order = orderService.createOrder("C-1001", Map.of("KLV-001", 1));

        assertThrows(InvalidStatusTransitionException.class,
                () -> orderService.ship(order.getOrderNo()));
    }

    @Test
    @DisplayName("Tam akis: olustur -> ode -> kargola -> teslim et")
    void fullHappyPath() {
        Order order = orderService.createOrder("C-1001", Map.of("KLV-001", 1));
        orderService.pay(order.getOrderNo(),
                new CreditCardPayment("Garanti", "4506 3474 1234 5678", 1, Money.tryOf("50000.00")));
        orderService.ship(order.getOrderNo());
        orderService.deliver(order.getOrderNo());

        assertEquals(OrderStatus.DELIVERED, order.getStatus());
        assertEquals(100, order.getStatus().progressPercent());
    }

    @Test
    @DisplayName("Iptal edilen siparisin stogu geri yuklenir")
    void cancelRestoresStock() {
        Order order = orderService.createOrder("C-1001", Map.of("KLV-001", 3));
        assertEquals(7, productRepository.findBySku("KLV-001").orElseThrow().getStock());

        orderService.cancel(order.getOrderNo());

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertEquals(10, productRepository.findBySku("KLV-001").orElseThrow().getStock());
    }

    @Test
    @DisplayName("Ciro sadece odenmis siparisleri sayar")
    void revenueCountsOnlyPaidOrders() {
        Order paid = orderService.createOrder("C-1001", Map.of("KLV-001", 1));
        orderService.pay(paid.getOrderNo(),
                new CreditCardPayment("Garanti", "4506 3474 1234 5678", 1, Money.tryOf("50000.00")));
        orderService.createOrder("C-1002", Map.of("MSE-220", 1));   // odenmedi

        assertEquals(Money.tryOf("2500.00"), orderService.revenue());
    }

    @Test
    @DisplayName("Odenmis siparise yeni satir eklenemez")
    void cannotAddLineToPaidOrder() {
        Order order = orderService.createOrder("C-1001", Map.of("KLV-001", 1));
        orderService.pay(order.getOrderNo(),
                new CreditCardPayment("Garanti", "4506 3474 1234 5678", 1, Money.tryOf("50000.00")));

        assertThrows(IllegalStateException.class, () -> order.addLine(
                new com.meroddi.javaegitimi.m02oop.ders15.model.OrderLine(
                        "MSE-220", "Kablosuz Mouse", Money.tryOf("289.50"), 1)));
    }

    /** Testte gercek e-posta gondermemek icin sahte kanal (fake). */
    private static class RecordingSender implements NotificationSender {
        private int sentCount = 0;

        @Override
        public void send(String recipient, String subject, String body) {
            sentCount++;
        }

        @Override
        public String channel() {
            return "FAKE";
        }

        int getSentCount() {
            return sentCount;
        }
    }
}
