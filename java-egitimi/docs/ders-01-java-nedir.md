# Ders 01 — Java nedir, kod nasıl çalışır?

> Kod: [`m01temeller/ders01/HelloJava.java`](../src/main/java/com/meroddi/javaegitimi/m01temeller/ders01/HelloJava.java)

## 1. Bu ne?

Java, 1995'te Sun Microsystems'te doğmuş, **derlenen + sanal makinede çalışan**, nesne yönelimli bir programlama dilidir.

En kritik özelliği şu: Java kodu doğrudan işlemcinin anlayacağı makine koduna değil, **bytecode** denen ara bir dile derlenir. Bu bytecode'u da **JVM (Java Virtual Machine)** çalıştırır.

```
HelloJava.java   →  javac  →  HelloJava.class  →  JVM  →  çalışan program
(kaynak kod)      (derleyici)   (bytecode)      (sanal makine)
```

Üç kısaltmayı karıştırma:

| Kısaltma | Açılım | Ne işe yarar |
|---|---|---|
| **JVM** | Java Virtual Machine | Bytecode'u çalıştırır |
| **JRE** | Java Runtime Environment | JVM + standart kütüphaneler (çalıştırmak için) |
| **JDK** | Java Development Kit | JRE + `javac` derleyici + araçlar (geliştirmek için) |

Sende **JDK 21** kurulu — yani hem yazabiliyor hem çalıştırabiliyorsun.

## 2. Nasıl çalışır?

1. `javac` kaynak kodu bytecode'a çevirir. Bu aşamada **tip hataları yakalanır** — Java "statically typed" bir dildir, hatanın çoğunu program çalışmadan önce görürsün. (Python'da aynı hatayı kullanıcı görür.)
2. JVM bytecode'u yorumlar.
3. **JIT (Just-In-Time) derleyici**, sık çalışan kod parçalarını çalışma anında makine koduna çevirir. Bu yüzden Java uzun süre çalışan sunucu uygulamalarında çok hızlıdır: program ısındıkça hızlanır.
4. **Garbage Collector (GC)** kullanılmayan nesneleri bellekten temizler. C/C++'taki gibi elle bellek yönetmezsin.

Yol haritasının "kalıcı çekirdek" katmanında *"bellek modeli: stack/heap, garbage collection nasıl çalışır"* ve *"derleyici/runtime temeli: JIT nedir"* yazıyor. İşte tam olarak bu.

## 3. Nerede kullanılıyor?

Java "eski dil" değil; dünyanın en çok para dönen sistemlerinin altında o var:

- **Bankacılık ve ödeme**: Türkiye'deki bankaların core banking sistemlerinin çok büyük kısmı Java. Aynısı dünyada da geçerli (Goldman Sachs, PayPal).
- **Büyük ölçekli backend**: Netflix, Uber, LinkedIn, Amazon, Google'ın birçok servisi.
- **Big Data / altyapı yazılımları**: Kafka, Elasticsearch, Hadoop, Spark, Cassandra — hepsi JVM üzerinde.
- **Android**: Android uygulamalarının tarihsel dili (bugün Kotlin öne çıksa da Kotlin de JVM'de çalışır).
- **Kurumsal sistemler**: ERP, sigorta, telekom, havayolu rezervasyon.

Yol haritasındaki "araç katmanı" tablosunda Java/JVM'in yarı ömrü **5–10 yıl** olarak geçiyor ve "sabit kalır" deniyor. Yani öğrendiğin şey 10 yıl boyunca değerini koruyacak.

## 4. Gerçek hayat örneği — kod

`HelloJava.java` dosyasındaki `main` metodunun her kelimesi bir şey ifade eder:

```java
public static void main(String[] args) { ... }
```

| Kelime | Neden var |
|---|---|
| `public` | JVM bu metodu dışarıdan çağırabilsin |
| `static` | Çağırmak için önce nesne üretmek gerekmesin |
| `void` | Geriye değer döndürmez |
| `String[] args` | Komut satırından gelen parametreler |

Program kendi çalışma ortamını da yazdırıyor:

```java
System.out.println("Java surumu   : " + System.getProperty("java.version"));
System.out.println("JVM adi       : " + System.getProperty("java.vm.name"));
```

Bu, gerçek projelerde "sunucuda hangi Java sürümü çalışıyor?" sorusunun cevabını almanın yoludur — üretim hatalarında ilk sorulan sorulardan biri.

## 5. Çalıştır

```bash
cd java-egitimi
mvn -q exec:java -Dexec.mainClass=com.meroddi.javaegitimi.m01temeller.ders01.HelloJava

# parametre vererek
mvn -q exec:java -Dexec.mainClass=com.meroddi.javaegitimi.m01temeller.ders01.HelloJava -Dexec.args="prod 8080"
```

### Maven nedir?

`mvn` komutu Maven'i çalıştırır. Maven üç iş yapar:
1. **Bağımlılık yönetimi**: `pom.xml`'e yazdığın kütüphaneleri internetten indirir (biz JUnit'i böyle ekledik).
2. **Derleme**: `src/main/java` altındaki kodu derler, `target/classes`'a koyar.
3. **Yaşam döngüsü**: `compile → test → package` adımlarını standartlaştırır.

Türkiye'de ve dünyada iş ilanlarında "Maven/Gradle" hep birlikte geçer; ikisi de aynı işi yapar.

## 6. Sık yapılan hatalar

- **Dosya adı ≠ sınıf adı.** `public class HelloJava` ise dosya `HelloJava.java` olmak zorunda.
- **Paket yapısı ≠ klasör yapısı.** `package com.meroddi.javaegitimi.m01temeller.ders01;` yazdıysan dosya tam olarak o klasör zincirinde olmalı.
- **`main` imzasını değiştirmek.** `static` yazmayı unutursan program "main method not found" der.
- Java'da her satır `;` ile biter, her blok `{ }` ile çevrilir. Girinti Python'daki gibi anlam taşımaz ama okunabilirlik için zorunludur.

## 7. Mülakat notu

> "Java neden platform bağımsızdır?"

Cevap: Derleme çıktısı makine koduna değil bytecode'a yapılır; her işletim sistemi için ayrı bir JVM vardır ve aynı `.class` dosyasını çalıştırır. "Write once, run anywhere."

## 8. Alıştırma

1. `HelloJava` dosyasını kopyalayıp `SystemInfo` adında yeni bir sınıf yap; şu bilgileri yazdırsın: kullanıcı adı (`user.name`), çalışma dizini (`user.dir`), kullanılabilir işlemci sayısı (`Runtime.getRuntime().availableProcessors()`), toplam bellek (`Runtime.getRuntime().totalMemory()`).
2. Programa 2 parametre gönder ve bunları "Ortam: X, Port: Y" biçiminde yazdır. Parametre verilmezse varsayılan olarak "dev" ve "8080" kullansın.

---
**Sonraki:** [Ders 02 — Değişkenler, tipler ve paranın doğru tutulması](ders-02-degiskenler-ve-tipler.md)
