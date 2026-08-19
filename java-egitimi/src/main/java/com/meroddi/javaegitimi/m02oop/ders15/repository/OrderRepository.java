package com.meroddi.javaegitimi.m02oop.ders15.repository;

import com.meroddi.javaegitimi.m02oop.ders15.model.Order;

import java.util.List;
import java.util.Optional;

/**
 * DERS 15 - Siparis deposu sozlesmesi.
 */
public interface OrderRepository {

    void save(Order order);

    Optional<Order> findByOrderNo(String orderNo);

    List<Order> findByCustomerNo(String customerNo);

    List<Order> findAll();

    default int count() {
        return findAll().size();
    }
}
