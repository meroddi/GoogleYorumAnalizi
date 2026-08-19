package com.meroddi.javaegitimi.m02oop.ders15.repository;

import com.meroddi.javaegitimi.m02oop.ders15.model.Product;

import java.util.List;
import java.util.Optional;

/**
 * DERS 15 - REPOSITORY KATMANI: veri erisim sozlesmesi (Ders 13).
 */
public interface ProductRepository {

    void save(Product product);

    Optional<Product> findBySku(String sku);

    List<Product> findAll();
}
