package com.meroddi.javaegitimi.m02oop.ders15.repository;

import com.meroddi.javaegitimi.m02oop.ders15.model.Order;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * DERS 15 - Bellek ici siparis deposu.
 */
public class InMemoryOrderRepository implements OrderRepository {

    private final Map<String, Order> storage = new LinkedHashMap<>();

    @Override
    public void save(Order order) {
        storage.put(order.getOrderNo(), order);
    }

    @Override
    public Optional<Order> findByOrderNo(String orderNo) {
        return Optional.ofNullable(storage.get(orderNo));
    }

    @Override
    public List<Order> findByCustomerNo(String customerNo) {
        List<Order> result = new ArrayList<>();
        for (Order order : storage.values()) {
            if (order.getCustomerNo().equals(customerNo)) {
                result.add(order);
            }
        }
        return result;
    }

    @Override
    public List<Order> findAll() {
        return new ArrayList<>(storage.values());
    }
}
