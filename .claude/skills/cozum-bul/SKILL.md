---
name: cozum-bul
description: Bir hata, eksik kütüphane/paket, kurulum sorunu, "bunu nasıl yaparım" tıkanması ya da eksik bir yetenek (skill/plugin/MCP) ile karşılaşıldığında kullan. Önce proje hafızasına (.claude/COZUMLER.md) bakar — daha önce çözülmüşse aynı çözümü tekrar uygular; çözülmemişse internetten/kayıtlardan araştırır, gerekli kütüphaneyi veya skill'i kurar, sonra çözümü hafızaya YAZAR ki bir daha aranmasın. Tetikleyiciler - "hata alıyorum", "çalışmıyor", "kurulmuyor", "eksik paket", "module not found", "nasıl yaparım", "bunun için bir araç var mı", "kütüphane indir", "skill bul/indir", build/derleme hatası, bağımlılık çakışması, Unity/Android/iOS build sorunları.
---

# Çözüm Bul — otomatik araştır, kur, hatırla

**Amaç:** Aynı sorunu iki kez çözmemek. Her tıkanma bir kez araştırılır, çözülür ve
**proje hafızasına yazılır**. Zamanla proje kendi kendine "bunu biliyorum" der hâle gelir.

**Hafıza dosyası:** `.claude/COZUMLER.md` — tek doğruluk kaynağı.

---

## Akış (sırayla, atlamadan)

### 1. ÖNCE HAFIZAYA BAK — her zaman ilk adım

`.claude/COZUMLER.md` dosyasını oku ve sorunu ara (hata metni, paket adı, konu
etiketi). Araştırmaya başlamadan önce bunu yap; çoğu tekrar eden sorun burada
zaten çözülmüştür.

- **Kayıt varsa:** çözümü uygula. İşe yaradıysa kaydın `Son doğrulama` tarihini
  güncelle. Tek satırla bildir: *"Bu sorun hafızada vardı — çözümü uyguladım."*
- **Kayıt varsa ama artık işe yaramıyorsa:** kaydı **güncelle** (silme, üstüne
  yaz ve neyin değiştiğini not düş). Sürüm/araç değişimi en sık sebeptir.
- **Kayıt yoksa:** 2. adıma geç.

### 2. SORUNU SINIFLANDIR

Hangi tür eksiklik olduğunu belirle — araştırma yolu buna göre değişir:

| Tür | Ne yapılır |
|---|---|
| **Kütüphane / paket eksik** | Paket kayıtlarından doğru paketi bul ve kur (aşağıya bak) |
| **Yetenek eksik (skill)** | `SearchSkills` → `SuggestSkills` ile ara ve öner/kur |
| **Eklenti / araç eksik** | `SearchPlugins` → `SuggestPluginInstall` |
| **Dış servis bağlantısı** | `SearchMcpRegistry` (MCP sunucusu), `ListConnectors` |
| **Hata / build sorunu** | Hata metnini birebir ara (aşağıya bak) |
| **"Nasıl yaparım"** | Önce resmî dokümantasyon, sonra topluluk çözümleri |

### 3. ARAŞTIR

**Kütüphane/paket için** — ekosisteme göre doğru kaynak:
- Python → `pip index` / PyPI, Node → `npm view`, Unity → Package Manager (UPM) +
  Asset Store, C# → NuGet.
- Önce **resmî dokümantasyon**, sonra topluluk. Paket seçerken bak: son güncelleme
  tarihi, indirme sayısı, **lisans**, ve hedef platform uyumu (mobil/Unity için bu kritik).

**Hata mesajı için:**
- Hata metninin **en ayırt edici kısmını** ara (dosya yolu, kendi değişken adların
  gibi projeye özel parçaları çıkar).
- Sürüm numaralarını sorguya ekle — çözümler sürüme bağlıdır.
- `WebSearch` ile ara, umut verici sonucu `WebFetch` ile aç ve gerçekten oku.

**Yetenek/araç için:** yukarıdaki tabloda geçen arama araçlarını kullan. Yalnızca
listede/kayıtta **gerçekten var olan** şeyleri öner — isim uydurma.

### 4. UYGULA

- Kurulum komutunu çalıştır ya da yapılandırmayı değiştir.
- **Doğrula:** gerçekten çalıştığını gör (import et, build al, testi koştur).
  Doğrulamadan "çözüldü" deme.
- Çalışmadıysa bir sonraki adayı dene. **2 başarısız denemeden sonra** dur ve
  kullanıcıya bulduklarını + tıkandığın yeri anlat.

### 5. HAFIZAYA YAZ — bu adımı asla atlama

Çözüm çalıştıysa `.claude/COZUMLER.md` dosyasına bir kayıt **ekle**. Bu skill'in
tüm değeri bu adımda. Kayıt biçimi dosyanın kendi başında tanımlı.

**Ne zaman yazılır:** yeni bir sorun çözüldüğünde, mevcut bir çözüm bozulduğunda
(güncelle), ya da işe yaramayan bir yol denendiğinde ("Çıkmaz sokaklar" bölümüne
yaz — bu da zaman kazandırır).

**Ne zaman yazılmaz:** tek seferlik, projeye özgü olmayan, bir daha karşımıza
çıkmayacak şeyler. Hafızayı gürültüyle doldurma.

---

## Kurallar

- **Kurulumdan önce söyle.** Yeni bir bağımlılık, plugin veya araç kurmak projeyi
  kalıcı etkiler. Ne kuracağını ve neden gerektiğini bir cümleyle belirt.
  Kullanıcı zaten "otomatik kur" dediyse tekrar tekrar izin isteme — sadece bildir.
- **Sürümleri sabitle.** Kurduğun şeyin sürümünü kayda yaz; "en son sürüm" altı ay
  sonra başka bir şey demektir.
- **Lisansa bak.** Özellikle Unity Asset Store ve hazır 3D model/animasyon
  paketlerinde: ticari kullanım hakkı var mı?
- **Uydurma.** Var olduğundan emin olmadığın paket, skill veya komut önerme.
  Emin değilsen ara; bulamazsan "bulamadım" de.
- **Hafızayı temiz tut.** Kayıtlar kısa olsun: sorun, sebep, çözüm, kaynak. Uzun
  log dökümü yapıştırma.

## Bu projeye özgü sık alanlar

Bu depo bir **Unity mobil oyun** (bkz. plan: *BAĞIŞIK*) ve bir **Python/Streamlit**
uygulaması (`ömer/`) barındırır. Beklenen tıkanma alanları:

- Unity: URP mobil ayarları, IL2CPP/ARM64 build, Android SDK/NDK, Addressables,
  ink entegrasyonu, Mixamo rig/animasyon aktarımı
- Mobil: performans (draw call, lightmap boyutu), ekran/çentik uyumu, imzalama
- Python: paket çakışmaları, Streamlit sürüm farkları

Bu alanlardaki çözümleri kaydederken **sürüm bilgisini mutlaka yaz** — Unity ve
Android tarafında çözümler sürümden sürüme değişir.
