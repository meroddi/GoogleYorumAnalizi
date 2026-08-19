# Ders 14 — Enum, kendi exception'ın, iç sınıflar, erişim belirleyiciler

> Kod: [`m02oop/ders14/`](../src/main/java/com/meroddi/javaegitimi/m02oop/ders14/) — `OrderStatus.java`, `InvalidStatusTransitionException.java`, `EnumDemo.java`

## 1. Bu ne?

**Enum**, sabit ve sonlu bir kümeyi tip güvenli biçimde ifade eder: sipariş durumları, para birimleri, roller, gün adları.

```java
String status = "PAİD";        // yazım hatası — derleyici göremez, üretimde patlar
OrderStatus.PAID               // yanlış yazarsan DERLENMEZ
```

Ama enum sadece etiket değildir: **davranış taşır**.

## 2. Nasıl çalışır?

Her enum sabiti aslında o enum sınıfının bir **nesnesidir** ve JVM'de tek örnektir (singleton). Bu yüzden enum'larda `==` güvenlidir.

```java
public enum OrderStatus {
    CREATED("Siparis alindi", false),
    PAID("Odeme tamamlandi", false),
    SHIPPED("Kargoya verildi", false),
    DELIVERED("Teslim edildi", true),
    CANCELLED("Iptal edildi", true),
    REFUNDED("Iade edildi", true);

    private final String label;
    private final boolean terminal;

    OrderStatus(String label, boolean terminal) {    // constructor her zaman private
        this.label = label;
        this.terminal = terminal;
    }
}
```

## 3. Gerçek hayat örneği — durum makinesi (state machine)

Her sipariş sisteminin kalbinde bir durum makinesi vardır. Kuralları enum'un içine koymak, onların kodun 10 farklı yerine dağılmasını önler:

```java
public Set<OrderStatus> allowedTransitions() {
    return switch (this) {
        case CREATED -> EnumSet.of(PAID, CANCELLED);
        case PAID -> EnumSet.of(SHIPPED, REFUNDED, CANCELLED);
        case SHIPPED -> EnumSet.of(DELIVERED);
        case DELIVERED -> EnumSet.of(REFUNDED);
        case CANCELLED, REFUNDED -> EnumSet.noneOf(OrderStatus.class);
    };
}

public boolean canTransitionTo(OrderStatus target) {
    return allowedTransitions().contains(target);
}
```

Bu switch'te `default` yok — bilerek. Yeni bir durum eklersen **derleyici eksik dalı sana söyler**. `String` kullansaydın hiçbir uyarı almazdın; enum'un asıl değeri budur.

Çıktı:

```
CREATED    (0) Siparis alindi     ilerleme:  10%  son durum: false  gidebilecekleri: [PAID, CANCELLED]
PAID       (1) Odeme tamamlandi   ilerleme:  40%  son durum: false  gidebilecekleri: [SHIPPED, CANCELLED, REFUNDED]
SHIPPED    (2) Kargoya verildi    ilerleme:  75%  son durum: false  gidebilecekleri: [DELIVERED]
CANCELLED  (4) Iptal edildi       ilerleme:   0%  son durum: true   gidebilecekleri: []
```

## 4. Kendi exception sınıfın

```java
public class InvalidStatusTransitionException extends RuntimeException {
    public InvalidStatusTransitionException(String orderNo, OrderStatus from, OrderStatus to) {
        super("%s: %s -> %s gecisi yapilamaz. Izin verilenler: %s"
                .formatted(orderNo, from, to, from.allowedTransitions()));
        ...
    }
}
```

İyi bir exception mesajı **sorunu çözecek bilgiyi taşır**:

```
ORD-00502: CREATED -> SHIPPED gecisi yapilamaz. Izin verilenler: [PAID, CANCELLED]
```

"Geçersiz durum" yazan bir mesaj, gece 3'te seni log'ların içinde 40 dakika dolaştırır.

### Checked vs unchecked

| | Checked (`Exception`) | Unchecked (`RuntimeException`) |
|---|---|---|
| Yakalamak | zorunlu (`throws` veya `try`) | isteğe bağlı |
| Ne için | çağıranın **anlamlı şekilde ele alabileceği** durumlar (dosya yok, ağ hatası) | programlama hatası + iş kuralı ihlali |

Modern Java'da iş kuralı ihlalleri genelde unchecked yapılır (Spring'in tüm exception hiyerarşisi böyledir): çağıran katman zaten yapabileceği tek şey kullanıcıya bildirmektir.

## 5. İç sınıflar (nested class)

```java
static class Order {
    private OrderStatus status = OrderStatus.CREATED;
    private final List<StatusChange> history = new ArrayList<>();

    record StatusChange(OrderStatus from, OrderStatus to, LocalDateTime at) { }
}
```

- **static nested class**: dış sınıfın örneğine ihtiyaç duymaz. Küçük yardımcı tipler için idealdir.
- **inner class** (`static` olmayan): dış nesneye referans tutar. Bellek sızıntısı kaynağı olabilir; nadiren gerekir.
- Kendi dosyasına taşınacak kadar büyüdüyse taşı.

Durum geçmişi (audit log), gerçek sistemlerde denetim ve müşteri şikâyeti çözümü için zorunludur:

```
16:24:34  (yeni)     -> CREATED
16:24:34  CREATED    -> PAID
16:24:34  PAID       -> SHIPPED
16:24:34  SHIPPED    -> DELIVERED
```

## 6. Erişim belirleyiciler

| Belirleyici | Kimler görür | Ne zaman |
|---|---|---|
| `private` | sadece kendi sınıfı | **varsayılan tercihin bu olsun** |
| (boş) | aynı paket | test sınıflarında işe yarar |
| `protected` | aynı paket + alt sınıflar | kalıtım için açılan kapı |
| `public` | herkes | bir kez açarsan geri almak zordur |

**Kural: en dar erişimle başla, ihtiyaç doğdukça genişlet.** `public` yaptığın her şey, ileride değiştiremeyeceğin bir sözdür.

## 7. Çalıştır

```bash
mvn -q exec:java -Dexec.mainClass=com.meroddi.javaegitimi.m02oop.ders14.EnumDemo
mvn -q test -Dtest=OrderStatusTest
```

## 8. Sık yapılan hatalar

- **Durumu `String` tutmak.** `"SHİPPED"` (Türkçe İ) hatası, derleyicinin göremediği en sinsi buglardan.
- **Veri tabanına `ordinal()` yazmak.** Enum sırasını değiştirince tüm geçmiş veri anlamını yitirir. Her zaman `name()` (metin) sakla.
- `valueOf("YOK")` çağrısını try/catch'siz yapmak → `IllegalArgumentException`.
- **Exception yutmak**: `catch (Exception e) { }`. Hata kayboldu, sistem yanlış çalışmaya devam ediyor. En kötü pratiktir.
- `catch (Exception e)` ile her şeyi yakalamak — `NullPointerException` de yakalanır ve gerçek bug gizlenir.
- Exception'ı akış kontrolü için kullanmak (döngüden çıkmak vb.) — pahalı ve yanıltıcıdır.

## 9. Mülakat notu

> "Enum neden String'den iyidir?"

Tip güvenliği (yanlış değer derlenmez), sonlu küme garantisi, davranış taşıyabilmesi, `switch`'te tamlık kontrolü, `EnumSet`/`EnumMap` ile çok verimli koleksiyonlar ve JVM'de tek örnek olması (`==` güvenli).

## 10. Alıştırma

1. `PaymentStatus` enum'u yaz (`PENDING`, `AUTHORIZED`, `CAPTURED`, `FAILED`, `REFUNDED`) ve geçiş kurallarını tanımla; geçersiz geçişte kendi exception'ını fırlat.
2. `OrderStatus`'a `RETURNED` durumu ekle. `allowedTransitions()` ve `progressPercent()` switch'lerinde derleyicinin seni nasıl uyardığını gözlemle.
3. `Currency` enum'u yaz (`TRY`, `USD`, `EUR`): sembol, ondalık hane sayısı ve `format(BigDecimal)` metodu olsun.
4. `EnumMap<OrderStatus, Integer>` ile sipariş durumlarına göre sayım yapan bir rapor metodu yaz.

---
**Önceki:** [Ders 13](ders-13-interface.md) | **Sonraki:** [Ders 15 — Modül 2 projesi](ders-15-modul2-projesi.md)
