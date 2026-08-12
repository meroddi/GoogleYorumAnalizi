# Çözüm Hafızası

Bu dosya projenin **tıkanma hafızasıdır**. Bir sorun bir kez çözülür, buraya
yazılır ve bir daha aranmaz.

**Nasıl kullanılır:** Bir hata/eksiklikle karşılaşıldığında **önce burası aranır**
(`cozum-bul` skill'i bunu otomatik yapar). Kayıt varsa uygulanır; yoksa araştırılır,
çözülür ve buraya eklenir.

---

## Kayıt biçimi

Yeni kayıtları ilgili başlığın altına, **en yeni üstte** olacak şekilde ekle:

```markdown
### <Kısa ve aranabilir başlık>
- **Belirti:** Hata mesajının ayırt edici kısmı / gözlenen davranış
- **Sebep:** Neden oluyor (biliniyorsa)
- **Çözüm:** Uygulanan adım veya komut
- **Ortam:** Sürümler (ör. Unity 6000.x, Android NDK r23, Python 3.11)
- **Kaynak:** Bağlantı veya "kendi denememiz"
- **Son doğrulama:** YYYY-AA-GG
```

**Kurallar**
- Kısa tut: sorun → sebep → çözüm. Uzun log dökümü yapıştırma.
- **Sürümü mutlaka yaz** — özellikle Unity/Android tarafında çözümler sürüme bağlı.
- Bir çözüm artık işe yaramıyorsa **sil değil, güncelle** ve neyin değiştiğini not düş.
- Tek seferlik, tekrar etmeyecek şeyleri yazma. Hafıza gürültüyle dolmasın.

---

## Unity / Oyun

### Unity Hub'da "LTS" etiketi görünmüyor — hangi sürümü seçmeli?
- **Belirti:** Hub → Install Editor listesinde sürümlerin yanında "LTS" yazmıyor,
  hangisinin uzun destekli olduğu anlaşılmıyor.
- **Sebep:** Hub yeni sürümlerde LTS etiketini her satırda göstermiyor; bazen
  sadece "Recommended" rozeti koyuyor, bazen hiç rozet olmuyor.
- **Çözüm:** Etikete değil **sürüm numarasına** bak. Eşleme:
  `6000.0.x` = Unity 6.0 · `6000.1.x` = 6.1 · `6000.2.x` = 6.2 · `6000.3.x` = 6.3.
  **Official Releases** sekmesinde kal, **`6000.3.` ile başlayan en güncel**
  sürümü seç (Unity 6.3 LTS). "Recommended" rozeti varsa doğrudan onu seç.
- **Dikkat:** Unity 6.0 LTS desteği **Ekim 2026**'da bitiyor — yeni projede seçme.
  Unity 6.3 LTS **Aralık 2027**'ye kadar destekli.
- **Kaynak:** unity.com/releases/unity-6/support
- **Son doğrulama:** 2026-08-12

*Beklenen diğer alanlar: URP mobil profili, IL2CPP + ARM64 build, Android SDK/NDK
sürüm uyumu, Addressables, ink entegrasyonu, Mixamo rig/animasyon aktarımı,
Asset Store paketlerinin mobil uyumu.*

---

## Mobil / Build & Yayın

*(Henüz kayıt yok.)*

Beklenen alanlar: APK/AAB imzalama, Play Console yükleme hataları, ekran ve
çentik uyumu, performans (draw call, lightmap boyutu, texture bellek), iOS için
Mac/Xcode gereksinimleri.

---

## Python / Streamlit (`ömer/`)

*(Henüz kayıt yok.)*

Beklenen alanlar: paket sürüm çakışmaları, Streamlit sürüm farkları, secrets
yönetimi, bellek/performans.

---

## Araçlar & Yetenekler (skill / plugin / MCP)

Kurulan veya denenen yetenekler burada takip edilir — aynı aramayı iki kez yapmamak için.

### cozum-bul (bu sistemin kendisi)
- **Ne işe yarar:** Tıkanmalarda önce bu hafızaya bakar; yoksa araştırır, kurar ve
  çözümü buraya yazar.
- **Yeri:** `.claude/skills/cozum-bul/SKILL.md`
- **Son doğrulama:** 2026-08-12

---

## Çıkmaz sokaklar (denendi, işe yaramadı)

İşe **yaramayan** yollar da zaman kazandırır — aynı yanlış yolu tekrar denememek için.

### Cel-shaded / çizgi-roman görsel stili
- **Ne denendi:** Oyunun sanat stili olarak Telltale-vari kalın kontur + cel-shading.
- **Neden bırakıldı:** Sabit karede iyi durdu ama **hareketli videoda** fazla
  anime/çizgi-film hissi verdi; "POW" tarzı çizgi-roman efektleri gerçekçiliği kaçırdı.
- **Yerine:** Yarı-gerçekçi (Detroit-leaning) stil. Bkz. plan §2a.
- **Son doğrulama:** 2026-08-12

### AI video ile kusursuz tutarlılık kovalamak
- **Ne denendi:** Prompt'u gitgide detaylandırarak AI videoda bozuk el, yüz
  değişimi ve karakter stil kaymasını sıfırlamak.
- **Neden bırakıldı:** Bunlar prompt hatası değil, mevcut AI video teknolojisinin
  sınırı — hiçbir prompt %100 çözmüyor.
- **Yerine:** Karakter referans görseli (i2v) + klip başına tek hareket + tek zombi
  ile **azalt**, sonra kabul et. Bu klipler previs/teaser; oyunun asıl animasyonu
  elle/rig ile yapılacağı için bu glitch'ler oyuna geçmez.
- **Son doğrulama:** 2026-08-12
