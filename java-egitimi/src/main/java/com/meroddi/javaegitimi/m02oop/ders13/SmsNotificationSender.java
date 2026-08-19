package com.meroddi.javaegitimi.m02oop.ders13;

/**
 * DERS 13 - SMS kanali.
 *
 * Ayni sozlesme, tamamen farkli davranis: 160 karakter siniri ve
 * gece saatlerinde gonderim kapali (KVKK / ticari ileti mevzuati).
 */
public class SmsNotificationSender implements NotificationSender {

    private final String operator;
    private final int currentHour;   // gercek hayatta saat sistemden okunur; testte sabitlenir

    public SmsNotificationSender(String operator, int currentHour) {
        this.operator = operator;
        this.currentHour = currentHour;
    }

    @Override
    public void send(String recipient, String subject, String body) {
        String text = NotificationSender.shortenForSms(subject + ": " + body.replace("\n", " "), 160);
        System.out.printf("  [SMS via %s] -> %s : %s%n", operator, recipient, text);
    }

    @Override
    public String channel() {
        return "SMS";
    }

    /** Varsayilan davranisi eziyoruz: 22:00 - 08:00 arasi kapali. */
    @Override
    public boolean enabled() {
        return currentHour >= 8 && currentHour < 22;
    }
}
