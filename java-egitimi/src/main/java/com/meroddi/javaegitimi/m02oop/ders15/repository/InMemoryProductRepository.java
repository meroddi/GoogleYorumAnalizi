package com.meroddi.javaegitimi.m02oop.ders15.repository;

import com.meroddi.javaegitimi.m02oop.ders15.model.Product;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * DERS 15 - Bellek ici urun deposu.
 * Yarin bunun yerine JDBC/JPA uygulamasi konur; servis kodu degismez.
 */
public class InMemoryProductRepository implements ProductRepository {

    private final Map<String, Product> storage = new LinkedHashMap<>();

    @Override
    public void save(Product product) {
        storage.put(product.getSku(), product);
    }

    @Override
    public Optional<Product> findBySku(String sku) {
        return Optional.ofNullable(storage.get(sku));
    }

    @Override
    public List<Product> findAll() {
        return new ArrayList<>(storage.values());
    }
}
