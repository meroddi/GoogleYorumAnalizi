package com.meroddi.javaegitimi.m02oop.ders13;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * DERS 13 - Arayuzun bellek ici uygulamasi.
 *
 * Gercek projede ayni arayuzun JDBC/JPA uygulamasi olur. Test ederken bu
 * bellek ici surum kullanilir: veri tabani kurmadan, saniyeler icinde test.
 *
 * (Map/List = Collections konusu, Modul 3'te detayli islenecek.)
 */
public class InMemoryOrderRepository implements OrderRepository {

    private final Map<String, OrderSummary> storage = new LinkedHashMap<>();

    @Override
    public void save(OrderSummary order) {
        storage.put(order.orderNo(), order);
    }

    @Override
    public Optional<OrderSummary> findByOrderNo(String orderNo) {
        return Optional.ofNullable(storage.get(orderNo));
    }

    @Override
    public List<OrderSummary> findAll() {
        return new ArrayList<>(storage.values());   // savunmaci kopya (Ders 10)
    }
}
