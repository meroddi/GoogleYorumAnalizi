package com.meroddi.javaegitimi.m02oop.ders13;

import com.meroddi.javaegitimi.m02oop.ders10.Money;

/**
 * DERS 13 - Ornek veri nesnesi (kayit ozeti).
 */
public record OrderSummary(String orderNo, String customerEmail, String customerPhone, Money total) {
}
