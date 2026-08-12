# Yürüyen Kutu Testi — adım adım Unity rehberi

*BAĞIŞIK* için ilk teknik doğrulama. Tek soruyu cevaplar: **hareket hissi doğru mu
ve telefonda akıcı çalışıyor mu?** Güzel modellere para/zaman harcamadan önce bunu
bilmek gerekir — hareket kötüyse hiçbir model onu kurtarmaz.

**Ortam:** Windows + gerçek Android telefon.
**Süre:** İlk kurulum 1–2 saat (çoğu indirme bekleme), sonrası dakikalar.

---

# ADIM 1 — Unity kurulumu

### 1.1 Unity Hub'ı kur
1. `unity.com/download` → **Download for Windows** → çıkan `UnityHubSetup.exe`'yi çalıştır.
2. Hub açılınca Unity hesabı ister → oluştur/giriş yap.
3. Lisans: **Personal** (ücretsiz). Yıllık geliri belli bir eşiğin altındaki
   herkes kullanabilir — senin durumunda uygun.

### 1.2 Editörü kur
1. Hub → sol menü **Installs** → **Install Editor**.
2. **Official Releases** sekmesinde kal → sürüm numarası **`6000.3.` ile başlayan
   en güncel** olanı seç (= **Unity 6.3 LTS**, Aralık 2027'ye kadar destekli).

   > **"LTS" yazısını göremiyorsan normal** — Hub bu etiketi her satırda
   > göstermiyor. Etikete değil sürüm numarasına bak:
   > `6000.0.x`=6.0 · `6000.1.x`=6.1 · `6000.2.x`=6.2 · `6000.3.x`=6.3.
   > **"Recommended" rozeti** varsa doğrudan onu seç.
   >
   > ⚠️ **`6000.0.x` (Unity 6.0 LTS) seçme** — desteği Ekim 2026'da bitiyor.
3. **Add modules** ekranında şu üçünü işaretle — *sonradan eklemek zahmetli:*
   - ☑ **Android Build Support**
   - ☑ └ **OpenJDK**
   - ☑ └ **Android SDK & NDK Tools**
   - (Documentation'ı da işaretleyebilirsin, offline dokümantasyon verir.)
4. Install → **10–15 GB indirir**, sabırlı ol.

### 1.3 Neden bu seçimler
- **LTS** = Long Term Support: 2 yıl hata düzeltmesi alır. Uzun soluklu proje için şart.
- **Android SDK/NDK** = telefona build almanın önkoşulu. Unity'nin kendi kurduğu
  sürümü kullanmak, elle kurmaktan çok daha az sorun çıkarır.

---

# ADIM 2 — Proje ve script'ler

### 2.1 Projeyi oluştur
1. Hub → **Projects** → **New project**.
2. Sürüm: az önce kurduğun LTS.
3. Şablon: **Universal 3D** *(URP)*.
   > ⚠️ **HDRP seçme** — masaüstü/konsol içindir, mobilde çalışmaz.
   > Düz "3D (Built-in)" de seçme — URP mobil için doğru olan.
4. Proje adı: `Bagisik`. Konum: içinde Türkçe karakter/boşluk olmayan bir yol seç
   (ör. `D:\Projects\Bagisik`) — bazı Android araçları bunlara takılır.
5. **Create project** → ilk açılış birkaç dakika sürer.

### 2.2 Paketleri ekle
**Window → Package Manager** → sol üstte **Unity Registry** seç:

| Paket | Zorunlu mu | Ne için |
|---|---|---|
| **Input System** | ✅ Evet | Dokunmatik joystick |
| **Cinemachine** | Opsiyonel | Sinematik kamera (bizim `CameraRig.cs` alternatifi var) |
| **TextMeshPro** | Genelde hazır gelir | Arayüz metni |

Input System kurulunca Unity **"yeniden başlatılsın mı?"** diye sorar → **Yes**.
Hangi girdi sistemi sorusunda **Both** seçebilirsin (eski + yeni birlikte çalışır).

### 2.3 Script'leri kopyala
1. Unity'de Project panelinde `Assets` klasörüne sağ tık → **Create → Folder** →
   adı: `Scripts`.
2. Bu depodaki `unity/Scripts/` içindeki **4 dosyayı** Windows Gezgini'nden
   `Assets/Scripts/` klasörüne kopyala:
   - `PlayerMover.cs`
   - `CameraRig.cs`
   - `HotspotTarget.cs`
   - `HotspotDetector.cs`
3. Unity'ye geri dön — otomatik derler. **Console'da (Window → General → Console)
   kırmızı hata olmamalı.** Varsa Input System kurulmamış demektir.

---

# ADIM 3 — Sahneyi kur

### 3.1 Zemin
1. Hierarchy → sağ tık → **3D Object → Plane**.
2. Inspector'da: Position `(0, 0, 0)`, Scale `(3, 1, 3)`.
   *(Plane varsayılan 10×10 birimdir; ×3 = 30×30 metre — küçük bir sahne için bol.)*

### 3.2 Duvarlar / engeller
Yürünecek alanı **sınırla** — unutma, bu sınırlı serbest yürüme, açık dünya değil.

1. Hierarchy → sağ tık → **3D Object → Cube**.
2. Bir duvar için: Position `(0, 1.5, 15)`, Scale `(30, 3, 0.5)`.
3. `Ctrl+D` ile çoğaltıp diğer üç kenara koy (`z = -15`, `x = 15`, `x = -15` —
   yanlardakiler için Scale `(0.5, 3, 30)`).
4. Ortaya 2–3 kutu daha at (raf/engel taklidi) — çarpışmayı ve kamerayı test etmek için.

### 3.3 Oyuncu
1. Hierarchy → sağ tık → **3D Object → Capsule**.
2. Adını `Player` yap. Position `(0, 1, 0)`.
   *(Kapsül 2 birim boyunda, pivot ortada → y=1 tam zemine oturur.)*
3. Inspector → **Add Component** ile şunları ekle:
   - **Character Controller**
   - **Player Mover** (bizim script)
   - **Hotspot Detector** (bizim script)
4. Capsule'ün kendi **Capsule Collider**'ını kaldır (sağ tıkla → Remove Component)
   — CharacterController zaten çarpışmayı yönetir, ikisi çakışır.

### 3.4 Kamera
1. Hierarchy'de **Main Camera**'yı seç.
2. **Add Component → Camera Rig**.
3. `Target` alanına Hierarchy'den `Player`'ı sürükle.
4. Varsayılan ofset `(0, 3.2, -4.5)` — sonra hissine göre ayarlayacaksın.

---

# ADIM 4 — Dokunmatik joystick

Bu, kurulumun en kurcalamalı kısmı. Sırayla git.

### 4.1 Input Actions varlığı oluştur
1. Project → `Assets` → sağ tık → **Create → Input Actions**
   *(bazı sürümlerde: Create → Input System → Input Actions)*.
2. Adı: `PlayerControls`. Üzerine **çift tıkla** → düzenleyici penceresi açılır.
3. **Action Maps** sütununda `+` → adı: `Gameplay`.
4. **Actions** sütununda `+` → adı: `Move`.
5. `Move`'u seç, sağdaki Inspector'da:
   - Action Type: **Value**
   - Control Type: **Vector2**

### 4.2 Bağlantıları (binding) ekle
`Move`'un altındaki üçgene tıkla, `<No Binding>` satırını seç:

**Telefon için (joystick bunu taklit eder):**
- Path → arama kutusuna `Left Stick` yaz → **Gamepad → Left Stick** seç.

**Editörde klavyeyle test için (opsiyonel ama çok işe yarar):**
- `Move`'a sağ tık → **Add Up/Down/Left/Right Composite** → W/A/S/D tuşlarını ata.

6. Pencerenin üstündeki **Save Asset** butonuna bas. *(Bunu unutmak en sık hatadır.)*

### 4.3 Ekran joystick'ini kur
1. Hierarchy → sağ tık → **UI → Image**.
   Canvas ve EventSystem otomatik oluşur. Bu Image joystick'in **arka halkası**.
   - Adını `JoystickBase` yap.
   - Rect Transform: sol alt köşeye yerleştir (anchor'ı sol-alt yap),
     boyut `200×200` civarı — parmakla rahat kullanılacak kadar büyük olsun.
2. `JoystickBase`'e sağ tık → **UI → Image** → adı `JoystickHandle`. Bu **sap**.
   - Boyut `90×90`, konum `(0,0)` (base'in ortasında).
3. `JoystickHandle` seçiliyken **Add Component → On-Screen Stick**.
   - **Control Path** alanına tıkla → `Gamepad` → **Left Stick** seç.
     *(4.2'de Move'a bağladığın yolun aynısı olmalı — joystick bu sanal cihazı besler.)*
   - Movement Range: `70` civarı.

### 4.4 Canvas'ı mobile ayarla
`Canvas` nesnesini seç → **Canvas Scaler**:
- UI Scale Mode: **Scale With Screen Size**
- Reference Resolution: `1920 × 1080`
- Match: `0.5`

*Bu olmazsa joystick bazı telefonlarda minicik, bazılarında devasa görünür.*

### 4.5 Script'e bağla
1. `Player`'ı seç → **Player Mover** bileşeni.
2. Project panelinde `PlayerControls` varlığının **yanındaki oku aç** — içindeki
   `Move` action referansı görünür.
3. Onu `Move Action` alanına sürükle.

> **Şimdi editörde test et:** Play'e bas, WASD ile yürü. Çalışıyorsa temel doğru.

---

# ADIM 5 — Telefona build al

### 5.1 Telefonu hazırla
1. Telefon → **Ayarlar → Telefon hakkında** → **Yapı numarası**na **7 kez** dokun.
   "Artık geliştiricisiniz" yazısını göreceksin.
2. Ayarlar → Sistem → **Geliştirici seçenekleri** → **USB hata ayıklama**'yı aç.
3. USB kablosuyla bilgisayara bağla → telefonda çıkan **"Bu bilgisayara izin ver"**
   onayını ver (kutucuğu işaretleyip Her zaman izin ver de).

### 5.2 Platformu değiştir
1. **File → Build Settings** *(bazı sürümlerde Build Profiles)*.
2. Sahneni kaydet (`Ctrl+S`), sonra **Add Open Scenes** ile listeye ekle.
3. Platform listesinden **Android** → **Switch Platform**.
   *(İlk geçiş tüm asset'leri yeniden işler, birkaç dakika sürebilir.)*

### 5.3 Player Settings (mobil için kritik)
Aynı pencerede **Player Settings** → Player → Android sekmesi:

**Other Settings:**
- Scripting Backend: **IL2CPP**
- Target Architectures: ☑ **ARM64** *(Play Store zorunlu kılıyor)*
- Graphics APIs: **Vulkan** üstte, altında GLES3 (yedek)
- Minimum API Level: Android 8.0 (API 26) civarı yeterli

**URP ayarı:** Project → `Assets/Settings` klasöründe URP Asset dosyaları var.
Mobil olanı seç:
- Shadow Distance: `20–30`
- Cascade Count: `1` veya `2`
- MSAA: `2x`
- Ağır post-process ekleme — grade + vignette + hafif grain yeter

### 5.4 Build
1. Build Settings → **Run Device** listesinden telefonunu seç.
   *(Görünmüyorsa: kabloyu çıkar-tak, USB modunu "Dosya aktarımı" yap, Refresh'e bas.)*
2. **Build And Run** → `.apk` konumu sorar, bir klasör seç.
3. **İlk build uzun sürer** (IL2CPP derlemesi, 10–20 dk olabilir). Sonrakiler hızlanır.
4. Bitince oyun telefonunda otomatik açılır.

---

# ADIM 6 — Yürü ve hisset

Bu testin **tek amacı** üç soruyu cevaplamak:

### ✋ Hareket tepkili mi?
Parmağını ittiğinde karakter gecikmeden gidiyor mu? Durdurduğunda ani mi duruyor,
kayıyor mu? Yön değiştirmek rahat mı?
→ Ayar: `PlayerMover` üzerinde **`acceleration`** (büyük = daha keskin) ve
**`turnSpeed`** (büyük = daha hızlı dönüş).

### 🎥 Kamera rahat mı?
Takip yumuşak mı yoksa mide mi bulandırıyor? Duvara yaklaşınca ne oluyor?
Karakteri görebiliyor musun?
→ Ayar: `CameraRig` üzerinde **`offset`** (açı/mesafe) ve
**`followSmoothTime`** (büyük = daha tembel, sinematik).

### ⚡ Akıcı mı?
FPS sabit 30+ mı, tercihen 60? Telefon ısınıyor mu? Pil hızlı mı bitiyor?
→ Ölçmek için: **Window → Analysis → Profiler** (telefon bağlıyken) ya da
sahneye basit bir FPS sayacı koy.

> **En verimli yöntem:** Editörde **Play modundayken** değerleri canlı değiştir,
> hissi bul, **sonra değerleri not al** (Play'den çıkınca sıfırlanır!).
> Doğru değerleri bulunca Play dışında tekrar gir ve kaydet.

---

# ADIM 7 — Sonra: gerçek varlıkları tak

Hareket hissi doğruysa sıra asıl merak ettiğin soruda: **görüntü kalitesi**.

1. **Mixamo** (`mixamo.com`, Adobe hesabıyla ücretsiz):
   - Bir karakter seç → `Idle`, `Walking`, `Running` animasyonlarını indir
   - Format: **FBX for Unity**, Skin: **With Skin** (ilkinde), diğerlerinde
     **Without Skin** yeterli
2. Unity'de model → Inspector → **Rig** sekmesi → Animation Type: **Humanoid** → Apply.
3. **Animator Controller** oluştur (Create → Animator Controller):
   - Parameters'a `Speed` adında **float** ekle
   - Bir **Blend Tree** yap: Idle (0) ↔ Walk (0.5) ↔ Run (1)
   - `PlayerMover` bu parametreyi otomatik besler
4. Kapsülü karakterle değiştir, `PlayerMover`'ın **Animator** alanına bağla.
5. **Asset Store**'dan hastane/market ortam kiti al. Alırken **mutlaka** kontrol et:
   - **Mobil uyumlu mu?** (poly sayısı, texture boyutu — "mobile ready" etiketi ara)
   - **Lisans ticari kullanıma açık mı?**
6. Baked lighting (Window → Rendering → Lighting → Generate Lighting) + sis +
   post-process ekle.

Bu adım bitince *"oyunumuz böyle görünecek"* sorusunun **somut cevabı** elinde olur.

---

## Script referansı

| Dosya | Ne yapar |
|---|---|
| `PlayerMover.cs` | Joystick girdisini kameraya göre dünya yönüne çevirir; CharacterController ile hareket + yerçekimi; Animator'e `Speed` gönderir |
| `CameraRig.cs` | Yumuşak sinematik takip; duvar araya girerse kamerayı öne çeker |
| `HotspotTarget.cs` | Etkileşim noktası (göz = incele, el = al, konuşma); yaklaşınca ipucu açılır |
| `HotspotDetector.cs` | En yakın hotspot'u aktif yapar — aynı anda tek ikon; pil için zamanlı tarar |

Hepsi `Bagisik` namespace'i altında.

## Sık karşılaşılan sorunlar

| Belirti | Sebep / çözüm |
|---|---|
| Console'da `InputSystem` hatası | Input System paketi kurulmamış → Package Manager'dan kur, Unity'yi yeniden başlat |
| Joystick çalışmıyor | `Save Asset`'e basılmamış, ya da On-Screen Stick'in Control Path'i Move'un binding'iyle aynı değil |
| Karakter yere gömülüyor / uçuyor | Capsule pozisyonu `y=1` olmalı; CharacterController'ın Center/Height değerlerine bak |
| Karakter hiç hareket etmiyor | `Move Action` alanı boş — `PlayerControls` içindeki `Move` referansını sürükle |
| Telefon Run Device'ta görünmüyor | USB hata ayıklama kapalı, kablo veri taşımıyor, ya da onay verilmemiş |
| Build "SDK bulunamadı" diyor | Android SDK/NDK modülleri kurulmamış → Hub → Installs → dişli → Add modules |

**Yeni bir sorun çözdüğünde** `.claude/COZUMLER.md` dosyasına ekle — proje hafızası
orada birikiyor, aynı sorunu iki kez çözmeyelim.
