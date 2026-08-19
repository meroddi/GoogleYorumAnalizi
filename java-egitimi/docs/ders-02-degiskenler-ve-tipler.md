# Ders 02 — Değişkenler, veri tipleri ve paranın doğru tutulması

> Kod: [`m01temeller/ders02/MoneyPrecisionDemo.java`](../src/main/java/com/meroddi/javaegitimi/m01temeller/ders02/MoneyPrecisionDemo.java)

## 1. Bu ne?

Değişken = bellekteki bir değere verdiğin isim. Java'da her değişkenin bir **tipi** vardır ve bu tip derleme anında sabittir:

```java
int orderCount = 1250;      // artık orderCount'a String atayamazsın
```

Java'da iki büyük tip ailesi var:

**Primitive tipler** (8 tane) — değeri doğrudan tutar:

| Tip | Boyut | Aralık / kullanım |
|---|---|---|
| `byte` | 8 bit | -128 … 127 |
| `short` | 16 bit | -32.768 … 32.767 |
| `int` | 32 bit | ±2.1 milyar — **varsayılan tam sayı** |
| `long` | 64 bit | ±9.2 kentilyon — id, timestamp, para (kuruş) |
| `float` | 32 bit | ondalıklı, düşük hassasiyet |
| `double` | 64 bit | ondalıklı — **varsayılan** (ama parada ASLA) |
| `boolean` | — | `true` / `false` |
| `char` | 16 bit | tek karakter, `'A'` |

**Referans tipler** — nesnenin adresini tutar: `String`, `BigDecimal`, dizi, kendi yazdığın sınıflar…

## 2. Nasıl çalışır? (stack / heap)

```
int a = 5;                        STACK              HEAP
BigDecimal b = new BigDecimal("5");
                                a │  5  │
                                b │ ref ├──────────► BigDecimal nesnesi
```

- **Stack**: metot çağrıları ve primitive değerler. Hızlı, metot bitince otomatik temizlenir.
- **Heap**: `new` ile üretilen tüm nesneler. Garbage Collector burayı temizler.

Bu ayrım sonuçları doğrudan etkiler:

```java
int a = 5;  int b = a;   b = 10;   // a hâlâ 5  → değerin kopyası
int[] x = {1,2,3};  int[] y = x;  y[0] = 99;   // x[0] de 99 → adresin kopyası
```

Yol haritasının kalıcı çekirdeğinde *"bellek modeli: stack/heap, pointer, garbage collection"* diye geçen konunun giriş kapısı burasıdır.

## 3. Nerede kullanılır?

Her yerde — ama asıl önemli olan **doğru tipi seçmek**:

- **Para** → `BigDecimal` (asla `double`), ya da tam sayı kuruş (`long`)
- **Kimlik/ID** → `long` veya `String`/UUID (int taşabilir: Instagram, Twitter bu yüzden id'lerini büyüttü)
- **Zaman** → `Instant`, `LocalDateTime` (kendi yazdığın `int gun, ay, yil` değil)
- **Sayaç** → `int` yeterli, ama "toplam görüntülenme" gibi büyüyen alanlarda `long`

## 4. Gerçek hayat örneği — `double` ile para tutmanın bedeli

```java
double sum = 0.0;
for (int i = 0; i < 10; i++) sum += 0.1;   // 10 adet 0.10 TL
System.out.println(sum);                   // 0.9999999999999999
```

Neden? `double` ikilik tabanda çalışır; `0.1` ikilik tabanda tam ifade edilemez (tıpkı 1/3'ün onluk tabanda 0.333… olması gibi). Milyonlarca işlemde bu fark **mutabakat farkına** dönüşür ve gerçek bankacılık projelerinde incident sebebidir.

Doğrusu:

```java
BigDecimal unitPrice = new BigDecimal("0.10");   // String ile! double ile DEĞİL
BigDecimal total = BigDecimal.ZERO;
for (int i = 0; i < 10; i++) {
    total = total.add(unitPrice);   // BigDecimal immutable → sonucu geri atamak ZORUNLU
}
// 1.00
```

Fatura hesabı:

```java
BigDecimal cartAmount = new BigDecimal("1499.99");
BigDecimal vat = cartAmount.multiply(new BigDecimal("0.20"))
                           .setScale(2, RoundingMode.HALF_UP);   // 300.00
BigDecimal grandTotal = cartAmount.add(vat);                     // 1799.99
```

`setScale(2, HALF_UP)` = 2 ondalık haneye, yarım yukarı yuvarla. Muhasebe kuralları yuvarlama biçimini belirler; rastgele seçilmez.

### Taşma (overflow)

```java
int max = Integer.MAX_VALUE;
System.out.println(max + 1);   // -2147483648  ← sessizce negatife döner!
```

Java hata vermez. "Bakiye negatif göründü" tipi bug'ların klasik kaynağı budur.

## 5. `var` ne yapar?

```java
var customerName = "Ayse Yilmaz";        // derleyici String olduğunu anlar
var invoiceTotal = new BigDecimal("250.00");
```

Tip kaybolmaz, sadece yazmıyorsun. Sağ tarafa bakınca tip belli oluyorsa `var` okunabilirliği artırır; belli değilse (`var result = process();`) zararlıdır.

## 6. Çalıştır

```bash
mvn -q exec:java -Dexec.mainClass=com.meroddi.javaegitimi.m01temeller.ders02.MoneyPrecisionDemo
```

## 7. Sık yapılan hatalar

- `new BigDecimal(0.1)` — double'dan üretmek. Çıktı: `0.1000000000000000055511151231257827…`. Her zaman `new BigDecimal("0.1")`.
- `BigDecimal`'de `total.add(x);` yazıp sonucu atamamak. Nesne değişmez (immutable), dönen yeni nesneyi kullanmalısın.
- `int` ile milisaniye/tutar tutmak → taşma.
- `float` kullanmak. Modern Java'da neredeyse hiç yeri yok; `double` ya da `BigDecimal`.

## 8. Mülakat notu

> "Neden parayı double ile tutmayız?"

İkilik kayan nokta gösterimi ondalık kesirleri tam ifade edemez; toplama sırasında hata birikir ve karşılaştırmalar (`==`) güvenilmez olur. Çözüm: `BigDecimal` veya en küçük birimi tam sayı olarak tutmak (kuruş → `long`).

## 9. Alıştırma

1. `BigDecimal` kullanarak bir **kredi taksit hesaplayıcı** yaz: anapara, yıllık faiz oranı ve vade (ay) alsın; aylık taksiti ve toplam geri ödemeyi yazdırsın.
2. `int` taşmasını kendi kodunda göster: 2 milyar + 2 milyar hesabını `int` ve `long` ile ayrı ayrı yazdır.
3. `0.1 + 0.2 == 0.3` karşılaştırmasını `double` ve `BigDecimal` ile ayrı ayrı dene, farkı yazdır.

---
**Önceki:** [Ders 01](ders-01-java-nedir.md) | **Sonraki:** [Ders 03 — Operatörler ve String](ders-03-operatorler-ve-string.md)
