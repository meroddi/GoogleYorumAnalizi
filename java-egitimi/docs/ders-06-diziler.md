# Ders 06 — Diziler ve çok boyutlu diziler

> Kod: [`m01temeller/ders06/StockArrayDemo.java`](../src/main/java/com/meroddi/javaegitimi/m01temeller/ders06/StockArrayDemo.java)

## 1. Bu ne?

Dizi (array), **aynı tipten, sabit sayıda** elemanı bellekte **yan yana** tutan yapıdır.

```java
int[] stok = new int[5];                    // 5 elemanlık, hepsi 0
String[] skus = {"KLV-001", "MNT-014"};     // doğrudan değerlerle
int[][] matris = new int[3][5];             // 3 satır, 5 sütun
```

## 2. Nasıl çalışır?

- Dizi bir **nesnedir**, heap'te yaşar; değişken sadece adresini tutar.
- Boyut oluşturulurken belirlenir ve **asla değişmez**. "Diziye eleman eklemek" diye bir şey yoktur; yeni dizi oluşturup kopyalarsın (`ArrayList` tam olarak bunu senin yerine yapar).
- Elemanlar bellekte bitişik durduğu için erişim çok hızlıdır (`stok[3]` → tek adres hesabı, O(1)).
- Varsayılan değerler: sayısal tipler `0`, `boolean` `false`, referans tipler `null`.
- `.length` bir **alandır**, metot değil (String'de `length()` metottur — karıştırılır).

Yol haritasının "veri yapıları" maddesinin temeli budur: `ArrayList`, `HashMap`, `StringBuilder` — hepsi içeride dizi kullanır. Diziyi anlamayan, koleksiyonların performans karakterini de anlayamaz.

## 3. Nerede kullanılır?

Modern uygulama kodunda doğrudan dizi kullanımı azdır (yerine `List` kullanılır), ama şurada karşına çıkar:

- Performans kritik kod, düşük seviyeli kütüphaneler
- `byte[]` — dosya, resim, şifreleme, ağ üzerinden gelen ham veri
- `String[] args` — komut satırı parametreleri
- Matris/tablo hesapları (rapor, ısı haritası, oyun tahtası)
- Mülakat soruları ve algoritma çalışmaları (LeetCode'un ekmeği dizidir — yol haritasında Yıl 1 çıktısı "LeetCode 180+")

## 4. Gerçek hayat örneği — depo stok raporu

```java
String[] skus         = {"KLV-001", "MNT-014", "MSE-220", "KLK-077", "WEB-450"};
int[] stockCounts     = {42, 7, 0, 130, 15};
double[] prices       = {749.90, 4299.00, 289.50, 1150.00, 899.00};

for (int i = 0; i < skus.length; i++) {
    double lineValue = stockCounts[i] * prices[i];
    totalValue += lineValue;

    String status;
    if (stockCounts[i] == 0)      { status = "TUKENDI"; outOfStock++; }
    else if (stockCounts[i] < 10) { status = "KRITIK";  criticalStock++; }
    else                          { status = "NORMAL"; }
}
```

Bu "paralel diziler" tekniği (aynı indeks farklı dizilerde aynı ürünü temsil eder) **öğretici ama kırılgandır** — bir diziyi sıralarsan diğerleriyle bağı kopar. Doğrusu: bir `Product` sınıfı ve `Product[]`. Ders 09'da tam olarak buna geçeceğiz.

### Tek geçişte min / max / ortalama

```java
for (int i = 0; i < stockCounts.length; i++) {
    if (stockCounts[i] > stockCounts[maxIndex]) maxIndex = i;
    if (stockCounts[i] < stockCounts[minIndex]) minIndex = i;
    sum += stockCounts[i];
}
```

Diziyi üç kez gezmek yerine bir kez gezmek — O(3n) yerine O(n). Küçük veri için fark etmez, milyonluk veride eder.

### Çok boyutlu dizi — depo × ürün matrisi

```java
int[][] matrix = {
        {20, 3, 0, 60, 5},     // İstanbul
        {12, 2, 0, 40, 7},     // Ankara
        {10, 2, 0, 30, 3}      // İzmir
};
matrix[1][3]   // Ankara deposunda KLK-077 ürününden 40 adet
```

Java'da çok boyutlu dizi aslında **dizilerin dizisidir**; satırlar farklı uzunlukta olabilir (jagged array).

## 5. `Arrays` yardımcı sınıfı

```java
Arrays.toString(dizi)                  // yazdırmak için (dizi.toString() işe yaramaz!)
Arrays.copyOf(dizi, dizi.length)       // gerçek kopya
Arrays.copyOfRange(dizi, 0, 3)         // dilim
Arrays.sort(dizi)                      // ORİJİNALİ değiştirir
Arrays.binarySearch(siraliDizi, 15)    // SADECE sıralı dizide doğru çalışır
Arrays.fill(dizi, 2)                   // hepsini doldur
Arrays.equals(a, b)                    // içerik karşılaştırma
```

Kopya tuzağı:

```java
int[] alias = stockCounts;                                   // AYNI dizi
int[] copy  = Arrays.copyOf(stockCounts, stockCounts.length); // GERÇEK kopya
alias[0] = 999;    // stockCounts[0] de 999 oldu
```

## 6. Çalıştır

```bash
mvn -q exec:java -Dexec.mainClass=com.meroddi.javaegitimi.m01temeller.ders06.StockArrayDemo
```

## 7. Sık yapılan hatalar

- `ArrayIndexOutOfBoundsException` — geçerli indeksler `0 … length-1`.
- `dizi.length()` yazmak (dizide alan, String'de metot).
- `System.out.println(dizi)` → `[I@6d06d69c` basar. `Arrays.toString(dizi)` kullan.
- Sırasız dizide `binarySearch` çağırmak → çöp sonuç, hata yok.
- Diziyi `=` ile "kopyaladığını" sanmak.
- Sabit boyut sorununu fark etmemek: eleman ekleyip çıkaracaksan `ArrayList` kullan.

## 8. Mülakat notu

> "Array ile ArrayList farkı?"

Dizi sabit boyutlu, primitive tutabilir, `length` alanı var; `ArrayList` dinamik boyutlu, sadece nesne tutar (`int` yerine `Integer` — autoboxing), `size()` metodu var ve içeride dizi kullanıp dolunca ~1.5 katına büyütür (amortized O(1) ekleme).

## 9. Alıştırma

1. Stok dizisi üzerinde **yeniden sipariş listesi** üret: stoğu 10'un altındaki SKU'ları ve kaç adet sipariş edilmesi gerektiğini (hedef stok 50) yazdır.
2. Şube × gün ciro matrisinde: en yüksek cirolu şubeyi, en yüksek cirolu günü ve haftalık ortalamayı bul.
3. Bir tam sayı dizisini **kütüphane kullanmadan** küçükten büyüğe sırala (bubble sort). Sonra `Arrays.sort` ile karşılaştır.
4. İki dizinin kesişimini bulan metot yaz (`int[] intersect(int[] a, int[] b)`).

---
**Önceki:** [Ders 05](ders-05-donguler.md) | **Sonraki:** [Ders 07 — Metotlar](ders-07-metotlar.md)
