# Ders 05 — Döngüler: for, while, do-while, for-each

> Kod: [`m01temeller/ders05/LoopReportsDemo.java`](../src/main/java/com/meroddi/javaegitimi/m01temeller/ders05/LoopReportsDemo.java)

## 1. Bu ne?

Aynı işi tekrar tekrar yapmanın yolu. Dört biçim var ve hangisini seçtiğin niyetini anlatır:

```java
for (int i = 0; i < n; i++)  { }   // kaç kez döneceğini biliyorsun, indeks lazım
for (String s : list)        { }   // her elemanı gez, indeks lazım değil
while (koşul)                { }   // kaç kez döneceğini bilmiyorsun
do { } while (koşul);              // en az bir kez çalışmalı
```

## 2. Nasıl çalışır?

`for (başlangıç; şart; adım)`:
1. `başlangıç` bir kez çalışır
2. `şart` kontrol edilir → `false` ise döngü biter
3. Gövde çalışır
4. `adım` çalışır → 2'ye dön

**`break`** döngüyü tamamen bitirir, **`continue`** sadece o turu atlar.

## 3. Nerede kullanılır?

Backend'de yazdığın kodun büyük kısmı "bir listeyi baştan sona gez, bir şeyler hesapla, sonucu döndür" biçimindedir:

- Sepet toplamı, fatura satırları, taksit tablosu
- Rapor üretimi (günlük ciro, şube performansı)
- **Sayfalama (pagination)**: API'den 10.000 kaydı 100'erli çekmek
- **Retry**: başarısız isteği artan beklemeyle yeniden denemek
- Toplu işlem: 50.000 kullanıcıya bildirim gönderme

## 4. Gerçek hayat örnekleri — kod

### a) Taksit tablosu — yuvarlama artığı kime yazılır?

```java
BigDecimal monthly = amount.divide(BigDecimal.valueOf(installments), 2, RoundingMode.HALF_UP);
BigDecimal remaining = amount;

for (int i = 1; i <= installments; i++) {
    // Son taksit, yuvarlamadan kalan kuruşu üstlenir — gerçek bankacılıkta böyle yapılır
    BigDecimal current = (i == installments) ? remaining : monthly;
    remaining = remaining.subtract(current);
}
```

Bu detay önemli: 12.000 TL'yi 7 taksite bölersen 1714.29 × 7 = 12.000,03 eder. Fark kuruşu birine yazmak zorundasın; standart yaklaşım son taksittir.

### b) Sayfalama — while

```java
int processed = 0;
while (processed < totalRecords) {
    int batch = Math.min(pageSize, totalRecords - processed);
    // ... bu sayfayı işle
    processed += batch;
}
```

Gerçek API entegrasyonlarının kalıbı budur: kaç sayfa olduğunu baştan bilmezsin.

### c) Retry + exponential backoff — do-while

```java
int attempt = 0;
boolean success = false;
do {
    attempt++;
    success = callExternalService();
    long backoffMs = (long) Math.pow(2, attempt) * 100;   // 200, 400, 800 ms
} while (!success && attempt < 5);
```

Yol haritasının kalıcı çekirdeğinde *"idempotency, retry, exponential backoff, circuit breaker"* diye geçen konunun en basit hali. Neden artan bekleme? Çünkü hepsi aynı anda tekrar denerse çöken servisi bir daha çökertirsin (thundering herd).

### d) break / continue — günlük limit kontrolü

```java
for (int value : paymentAmounts) {
    if (value <= 0) continue;                    // geçersiz kaydı ATLA
    if (sum + value > 10000) { ... break; }      // limiti aşacaksa DUR
    sum += value;
}
```

### e) İç içe döngü — şube × gün ciro matrisi

```java
for (int b = 0; b < branches.length; b++) {
    for (int d = 0; d < days.length; d++) {
        System.out.printf("%7d", dailySales[b][d]);
        branchTotal += dailySales[b][d];
    }
}
```

Çıktı:

```
SUBE          Pzt    Sal    Car    Per    Cum   TOPLAM
Kadikoy      1200   1500    900   1750   2100     7450
Besiktas      800    950   1100   1050   1600     5500
```

## 5. Çalıştır

```bash
mvn -q exec:java -Dexec.mainClass=com.meroddi.javaegitimi.m01temeller.ders05.LoopReportsDemo
```

## 6. Sık yapılan hatalar

1. **Sonsuz döngü** — `while` içinde sayacı artırmayı unutmak.
2. **Off-by-one** — `i <= dizi.length` yazmak. Son geçerli indeks `length - 1`'dir.
3. **Döngü içinde veritabanı sorgusu (N+1 problemi)** — 100 sipariş için 1 + 100 sorgu atmak. Bu, yol haritasının 2. yılında en sık karşılaşacağın performans incident'idir. Çözüm: tek sorguda toplu çekmek (`IN (...)`, `JOIN`).
4. **Döngü içinde String `+=`** — Ders 03'teki 100 katlık fark.
5. **Döngü içinde nesne üretmek** — gereksiz GC baskısı (`new SimpleDateFormat()` her turda).

## 7. Mülakat notu

> "for ile for-each arasındaki fark nedir, hangisini seçersin?"

`for-each` daha okunabilirdir ve indeks hatası yapamazsın; ama indekse ihtiyacın varsa, ters gezmek istiyorsan veya döngü içinde eleman silmen gerekiyorsa klasik `for` (ya da `Iterator`) kullanırsın. Koleksiyonu gezerken içinden eleman silmek `ConcurrentModificationException` fırlatır — `Iterator.remove()` gerekir.

## 8. Alıştırma

1. **Kredi amortisman tablosu**: 100.000 TL, %35 yıllık faiz, 24 ay. Her ay için ana para, faiz ve kalan borcu yazdır.
2. **Fibonacci** dizisini hem `for` hem `while` ile üret (ilk 20 terim), `long` kullan ve nerede taştığını gözlemle.
3. **Yıldız üçgeni**: kullanıcıdan alınan n için iç içe döngüyle piramit çiz. (Basit görünür ama iç içe döngü mantığını oturtur.)
4. `paymentAmounts` dizisinde limiti aşan kaydı atlayıp devam eden bir sürüm yaz (`break` yerine `continue`) ve sonucun nasıl değiştiğini yorumla.

---
**Önceki:** [Ders 04](ders-04-karar-yapilari.md) | **Sonraki:** [Ders 06 — Diziler](ders-06-diziler.md)
