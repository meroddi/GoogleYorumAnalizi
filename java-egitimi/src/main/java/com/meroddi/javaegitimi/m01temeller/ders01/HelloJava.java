package com.meroddi.javaegitimi.m01temeller.ders01;

/**
 * DERS 01 - Java nedir, ilk program.
 *
 * Bu dosyanin her satirini "neden boyle?" sorusuyla oku. Java'da hicbir sey
 * suslemek icin degildir; her kelimenin JVM tarafinda bir karsiligi vardir.
 *
 * Calistirma:
 *   mvn -q exec:java -Dexec.mainClass=com.meroddi.javaegitimi.m01temeller.ders01.HelloJava
 */
public class HelloJava {

    /**
     * main metodu: JVM'in programa girdigi tek kapi.
     *
     * public  -> JVM disaridan cagirabilsin diye herkese acik
     * static  -> Cagirmak icin once bir nesne olusturmak gerekmesin diye
     * void    -> Geriye deger dondurmez
     * String[] args -> Komut satirindan gelen parametreler
     */
    public static void main(String[] args) {
        System.out.println("Merhaba Java!");
        System.out.println();

        // 1) Bu kod nasil calisiyor? Zinciri programin kendisine yazdiralim.
        System.out.println("--- Kaynak koddan calisan programa giden yol ---");
        System.out.println("HelloJava.java  (kaynak kod, insanin okudugu)");
        System.out.println("      | javac  -> derleyici");
        System.out.println("HelloJava.class (bytecode, JVM'in okudugu)");
        System.out.println("      | java   -> JVM baslatir, bytecode'u yorumlar");
        System.out.println("      | JIT    -> sik calisan kismi makine koduna cevirir");
        System.out.println("Islemci uzerinde calisan program");
        System.out.println();

        // 2) Uzerinde calistigimiz JVM kendini tanitsin.
        System.out.println("--- Bu programi calistiran JVM ---");
        System.out.println("Java surumu   : " + System.getProperty("java.version"));
        System.out.println("JVM adi       : " + System.getProperty("java.vm.name"));
        System.out.println("Isletim sistemi: " + System.getProperty("os.name"));
        // Ayni .class dosyasi Windows'ta, Mac'te, Linux sunucuda ayni sekilde calisir.
        // "Write once, run anywhere" cumlesinin somut karsiligi tam olarak budur.
        System.out.println();

        // 3) Komut satiri parametreleri: gercek uygulamalarda ortam/konfigurasyon gecmenin
        //    en ilkel ama en yaygin yolu. Ornek:
        //    mvn -q exec:java -Dexec.mainClass=...HelloJava -Dexec.args="prod 8080"
        System.out.println("--- Komut satirindan gelen parametreler ---");
        if (args.length == 0) {
            System.out.println("Parametre verilmedi.");
        } else {
            for (int i = 0; i < args.length; i++) {
                System.out.println("args[" + i + "] = " + args[i]);
            }
        }
    }
}
