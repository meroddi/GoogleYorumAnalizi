package com.meroddi.javaegitimi.m02oop.ders14;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DERS 14 - Enum, ic sinif, erisim belirleyiciler, kendi exception'in.
 *
 * Calistirma:
 *   mvn -q exec:java -Dexec.mainClass=com.meroddi.javaegitimi.m02oop.ders14.EnumDemo
 */
public class EnumDemo {

    public static void main(String[] args) {

        // ---------------------------------------------------------------
        // 1) ENUM TEMELLERI
        // ---------------------------------------------------------------
        System.out.println("--- Tum durumlar ---");
        for (OrderStatus status : OrderStatus.values()) {   // values(): derleyicinin urettigi metot
            System.out.printf("%-10s (%d) %-22s ilerleme: %3d%%  son durum: %-5b  gidebilecekleri: %s%n",
                    status,                  // toString -> sabit adi
                    status.ordinal(),        // tanim sirasi (veri tabanina ordinal YAZMA, ad yaz!)
                    status.getLabel(),
                    status.progressPercent(),
                    status.isTerminal(),
                    status.allowedTransitions());
        }
        System.out.println();

        // ---------------------------------------------------------------
        // 2) DURUM MAKINESI - gecerli akis
        // ---------------------------------------------------------------
        System.out.println("--- Normal siparis akisi ---");
        Order order = new Order("ORD-00501");
        order.changeStatus(OrderStatus.PAID);
        order.changeStatus(OrderStatus.SHIPPED);
        order.changeStatus(OrderStatus.DELIVERED);
        System.out.println("Son durum : " + order.getStatus().getLabel()
                + " (%" + order.getStatus().progressPercent() + ")");
        System.out.println();

        // ---------------------------------------------------------------
        // 3) GECERSIZ GECIS - kendi exception'imiz devrede
        // ---------------------------------------------------------------
        System.out.println("--- Gecersiz gecis denemeleri ---");
        Order second = new Order("ORD-00502");
        try {
            second.changeStatus(OrderStatus.SHIPPED);    // odeme yapilmadan kargo
        } catch (InvalidStatusTransitionException e) {
            System.out.println("Engellendi -> " + e.getMessage());
        }

        second.changeStatus(OrderStatus.CANCELLED);
        try {
            second.changeStatus(OrderStatus.PAID);       // iptal edilmis siparise odeme
        } catch (InvalidStatusTransitionException e) {
            System.out.println("Engellendi -> " + e.getMessage());
            System.out.println("            (exception nesnesinden veri okunabiliyor: "
                    + e.getOrderNo() + ", " + e.getFrom() + " -> " + e.getTo() + ")");
        }
        System.out.println();

        // ---------------------------------------------------------------
        // 4) DURUM GECMISI - ic sinif (nested class) kullanimi
        // ---------------------------------------------------------------
        System.out.println("--- Siparis gecmisi (audit log) ---");
        for (Order.StatusChange change : order.getHistory()) {
            System.out.println("  " + change);
        }
        System.out.println();

        // ---------------------------------------------------------------
        // 5) ENUM'DAN METIN, METINDEN ENUM
        // ---------------------------------------------------------------
        System.out.println("--- valueOf / karsilastirma ---");
        OrderStatus parsed = OrderStatus.valueOf("PAID");    // veri tabanindan okunan metin
        System.out.println("valueOf(\"PAID\") : " + parsed + " -> " + parsed.getLabel());
        try {
            OrderStatus.valueOf("ODENDI");
        } catch (IllegalArgumentException e) {
            System.out.println("valueOf(\"ODENDI\") -> IllegalArgumentException (tanimli degil)");
        }
        // Enum'larda == guvenlidir: her sabitin tek bir nesnesi vardir.
        System.out.println("parsed == OrderStatus.PAID : " + (parsed == OrderStatus.PAID));
        System.out.println();

        // ---------------------------------------------------------------
        // 6) ERISIM BELIRLEYICILER
        // ---------------------------------------------------------------
        System.out.println("--- Erisim belirleyiciler ---");
        System.out.println("private   : sadece kendi sinifi         -> varsayilan tercihin bu olsun");
        System.out.println("(bossa)   : ayni PAKET                  -> test siniflarinda ise yarar");
        System.out.println("protected : ayni paket + alt siniflar   -> kalitim icin acilan kapi");
        System.out.println("public    : herkes                      -> bir kez acarsan geri almak zordur");
        System.out.println();
        System.out.println("Kural: en dar erisimle basla, ihtiyac dogdukca genislet.");
        System.out.println("public yaptigin her sey, ileride degistiremeyecegin bir sozdur.");
    }

    // ==================================================================
    //  Ic siniflar
    // ==================================================================

    /**
     * static nested class: dis sinifin ornegine ihtiyac duymaz.
     * Kendi dosyasina tasinacak kadar buyuk degilse boyle tutulur.
     */
    static class Order {

        private final String orderNo;
        private OrderStatus status = OrderStatus.CREATED;
        private final List<StatusChange> history = new ArrayList<>();

        Order(String orderNo) {                       // paket seviyesinde erisim
            this.orderNo = orderNo;
            history.add(new StatusChange(null, OrderStatus.CREATED));
        }

        void changeStatus(OrderStatus newStatus) {
            if (!status.canTransitionTo(newStatus)) {
                throw new InvalidStatusTransitionException(orderNo, status, newStatus);
            }
            history.add(new StatusChange(status, newStatus));
            System.out.printf("%s : %-10s -> %-10s (%s)%n",
                    orderNo, status, newStatus, newStatus.getLabel());
            this.status = newStatus;
        }

        OrderStatus getStatus() {
            return status;
        }

        List<StatusChange> getHistory() {
            return List.copyOf(history);      // degistirilemez kopya
        }

        /** Ic ice record: durum degisikligi kaydi. */
        record StatusChange(OrderStatus from, OrderStatus to, LocalDateTime at) {

            StatusChange(OrderStatus from, OrderStatus to) {
                this(from, to, LocalDateTime.now());
            }

            @Override
            public String toString() {
                return "%s  %-10s -> %-10s".formatted(
                        at.toLocalTime().withNano(0), from == null ? "(yeni)" : from, to);
            }
        }
    }
}
