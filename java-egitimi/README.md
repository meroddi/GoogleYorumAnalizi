# Java Eğitimi — Modül 1 (Temeller) + Modül 2 (OOP)

Bu eğitim, **10 Yıllık Eğitim Haritası — Yazılım Mühendisliği** belgesindeki
**YIL 1** hattının ilk adımıdır:

> **Öğrenilecek:** Java core → **İleri Java/OOP** → SQL → Spring Boot → REST API → Git → Docker → temel test → temel cloud
> **Çıktı:** 2 tam proje, canlı deploy, İngilizce CV, LeetCode 180+

Bu depodaki 15 ders, o hattın ilk iki kutusunu (**Java core** ve **İleri Java/OOP**) çalışan kod ve testlerle kapatır.

## Bu eğitimin yöntemi

Yol haritasının öğrenme hiyerarşisi:

```
Video izlemek → %5      Kod yazmak   → %40
Okumak        → %10     Bitmiş proje → %60      Başkasına anlatmak → %95
```

Buna göre her ders şu sırayla ilerler:

1. **Bu ne?** — kavramın tanımı
2. **Nasıl çalışır?** — JVM/bellek düzeyinde ne oluyor
3. **Nerede kullanılır?** — hangi gerçek sistemlerde, hangi problemde
4. **Gerçek hayat örneği — kodla** — bankacılık / e-ticaret / ödeme senaryoları
5. **Sık yapılan hatalar + mülakat notu**
6. **Alıştırma**

Her modül **çalışan bir projeyle** biter: Modül 1 → konsol POS, Modül 2 → katmanlı mini e-ticaret + 22 birim test.

## Kurulum

Gereken: **JDK 17+** (bu proje Java 21 ile derlenir) ve **Maven**.

```bash
java -version     # 21.x
mvn -version

cd java-egitimi
mvn -q compile    # tüm dersleri derle
mvn -q test       # testleri çalıştır (22 test)
```

## Ders indeksi

### Modül 1 — Java Temelleri

| # | Ders | Konu | Kod |
|---|---|---|---|
| 01 | [Java nedir, kod nasıl çalışır](docs/ders-01-java-nedir.md) | JVM/JDK/JRE, bytecode, JIT, Maven | [`HelloJava`](src/main/java/com/meroddi/javaegitimi/m01temeller/ders01/HelloJava.java) |
| 02 | [Değişkenler ve tipler](docs/ders-02-degiskenler-ve-tipler.md) | primitive/referans, stack-heap, taşma, `BigDecimal` ile para | [`MoneyPrecisionDemo`](src/main/java/com/meroddi/javaegitimi/m01temeller/ders02/MoneyPrecisionDemo.java) |
| 03 | [Operatörler ve String](docs/ders-03-operatorler-ve-string.md) | `==` vs `equals`, immutability, `StringBuilder`, IBAN doğrulama | [`TextAndValidationDemo`](src/main/java/com/meroddi/javaegitimi/m01temeller/ders03/TextAndValidationDemo.java) |
| 04 | [Karar yapıları](docs/ders-04-karar-yapilari.md) | `if/else`, ternary, modern `switch`, kargo + risk skoru | [`ShippingRulesDemo`](src/main/java/com/meroddi/javaegitimi/m01temeller/ders04/ShippingRulesDemo.java) |
| 05 | [Döngüler](docs/ders-05-donguler.md) | `for/while/do-while`, taksit tablosu, sayfalama, retry | [`LoopReportsDemo`](src/main/java/com/meroddi/javaegitimi/m01temeller/ders05/LoopReportsDemo.java) |
| 06 | [Diziler](docs/ders-06-diziler.md) | tek/çok boyutlu dizi, `Arrays`, stok raporu | [`StockArrayDemo`](src/main/java/com/meroddi/javaegitimi/m01temeller/ders06/StockArrayDemo.java) |
| 07 | [Metotlar](docs/ders-07-metotlar.md) | overloading, varargs, pass-by-value, recursion, fiyatlandırma | [`PricingMethodsDemo`](src/main/java/com/meroddi/javaegitimi/m01temeller/ders07/PricingMethodsDemo.java) |
| 08 | [**Modül 1 projesi: POS**](docs/ders-08-modul1-projesi-pos.md) | konsol kasa uygulaması, fiş, indirim, gün sonu | [`PosApp`](src/main/java/com/meroddi/javaegitimi/m01temeller/ders08/PosApp.java) |

### Modül 2 — Nesne Yönelimli Programlama

| # | Ders | Konu | Kod |
|---|---|---|---|
| 09 | [Sınıf ve nesne](docs/ders-09-sinif-ve-nesne.md) | field, constructor, `this`, `null`, kural nesnenin içinde | [`ders09/`](src/main/java/com/meroddi/javaegitimi/m02oop/ders09/) |
| 10 | [Encapsulation, record, static](docs/ders-10-encapsulation-ve-record.md) | invariant, savunmacı kopya, immutability, `Money` value object | [`ders10/`](src/main/java/com/meroddi/javaegitimi/m02oop/ders10/) |
| 11 | [Kalıtım](docs/ders-11-kalitim.md) | `extends`, `super`, `@Override`, `toString/equals/hashCode`, banka hesapları | [`ders11/`](src/main/java/com/meroddi/javaegitimi/m02oop/ders11/) |
| 12 | [Polymorphism ve abstract](docs/ders-12-polymorphism-ve-abstract.md) | template method, ödeme yöntemleri, Open/Closed | [`ders12/`](src/main/java/com/meroddi/javaegitimi/m02oop/ders12/) |
| 13 | [Interface ve DI](docs/ders-13-interface.md) | sözleşme, `default` metot, `Optional`, dependency injection | [`ders13/`](src/main/java/com/meroddi/javaegitimi/m02oop/ders13/) |
| 14 | [Enum ve exception](docs/ders-14-enum-ve-exception.md) | davranışlı enum, durum makinesi, kendi exception'ın, erişim belirleyiciler | [`ders14/`](src/main/java/com/meroddi/javaegitimi/m02oop/ders14/) |
| 15 | [**Modül 2 projesi: e-ticaret**](docs/ders-15-modul2-projesi.md) | model/repository/service katmanları + JUnit 5 testleri | [`ders15/`](src/main/java/com/meroddi/javaegitimi/m02oop/ders15/) |

## Dersleri çalıştırma

Her ders bağımsız çalışır:

```bash
mvn -q exec:java -Dexec.mainClass=com.meroddi.javaegitimi.m01temeller.ders01.HelloJava
mvn -q exec:java -Dexec.mainClass=com.meroddi.javaegitimi.m01temeller.ders02.MoneyPrecisionDemo
mvn -q exec:java -Dexec.mainClass=com.meroddi.javaegitimi.m01temeller.ders03.TextAndValidationDemo
mvn -q exec:java -Dexec.mainClass=com.meroddi.javaegitimi.m01temeller.ders04.ShippingRulesDemo
mvn -q exec:java -Dexec.mainClass=com.meroddi.javaegitimi.m01temeller.ders05.LoopReportsDemo
mvn -q exec:java -Dexec.mainClass=com.meroddi.javaegitimi.m01temeller.ders06.StockArrayDemo
mvn -q exec:java -Dexec.mainClass=com.meroddi.javaegitimi.m01temeller.ders07.PricingMethodsDemo
mvn -q exec:java -Dexec.mainClass=com.meroddi.javaegitimi.m01temeller.ders08.PosApp

mvn -q exec:java -Dexec.mainClass=com.meroddi.javaegitimi.m02oop.ders09.ObjectBasicsDemo
mvn -q exec:java -Dexec.mainClass=com.meroddi.javaegitimi.m02oop.ders10.EncapsulationDemo
mvn -q exec:java -Dexec.mainClass=com.meroddi.javaegitimi.m02oop.ders11.InheritanceDemo
mvn -q exec:java -Dexec.mainClass=com.meroddi.javaegitimi.m02oop.ders12.PolymorphismDemo
mvn -q exec:java -Dexec.mainClass=com.meroddi.javaegitimi.m02oop.ders13.InterfaceDemo
mvn -q exec:java -Dexec.mainClass=com.meroddi.javaegitimi.m02oop.ders14.EnumDemo
mvn -q exec:java -Dexec.mainClass=com.meroddi.javaegitimi.m02oop.ders15.ShopApp
```

**Ders 08 (POS)** etkileşimlidir; girdiyi boru ile de verebilirsin:

```bash
printf "1\n2\n1\n2\n2\n3\n1\n3\n4\n5\n" | mvn -q exec:java \
  -Dexec.mainClass=com.meroddi.javaegitimi.m01temeller.ders08.PosApp
```

Derlenmiş sınıfları doğrudan çalıştırmak daha hızlıdır (önce `mvn -q compile`):

```bash
java -cp target/classes com.meroddi.javaegitimi.m02oop.ders15.ShopApp
```

## Testler

```bash
mvn -q test                     # hepsi
mvn -q test -Dtest=MoneyTest    # tek sınıf
```

| Test sınıfı | Ne doğrular |
|---|---|
| `MoneyTest` (7) | yuvarlama, immutability, değer eşitliği, para birimi kontrolü |
| `OrderStatusTest` (5) | sipariş durum geçiş kuralları |
| `OrderServiceTest` (10) | sipariş akışı, stok, ödeme, iptal, ciro |

## Proje yapısı

```
java-egitimi/
├── pom.xml                       # Java 21, UTF-8, JUnit 5, exec plugin
├── docs/                         # 15 ders anlatımı (Türkçe)
└── src/
    ├── main/java/com/meroddi/javaegitimi/
    │   ├── m01temeller/ders01 … ders08/
    │   └── m02oop/ders09 … ders15/
    └── test/java/com/meroddi/javaegitimi/
```

## Nasıl çalışmalı?

1. Ders dosyasını (`docs/ders-XX-*.md`) oku.
2. İlgili `.java` dosyasını **satır satır** oku — yorumlar "ne" değil "neden" anlatır.
3. Programı çalıştır, çıktıyı gördüğünle karşılaştır.
4. Kodu **boz**: bir `if`'i ters çevir, `BigDecimal`'i `double` yap, `@Override`'ı sil. Ne olduğunu gör.
5. Alıştırmaları yap. Cevabı olmayan sorular kasıtlıdır; araştırma da mesleğin parçası.
6. Öğrendiğini bir paragrafla kendi cümlelerinle yaz. (%95 kalıcılık.)

## Sıradaki modüller

| Modül | Konu |
|---|---|
| 3 | Collections & Generics — `List`, `Map`, `Set`, `Comparator`, `Optional` |
| 4 | Exception derinleşme, I/O, `java.time`, Stream API & lambda |
| 5 | SQL + JDBC — modelleme, join, index, transaction |
| 6 | Spring Boot + REST API |
| 7 | Test (JUnit + Mockito), Git, Docker, temel cloud |

## Kanonik kaynaklar (yol haritası, Yıl 1–2)

- **Effective Java** — Joshua Bloch (Java yazan herkes için zorunlu)
- **The Pragmatic Programmer** — Hunt & Thomas
- **Operating Systems: Three Easy Pieces** — ücretsiz, ostep.org
- **Computer Networking: A Top-Down Approach** — Kurose & Ross
