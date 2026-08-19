# Ders 07 — Metotlar: parametre, overloading, varargs, pass-by-value

> Kod: [`m01temeller/ders07/PricingMethodsDemo.java`](../src/main/java/com/meroddi/javaegitimi/m01temeller/ders07/PricingMethodsDemo.java)

## 1. Bu ne?

Metot, isim verilmiş bir kod bloğudur: girdi alır (parametre), iş yapar, sonuç döndürür.

```java
private static BigDecimal calculateVat(BigDecimal amount) {
//  │       │      │            │        └─ parametre
//  │       │      │            └─ metot adı (fiil olmalı)
//  │       │      └─ dönüş tipi (void = döndürmez)
//  │       └─ nesne gerektirmez
//  └─ erişim belirleyici
    return amount.multiply(VAT_RATE).setScale(2, RoundingMode.HALF_UP);
}
```

**Tek kural:** bir metot TEK iş yapar ve adı ne yaptığını söyler. `process()`, `handle()`, `doWork()` isimleri hiçbir şey anlatmaz.

## 2. Nasıl çalışır?

Her metot çağrısı **stack**'e bir çerçeve (stack frame) koyar: parametreler, yerel değişkenler, dönüş adresi. Metot bitince çerçeve atılır. Ozyineleme (recursion) durmazsa stack dolar → `StackOverflowError`.

### Java'da her şey "pass-by-value"

Bu, mülakatların klasik tuzak sorusudur:

```java
static void tryToChangePrimitive(int value) { value = 999; }        // dışarıyı etkilemez
static void tryToChangeArrayContent(int[] a) { a[0] = 99; }         // ETKİLER (aynı nesne)
static void tryToReassignArray(int[] a) { a = new int[]{7,7}; }     // etkilemez (yerel değişken)
```

Java **her zaman değerin kopyasını** geçer. Primitive'de değerin kendisi, nesnede **adresin** kopyası geçer. Yani nesnenin içeriğini değiştirebilirsin ama değişkenin neyi gösterdiğini dışarıdan değiştiremezsin.

## 3. Nerede kullanılır?

Metotlara bölmek estetik değil, **test edilebilirlik** meselesidir. 300 satırlık tek bir `processOrder()` metodunu test edemezsin; `calculateVat`, `applyDiscount`, `validateStock` metotlarını tek tek test edersin.

Gerçek örnek: bir e-ticaret sisteminde "ürünün son fiyatı" hesabı 10+ kuraldan oluşur — liste fiyatı, kampanya, kupon, üyelik indirimi, KDV, kargo, taksit farkı. Hepsi ayrı metottur.

## 4. Gerçek hayat örneği — fiyatlandırma servisi

```java
private static final BigDecimal VAT_RATE = new BigDecimal("0.20");   // sabit: static final

private static BigDecimal calculateVat(BigDecimal amount) { ... }
private static BigDecimal addVat(BigDecimal amount) {
    return amount.add(calculateVat(amount));      // metot metodu çağırır
}
```

### Overloading (aşırı yükleme) — aynı isim, farklı imza

```java
applyDiscount(amount, 15)                          // yüzde indirim
applyDiscount(amount, "WELCOME10")                 // kupon kodu
applyDiscount(amount, 15, new BigDecimal("300"))   // yüzde + üst sınır
```

Çağıran taraf için tek bir kavram (`applyDiscount`), üç farklı kullanım. Derleyici hangisini çağıracağına **parametre tiplerine bakarak** karar verir.

Kupon sürümü, yüzde sürümünü çağırıyor — kod tekrarı yok:

```java
private static BigDecimal applyDiscount(BigDecimal amount, String couponCode) {
    int percent = switch (couponCode == null ? "" : couponCode) {
        case "WELCOME10" -> 10;
        case "BLACKFRIDAY" -> 40;
        default -> 0;
    };
    return applyDiscount(amount, percent);
}
```

### Varargs — değişken sayıda parametre

```java
private static BigDecimal sumAll(BigDecimal... amounts) {   // amounts aslında dizi
    BigDecimal total = BigDecimal.ZERO;
    for (BigDecimal a : amounts) total = total.add(a);
    return total;
}

sumAll();                                    // 0.00
sumAll(fiyat1, fiyat2, fiyat3);              // toplam
```

`String.format`, `List.of`, `printf` — hepsi varargs kullanır.

### Recursion — bileşik faiz

```java
private static BigDecimal compoundInterest(BigDecimal principal, BigDecimal rate, int years) {
    if (years == 0) return principal;                              // DURDURMA KOŞULU
    return compoundInterest(principal, rate, years - 1)
              .multiply(BigDecimal.ONE.add(rate));
}
```

Her özyinelemede mutlaka bir durdurma koşulu olmalı. Recursion ağaç yapılarında (kategori ağacı, dosya sistemi, JSON) doğaldır; düz listelerde döngü tercih edilir.

## 5. Çalıştır

```bash
mvn -q exec:java -Dexec.mainClass=com.meroddi.javaegitimi.m01temeller.ders07.PricingMethodsDemo
```

## 6. Sık yapılan hatalar

- **Çok uzun metot.** 30 satırı geçtiyse muhtemelen 2-3 iş yapıyordur.
- **Boolean parametre.** `createOrder(customer, true, false)` — 6 ay sonra bu `true` neydi? Enum veya ayrı metot kullan.
- **5+ parametre.** Parametreleri bir nesnede grupla.
- **Yan etkili "hesaplama" metodu.** `calculateTotal()` adlı metot stoktan düşüyorsa, ismi yalan söylüyor demektir.
- **`null` döndürmek.** Çağıran her yerde null kontrolü yapmak zorunda kalır → `Optional` (Ders 13).
- Overloading'i `int`/`long`/`Integer` gibi benzer tiplerle yapmak — hangisinin seçileceği kafa karıştırır.

## 7. Mülakat notu

> "Java pass-by-value mi pass-by-reference mı?"

**Her zaman pass-by-value.** Nesnelerde geçen değer, referansın (adresin) kopyasıdır. Bu yüzden nesnenin *içini* değiştirebilirsin ama parametreye yeni nesne atamak çağıranı etkilemez.

> "Overloading ile overriding farkı?"

Overloading: aynı sınıfta aynı isim, farklı imza — derleme anında seçilir (static binding). Overriding: alt sınıfın üst sınıf metodunu ezmesi — çalışma anında seçilir (dynamic binding). Ders 11-12'de göreceğiz.

## 8. Alıştırma

1. `calculateShipping(BigDecimal cartTotal, double weightKg, String city)` metodunu yaz; Ders 04'teki kuralları metotlara böl (`isHeavy`, `isFreeShipping`, `baseFee`).
2. Overloading ile üç sürümlü `formatMoney` yaz: `(BigDecimal)`, `(BigDecimal, String currency)`, `(BigDecimal, String currency, boolean showSymbol)`.
3. Varargs ile `average(double... values)` yaz; boş çağrıda 0 döndürsün.
4. Recursion ile faktöriyel ve n. Fibonacci sayısını yaz. Fibonacci'yi 45 için çalıştır, ne kadar sürdüğünü ölç; sonra döngüyle yaz ve karşılaştır. (Bu, "aynı sonucu üreten iki kodun maliyeti aynı değildir" dersidir.)

---
**Önceki:** [Ders 06](ders-06-diziler.md) | **Sonraki:** [Ders 08 — Modül 1 projesi: POS](ders-08-modul1-projesi-pos.md)
