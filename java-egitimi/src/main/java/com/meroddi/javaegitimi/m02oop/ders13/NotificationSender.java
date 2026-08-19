package com.meroddi.javaegitimi.m02oop.ders13;

/**
 * DERS 13 - Bildirim sozlesmesi.
 *
 * Bir sinif TEK bir sinifi extend edebilir ama BIRCOK arayuzu implement edebilir.
 * Bu yuzden yetenekler (capability) arayuzle, kimlik (identity) kalitimla ifade edilir.
 */
public interface NotificationSender {

    /** Bildirimi gonderir. */
    void send(String recipient, String subject, String body);

    /** Kanal adi (EMAIL, SMS, PUSH...). */
    String channel();

    /** Kanal aktif mi? Varsayilan: aktif. */
    default boolean enabled() {
        return true;
    }

    /**
     * STATIC METOT (Java 8+): arayuze ait yardimci.
     * Ilgili yardimci kodu ayri bir "Utils" sinifina dagitmadan tutar.
     */
    static String shortenForSms(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        return text.length() <= maxLength ? text : text.substring(0, maxLength - 3) + "...";
    }
}
