package com.meroddi.javaegitimi.m02oop.ders15.service;

/**
 * DERS 15 - Aranan kayit yoksa firlatilir.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
