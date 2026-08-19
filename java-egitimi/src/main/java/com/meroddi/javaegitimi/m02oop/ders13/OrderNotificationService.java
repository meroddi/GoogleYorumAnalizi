package com.meroddi.javaegitimi.m02oop.ders13;

import java.util.List;

/**
 * DERS 13 - Servis: sadece ARAYUZLERE bagli.
 *
 * Dikkat: bu sinifin icinde ne "InMemoryOrderRepository" gecer, ne "EmailNotificationSender".
 * Bagimliliklar disaridan constructor ile verilir -> "dependency injection".
 * Spring Boot'un yaptigi sey, bu enjeksiyonu senin yerine otomatik yapmaktir.
 */
public class OrderNotificationService {

    private final OrderRepository repository;             // sozlesme
    private final List<NotificationSender> senders;       // sozlesme listesi

    public OrderNotificationService(OrderRepository repository, List<NotificationSender> senders) {
        this.repository = repository;
        this.senders = senders;
    }

    /** Siparisi kaydeder ve acik olan tum kanallardan bildirim gonderir. */
    public void placeOrder(OrderSummary order) {
        repository.save(order);

        String subject = "Siparisiniz alindi";
        String body = """
                Sayin musterimiz,
                %s numarali siparisiniz alinmistir.
                Tutar: %s""".formatted(order.orderNo(), order.total());

        System.out.println("Siparis kaydedildi: " + order.orderNo() + " (" + order.total() + ")");
        for (NotificationSender sender : senders) {
            if (!sender.enabled()) {                       // default/override edilmis metot
                System.out.println("  [" + sender.channel() + "] kanal kapali, atlandi");
                continue;
            }
            String recipient = "SMS".equals(sender.channel())
                    ? order.customerPhone()
                    : order.customerEmail();
            sender.send(recipient, subject, body);
        }
    }

    public int orderCount() {
        return repository.count();      // arayuzun default metodu
    }
}
