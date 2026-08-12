# Yürüyen Kutu Testi — Unity kurulum rehberi

Bu klasör, *BAĞIŞIK* için ilk teknik doğrulamanın script'lerini içerir.

**Amaç:** En ucuz yoldan tek bir soruyu cevaplamak — *hareket hissi doğru mu ve
telefonda akıcı çalışıyor mu?* Güzel modellerle uğraşmadan önce bunu bilmek
gerekir; çünkü hareket hissi kötüyse hiçbir model onu kurtarmaz.

**Ortam:** Windows + gerçek Android telefon.

---

## 1. Unity kurulumu

1. **Unity Hub**'ı indir ve kur.
2. Hub → *Installs* → *Install Editor* → **en güncel LTS** (Unity 6 / 6000.x LTS hattı).
3. Modül seçiminde şu üçünü **mutlaka** işaretle — sonradan eklemek zahmetli:
   - Android Build Support
   - └ OpenJDK
   - └ Android SDK & NDK Tools
4. Yeni proje: **Universal 3D (URP)** şablonu.
   *HDRP seçme — masaüstü/konsol içindir, mobilde çalışmaz.*

## 2. Paketler

Window → Package Manager → Unity Registry:

| Paket | Ne için |
|---|---|
| **Input System** | Dokunmatik joystick (zorunlu) |
| **Cinemachine** | Sinematik kamera (opsiyonel — `CameraRig.cs` alternatifi var) |
| **TextMeshPro** | Arayüz metni |

Input System kurulunca Unity yeniden başlatmak isteyecek — kabul et.
Sorarsa **Both** (eski + yeni) seçebilirsin.

## 3. Telefonu hazırla

1. Telefon → Ayarlar → *Telefon hakkında* → **Yapı numarası**na 7 kez dokun.
2. Geliştirici seçenekleri → **USB hata ayıklama**'yı aç.
3. USB ile bilgisayara bağla, çıkan "Bu bilgisayara izin ver" onayını ver.

## 4. Mobil build ayarları (baştan yap)

**File → Build Settings** → Platform: **Android** → *Switch Platform*.

**Player Settings**:
- Scripting Backend: **IL2CPP**
- Target Architectures: **ARM64** *(Play Store zorunlu kılıyor)*
- Graphics APIs: **Vulkan** (yedek olarak GLES3)

**URP Asset** (Project'te `Settings` klasöründe) — mobil profili:
- Shadow Distance: kısa (20–30)
- Cascade Count: 1–2
- MSAA: 2x
- Ağır post-process yok — grade + vignette + hafif grain yeter

## 5. Sahneyi kur

1. **Zemin:** GameObject → 3D Object → Plane (ölçek 3×1×3 civarı).
2. **Duvarlar/engeller:** birkaç Cube — yürünecek alanı sınırla
   (unutma: **sınırlı** serbest yürüme, açık dünya değil).
3. **Oyuncu:** GameObject → 3D Object → **Capsule**
   - `CharacterController` bileşeni ekle
   - `PlayerMover.cs` ekle
   - `HotspotDetector.cs` ekle
4. **Kamera:** Main Camera'ya `CameraRig.cs` ekle → *Target* alanına Capsule'ü sürükle.

## 6. Dokunmatik joystick

1. **Input Actions varlığı:** Project → sağ tık → Create → Input Actions.
   Çift tıkla, bir Action Map ekle, içine **Move** adında `Value / Vector2` action ekle.
   *Save Asset* demeyi unutma.
2. **Ekran joystick'i:**
   - Hierarchy → UI → Image (Canvas otomatik oluşur) — bu joystick'in **arka halkası**
   - İçine bir Image daha ekle — bu **sap** (stick)
   - Sap nesnesine `On-Screen Stick` bileşeni ekle
   - *Control Path* → `Move` action'ına bağla
   - Sol alt köşeye yerleştir, boyutu parmakla rahat kullanılacak kadar büyük tut
3. Capsule'deki `PlayerMover` → *Move Action* alanına Move action'ını sürükle.

> Editörde klavye ile de test edebilirsin — `PlayerMover` WASD'yi yedek olarak destekler.

## 7. Telefonda çalıştır

**File → Build And Run** (telefon USB'de bağlıyken).
İlk build uzun sürer; sonrakiler hızlanır.

### Neye bakacaksın

Bu testin tek amacı şu üç soruyu cevaplamak:

- **Hareket tepkili mi?** Parmağını ittiğinde karakter gecikmeden gidiyor mu?
- **Kamera rahat mı?** Takip yumuşak mı, mide bulandırıyor mu, duvara girince ne oluyor?
- **Akıcı mı?** FPS sabit 30+ mı (tercihen 60)? Telefon ısınıyor mu?

Ayar için `PlayerMover` üzerinde `acceleration` ve `turnSpeed`, `CameraRig`
üzerinde `offset` ve `followSmoothTime` ile oyna. **Play modunda canlı
değiştirebilirsin** — hissi bulunca değerleri not al.

---

## 8. Sonra: gerçek varlıkları tak

Hareket hissi doğruysa sıra görüntü kalitesinde:

1. **Mixamo**'dan (ücretsiz) bir karakter + `Idle`, `Walking`, `Running`
   animasyonlarını FBX olarak indir.
2. Unity'de model → Inspector → Rig → Animation Type: **Humanoid** → Apply.
3. Animator Controller oluştur: `Speed` adında float parametresi + Idle↔Walk↔Run
   **Blend Tree**. `PlayerMover` bu parametreyi otomatik besler
   (0 = duruyor, 1 = koşuyor).
4. Capsule'ün yerine karakteri koy, `PlayerMover`'ın *Animator* alanına bağla.
5. **Asset Store**'dan hastane ya da market ortam kiti al — alırken kontrol et:
   **mobil uyumlu mu** (poly/texture bütçesi) ve **lisans ticari kullanıma açık mı**.
6. Baked lighting + sis + post-process ekle.

Bu adım bitince elinde *"oyunumuz böyle görünecek"* sorusunun somut cevabı olur.

---

## Script'ler

| Dosya | Ne yapar |
|---|---|
| `PlayerMover.cs` | Joystick girdisini kameraya göre dünya yönüne çevirir; CharacterController ile hareket + yerçekimi; Animator'e `Speed` gönderir |
| `CameraRig.cs` | Yumuşak sinematik takip; duvar arkasına düşmemek için kamerayı öne çeker |
| `HotspotTarget.cs` | Etkileşim noktası (göz = incele, el = al, konuşma); yaklaşınca ipucu açılır |
| `HotspotDetector.cs` | En yakın hotspot'u bulup aktif yapar — aynı anda tek ikon, ekran kalabalıklaşmasın |

Hepsi `Bagisik` namespace'i altında. Unity projesine `Assets/Scripts/` klasörüne kopyala.

**Takıldığın yerde:** `.claude/COZUMLER.md` dosyasına bak — çözülmüş sorunlar orada
birikiyor. Yeni bir sorun çözdüğünde oraya eklemeyi unutma.
