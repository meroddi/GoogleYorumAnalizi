package com.meroddi.javaegitimi.m02oop.ders10;

/**
 * DERS 10 - static vs instance.
 *
 * static alan: SINIFA aittir, tum nesneler tek kopyayi paylasir.
 * instance alan: her NESNENIN kendi kopyasi vardir.
 *
 * Burada siparis numarasi sayaci butun uygulamada tek olmali -> static.
 */
public class OrderIdGenerator {

    private static final String PREFIX = "ORD";   // static final = sabit
    private static int counter = 0;               // paylasilan sayac

    private int localCounter = 0;                 // her nesnenin kendi sayaci (karsilastirma icin)

    /** static metot: nesne olusturmadan cagrilir -> OrderIdGenerator.nextId() */
    public static synchronized String nextId() {
        // synchronized: ayni anda iki thread girerse ayni numarayi uretmesin.
        // (Concurrency, yol haritasinda Yil 2-3'un konusu; burada sadece isaret ediyoruz.)
        counter++;
        return String.format("%s-%05d", PREFIX, counter);
    }

    public static int generatedCount() {
        return counter;
    }

    /** instance metot: nesneye ait sayac */
    public int nextLocal() {
        localCounter++;
        return localCounter;
    }

    public int getLocalCounter() {
        return localCounter;
    }
}
