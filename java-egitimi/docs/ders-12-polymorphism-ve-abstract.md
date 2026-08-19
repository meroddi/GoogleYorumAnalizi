# Ders 12 — Polymorphism ve abstract class

> Kod: [`m02oop/ders12/`](../src/main/java/com/meroddi/javaegitimi/m02oop/ders12/) — `PaymentMethod.java`, `CreditCardPayment.java`, `BankTransferPayment.java`, `CashOnDeliveryPayment.java`, `PaymentResult.java`, `PolymorphismDemo.java`

## 1. Bu ne?

**Polymorphism (çok biçimlilik)**: aynı çağrı, nesnenin gerçek tipine göre farklı davranır.

**Abstract class**: ortak iskeleti taşıyan ama tek başına nesnesi üretilemeyen sınıf.

```java
public abstract class PaymentMethod {
    public abstract Money commission(Money amount);   // gövdesi yok → alt sınıf doldurmak ZORUNDA
    public int settlementDays() { return 1; }         // gövdesi var → istersen ez
}
```

"Genel ödeme yöntemi" diye somut bir şey yoktur; kredi kartı, havale, kapıda ödeme vardır. `new PaymentMethod(...)` yazamazsın — derleyici izin vermez.

## 2. Nasıl çalışır?

JVM, metot çağrısında **değişkenin tipine değil, nesnenin gerçek sınıfına** bakar (dynamic dispatch / sanal metot tablosu). Bu yüzden şu döngü, hangi ödeme yöntemleriyle çalıştığını bilmeden doğru komisyonu hesaplar:

```java
for (PaymentMethod method : methods) {
    System.out.println(method.displayName() + " komisyon: " + method.commission(cartTotal));
}
```

## 3. Nerede kullanılır?

Bu, yazılım tasarımının en çok karşılığı olan konusudur:

- Ödeme yöntemleri (bu ders)
- Bildirim kanalları: e-posta / SMS / push
- Dosya dışa aktarma: PDF / Excel / CSV
- Kargo firmaları, fiyatlandırma stratejileri, indirim kampanyaları
- Spring'de `JdbcTemplate`, `RestTemplate`, tüm framework genişletme noktaları

**SOLID'in "O"su — Open/Closed**: yeni davranış eklemek için var olan kodu değiştirmemek, yeni sınıf eklemek. Yol haritasının kalıcı çekirdeğinde *"SOLID (kural olarak değil, araç olarak)"* diye geçer.

## 4. Gerçek hayat örneği — ödeme akışı

Ödeme sistemlerinde akış her yöntemde aynıdır, adımlar değişir:

```java
public final PaymentResult pay(Money amount, String orderNo) {
    String validationError = validate(amount);                    // 1. doğrula
    if (validationError != null)
        return PaymentResult.failed(orderNo, providerName, validationError);

    Money commission = commission(amount);                        // 2. komisyonu hesapla
    Money charged = amount.add(commission);
    String reference = execute(charged, orderNo);                 // 3. tahsil et
    return PaymentResult.success(orderNo, providerName, charged, commission,
                                 reference, settlementDays());    // 4. sonucu döndür
}
```

Alt sınıflar sadece boşlukları dolduruyor:

| | Kredi kartı | Havale/EFT | Kapıda ödeme |
|---|---|---|---|
| Komisyon | %2.3 + taksit başına %0.4 | 0 | sabit 29,90 TL |
| Doğrulama | limit + taksit 1-12 | IBAN geçerli mi | tutar ≤ 5.000 TL |
| Valör (gün) | tek çekim 2, taksitli 30 | 0 | 7 |

```java
// CreditCardPayment
@Override
public Money commission(Money amount) {
    BigDecimal rate = new BigDecimal("0.023")
            .add(new BigDecimal("0.004").multiply(BigDecimal.valueOf(installments - 1L)));
    return new Money(amount.amount().multiply(rate), amount.currency());
}

@Override
protected String validate(Money amount) {
    String base = super.validate(amount);          // üst sınıfın genel kontrolü
    if (base != null) return base;
    if (amount.isGreaterThan(limit)) return "Kart limiti yetersiz (limit: " + limit + ")";
    if (installments < 1 || installments > 12) return "Taksit sayisi 1-12 arasinda olmali";
    return null;
}
```

Çıktı:

```
Kredi Karti 4506 **** **** 5678 (tek cekim)  komisyon: 79.35 TRY   valor: 2 gun
Kredi Karti 5528 **** **** 4321 (6 taksit)   komisyon: 148.35 TRY  valor: 30 gun
Havale/EFT (TR3300...1326)                   komisyon: 0.00 TRY    valor: 0 gun
Kapida Odeme (Aras Kargo)                    komisyon: 29.90 TRY   valor: 7 gun
```

Ve iş mantığı — "en ucuz yöntemi seç" — hiçbir `if` içermeden yazılabiliyor:

```java
PaymentMethod cheapest = methods[0];
for (PaymentMethod m : methods) {
    if (cheapest.commission(cartTotal).isGreaterThan(m.commission(cartTotal))) cheapest = m;
}
```

## 5. Exception mi, sonuç nesnesi mi?

`pay()` başarısızlıkta exception fırlatmıyor, `PaymentResult` döndürüyor:

```java
public record PaymentResult(boolean successful, String orderNo, String provider,
                            Money chargedAmount, Money commission, String reference,
                            int settlementDays, String errorMessage) { }
```

Kural: **beklenen iş kuralı sonuçları için sonuç nesnesi, beklenmeyen durumlar için exception.** "Kart limiti yetersiz" beklenen bir sonuçtur — kullanıcıya gösterilir, akış devam eder. "Veritabanı bağlantısı koptu" beklenmeyen bir durumdur — exception.

## 6. Çalıştır

```bash
mvn -q exec:java -Dexec.mainClass=com.meroddi.javaegitimi.m02oop.ders12.PolymorphismDemo
```

## 7. Sık yapılan hatalar

- **Tip kontrolüyle dallanmak**: `if (method instanceof CreditCardPayment) { ... } else if (...)`. Bu, polymorphism'in var olma sebebini yok eder. Yeni yöntem eklendiğinde bu `if` zincirlerinin hepsini bulman gerekir.
- Abstract sınıfa çok fazla soyut metot koymak: alt sınıflar 10 metot doldurmak zorunda kalır.
- Alt sınıfın üst sınıf sözleşmesini bozması (Liskov ihlali): `withdraw` her yerde bakiyeyi azaltmalı; bir alt sınıf sessizce hiçbir şey yapmıyorsa, çağıran kod yanlış çalışır.
- `protected` metotları `public` yapıp genişletme noktasını API'ye dönüştürmek.

## 8. Mülakat notu

> "Abstract class ile interface arasındaki fark?"

Abstract class **durum** (alan) ve ortak kod taşır, tek atadan türenirsin, constructor'ı vardır. Interface sözleşmedir, bir sınıf birçoğunu uygulayabilir, alan tutamaz (sadece `static final` sabit). Ortak *durum ve iskelet* varsa abstract class; sadece *yetenek* tanımlıyorsan interface (Ders 13).

## 9. Alıştırma

1. `DigitalWalletPayment` ekle (Papara/Ininal gibi): komisyon %1, valör 1 gün, 10.000 TL üstü işlemi reddetsin. **`PolymorphismDemo` içindeki döngülerin hiçbirini değiştirmeden** çalıştığını gör.
2. `PaymentMethod`'a `refund(Money amount, String reference)` ekle: kredi kartında 30 gün içinde, havalede her zaman, kapıda ödemede hiç mümkün olmasın.
3. `ShippingProvider` hiyerarşisi kur (Aras/MNG/Yurtiçi): desi hesabı ve teslim süresi farklı olsun; en ucuz kargoyu seçen metodu yaz.
4. Ders 04'teki `switch (paymentMethod)` ile komisyon hesaplayan kodu bu tasarımla karşılaştır: yeni yöntem eklemek her ikisinde kaç dosyayı değiştirmeni gerektiriyor?

---
**Önceki:** [Ders 11](ders-11-kalitim.md) | **Sonraki:** [Ders 13 — Interface ve dependency injection](ders-13-interface.md)
