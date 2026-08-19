# Ders 10 — Encapsulation, immutability, record, static

> Kod: [`m02oop/ders10/`](../src/main/java/com/meroddi/javaegitimi/m02oop/ders10/) — `Money.java`, `LoyaltyAccount.java`, `OrderIdGenerator.java`, `EncapsulationDemo.java`

## 1. Bu ne?

**Encapsulation (kapsülleme)**: alanlar `private`, dışarıya sadece kontrollü kapılar açılır.

Amaç "getter/setter yazmak" değildir — amaç, nesnenin **geçersiz duruma düşmesini imkânsız kılmak**tır. Bir nesnenin her an doğru olması gereken kurallarına **invariant** denir:

- Sadakat puanı asla negatif olamaz
- Sipariş toplamı satırların toplamına eşittir
- Kapalı hesaba para yatırılamaz

**Immutability (değişmezlik)**: nesne oluştuktan sonra hiç değişmez. Değiştirmek istersen yeni nesne üretirsin.

## 2. Nasıl çalışır?

### Setter yerine anlamlı metot

```java
// KÖTÜ: kuralı çağırana bırakır
public void setPoints(int points) { this.points = points; }
account.setPoints(account.getPoints() - 50);     // negatif olabilir, kimse engellemez

// İYİ: kural nesnenin içinde
public void spend(int amountToSpend) {
    if (amountToSpend <= 0)      throw new IllegalArgumentException("...");
    if (amountToSpend > points)  throw new IllegalStateException("Yetersiz puan...");
    this.points -= amountToSpend;        // invariant korunur
}
```

`setPoints(-50)` bir iş kuralı değildir; `spend(50)` iş kuralıdır ve kontrol edilebilir.

### Savunmacı kopya (defensive copy)

Değiştirilebilir bir alanı olduğu gibi dışarı vermek, kapsüllemeyi delip geçer:

```java
// SIZDIRAN
public String[] getUsedCampaignCodesLeaky() { return usedCampaignCodes; }

String[] leaked = account.getUsedCampaignCodesLeaky();
leaked[0] = "HACKED";        // nesnenin İÇ durumu dışarıdan bozuldu

// DOĞRU
public String[] getUsedCampaignCodes() {
    return Arrays.copyOf(usedCampaignCodes, usedCampaignCodes.length);
}
```

Aynı şey constructor'da da geçerli: dışarıdan gelen diziyi/listeyi doğrudan saklama, kopyala.

## 3. `record` — Java'nın en verimli özelliği

```java
public record Money(BigDecimal amount, String currency) { }
```

Bu tek satır şunları üretir: `private final` alanlar, constructor, `equals`, `hashCode`, `toString`, `amount()` / `currency()` erişimcileri. Elle yazsan ~60 satır.

**Compact constructor** ile doğrulama ve normalizasyon:

```java
public Money {
    if (amount == null) throw new IllegalArgumentException("Tutar null olamaz");
    if (currency == null || currency.length() != 3)
        throw new IllegalArgumentException("Para birimi 3 harf olmali");
    amount = amount.setScale(2, RoundingMode.HALF_UP);    // her zaman 2 hane
    currency = currency.toUpperCase();
}
```

İşlemler yeni nesne döndürür — orijinal asla değişmez:

```java
public Money add(Money other) {
    requireSameCurrency(other);
    return new Money(this.amount.add(other.amount), this.currency);
}
```

### Value object nedir?

`Money` bir **value object**tir: kimliği yoktur, **değeri** onu tanımlar. 100 TL ile 100 TL aynı şeydir — tıpkı 5 sayısı gibi. Buna karşılık `Customer` bir **entity**dir: adı değişse bile aynı müşteridir, kimliği `customerNo`'dur.

Neden immutable?
- Çoklu thread'de güvenli (yol haritasının Yıl 2 konusu: thread safety, race condition)
- "Bu değer nerede değişti?" sorusu ortadan kalkar
- `Map` anahtarı olarak güvenle kullanılır (hash değeri değişmez)

Farklı para birimlerini toplamayı engellemek de burada olur — muhasebe hatalarının klasik kaynağı:

```java
Money.tryOf("100.00").add(new Money(new BigDecimal("50"), "USD"));
// IllegalArgumentException: Farkli para birimleri toplanamaz: TRY + USD
```

## 4. `static` vs instance

```java
public class OrderIdGenerator {
    private static int counter = 0;      // SINIFA ait — tüm nesneler paylaşır
    private int localCounter = 0;        // her NESNENİN kendi kopyası

    public static synchronized String nextId() {
        counter++;
        return String.format("ORD-%05d", counter);
    }
}

OrderIdGenerator.nextId();   // nesne üretmeden çağrılır → ORD-00001
```

Sipariş numarası tüm uygulamada tek olmalı → `static`. `synchronized` de burada bir ipucu: iki thread aynı anda girerse aynı numarayı üretebilir (Yıl 2 konusu).

Sabitler de `static final`:

```java
private static final BigDecimal VAT_RATE = new BigDecimal("0.20");
```

## 5. Çalıştır

```bash
mvn -q exec:java -Dexec.mainClass=com.meroddi.javaegitimi.m02oop.ders10.EncapsulationDemo
mvn -q test -Dtest=MoneyTest        # Money için birim testler
```

## 6. Sık yapılan hatalar

- **Refleks getter/setter.** IDE'nin ürettiği 40 satır setter, kapsüllemeyi ortadan kaldırır. Her setter için sor: "bunu dışarıdan değiştirmek gerçekten bir iş kuralı mı?"
- Değiştirilebilir alanı (dizi, `List`, `Date`) kopyalamadan geri vermek.
- `record`'u sadece "kısa sınıf" sanmak; asıl değeri değişmezlik + değer eşitliğidir.
- Her şeyi `static` yapmak. Static alan = global değişken; test edilemez, thread-safe değil.
- `BigDecimal.equals` ile karşılaştırma (scale farkı yüzünden `1.0 != 1.00`) — `compareTo` kullan. Bizim `Money` bunu compact constructor'da scale'i sabitleyerek çözüyor.

## 7. Mülakat notu

> "equals ile hashCode'u neden birlikte ezersin?"

Sözleşme: iki nesne `equals` ise `hashCode`'ları da eşit olmalı. `HashMap`/`HashSet` önce `hashCode` ile kovayı bulur, sonra `equals` ile karşılaştırır. Sadece `equals`'ı ezersen nesneni koyduğun `HashMap`'te bir daha bulamazsın. `record` ikisini de senin için üretir.

## 8. Alıştırma

1. `Money`'ye `divide(int parts)` ekle: 100.00 TL'yi 3'e böl; kuruş artığını **son parçaya** yaz, toplamın hâlâ 100.00 olduğunu bir testle kanıtla.
2. `record Address(String city, String district, String postalCode)` yaz; posta kodu 5 haneli değilse constructor reddetsin.
3. `LoyaltyAccount`'a `transferTo(LoyaltyAccount other, int points)` ekle: iki hesabın toplam puanının değişmediğini test et.
4. Değiştirilebilir bir sınıfı (`Product`) immutable hâle getir: her değişiklikte yeni nesne dönsün (`withPrice`, `withStock`). Hangi durumlarda bu iyi, hangi durumlarda zahmetli — bir paragraf yaz.

---
**Önceki:** [Ders 09](ders-09-sinif-ve-nesne.md) | **Sonraki:** [Ders 11 — Kalıtım](ders-11-kalitim.md)
