# Ders 03 — Operatörler, String ve metin doğrulama

> Kod: [`m01temeller/ders03/TextAndValidationDemo.java`](../src/main/java/com/meroddi/javaegitimi/m01temeller/ders03/TextAndValidationDemo.java)

## 1. Bu ne?

**Operatörler**, değerler üzerinde işlem yapan semboller: `+ - * / %`, `== != < >`, `&& || !`, `+= -=`…

**String**, Java'da metni temsil eden sınıftır. Primitive değildir — nesnedir. Ve **immutable**'dır: bir kez oluşan String asla değişmez; "değiştirdiğin" her an aslında yeni bir String üretirsin.

## 2. Nasıl çalışır?

### Tam bölme ve kalan

```java
17 / 5   // 3   (int/int → int, ondalık kısım ATILIR)
17 % 5   // 2   (kalan)
(17 + 5 - 1) / 5   // 4  → yukarı yuvarlama hilesi: kaç kutu gerekir?
```

`%` operatörü sandığından çok işe yarar: sayfalama, hash bucket seçimi, "her N kayıtta bir log at", IBAN doğrulama.

### Kısa devre (short-circuit)

```java
if (customer != null && customer.isActive()) { ... }
```

`&&` solu `false` ise sağı **hiç çalıştırmaz**. Null kontrolünün temel silahı budur. `|` ve `&` (tek sembollü) kısa devre yapmaz — bu yüzden neredeyse hiç kullanılmaz.

### String havuzu (string pool) ve `==` tuzağı

```java
String a = "TR33";
String b = "TR33";                 // aynı havuzdaki nesne
String c = new String("TR33");     // heap'te YENİ nesne

a == b        // true   ← adres karşılaştırması, tesadüfen doğru
a == c        // false  ← ADRESLER farklı
a.equals(c)   // true   ← DOĞRU kontrol bu
```

**Kural: nesnelerde her zaman `equals`, sadece primitive'lerde `==`.** (Enum'lar istisna — Ders 14.)

## 3. Nerede kullanılır?

Backend'de yazdığın kodun ciddi bir kısmı metin işidir:

- Kullanıcı girdisini temizleme ve doğrulama (IBAN, TCKN, e-posta, sipariş kodu)
- Log satırı üretme
- API'den gelen JSON/CSV içeriğini ayrıştırma
- Hassas veriyi maskeleme (KVKK/GDPR: log'a tam kart numarası yazmak ihlaldir)

## 4. Gerçek hayat örneği — IBAN doğrulama

Ödeme servisine gelen IBAN'ı kabul etmeden önce doğrularsın. Standart (ISO 13616) şunu söyler:

1. İlk 4 karakteri sona taşı
2. Harfleri sayıya çevir (A=10 … Z=35)
3. Oluşan dev sayının 97'ye bölümünden kalan **1** olmalı

```java
String rearranged = iban.substring(4) + iban.substring(0, 4);

long remainder = 0;
for (int i = 0; i < rearranged.length(); i++) {
    char ch = rearranged.charAt(i);
    if (ch >= '0' && ch <= '9') {
        remainder = (remainder * 10 + (ch - '0')) % 97;
    } else if (ch >= 'A' && ch <= 'Z') {
        remainder = (remainder * 100 + (ch - 'A' + 10)) % 97;   // harf = 2 basamak
    } else {
        return false;
    }
}
return remainder == 1;
```

26 haneli sayı `long`'a sığmadığı için kalanı **adım adım taşıyoruz** (modüler aritmetik). Bu, "matematiği bilmek kod yazmayı değiştirir"in küçük ama net bir örneği.

Girdi temizleme (kullanıcı boşluklu ve küçük harfle yapıştırır):

```java
String iban = rawInput.trim().replace(" ", "").toUpperCase();
```

Maskeleme (log'a giden hali):

```java
value.substring(0, 4) + " **** **** " + value.substring(value.length() - 4);
// TR33 **** **** 1326
```

## 5. `String` + vs `StringBuilder`

```java
String s = "";
for (int i = 0; i < 30_000; i++) s += "x";        // ~123 ms  ← her adımda YENİ String

StringBuilder sb = new StringBuilder();
for (int i = 0; i < 30_000; i++) sb.append("x");  // ~1 ms    ← aynı tampon
```

100 kat fark. Sebep: String immutable olduğu için `+=` her adımda yeni nesne üretir; GC boğulur. **Döngü içinde metin birleştiriyorsan `StringBuilder`.** (Döngü dışında `+` gayet iyidir, derleyici zaten optimize eder.)

## 6. Biçimlendirme

```java
System.out.printf("%-12s %8d %10.2f%n", "Klavye", 2, 749.90);
//                 │      │    │
//                 │      │    └─ 10 karakter genişlik, 2 ondalık
//                 │      └────── 8 karakter, tam sayı
//                 └───────────── sola yaslı 12 karakter

String log = String.format("[%s] level=%s user=%s", zaman, "INFO", "u-1042");
```

Java 15+ ile çok satırlı metin (text block):

```java
String mail = """
        Sayin musterimiz,
        %s numarali hesabiniz dogrulanmistir.""".formatted(maskedIban);
```

## 7. Çalıştır

```bash
mvn -q exec:java -Dexec.mainClass=com.meroddi.javaegitimi.m01temeller.ders03.TextAndValidationDemo
```

## 8. Sık yapılan hatalar

- String karşılaştırmasında `==` kullanmak. Bazen çalışır (havuz sayesinde), üretimde çalışmaz — en sinsi bug türü.
- `input.equals("EXPRESS")` yazıp `input` null gelince patlamak. Sabiti sola al: `"EXPRESS".equals(input)`.
- Döngüde `+=` ile metin birleştirmek.
- `substring` sınırlarını kontrol etmemek → `StringIndexOutOfBoundsException`.
- Log'a tam IBAN/kart numarası/TCKN yazmak. Bu bir güvenlik olayıdır, "sadece debug için" mazereti yoktur.

## 9. Mülakat notu

> "String neden immutable?"

Üç sebep: (1) güvenlik — dosya yolu/DB bağlantısı gibi değerler kontrol edildikten sonra değiştirilemez; (2) string pool ile bellek paylaşımı ancak değişmezlikle güvenlidir; (3) thread-safe'tir, hash değeri önbelleklenebilir (HashMap anahtarı olarak hızlıdır).

## 10. Alıştırma

1. **TCKN doğrulayıcı** yaz: 11 hane, ilk hane 0 olamaz, 10. ve 11. hane algoritmaya uygun olmalı (kural: 1,3,5,7,9. hanelerin toplamı×7 − 2,4,6,8. hanelerin toplamı, mod 10 = 10. hane; ilk 10 hanenin toplamının mod 10'u = 11. hane).
2. Bir e-posta listesini maskele: `ayse.yilmaz@example.com` → `ay***@example.com`.
3. Verilen bir cümlede en çok geçen kelimeyi bul (ipucu: `split(" ")` ve döngü).

---
**Önceki:** [Ders 02](ders-02-degiskenler-ve-tipler.md) | **Sonraki:** [Ders 04 — Karar yapıları](ders-04-karar-yapilari.md)
