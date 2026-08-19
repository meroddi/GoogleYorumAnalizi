# Ders 09 — Sınıf ve nesne (OOP'un temeli)

> Kod: [`m02oop/ders09/`](../src/main/java/com/meroddi/javaegitimi/m02oop/ders09/) — `Customer.java`, `Product.java`, `ObjectBasicsDemo.java`

## 1. Bu ne?

**Sınıf**, bir kalıptır (blueprint) — bellekte yer kaplamaz.
**Nesne**, o kalıptan üretilmiş somut örnektir — heap'te yaşar.

> "Müşteri" bir kavramdır → sınıf.
> "Ayşe Yılmaz, müşteri no C-1001" somut bir şeydir → nesne.

Bir sınıf iki şeyden oluşur:
- **Alanlar (field)**: nesnenin durumu — `customerNo`, `balance`, `stockQuantity`
- **Metotlar**: nesnenin davranışı — `deactivate()`, `decreaseStock()`

OOP'un asıl fikri budur: **veri ve o veriyi değiştiren kurallar bir arada durur.** Modül 1'de veriyi dizide, kuralı `main`'de tutuyorduk; ikisinin arasındaki bağ sadece bizim dikkatimizdi.

## 2. Nasıl çalışır?

```java
Customer ayse = new Customer("C-1001", "Ayse Yilmaz", "ayse@example.com");
```

1. `new` heap'te yer ayırır, alanları varsayılan değerle doldurur.
2. **Constructor** çalışır: nesneyi *geçerli* bir hâle getirir.
3. Nesnenin adresi `ayse` değişkenine yazılır.
4. Nesneye artık kimse referans tutmuyorsa **Garbage Collector** onu temizler.

### Constructor

```java
public Customer(String customerNo, String fullName, String email) {
    if (customerNo == null || customerNo.isBlank())
        throw new IllegalArgumentException("Musteri no bos olamaz");
    if (email == null || !email.contains("@"))
        throw new IllegalArgumentException("Gecersiz e-posta: " + email);

    this.customerNo = customerNo;    // this = "şu anda oluşturulan nesne"
    this.fullName = fullName;
    this.registeredAt = LocalDate.now();
    this.active = true;
}
```

**Kritik fikir:** geçersiz nesnenin doğmasına izin verme. Bozuk veriyi 3 katman sonra yakalamak, hiç doğmasına izin vermemekten kat kat pahalıdır. Üretimde "bu kaydın e-postası neden boş?" sorusunun cevabı genelde "constructor kontrol etmiyordu"dur.

Constructor overloading + `this(...)` ile tekrar yok:

```java
public Customer(String customerNo, String fullName, String email, LocalDate registeredAt) {
    this(customerNo, fullName, email);      // doğrulamayı tekrar yazmıyoruz
    this.registeredAt = registeredAt;
}
```

## 3. Nerede kullanılır?

Her kurumsal uygulamada bir **domain modeli** vardır: `Customer`, `Order`, `Invoice`, `Account`, `Shipment`. Spring Boot ile REST API yazdığında bu sınıflar hem veri tabanı tablolarına (JPA entity) hem JSON gövdelerine karşılık gelir.

Yol haritasının kalıcı çekirdeğinde geçen *"domain modelleme, DDD kavramları"* burada başlar.

## 4. Gerçek hayat örneği — kural nesnenin içinde

```java
public class Product {
    private final String sku;     // final: bir kez atanır, asla değişmez
    private BigDecimal price;
    private int stockQuantity;

    public void decreaseStock(int quantity) {
        if (quantity <= 0)
            throw new IllegalArgumentException("Adet pozitif olmali");
        if (quantity > stockQuantity)
            throw new IllegalStateException(
                "Yetersiz stok. Istenen: " + quantity + ", mevcut: " + stockQuantity);
        this.stockQuantity -= quantity;
    }

    public void changePrice(BigDecimal newPrice) {
        BigDecimal limit = price.multiply(new BigDecimal("1.5"));
        if (newPrice.compareTo(limit) > 0)
            throw new IllegalArgumentException("Fiyat tek seferde %50'den fazla artirilamaz...");
        this.price = newPrice;
    }
}
```

Neden fiyat artışına sınır? Çünkü gerçek sistemlerde bir entegrasyon hatası "2.500 TL" yerine "999.900 TL" yazabilir ve bu doğrudan müşteriye yansır. Kuralı veriye en yakın yere koymak, hangi koddan çağrılırsa çağrılsın korumayı garanti eder.

## 5. Referans ve `null`

```java
Product sameKeyboard = keyboard;   // KOPYA DEĞİL — aynı nesne
sameKeyboard.decreaseStock(2);     // keyboard'un da stoğu düştü

Product missing = null;            // hiçbir nesneyi göstermiyor
missing.getName();                 // NullPointerException
```

`null`, Java'nın en pahalı hatasıdır (mucidi Tony Hoare buna "benim milyar dolarlık hatam" der). Korunma: `if (x != null)`, daha iyisi `Optional` (Ders 13).

## 6. Çalıştır

```bash
mvn -q exec:java -Dexec.mainClass=com.meroddi.javaegitimi.m02oop.ders09.ObjectBasicsDemo
```

## 7. Sık yapılan hatalar

- **Anemik model**: sadece getter/setter içeren, hiç kuralı olmayan sınıflar. Kurallar servislere dağılır, aynı kural 5 yerde yarım yamalak tekrarlanır.
- Constructor'da doğrulama yapmamak.
- `this` kullanmayı unutmak: `customerNo = customerNo;` parametreyi kendine atar, alan `null` kalır.
- Her şeyi `public` yapmak (Ders 10'da düzelteceğiz).
- Sınıfı "veri kutusu" sanmak. `Product` sadece veri değil, stok kurallarının sahibidir.

## 8. Mülakat notu

> "Constructor ile metot farkı nedir?"

Constructor sınıfla aynı adı taşır, dönüş tipi yoktur, `new` ile nesne üretilirken bir kez çalışır ve nesneyi geçerli duruma getirmekten sorumludur. Hiç constructor yazmazsan derleyici parametresiz bir tane üretir; parametreli bir tane yazarsan o varsayılan **kaybolur**.

## 9. Alıştırma

1. `Customer` sınıfına `changeEmail(String newEmail)` metodu ekle: aynı doğrulama kuralı geçerli olsun, ayrıca eski e-postayı `previousEmail` alanında sakla.
2. `Order` sınıfı yaz: `orderNo`, `customerNo`, `createdAt`, `totalAmount`; `applyDiscount(int percent)` metodu %50'den fazla indirimi reddetsin.
3. `BankAccount` sınıfı yaz: `deposit`, `withdraw` (bakiye yetmezse exception), `transferTo(BankAccount other, BigDecimal amount)`.
4. Ders 08'deki POS uygulamasını `Product` sınıfı kullanacak şekilde yeniden yaz (paralel dizileri kaldır). Kodun ne kadar kısaldığına dikkat et.

---
**Önceki:** [Ders 08](ders-08-modul1-projesi-pos.md) | **Sonraki:** [Ders 10 — Encapsulation, record, static](ders-10-encapsulation-ve-record.md)
