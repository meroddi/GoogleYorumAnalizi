# Ders 08 — Modül 1 Projesi: Konsol POS (kasa) uygulaması

> Kod: [`m01temeller/ders08/PosApp.java`](../src/main/java/com/meroddi/javaegitimi/m01temeller/ders08/PosApp.java)

## 1. Neden proje?

Yol haritasının öğrenme hiyerarşisi net:

```
Video izlemek → %5 kalıcı      Kod yazmak → %40
Okumak        → %10            Bitmiş proje → %60
```

Ders 01-07'de öğrendiğin her şey burada tek bir çalışan programda birleşiyor. "Konu bitti" demek yerine "program çalışıyor" diyebilmek modülü tamamlar.

## 2. Ne yapıyor?

Bir mağaza kasası:

```
1) Ürünleri listele
2) Fişe ürün ekle
3) Açık fişi göster
4) Fişi kapat (ödeme)
5) Gün sonu ve çıkış
```

- Ürün kataloğu ve stok takibi
- Fişe satır ekleme, stok rezervasyonu
- Kademeli indirim (5.000 TL üstü %5, 10.000 TL üstü %10)
- KDV hesabı ve fiş çıktısı
- Gün sonu raporu: fiş sayısı, ciro, ortalama sepet, kalan stoklar

## 3. Hangi ders nerede kullanıldı?

| Ders | Projede karşılığı |
|---|---|
| 02 — tipler | `BigDecimal` ile fiyat/tutar, `int` ile adet |
| 03 — String | `printf` ile hizalı fiş çıktısı, text block ile başlık |
| 04 — karar | `switch` ile menü, `if/else` ile kademeli indirim |
| 05 — döngü | Menü döngüsü (`while`), fiş satırlarını gezme (`for`) |
| 06 — dizi | Paralel diziler: ürün kodu, ad, fiyat, stok; fiş satırları |
| 07 — metot | `subtotal()`, `calculateDiscount()`, `lineTotal()`, `readInt()` |

## 4. Öne çıkan tasarım kararları

### a) Savunmacı girdi okuma

Kullanıcıdan gelen her şey yanlış olabilir; program çökmemelidir:

```java
private static Integer readInt(Scanner scanner) {
    String line = readLine(scanner);
    if (line == null) return null;                 // girdi akışı bitti
    try {
        return Integer.parseInt(line.trim());
    } catch (NumberFormatException e) {
        System.out.println(">> Sayi bekleniyordu, '" + line.trim() + "' girildi.");
        return null;                               // program devam ediyor
    }
}
```

Bu kalıp gerçek sistemlerde "kullanıcı hatası ≠ sistem hatası" ayrımıdır. Kullanıcının yanlış tuşa basması log'a `ERROR` yazmaz.

### b) Stok, fişe eklerken düşülür

```java
if (quantity > STOCK[index]) {
    System.out.println(">> Yetersiz stok. Mevcut: " + STOCK[index]);
    return;
}
cartProductIndex[cartLineCount] = index;
cartQuantity[cartLineCount] = quantity;
cartLineCount++;
STOCK[index] -= quantity;      // rezervasyon
```

Gerçek e-ticarette de böyledir: sepete atınca değil ama ödemeye geçince stok rezerve edilir. Aksi halde iki müşteri son ürünü aynı anda satın alır. (Bu problemin adı **race condition** — yol haritasında Yıl 2-3 konusu.)

### c) Hesaplama metotları ayrı

`subtotal()`, `calculateDiscount()`, `lineTotal()` ayrı metotlar olduğu için, ilerideki modülde bunları **birim testle** doğrulayabileceğiz. Her şeyi `main` içine yazsaydık test edilemezdi.

## 5. Çalıştır

Etkileşimli:

```bash
mvn -q exec:java -Dexec.mainClass=com.meroddi.javaegitimi.m01temeller.ders08.PosApp
```

Girdiyi boru ile vererek (otomatik senaryo — 2 ürün ekle, fişi göster, kapat, gün sonu):

```bash
printf "1\n2\n1\n2\n2\n3\n1\n3\n4\n5\n" | mvn -q exec:java \
  -Dexec.mainClass=com.meroddi.javaegitimi.m01temeller.ders08.PosApp
```

Örnek çıktı:

```
--------------- FIS ---------------
Mekanik Klavye       2 x   2500.00 =    5000.00
Kablosuz Mouse       1 x    289.50 =     289.50
-----------------------------------
Ara toplam                    5289.50
Indirim                        264.48  (5000 TL ustu %5)
KDV %20                       1005.00
ODENECEK                      6030.02
```

## 6. Bu kodun zayıf yanları (kasıtlı)

Modül 2'ye geçiş sebebimiz tam olarak bunlar:

1. **Paralel diziler kırılgan.** `PRODUCT_CODES[i]`, `PRODUCT_PRICES[i]`, `STOCK[i]` — birini sıralarsan hepsi bozulur. Çözüm: `Product` **sınıfı** (Ders 09).
2. **Her şey `static`.** Program tek bir kasa varsayıyor. İki kasa olsa ne yapardık? Çözüm: nesne (Ders 09-10).
3. **Stok kuralı `main` akışında.** Kural veriye yapışık değil; başka bir yerden stok düşülürse kontrol atlanır. Çözüm: kuralı nesnenin içine koymak (encapsulation, Ders 10).
4. **Sabit boyutlu fiş (`MAX_LINES = 20`).** Çözüm: `List` (Modül 3).
5. **Test yok.** Konsol çıktısını gözle kontrol ediyoruz. Çözüm: iş mantığını UI'dan ayırmak + JUnit (Ders 15).

Bu liste bir eksiklik itirafı değil, öğrenme sırasıdır: önce çalışan bir şey, sonra iyi tasarlanmış bir şey.

## 7. Alıştırma (projeyi büyüt)

1. **6) Fişi iptal et** menüsü ekle: açık fişteki ürünlerin stoğunu geri ver, fişi sıfırla.
2. **Ödeme tipi sor** (nakit/kart) ve gün sonu raporunda ayrı ayrı topla. Kartta %1.5 komisyonu ciro raporunda göster.
3. **Barkod ile ekleme**: ürün numarası yerine SKU kodu girilebilsin (`KLV-001`). Dizide arama yapan bir metot yaz.
4. **En çok satan ürün**: gün sonunda hangi üründen kaç adet satıldığını yazdır (ipucu: ürün sayısı kadar `int[] soldCount`).
5. **Kupon kodu**: `WELCOME10` girilirse ek %10 indirim uygula, aynı fişte iki kez kullanılamasın.

---
**Önceki:** [Ders 07](ders-07-metotlar.md) | **Sonraki:** [Ders 09 — Sınıf ve nesne](ders-09-sinif-ve-nesne.md)

> **Modül 1 tamamlandı.** Buraya kadar olan her şeyi kendi kelimelerinle anlatabiliyor musun? Anlatamıyorsan tekrar et — yol haritası: *"Başkasına öğretmek/yazmak → %95 kalıcı."*
