package com.meroddi.javaegitimi.m02oop.ders15;

import com.meroddi.javaegitimi.m02oop.ders10.Money;
import com.meroddi.javaegitimi.m02oop.ders12.CashOnDeliveryPayment;
import com.meroddi.javaegitimi.m02oop.ders12.CreditCardPayment;
import com.meroddi.javaegitimi.m02oop.ders12.PaymentResult;
import com.meroddi.javaegitimi.m02oop.ders13.EmailNotificationSender;
import com.meroddi.javaegitimi.m02oop.ders15.model.Order;
import com.meroddi.javaegitimi.m02oop.ders15.model.OrderLine;
import com.meroddi.javaegitimi.m02oop.ders15.model.Product;
import com.meroddi.javaegitimi.m02oop.ders15.repository.InMemoryOrderRepository;
import com.meroddi.javaegitimi.m02oop.ders15.repository.InMemoryProductRepository;
import com.meroddi.javaegitimi.m02oop.ders15.repository.OrderRepository;
import com.meroddi.javaegitimi.m02oop.ders15.repository.ProductRepository;
import com.meroddi.javaegitimi.m02oop.ders15.service.OrderService;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * DERS 15 - MODUL 2 PROJESI: katmanli mini e-ticaret.
 *
 * Modul 2'nin tamami calisir halde:
 *   sinif/nesne (09), encapsulation+record+static (10), kalitim (11),
 *   polymorphism (12), interface + DI (13), enum + exception (14).
 *
 * Calistirma:
 *   mvn -q exec:java -Dexec.mainClass=com.meroddi.javaegitimi.m02oop.ders15.ShopApp
 *
 * Testler:
 *   mvn -q test
 */
public class ShopApp {

    public static void main(String[] args) {

        // ---------------------------------------------------------------
        // KURULUM - hangi uygulamalarin kullanilacagi TEK yerde belirlenir.
        // (Spring Boot'ta bu isi @Component/@Autowired yapar.)
        // ---------------------------------------------------------------
        ProductRepository productRepository = new InMemoryProductRepository();
        OrderRepository orderRepository = new InMemoryOrderRepository();
        OrderService orderService = new OrderService(
                productRepository, orderRepository, new EmailNotificationSender("smtp.meroddi.com"));

        seedProducts(productRepository);

        System.out.println("=== KATALOG ===");
        for (Product product : productRepository.findAll()) {
            System.out.println("  " + product);
        }
        System.out.println();

        // ---------------------------------------------------------------
        // SENARYO 1 - basarili siparis: olustur -> ode -> kargola -> teslim
        // ---------------------------------------------------------------
        System.out.println("=== SENARYO 1: Basarili siparis ===");
        Map<String, Integer> basket = new LinkedHashMap<>();
        basket.put("KLV-001", 1);
        basket.put("MSE-220", 2);

        Order order = orderService.createOrder("C-1001", basket);
        printOrder(order);

        PaymentResult payment = orderService.pay(order.getOrderNo(),
                new CreditCardPayment("Garanti", "4506 3474 1234 5678", 3, Money.tryOf("50000.00")));
        System.out.println("  " + payment);

        orderService.ship(order.getOrderNo());
        orderService.deliver(order.getOrderNo());
        System.out.println("  Son durum: " + order.getStatus().getLabel()
                + " (%" + order.getStatus().progressPercent() + ")");
        System.out.println();

        // ---------------------------------------------------------------
        // SENARYO 2 - odeme reddedilirse siparis durumu degismez
        // ---------------------------------------------------------------
        System.out.println("=== SENARYO 2: Odeme reddi ===");
        Order bigOrder = orderService.createOrder("C-1002", Map.of("MNT-014", 2));
        System.out.println("  Sepet tutari : " + bigOrder.total() + " (kapida odeme siniri: 5000.00 TRY)");
        // Kapida odemenin ust siniri asildigi icin tahsilat reddedilecek (Ders 12).
        PaymentResult failed = orderService.pay(bigOrder.getOrderNo(), new CashOnDeliveryPayment("Aras Kargo"));
        System.out.println("  " + failed);
        System.out.println("  Siparis durumu : " + bigOrder.getStatus() + " (degismedi)");
        System.out.println();

        // ---------------------------------------------------------------
        // SENARYO 3 - iptal ve stok iadesi
        // ---------------------------------------------------------------
        System.out.println("=== SENARYO 3: Iptal ve stok iadesi ===");
        Product monitor = productRepository.findBySku("MNT-014").orElseThrow();
        System.out.println("  Iptal oncesi monitor stogu : " + monitor.getStock());
        orderService.cancel(bigOrder.getOrderNo());
        System.out.println("  Iptal sonrasi monitor stogu: " + monitor.getStock());
        System.out.println("  Siparis durumu : " + bigOrder.getStatus());
        System.out.println();

        // ---------------------------------------------------------------
        // SENARYO 4 - kural ihlalleri
        // ---------------------------------------------------------------
        System.out.println("=== SENARYO 4: Kural ihlalleri ===");
        tryAndPrint("Yetersiz stokla siparis",
                () -> orderService.createOrder("C-1003", Map.of("MNT-014", 999)));
        tryAndPrint("Olmayan urunle siparis",
                () -> orderService.createOrder("C-1003", Map.of("YOK-000", 1)));
        tryAndPrint("Teslim edilmis siparisi tekrar kargolama",
                () -> orderService.ship(order.getOrderNo()));
        tryAndPrint("Odenmis siparise satir ekleme",
                () -> order.addLine(new OrderLine("KLV-001", "Mekanik Klavye", Money.tryOf("2500.00"), 1)));
        System.out.println();

        // ---------------------------------------------------------------
        // RAPOR
        // ---------------------------------------------------------------
        System.out.println("=== RAPOR ===");
        System.out.println("  Toplam siparis : " + orderRepository.count());
        System.out.println("  Ciro           : " + orderService.revenue());
        System.out.println("  C-1001 siparis : " + orderService.ordersOf("C-1001").size());
        System.out.println();
        System.out.println("  Guncel stoklar:");
        for (Product product : productRepository.findAll()) {
            System.out.println("    " + product);
        }
    }

    private static void seedProducts(ProductRepository repository) {
        repository.save(new Product("KLV-001", "Mekanik Klavye", Money.tryOf("2500.00"), 10));
        repository.save(new Product("MNT-014", "Monitor 27\"", Money.tryOf("4299.00"), 4));
        repository.save(new Product("MSE-220", "Kablosuz Mouse", Money.tryOf("289.50"), 25));
        repository.save(new Product("KLK-077", "Kulaklik", Money.tryOf("1150.00"), 8));
    }

    private static void printOrder(Order order) {
        System.out.println("  " + order.getOrderNo() + " olusturuldu:");
        for (OrderLine line : order.getLines()) {
            System.out.println("    " + line);
        }
        System.out.println("    TOPLAM: " + order.total() + " (" + order.totalItemCount() + " parca)");
    }

    /** Hata firlatmasi beklenen islemleri okunakli sekilde denemek icin. */
    private static void tryAndPrint(String description, Runnable action) {
        try {
            action.run();
            System.out.printf("  %-42s -> beklenmedik sekilde BASARILI%n", description);
        } catch (RuntimeException e) {
            System.out.printf("  %-42s -> %s: %s%n",
                    description, e.getClass().getSimpleName(), e.getMessage());
        }
    }
}
