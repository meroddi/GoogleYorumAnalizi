package com.meroddi.javaegitimi.m02oop.ders15.model;

import com.meroddi.javaegitimi.m02oop.ders10.Money;
import com.meroddi.javaegitimi.m02oop.ders14.InvalidStatusTransitionException;
import com.meroddi.javaegitimi.m02oop.ders14.OrderStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DERS 15 - Siparis (aggregate root).
 *
 * Siparis, kendi satirlarinin ve kendi durumunun sahibidir. Disaridan
 * kimse "order.getLines().add(...)" diyerek satir ekleyemez; ekleme
 * kurali (sadece CREATED durumundayken) burada korunur.
 *
 * Modul 2'nin tamami bu sinifta bir arada:
 *   encapsulation (Ders 10), kalitim/Object metotlari (Ders 11),
 *   enum ve exception (Ders 14).
 */
public class Order {

    private final String orderNo;
    private final String customerNo;
    private final LocalDateTime createdAt;
    private final List<OrderLine> lines = new ArrayList<>();
    private OrderStatus status = OrderStatus.CREATED;
    private String paymentReference;

    public Order(String orderNo, String customerNo) {
        this.orderNo = orderNo;
        this.customerNo = customerNo;
        this.createdAt = LocalDateTime.now();
    }

    /** Satir eklemek sadece odeme oncesinde mumkundur. */
    public void addLine(OrderLine line) {
        if (status != OrderStatus.CREATED) {
            throw new IllegalStateException(
                    "Odenmis/kapanmis siparise satir eklenemez. Durum: " + status);
        }
        lines.add(line);
    }

    public Money total() {
        Money total = Money.zeroTry();
        for (OrderLine line : lines) {
            total = total.add(line.lineTotal());
        }
        return total;
    }

    public int totalItemCount() {
        int count = 0;
        for (OrderLine line : lines) {
            count += line.quantity();
        }
        return count;
    }

    /** Durum gecisi kurali enum'da tanimli (Ders 14). */
    public void changeStatus(OrderStatus newStatus) {
        if (!status.canTransitionTo(newStatus)) {
            throw new InvalidStatusTransitionException(orderNo, status, newStatus);
        }
        this.status = newStatus;
    }

    public void markPaid(String paymentReference) {
        changeStatus(OrderStatus.PAID);
        this.paymentReference = paymentReference;
    }

    public boolean isEmpty() {
        return lines.isEmpty();
    }

    public String getOrderNo() {
        return orderNo;
    }

    public String getCustomerNo() {
        return customerNo;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    /** Degistirilemez kopya: disari veriyi gorur ama bozamaz (Ders 10). */
    public List<OrderLine> getLines() {
        return List.copyOf(lines);
    }

    @Override
    public String toString() {
        return "Order[%s, musteri=%s, %d satir, %s, %s]"
                .formatted(orderNo, customerNo, lines.size(), total(), status);
    }
}
