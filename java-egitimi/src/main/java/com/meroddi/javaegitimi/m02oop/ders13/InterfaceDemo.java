package com.meroddi.javaegitimi.m02oop.ders13;

import com.meroddi.javaegitimi.m02oop.ders10.Money;

import java.util.List;
import java.util.Optional;

/**
 * DERS 13 - Interface, default metot, dependency injection.
 *
 * Calistirma:
 *   mvn -q exec:java -Dexec.mainClass=com.meroddi.javaegitimi.m02oop.ders13.InterfaceDemo
 */
public class InterfaceDemo {

    public static void main(String[] args) {

        // ---------------------------------------------------------------
        // 1) BAGIMLILIKLARI DISARIDAN VERMEK
        //    Servis, hangi somut siniflarla calistigini bilmiyor.
        // ---------------------------------------------------------------
        OrderRepository repository = new InMemoryOrderRepository();
        List<NotificationSender> senders = List.of(
                new EmailNotificationSender("smtp.meroddi.com"),
                new SmsNotificationSender("Turkcell", 14)   // saat 14:00 -> acik
        );
        OrderNotificationService service = new OrderNotificationService(repository, senders);

        System.out.println("--- Siparis olusturma (gunduz) ---");
        service.placeOrder(new OrderSummary("ORD-00101", "ayse@example.com", "+90555*******", Money.tryOf("3450.00")));
        System.out.println();

        // ---------------------------------------------------------------
        // 2) AYNI SERVIS, FARKLI YAPILANDIRMA
        //    Gece yarisi: SMS kanali kendini kapatir.
        // ---------------------------------------------------------------
        OrderNotificationService nightService = new OrderNotificationService(
                repository,
                List.of(new EmailNotificationSender("smtp.meroddi.com"),
                        new SmsNotificationSender("Turkcell", 23)));   // saat 23:00 -> kapali

        System.out.println("--- Siparis olusturma (gece 23:00) ---");
        nightService.placeOrder(new OrderSummary("ORD-00102", "mehmet@example.com", "+90533*******", Money.tryOf("899.90")));
        System.out.println();

        // ---------------------------------------------------------------
        // 3) DEFAULT METOTLAR - hicbir sinifta yazilmadi ama calisiyor
        // ---------------------------------------------------------------
        System.out.println("--- Arayuzun default metotlari ---");
        System.out.println("Toplam siparis   : " + repository.count());
        System.out.println("ORD-00101 var mi : " + repository.exists("ORD-00101"));
        System.out.println("ORD-99999 var mi : " + repository.exists("ORD-99999"));
        System.out.println();

        // ---------------------------------------------------------------
        // 4) OPTIONAL - "olmayabilir"i tip seviyesinde tasimak
        // ---------------------------------------------------------------
        System.out.println("--- Optional ---");
        Optional<OrderSummary> found = repository.findByOrderNo("ORD-00101");
        System.out.println("Bulunan : " + found.map(OrderSummary::orderNo).orElse("yok"));
        Optional<OrderSummary> missing = repository.findByOrderNo("ORD-00999");
        System.out.println("Bulunmayan : " + missing.map(OrderSummary::orderNo).orElse("yok"));
        System.out.println("Optional, NullPointerException'i tasarim seviyesinde onler.");
        System.out.println();

        // ---------------------------------------------------------------
        // 5) TESTTE SAHTE (fake) UYGULAMA
        //    Arayuz oldugu icin, gercekten SMS gondermeden test edebiliyoruz.
        // ---------------------------------------------------------------
        System.out.println("--- Test icin sahte kanal ---");
        RecordingSender recorder = new RecordingSender();
        OrderNotificationService testService = new OrderNotificationService(
                new InMemoryOrderRepository(), List.of(recorder));
        testService.placeOrder(new OrderSummary("ORD-TEST", "test@example.com", "+90000", Money.tryOf("10.00")));
        System.out.println("Kaydedilen bildirim sayisi : " + recorder.getSentCount());
        System.out.println("Son alici                  : " + recorder.getLastRecipient());
        System.out.println();

        // ---------------------------------------------------------------
        // 6) ANONIM SINIF - tek kullanimlik uygulama
        // ---------------------------------------------------------------
        System.out.println("--- Anonim sinif ---");
        NotificationSender console = new NotificationSender() {
            @Override
            public void send(String recipient, String subject, String body) {
                System.out.println("  [CONSOLE] " + recipient + " -> " + subject);
            }

            @Override
            public String channel() {
                return "CONSOLE";
            }
        };
        console.send("dev@meroddi.com", "Deneme", "govde");
        System.out.println();

        System.out.println("--- interface vs abstract class ---");
        System.out.println("interface : 'YAPABILIR' (can-do). Bir sinif bircogunu uygulayabilir.");
        System.out.println("abstract  : 'BIR TURUDUR' (is-a). Ortak durum + ortak kod tasir, tek atadan turenir.");
        System.out.println("Tereddutte kalirsan interface sec: bagi gevsek tutar, test edilebilirligi artirir.");
    }

    /** Testlerde kullanilan sahte kanal: gercekten gondermez, kaydeder. */
    static class RecordingSender implements NotificationSender {
        private int sentCount = 0;
        private String lastRecipient;

        @Override
        public void send(String recipient, String subject, String body) {
            sentCount++;
            lastRecipient = recipient;
        }

        @Override
        public String channel() {
            return "FAKE";
        }

        public int getSentCount() {
            return sentCount;
        }

        public String getLastRecipient() {
            return lastRecipient;
        }
    }
}
