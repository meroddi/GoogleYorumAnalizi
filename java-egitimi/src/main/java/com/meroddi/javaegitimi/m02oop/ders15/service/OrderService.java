package com.meroddi.javaegitimi.m02oop.ders15.service;

import com.meroddi.javaegitimi.m02oop.ders10.Money;
import com.meroddi.javaegitimi.m02oop.ders10.OrderIdGenerator;
import com.meroddi.javaegitimi.m02oop.ders12.PaymentMethod;
import com.meroddi.javaegitimi.m02oop.ders12.PaymentResult;
import com.meroddi.javaegitimi.m02oop.ders13.NotificationSender;
import com.meroddi.javaegitimi.m02oop.ders14.OrderStatus;
import com.meroddi.javaegitimi.m02oop.ders15.model.Order;
import com.meroddi.javaegitimi.m02oop.ders15.model.OrderLine;
import com.meroddi.javaegitimi.m02oop.ders15.model.Product;
import com.meroddi.javaegitimi.m02oop.ders15.repository.OrderRepository;
import com.meroddi.javaegitimi.m02oop.ders15.repository.ProductRepository;

import java.util.List;
import java.util.Map;

/**
 * DERS 15 - SERVIS KATMANI: is akisini yoneten katman.
 *
 * Sorumlulugu: adimlari SIRALAMAK.
 *   stok kontrolu -> siparis olustur -> odeme al -> durumu guncelle -> bildir
 *
 * Is KURALLARI modelde (Order, Product), veri erisimi repository'de,
 * odeme detayi PaymentMethod'da. Servis hicbirinin ic isine karismaz.
 * Bu ayrim sayesinde her parca ayri ayri test edilebilir.
 */
public class OrderService {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final NotificationSender notificationSender;

    /** Bagimliliklar disaridan verilir (Ders 13: dependency injection). */
    public OrderService(ProductRepository productRepository,
                        OrderRepository orderRepository,
                        NotificationSender notificationSender) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.notificationSender = notificationSender;
    }

    /**
     * Siparis olusturur. items: SKU -> adet
     *
     * Once TUM satirlar dogrulanir, sonra stok dusulur. Boylece 3. urunde
     * hata alinirsa ilk 2 urunun stogu bosuna dusmus olmaz.
     * (Gercek sistemde bunun adi "transaction"dir - SQL dersinde gorecegiz.)
     */
    public Order createOrder(String customerNo, Map<String, Integer> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Bos siparis olusturulamaz");
        }

        // 1. adim: dogrulama (hicbir sey degistirmeden)
        for (Map.Entry<String, Integer> item : items.entrySet()) {
            Product product = requireProduct(item.getKey());
            if (!product.hasStock(item.getValue())) {
                throw new IllegalStateException(
                        "Yetersiz stok [%s]: istenen %d, mevcut %d"
                                .formatted(product.getSku(), item.getValue(), product.getStock()));
            }
        }

        // 2. adim: uygulama
        Order order = new Order(OrderIdGenerator.nextId(), customerNo);
        for (Map.Entry<String, Integer> item : items.entrySet()) {
            Product product = requireProduct(item.getKey());
            product.decreaseStock(item.getValue());
            order.addLine(new OrderLine(
                    product.getSku(), product.getName(), product.getPrice(), item.getValue()));
            productRepository.save(product);
        }

        orderRepository.save(order);
        notify(customerNo, "Siparisiniz alindi",
                "%s numarali siparisiniz olusturuldu. Tutar: %s".formatted(order.getOrderNo(), order.total()));
        return order;
    }

    /** Odemeyi alir; basarisizsa siparis durumu degismez. */
    public PaymentResult pay(String orderNo, PaymentMethod paymentMethod) {
        Order order = requireOrder(orderNo);
        if (order.getStatus() != OrderStatus.CREATED) {
            throw new IllegalStateException("Sadece yeni siparisler odenebilir. Durum: " + order.getStatus());
        }

        PaymentResult result = paymentMethod.pay(order.total(), order.getOrderNo());
        if (result.successful()) {
            order.markPaid(result.reference());
            orderRepository.save(order);
            notify(order.getCustomerNo(), "Odemeniz alindi",
                    "%s icin %s tahsil edildi. Referans: %s"
                            .formatted(orderNo, result.chargedAmount(), result.reference()));
        }
        return result;
    }

    public void ship(String orderNo) {
        Order order = requireOrder(orderNo);
        order.changeStatus(OrderStatus.SHIPPED);       // gecersizse exception (Ders 14)
        orderRepository.save(order);
        notify(order.getCustomerNo(), "Siparisiniz kargoda", orderNo + " kargoya verildi.");
    }

    public void deliver(String orderNo) {
        Order order = requireOrder(orderNo);
        order.changeStatus(OrderStatus.DELIVERED);
        orderRepository.save(order);
    }

    /** Iptal: stoklar geri yuklenir. */
    public void cancel(String orderNo) {
        Order order = requireOrder(orderNo);
        order.changeStatus(OrderStatus.CANCELLED);
        for (OrderLine line : order.getLines()) {
            Product product = requireProduct(line.sku());
            product.restock(line.quantity());
            productRepository.save(product);
        }
        orderRepository.save(order);
        notify(order.getCustomerNo(), "Siparisiniz iptal edildi", orderNo + " iptal edildi.");
    }

    /** Teslim edilmis siparislerin toplam cirosu. */
    public Money revenue() {
        Money total = Money.zeroTry();
        for (Order order : orderRepository.findAll()) {
            if (order.getStatus() == OrderStatus.PAID
                    || order.getStatus() == OrderStatus.SHIPPED
                    || order.getStatus() == OrderStatus.DELIVERED) {
                total = total.add(order.total());
            }
        }
        return total;
    }

    public List<Order> ordersOf(String customerNo) {
        return orderRepository.findByCustomerNo(customerNo);
    }

    // ------------------------------------------------------------------
    //  Yardimcilar
    // ------------------------------------------------------------------

    private Product requireProduct(String sku) {
        return productRepository.findBySku(sku)
                .orElseThrow(() -> new NotFoundException("Urun bulunamadi: " + sku));
    }

    private Order requireOrder(String orderNo) {
        return orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new NotFoundException("Siparis bulunamadi: " + orderNo));
    }

    private void notify(String customerNo, String subject, String body) {
        if (notificationSender != null && notificationSender.enabled()) {
            notificationSender.send(customerNo, subject, body);
        }
    }
}
