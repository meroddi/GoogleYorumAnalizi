# Ders 15 — Modül 2 Projesi: Katmanlı mini e-ticaret + testler

> Kod: [`m02oop/ders15/`](../src/main/java/com/meroddi/javaegitimi/m02oop/ders15/) — `model/`, `repository/`, `service/`, `ShopApp.java`
> Testler: [`src/test/java/.../ders15/service/OrderServiceTest.java`](../src/test/java/com/meroddi/javaegitimi/m02oop/ders15/service/OrderServiceTest.java)

## 1. Ne yapıyoruz?

Modül 2'nin tamamını tek bir çalışan sistemde birleştiriyoruz: ürün kataloğu, sipariş oluşturma, ödeme, kargo, teslimat, iptal ve raporlama — **JUnit testleriyle doğrulanmış** hâlde.

## 2. Katmanlı mimari

```
ShopApp (kurulum + senaryolar)
   │
   ▼
service/     OrderService          → iş AKIŞINI sıralar
   │
   ├──► model/       Order, OrderLine, Product   → iş KURALLARINI taşır
   └──► repository/  OrderRepository (interface)  → veriye ERİŞİMİ soyutlar
                     InMemoryOrderRepository
```

Kural basit:

| Katman | Sorumluluk | Bilmediği şey |
|---|---|---|
| **model** | iş kuralları, değişmezler (invariant) | veri tabanı, HTTP, ekran |
| **repository** | veriyi saklama/okuma sözleşmesi | iş kuralları |
| **service** | adımları sıralama, katmanları bağlama | SQL detayı, ödeme detayı |

Bu ayrım bozulursa proje 2 yıl içinde test edilemez hâle gelir. Yol haritasının kalıcı çekirdeğindeki *"coupling/cohesion"*, *"domain modelleme"* maddeleri budur.

## 3. Modelin kuralları koruması

```java
public class Order {
    private final List<OrderLine> lines = new ArrayList<>();
    private OrderStatus status = OrderStatus.CREATED;

    public void addLine(OrderLine line) {
        if (status != OrderStatus.CREATED)
            throw new IllegalStateException("Odenmis/kapanmis siparise satir eklenemez. Durum: " + status);
        lines.add(line);
    }

    public void changeStatus(OrderStatus newStatus) {
        if (!status.canTransitionTo(newStatus))              // kural Ders 14'teki enum'da
            throw new InvalidStatusTransitionException(orderNo, status, newStatus);
        this.status = newStatus;
    }

    public List<OrderLine> getLines() { return List.copyOf(lines); }   // savunmacı kopya
}
```

Dışarıdan kimse `order.getLines().add(...)` diyerek satır ekleyemez. Sipariş, kendi satırlarının ve durumunun **sahibidir** (DDD terimiyle: aggregate root).

### Fiyat kopyalanır, referans verilmez

```java
public record OrderLine(String sku, String productName, Money unitPrice, int quantity) {
    public Money lineTotal() { return unitPrice.multiply(quantity); }
}
```

Ürünün fiyatı yarın değişirse dünkü siparişin tutarı **değişmemelidir**. Bu, gerçek sistemlerdeki en sık veri modelleme hatalarından biridir; yol haritasında *"şema tasarımı bir kere yanlış yapılırsa 5 yıl acı çektirir"* diye geçer.

## 4. Servisin işi: sıralamak

```java
public Order createOrder(String customerNo, Map<String, Integer> items) {
    // 1. adım: ÖNCE tümünü doğrula (hiçbir şey değiştirmeden)
    for (var item : items.entrySet()) {
        Product product = requireProduct(item.getKey());
        if (!product.hasStock(item.getValue()))
            throw new IllegalStateException("Yetersiz stok [%s]...".formatted(product.getSku()));
    }

    // 2. adım: SONRA uygula
    Order order = new Order(OrderIdGenerator.nextId(), customerNo);
    for (var item : items.entrySet()) {
        Product product = requireProduct(item.getKey());
        product.decreaseStock(item.getValue());
        order.addLine(new OrderLine(product.getSku(), product.getName(), product.getPrice(), item.getValue()));
        productRepository.save(product);
    }
    orderRepository.save(order);
    notify(...);
    return order;
}
```

**Neden iki adım?** 3 ürünlü sepette 3. üründe hata alırsan, ilk 2 ürünün stoğu boşuna düşmüş olmasın. Gerçek sistemlerde bunun adı **transaction**'dır (SQL modülünde göreceğiz); burada elle yapıyoruz ki mantığı anla.

Ödeme başarısızsa sipariş durumu değişmez:

```java
PaymentResult result = paymentMethod.pay(order.total(), order.getOrderNo());
if (result.successful()) {
    order.markPaid(result.reference());
    orderRepository.save(order);
}
return result;
```

## 5. Testler — asıl teslimat

```bash
mvn -q test
# Tests run: 22, Failures: 0, Errors: 0
```

Üç test sınıfı: `MoneyTest` (7), `OrderStatusTest` (5), `OrderServiceTest` (10).

```java
@BeforeEach
void setUp() {          // her testten önce TEMİZ dünya — testler birbirini etkilemez
    productRepository = new InMemoryProductRepository();
    orderRepository = new InMemoryOrderRepository();
    notificationSender = new RecordingSender();          // sahte kanal
    orderService = new OrderService(productRepository, orderRepository, notificationSender);
    ...
}

@Test
@DisplayName("Yetersiz stokta siparis olusmaz ve HICBIR urunun stogu dusmez")
void insufficientStockLeavesNothingChanged() {
    Map<String, Integer> basket = new LinkedHashMap<>();
    basket.put("KLV-001", 1);      // yeterli
    basket.put("MNT-014", 99);     // yetersiz

    assertThrows(IllegalStateException.class, () -> orderService.createOrder("C-1001", basket));

    assertEquals(10, productRepository.findBySku("KLV-001").orElseThrow().getStock());  // dokunulmadı
    assertEquals(0, orderRepository.count());
}
```

Testin yapısı her zaman aynı: **hazırla → çalıştır → doğrula** (given/when/then).

Veri tabanı yok, SMTP yok, POS cihazı yok — hepsi arayüz olduğu için testte sahte uygulamalar kullanılıyor ve 22 test **0,2 saniyede** bitiyor. Ders 13'teki "test edilebilir tasarım" cümlesinin somut karşılığı budur.

Test isimleri de belge niteliğinde:

```
Siparis olusturulur, tutar dogru hesaplanir ve stok duser
Odenmemis siparis kargoya verilemez
Basarisiz odeme siparis durumunu degistirmez
Iptal edilen siparisin stogu geri yuklenir
Ciro sadece odenmis siparisleri sayar
```

Bu isimler bir araya geldiğinde sistemin iş kuralları listesi oluyor. Yol haritasının *"test disiplini: sadece yazmak değil, neyi test etmemek gerektiğini bilmek"* maddesinin ilk adımı: **iş kurallarını test et, getter/setter'ı değil.**

## 6. Çalıştır

```bash
mvn -q exec:java -Dexec.mainClass=com.meroddi.javaegitimi.m02oop.ders15.ShopApp
mvn -q test
```

`ShopApp` dört senaryo çalıştırır:

1. **Başarılı sipariş**: oluştur → öde (kredi kartı, 3 taksit) → kargola → teslim et
2. **Ödeme reddi**: 8.598 TL'lik sepet, kapıda ödeme sınırı 5.000 TL → sipariş `CREATED` kalır
3. **İptal ve stok iadesi**: monitör stoğu 2 → 4'e döner
4. **Kural ihlalleri**: yetersiz stok, olmayan ürün, teslim edilmişi tekrar kargolama, ödenmişe satır ekleme

## 7. Bu proje neyi kanıtlıyor?

Modül 2'nin her dersi burada bir işe yarıyor:

| Ders | Projedeki karşılığı |
|---|---|
| 09 sınıf/nesne | `Product`, `Order`, `OrderLine` |
| 10 encapsulation/record/static | `Money`, `List.copyOf`, `OrderIdGenerator.nextId()` |
| 11 kalıtım/Object | `toString`, `equals` mantığı, `PaymentMethod` hiyerarşisi |
| 12 polymorphism | `orderService.pay(orderNo, herhangiBirÖdemeYöntemi)` |
| 13 interface + DI | `ProductRepository`, `OrderRepository`, `NotificationSender` |
| 14 enum + exception | `OrderStatus` durum makinesi, `InvalidStatusTransitionException` |

## 8. Alıştırma (projeyi büyüt)

1. **Kupon desteği**: `createOrder`'a kupon kodu parametresi ekle; indirim `Order`'a satır olarak yansısın, testle doğrula.
2. **İade akışı**: `refund(orderNo)` yaz — sadece `DELIVERED` durumundan, 14 gün içinde; stok geri yüklensin. Testini de yaz.
3. **`FileOrderRepository`**: siparişleri bir dosyaya yazan uygulama ekle; `OrderService`'in **hiç değişmeden** çalıştığını göster.
4. **Ciro raporu**: müşteri bazlı toplam harcama ve en çok satan 3 ürünü döndüren metotlar ekle.
5. **Eşzamanlılık sorusu**: iki müşteri aynı anda son ürünü sipariş ederse ne olur? Bir paragraf yaz — bu, Yıl 2'nin "race condition" konusuna açılan kapın.

---

## Modül 1 + 2 tamamlandı

Buraya kadar öğrendiklerin, yol haritasının **YIL 1 → "Java core → İleri Java/OOP"** adımının tamamı:

- Java'nın nasıl çalıştığı, JVM, Maven
- Tipler, operatörler, String, karar, döngü, dizi, metot
- Sınıf, nesne, encapsulation, immutability, record, static
- Kalıtım, polymorphism, abstract, interface, dependency injection
- Enum, exception, katmanlı mimari, birim test

### Sıradaki adımlar (Yıl 1 hattı)

| Modül | Konu | Neden |
|---|---|---|
| **3** | Collections & Generics: `List`, `Map`, `Set`, `Optional`, `Comparator` | Dizilerin yerini alacak, her gün kullanacaksın |
| **4** | Exception derinleşme, I/O, `java.time`, Stream API & lambda | Modern Java'nın günlük dili |
| **5** | SQL + JDBC: tablo tasarımı, join, index, transaction | Yol haritası: "SQL üzerinden veri modelleme" |
| **6** | Spring Boot + REST API | İlk işe girme kriteri |
| **7** | Test derinleşme (JUnit + Mockito), Git, Docker, temel cloud | Yıl 1 çıktısı: canlıya çıkmış 2 proje |

> Yol haritasının uyarısı: *"Sonsuz kurs döngüsü. Kurs bitirmek ilerleme hissi verir, ilerleme vermez. Bitmiş sistem verir."*
> Modül 3'e geçmeden önce yukarıdaki alıştırmalardan en az 3'ünü bitir ve `mvn test` yeşil kalsın.

---
**Önceki:** [Ders 14](ders-14-enum-ve-exception.md) | **Başa dön:** [README](../README.md)
