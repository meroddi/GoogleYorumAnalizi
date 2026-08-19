# Ders 04 — Karar yapıları: if, ternary, modern switch

> Kod: [`m01temeller/ders04/ShippingRulesDemo.java`](../src/main/java/com/meroddi/javaegitimi/m01temeller/ders04/ShippingRulesDemo.java)

## 1. Bu ne?

Program akışını koşula göre dallandıran yapılar:

```java
if (koşul) { ... } else if (başkaKoşul) { ... } else { ... }

String label = stok > 0 ? "Stokta" : "Tükendi";      // ternary

String metin = switch (durum) {                       // modern switch (Java 14+)
    case "PAID" -> "Ödeme onaylandı";
    default -> "Bilinmeyen";
};
```

## 2. Nasıl çalışır?

`if / else if` zincirinde **ilk eşleşen kural kazanır**, geri kalanlar hiç değerlendirilmez. Bu yüzden kuralların **sırası bir iş kararıdır**:

```java
if (weightKg > 15.0)            return new BigDecimal("149.90");  // ağır kargo önce
else if ("EXPRESS".equals(type)) return new BigDecimal("79.90");
else if (cartTotal >= 300)       return BigDecimal.ZERO;          // ücretsiz kargo
else                             return new BigDecimal("39.90");
```

Sırayı değiştirirsen 18 kg'lık ürünü de "300 TL üstü ücretsiz" diye bedava gönderirsin. Gerçek e-ticaret sistemlerinde bu tip sıra hataları doğrudan para kaybıdır.

### Modern switch neden daha iyi?

Eski `switch`'te her `case` sonuna `break` yazmayı unutursan alttaki case'e "düşer" (fall-through) ve sessizce yanlış çalışır. `->` biçiminde bu sorun yoktur, ayrıca **değer döndürebilir**:

```java
return switch (status) {
    case "CREATED" -> "Siparişiniz alındı";
    case "SHIPPED", "IN_TRANSIT" -> "Kargoya verildi";   // birden fazla etiket
    default -> "Bilinmeyen durum";                        // default yoksa DERLENMEZ
};
```

Derleyicinin "default yok" diye bağırması bir özelliktir: her ihtimali düşünmeye zorlar.

## 3. Nerede kullanılır?

Her kurumsal sistemin içinde bir **kural motoru** vardır:

- Kargo ücreti / teslimat süresi hesabı
- İndirim ve kampanya uygulanabilirliği
- Ödeme risk skoru ve fraud kararı (onay / manuel inceleme / red)
- Yetkilendirme: bu kullanıcı bu işlemi yapabilir mi
- Kredi başvurusu ön değerlendirme

## 4. Gerçek hayat örneği — risk skoru

Ödeme sistemlerinde her işlem bir **risk skoru** alır. Kurallar birikimlidir:

```java
private static int riskScore(BigDecimal amount, int pastOrderCount,
                             boolean newDevice, String country) {
    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
        return 100;                        // guard clause: anlamsız girdi hemen elenir
    }

    int score = 0;
    if (amount.compareTo(new BigDecimal("10000")) > 0)      score += 40;
    else if (amount.compareTo(new BigDecimal("2000")) > 0)  score += 15;

    if (pastOrderCount == 0)      score += 25;    // hiç sipariş vermemiş
    else if (pastOrderCount < 3)  score += 10;

    if (newDevice)                score += 20;
    if (!"TR".equals(country))    score += 15;

    return Math.min(score, 100);
}
```

Karar:

```java
String decision = switch (score / 25) {     // 0-24 / 25-49 / 50-74 / 75-100
    case 0 -> "OTOMATİK ONAY";
    case 1 -> "ONAY (izlemede)";
    case 2 -> "MANUEL İNCELEME";
    default -> "RED";
};
```

Çıktı:

```
    50.00 TL | geçmiş sipariş: 12 | yeni cihaz: false | ülke: TR -> skor   0 -> OTOMATİK ONAY
 25000.00 TL | geçmiş sipariş:  1 | yeni cihaz: false | ülke: TR -> skor  50 -> MANUEL İNCELEME
  8000.00 TL | geçmiş sipariş:  0 | yeni cihaz: true  | ülke: RU -> skor  75 -> RED
```

### Guard clause (erken çıkış)

İç içe `if` yerine, geçersiz durumları en başta eleyip fonksiyondan çık. Kod düz kalır:

```java
// KÖTÜ                            // İYİ
if (a != null) {                    if (a == null) return ...;
    if (b > 0) {                    if (b <= 0)    return ...;
        if (c.isValid()) {          if (!c.isValid()) return ...;
            ...                     ...
```

Yol haritasındaki *"kod okuma: kendi yazdığından 10 kat fazla başkasının kodunu okuyacaksın"* maddesi tam da burada karşılık bulur: okunabilir kod yazmak nezaket değil, mühendislik gereğidir.

## 5. Çalıştır

```bash
mvn -q exec:java -Dexec.mainClass=com.meroddi.javaegitimi.m01temeller.ders04.ShippingRulesDemo
```

## 6. Sık yapılan hatalar

- `if (x = 5)` yazmak (atama) — Java'da bu `boolean` olmadığı için derlenmez; C'de sessiz bug'dır. Java'nın tip sistemi seni korudu.
- Kayan noktalı sayıları `==` ile karşılaştırmak: `(0.1 + 0.2) == 0.3` → `false`. Parada `BigDecimal.compareTo` kullan.
- `BigDecimal`'de `equals` kullanmak: `new BigDecimal("1.0").equals(new BigDecimal("1.00"))` → `false` (scale farklı). Karşılaştırmada **her zaman `compareTo`**.
- İç içe 4-5 seviye `if`. Bu bir tasarım kokusudur; guard clause veya polymorphism (Ders 12) ile düzleştir.
- Ternary'yi iç içe yazmak: `a ? b : c ? d : e` — okunmaz.

## 7. Mülakat notu

> "switch expression ile klasik switch farkı nedir?"

`->` biçimi fall-through yapmaz (break gerekmez), değer döndürebilir, `default` zorunludur (enum'da tüm sabitler kapsanmışsa gerekmez) ve `yield` ile blok gövdesi kullanılabilir.

## 8. Alıştırma

1. **Kargo süresi hesaplayıcı**: şehir (İstanbul/Ankara/diğer), ürün tipi (normal/kırılgan) ve sipariş saatine göre teslim gün sayısını döndüren metot yaz.
2. **BMI sınıflandırıcı**: boy ve kilo alıp zayıf/normal/fazla kilolu/obez döndür; `if/else` ve `switch` ile iki ayrı sürümünü yaz.
3. Risk skoru metoduna "aynı karttan son 1 saatte 3'ten fazla işlem" kuralını ekle (+30 puan).

---
**Önceki:** [Ders 03](ders-03-operatorler-ve-string.md) | **Sonraki:** [Ders 05 — Döngüler](ders-05-donguler.md)
