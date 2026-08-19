package com.meroddi.javaegitimi.m02oop.ders13;

/**
 * DERS 13 - E-posta kanali.
 */
public class EmailNotificationSender implements NotificationSender {

    private final String smtpHost;

    public EmailNotificationSender(String smtpHost) {
        this.smtpHost = smtpHost;
    }

    @Override
    public void send(String recipient, String subject, String body) {
        // Gercek hayatta: SMTP baglantisi / SendGrid API cagrisi
        System.out.printf("  [EMAIL via %s] -> %s%n", smtpHost, recipient);
        System.out.printf("    Konu : %s%n", subject);
        System.out.printf("    Govde: %s%n", body.replace("\n", " | "));
    }

    @Override
    public String channel() {
        return "EMAIL";
    }
}
