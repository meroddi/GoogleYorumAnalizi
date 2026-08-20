# DEMO — "Benzin İstasyonu"

Tek oda, uçtan uca, telefonda çalışan bir dikey dilim. Amacı oyunu yapmak değil,
**yapabilir miyiz sorusunu cevaplamak.**

---

## Demo neyi kanıtlayacak

Beş soru, hepsi ölçülebilir:

1. **Yürüme hissi doğru mu?** — dokunmatik joystick, kamera takibi, tepkisellik
2. **Telefonda akıyor mu?** — sabit 30 fps, 15 dakika ısındıktan sonra da
3. **İstediğimiz görüntüyü verebiliyor muyuz?** — sanat anayasası gerçekten
   o kasvetli, sinematik kareyi üretiyor mu
4. **Etkileşim ve diyalog çalışıyor mu?** — hotspot, seçim, hafıza bildirimi
5. **Tek kişi bunu sürdürebilir mi?** — bir oda ne kadar sürdü, 5 lokasyon ne demek

---

## Kapsam — bunlar VAR

- **Mekân:** gece, terk edilmiş benzin istasyonu. Küçük, sınırlı, açık hava.
  *(Neden burası: tek sodyum lamba = tek ışık kaynağı. Görsel kimliğimiz olan
  "soğuk boşlukta tek sıcak ada" tam olarak bu. Ve en hızlı iyi görünen sahne.)*
- **Karakter:** koruyucu. Yürüme + idle animasyonu.
- **Etkileşim:** 2 hotspot — biri incele (göz), biri al (el).
- **Diyalog:** 1 sahne, 2 seçenekli, biri zamanlı. Sonunda hafıza bildirimi.
- **Atmosfer:** sis, sodyum lamba, baked ışık, grade, hafif toz.
- **Süre:** ~2 dakikalık oynanış.

## Kapsam — bunlar YOK

Demoya girmeyecek, sonraya kalacak:

- Ela (ikinci karakter) — sadece koruyucu var
- Zombi, dövüş, QTE
- Envanter ekranı
- Kayıt sistemi
- Ses ve müzik
- Menü, ayarlar
- İkinci mekân

> Bunlardan biri "ama şu da olsa" diye aklına gelirse: **hayır.** Demo büyürse
> demo olmaktan çıkar ve hiç bitmez.

---

## Aşamalar — sırayla, atlamadan

Her aşama telefonda test edilmeden bir sonrakine geçilmez.

### Aşama 1 — Yürüyen kutu
Zemin + duvarlar + kapsül + dokunmatik joystick + takip kamerası.
**Telefonda build al ve yürü.**
→ *Cevap verdiği soru: hareket hissi ve akıcılık.*

### Aşama 2 — Karakter
Kapsülün yerine gerçek karakter + Idle/Walk blend tree.
→ *Cevap verdiği soru: animasyon hattı çalışıyor mu.*

### Aşama 3 — Etkileşim ve diyalog
Hotspot'lar, etkileşim butonu, diyalog kutusu, seçim, hafıza bildirimi.
→ *Cevap verdiği soru: oyunun çekirdek döngüsü kuruldu mu.*

### Aşama 4 — Sanat
Benzin istasyonu olarak giydir: sodyum lamba, baked ışık, sis, grade, dekor.
Sanat anayasasındaki (`docs/SANAT-YONU.md`) kontrol listesine göre.
→ *Cevap verdiği soru: istediğimiz görüntüyü alabiliyor muyuz.*

### Aşama 5 — Ölç ve karar ver
Telefonda 15 dakika soak testi. Kare süresi, ısınma, pil.
Sonra otur ve konuş: bu tempoda 5 lokasyon ne demek, kapsam doğru mu.

---

## Kurallar

- **Hiçbir şey satın alınmayacak.** Demo tamamen ücretsiz varlıklarla bitecek.
  Alışveriş listesi ancak ölçümler geldikten sonra açılır.
- **Her aşama gerçek telefonda test edilecek.** Editör yalan söyler.
- **Her aşama sonunda ekran görüntüsü** → sanat anayasasına göre denetlenecek.
- Takılınan her sorun `.claude/COZUMLER.md`'ye yazılacak.

---

## Başarı ölçütü

Demo şu üçünü aynı anda yaparsa başarılıdır:

1. Telefonda **sabit 30 fps**, 15 dakika sonra da
2. Ekran görüntüsü **sanat anayasasının kontrol listesini geçiyor**
3. Yürümek ve etkileşmek **iyi hissettiriyor**

Üçü de olursa: kapsamı büyütmeye karar verebiliriz.
Biri olmazsa: neyin değişmesi gerektiğini öğrenmiş oluruz — ki demo tam da
bunun için var.
