# Ders 13 — Interface, default metot ve dependency injection

> Kod: [`m02oop/ders13/`](../src/main/java/com/meroddi/javaegitimi/m02oop/ders13/) — `OrderRepository.java`, `InMemoryOrderRepository.java`, `NotificationSender.java`, `EmailNotificationSender.java`, `SmsNotificationSender.java`, `OrderNotificationService.java`, `InterfaceDemo.java`

## 1. Bu ne?

Interface bir **sözleşmedir**: "bunu yapabilen her sınıf şu metotları sağlamalıdır" der, nasıl yapıldığını söylemez.

```java
public interface OrderRepository {
    void save(OrderSummary order);
    Optional<OrderSummary> findByOrderNo(String orderNo);
    List<OrderSummary> findAll();

    default int count() { return findAll().size(); }        // gövdeli metot (Java 8+)
    static String helper() { ... }                          // arayüze ait yardımcı
}
```

Bir sınıf **tek** bir sınıfı `extends` edebilir ama **birçok** interface'i `implements` edebilir. Bu yüzden: kimlik → kalıtım, yetenek → interface.

## 2. Nasıl çalışır? — bağımlılığı ters çevirmek

```java
public class OrderNotificationService {
    private final OrderRepository repository;           // SOMUT SINIF DEĞİL, sözleşme
    private final List<NotificationSender> senders;

    public OrderNotificationService(OrderRepository repository, List<NotificationSender> senders) {
        this.repository = repository;                   // bağımlılıklar DIŞARIDAN verilir
        this.senders = senders;
    }
}
```

Servisin içinde `InMemoryOrderRepository` veya `EmailNotificationSender` kelimeleri **geçmez**. Bunun adı **dependency injection (bağımlılık enjeksiyonu)**.

Ne kazandırır?

| | Somut sınıfa bağlı | Arayüze bağlı |
|---|---|---|
| Veri tabanını değiştirmek | servis kodu değişir | sadece kurulum satırı değişir |
| Test etmek | gerçek DB/SMTP gerekir | sahte (fake) sınıf yeterli |
| Aynı servisi farklı yapılandırmak | kopyala-yapıştır | aynı sınıf, farklı parametre |

**Spring Boot'un yaptığı iş tam olarak budur**: bu enjeksiyonu senin yerine otomatik yapar. Yıl 1 hattında Spring Boot'a geldiğinde `@Service`, `@Repository`, `@Autowired` anotasyonlarını görünce "aa bu Ders 13'teki şey" diyeceksin.

## 3. Nerede kullanılır?

- **Repository deseni**: `OrderRepository` arayüzü + JPA/JDBC/bellek-içi uygulamaları
- **Bildirim/entegrasyon**: e-posta, SMS, push, webhook
- **Strateji**: fiyatlandırma, indirim, sıralama kuralları
- **Java standart kütüphanesi**: `List`, `Map`, `Comparable`, `Runnable`, `Comparator` — hepsi interface

## 4. Gerçek hayat örneği — bildirim kanalları

```java
public interface NotificationSender {
    void send(String recipient, String subject, String body);
    String channel();
    default boolean enabled() { return true; }                       // varsayılan davranış
    static String shortenForSms(String text, int max) { ... }        // yardımcı
}
```

SMS kanalı varsayılanı eziyor — ticari ileti mevzuatı gereği gece gönderim yok:

```java
public class SmsNotificationSender implements NotificationSender {
    @Override
    public boolean enabled() { return currentHour >= 8 && currentHour < 22; }
}
```

Servis hiçbir kanalı tanımadan hepsiyle çalışıyor:

```java
for (NotificationSender sender : senders) {
    if (!sender.enabled()) { System.out.println("  [" + sender.channel() + "] kanal kapali"); continue; }
    sender.send(recipient, subject, body);
}
```

Çıktı (saat 14:00 vs 23:00):

```
--- Siparis olusturma (gunduz) ---      --- Siparis olusturma (gece 23:00) ---
  [EMAIL via smtp...] -> ayse@...          [EMAIL via smtp...] -> mehmet@...
  [SMS via Turkcell] -> +90555...          [SMS] kanal kapali, atlandi
```

## 5. `default` metot neden var?

Java 8'de `List` arayüzüne `sort`, `stream` gibi metotlar eklenmesi gerekti. Normal metot ekleseler, dünyadaki tüm `List` uygulamaları derlenmez hâle gelirdi. `default` metot, **var olan arayüze uygulamaları bozmadan yetenek eklemenin** yoludur.

Bizim örneğimizde `count()` ve `exists()` hiçbir sınıfta yazılmadı ama çalışıyor:

```java
repository.count()             // 2
repository.exists("ORD-00101") // true
```

## 6. Optional — "olmayabilir"i tipte taşımak

```java
Optional<OrderSummary> found = repository.findByOrderNo("ORD-00101");
System.out.println(found.map(OrderSummary::orderNo).orElse("yok"));
```

`null` döndürmek yerine `Optional` döndürmek, çağıranı "yoksa ne olacak?" sorusunu cevaplamaya zorlar. NullPointerException'ı tasarım seviyesinde önler.

## 7. Test edilebilirlik — asıl kazanç

```java
static class RecordingSender implements NotificationSender {
    private int sentCount = 0;
    @Override public void send(String r, String s, String b) { sentCount++; }
    @Override public String channel() { return "FAKE"; }
}
```

Bu sahte kanal sayesinde "sipariş oluşturunca bildirim gidiyor mu?" sorusunu **gerçekten SMS göndermeden** test edebiliyoruz. Ders 15'teki testlerin tamamı bu fikre dayanıyor.

Tek kullanımlık uygulamalar için anonim sınıf:

```java
NotificationSender console = new NotificationSender() {
    @Override public void send(String r, String s, String b) { System.out.println(r + " -> " + s); }
    @Override public String channel() { return "CONSOLE"; }
};
```

## 8. Çalıştır

```bash
mvn -q exec:java -Dexec.mainClass=com.meroddi.javaegitimi.m02oop.ders13.InterfaceDemo
```

## 9. Sık yapılan hatalar

- **Her sınıfa arayüz yazmak.** Tek uygulaması olan ve olmayacak bir sınıf için `IUserService`/`UserServiceImpl` çifti gereksiz gürültüdür. Arayüzü değişiklik ihtimali varken çıkar.
- Arayüz adına `I` öneki koymak — Java kültüründe kullanılmaz.
- Arayüzü somut uygulamaya sızdırmak: `OrderRepository` metotlarında SQL/JPA tipleri geçiyorsa soyutlama kırılmıştır.
- `default` metotları iş mantığıyla doldurmak; onlar ince yardımcılar içindir.
- Constructor injection yerine alan enjeksiyonu (`@Autowired` alan üstünde). Constructor injection zorunlu bağımlılığı görünür kılar ve testte kolaydır.

## 10. Mülakat notu

> "Interface mi abstract class mı?"

Ortak **durum ve iskelet** varsa abstract class (Ders 12'deki `PaymentMethod` gibi), sadece **yetenek/sözleşme** tanımlıyorsan interface. Tereddütte kalırsan interface: bağı gevşek tutar, test edilebilirliği artırır, çoklu implementasyona izin verir.

## 11. Alıştırma

1. `JsonFileOrderRepository` yaz (dosyaya yazan sahte bir uygulama olabilir): `OrderNotificationService`'in **tek satırını bile değiştirmeden** çalıştığını göster.
2. `PushNotificationSender` ekle; `enabled()` kullanıcı tercihine göre dönsün.
3. `OrderRepository`'ye `default List<OrderSummary> findAbove(Money threshold)` ekle — sadece arayüzde, hiçbir sınıfı değiştirmeden.
4. `Comparable` arayüzünü uygulayan bir `Invoice` sınıfı yaz (tutara göre sıralansın), `Arrays.sort` ile sırala. Sonra bir `Comparator` ile tarihe göre sırala.

---
**Önceki:** [Ders 12](ders-12-polymorphism-ve-abstract.md) | **Sonraki:** [Ders 14 — Enum, exception, iç sınıflar](ders-14-enum-ve-exception.md)
