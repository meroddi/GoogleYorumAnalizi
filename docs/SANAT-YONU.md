# BAĞIŞIK — Sanat Yönetimi Anayasası

> **Bu belge nasıl üretildi:** dört bağımsız araştırma merceği (URP mobil
> look-dev · bütçeler ve varlık tedariki · sinematik kamera · görsel bütünlük)
> → sanat yönetmeni sentezi → **şüpheci teknik sanatçı denetimi**.
> Denetim 13 gerçek hata buldu; hepsi aşağıdaki **Düzeltmeler** bölümünde.
>
> **Okuma sırası:** önce Düzeltmeler, sonra ana metin. Çelişki olursa
> **Düzeltmeler kazanır** — ana metin araştırma anındaki hâliyle duruyor ki
> gerekçe kaybolmasın.

---

## ⚠️ DÜZELTMELER — ana metni geçersiz kılar

Denetimin özeti:

> Bu belge, tek kişilik bir mobil projede gördüğüm sanat yönetimi dokümanlarının üst %5'inde: palet hex listesi, texel yoğunluğu hedefleri, Cinemachine 3 isim haritası, ASTC blok matematiği (128 bit/blok hesapları doğru), Opaque Texture'ın mobilde MSAA'yı sessizce yok saymasi, Depth Priming'in tile GPU'da kayıp olması, ACES'in doygun kırmızıyı turuncuya kaydırması, Shadowmask vs Distance Shadowmask ayrımı, "kit'in %60-80'ini sil", "tek pack = tek oda yapma", greyscale/thumbnail/flip testleri — bunların hepsi doğru, güncel ve uygulanabilir. §4 (kamera) ve §5 (materyal uyumu) gerçekten yüksek değerli ve büyük ölçüde kusursuz; §9 ve §12 bir solo projede nadiren görülen bir disiplin gösteriyor. AMA belgenin kendi "kilit taşı" dediği şey — Unity 6.5 on-tile post-processing'in her şeyi bedavaya getirmesi — belgenin geri kalanıyla teknik olarak çelişiyor ve bu çelişki hiçbir yerde çözülmemiş: on-tile PP, renderer üzerindeki entegre post-processing'in KAPALI olmasını ve render'ın tile'da kalmasını şart koşuyor; Bloom, Gaussian DoF, FXAA, STP ve Lens Flare (SRP) ise tam olarak o kapalı olması gereken entegre post yığınında yaşıyor. Yani §6'nın "bütçelenmiş ekstraları" ve §7'nin Tier A/Tier B tablosunun ikisi de, §6'nın kilit taşıyla aynı anda var olamayabilir. İkinci ciddi sorun: 30 fps kilidi `vSyncCount=0 + targetFrameRate=30` ile yazılmış, ki bu Android'de tam olarak judder üreten kombinasyon — "filmik" iddiasındaki bir oyunda ilk günden görünür bir hata. Üçüncüsü: 5 lokasyon × 250 MB resident doku ile <200 MB AAB base module aritmetik olarak imkânsız, ama Addressables/Play Asset Delivery §10'a "sonra bak" diye ertelenmiş — sonradan Addressables'a geçmek aylık bir refactor. Dördüncüsü: hue-seçici desatürasyon LUT'u, sıcak 3200K key ışığı ve ten tonlarıyla aynı hue bandında savaşıyor. Bunların hepsi düzeltilebilir, hiçbiri belgenin omurgasını yıkmıyor — ama §6, §7 ve §11'in ilk haftası bu haliyle inşa edilirse geliştirici, kendi belgesinin birbiriyle çelişen iki yarısını haftalarca debug eder. Kısa cevap: içerik olarak evet, üzerine inşa edilebilir; ama önce §6/§7 çelişkisi çözülmeli ve §11 Hafta 1'e "on-tile PP + Bloom/DoF/STP/FXAA birlikte çalışıyor mu" testi konmalı — o test bu belgenin en pahalı varsayımıdır ve şu anda test olarak değil, kesinlik olarak yazılmış.

---
### D1. §6: 'On-tile PP kilit taşıdır' + aynı bölümde 'Bloom lokasyon Volume'ünde açılır', 'Gaussian DoF diyalogda açılır', 'Lens Flare (SRP) kullan' + §7: Tier A = STP, Tier B = MSAA 2x + FXAA.

**Sorun:** Unity'nin kendi kurulum adımları on-tile PP için renderer üzerindeki entegre Post-processing'in KAPALI olmasını şart koşuyor ve 'render tile dışına çıkarsa on-tile PP açıkken derleme hatası alırsın' diyor. Bloom, Depth of Field, FXAA, STP ve Lens Flare (SRP) tam olarak o kapatılan entegre post yığınının parçası. Yani bunlar bir Volume override'ıyla 'bilinçli açılıp kapanan' şeyler değil; renderer seviyesinde ya hep ya hiç. Belge bunu 'tile yolunu kırar' diye yazıyor ama gerçekte muhtemelen hiç render edilmeyecekler. Ayrıca STP implicit olarak TAA'yı açıyor ve TAA ile MSAA birlikte kullanılamıyor — Tier B'nin MSAA 2x + FXAA'sı hem on-tile yoluyla hem de belgenin kendi §10/#4 notuyla ('MSAA sadece backbuffer'da destekleniyor, intermediate'te değil') çelişiyor; render scale 0.80 zaten intermediate texture zorluyor.

**Yerine:** §6'nın başına şunu ekle: '⚠️ MİMARİ ÇATIŞMA — HAFTA 1'DE ÇÖZÜLECEK. On-tile PP, URP Renderer üzerindeki entegre Post-processing kutusunun KAPALI olmasını gerektirir. Bloom, Depth of Field, FXAA, STP ve Lens Flare (SRP) o kutunun içinde yaşar. Bu ikisi aynı renderer'da bir arada olmayabilir. Hafta 1'de şu üç yapılandırmayı floor cihazda ölç ve BİRİNİ seç, sonra belgeyi ona göre yeniden yaz:

A) SAF ON-TILE (önerilen varsayılan): entegre post kapalı, sadece on-tile grade (tonemap + LUT + SMH + vignette + grain). Bloom YOK, DoF YOK, STP YOK, FXAA YOK. Render scale 0.85-0.9 + MSAA 2x backbuffer'da. Bloom'un yaptığı işi materyal emissive + mesh ışık şaftı + Lens Flare mesh quad'ları ile sahte yap; DoF'un yaptığı işi sisle ve arka planı fiziksel olarak uzağa koyarak yap. Bu yol belgenin geri kalanıyla en uyumlu olan.

B) KLASİK POST: entegre post açık, on-tile yok. Bloom + Gaussian DoF + STP/FXAA çalışır, ama grade artık bedava değil — 1080p'de tam ekran bir resolve + uber pass ödersin. Bütçe: 1.5-3 ms.

C) İKİ RENDERER: iki ayrı URP Renderer asset'i (biri on-tile, biri klasik post) ve sahne/kamera bazında geçiş. Karmaşıklık gerçek, sahne yükleme anında pipeline switch hitch'i riski var; tek kişi için son çare.

Karar verilene kadar §7'deki Tier A/Tier B tablosu GEÇERSİZDİR.'

---
### D2. §7: `QualitySettings.vSyncCount = 0; Application.targetFrameRate = 30;` — '30 fps kilitlidir, tartışma yok.'

**Sorun:** Android'de bu tam olarak judder üreten kombinasyon. Unity'nin kendi Scripting API dokümantasyonu 'vSyncCount = 0 ve targetFrameRate kullanmak tamamen stutter'sız çıktı üretmez; düzgün frame pacing gerektiğinde her zaman vSyncCount > 0 kullanın' diyor. 120 Hz'lik bir telefonda vsync kapalı 30 fps, kareleri 4'lü refresh aralıklarına düzensiz dağıtır ve yavaş sinematik kamera hareketlerinde — yani bu oyunun en çok baktığı şeyde — gözle görülür bir titreme olur. Ayrıca Android'in Optimized Frame Pacing özelliğinin kendi tuzağı var: sadece refresh rate bölenlerine kilitliyor ve düşük seviye cihazlarda stutter raporları var; ayrıca Optimized Frame Pacing açıkken Application.targetFrameRate'in çalışmadığına dair Unity issue tracker kaydı mevcut.

**Yerine:** §7'deki kod bloğunu değiştir ve altına şu notu koy: '30 fps'i vsync KAPALI kilitleme. Üç seçeneği floor cihazda, yavaş bir kamera kaydırmasıyla (dolly) yan yana test et ve seç:
(a) Player Settings > Android > Optimized Frame Pacing AÇIK + targetFrameRate = 30 (Unity'nin resmî yolu; ama Optimized Frame Pacing ile targetFrameRate çakışması bilinen bir issue — cihazda doğrula),
(b) 60 Hz cihazda `vSyncCount = 2` (yani refresh/2 = 30) ve targetFrameRate = -1,
(c) `Screen.SetResolution` yerine Android'in `Display.systemPreferredDisplayMode` / refresh rate API'siyle ekranı 60 Hz'e sabitleyip vSyncCount = 2.
Test metodu: 5 saniyelik sabit hızlı yatay bir kamera dolly'si çek, telefonda izle. Judder varsa yanlış seçenektesin. Bu, oyunun "filmik mi görünüyor" testinin ilk adımıdır ve grade'den önce gelir.'

---
### D3. §7 ve §12: 'Vulkan-only, GLES3'ü kaldır, min API 30. Aktif Android cihazların ~%85+'ı destekliyor.'

**Sorun:** %85 rakamı doğru (2026 itibarıyla aktif Android cihazların ~%85'i Vulkan destekliyor), ama sonuç yanlış. Google'ın kendi VkQuality Unity eklentisi tam olarak bunun için var: 'Vulkan'ın performans faydasını alırken Vulkan kullanımını yeni sürücülü yeni cihazlarla sınırlayıp oyunun sürücü hatalarına maruziyetini azaltmak.' VkQuality'nin çalışması için projede HEM GLES HEM Vulkan renderer'ının açık olması gerekiyor. Belge floor cihaz olarak Adreno 610 / Snapdragon 680 sınıfını seçiyor — yani tam olarak Vulkan sürücü kalitesinin şüpheli olduğu sınıf. GLES3'ü kaldırmak, sürücü hatası çıktığında elindeki tek kaçış kapısını kapatmak demek. Ayrıca Türkiye pazarı düşük-orta segment ağırlıklı; %15 kayıp burada %15'ten büyük olabilir.

**Yerine:** §7'deki 'Graphics APIs: sadece Vulkan' satırını şununla değiştir: 'Graphics APIs: Vulkan ilk sırada, OpenGLES3 ikinci sırada BIRAK — ve Google'ın VkQuality Unity eklentisini (developer.android.com/games/engines/unity/unity-vkquality) entegre et. VkQuality açılışta cihaza bakıp Vulkan mı GLES mi kullanılacağını öneriyor ve bilinen bozuk sürücülü cihazları GLES'e düşürüyor; çalışması için iki API'nin de projede açık olması şart.
⚠️ ÖDÜNLEŞİM: GLES3'ü açık bırakmak on-tile PP, STP ve APV per-pixel için ikinci bir kod yolu demek — yani shader variant sayısı ve build boyutu artar, ve GLES yolunda grade ON-TILE OLMAZ. Bu yüzden GLES tier'ı bilinçli olarak SOYULMUŞ bir Tier C'dir: grade klasik post ile uygulanır, SSAO/decal/DoF kapalı. Tier C'yi Hafta 1'de değil, analytics gerçek GLES oyuncu gösterdiğinde inşa et — AMA GLES3 kutusunu ilk günden açık bırak ki o gün geldiğinde build'i yeniden mimarlamak zorunda kalma.'

---
### D4. §2, adım 2: 'Hue-selective desaturation LUT'a gömülür: hue 350°-15° bandı %100'de kalır, geri kalan her hue %50-65 düşer. Artık dünya doygunluğunu kaybederken kapüşon kaybetmiyor — bedava ve her kared

**Sorun:** LUT hue ile çalışır, nesne ile değil. §3'ün kendi reçeteleri sahneleri 3200K sıcak kehribar key ışığı, 2000-2200K sodyum lambası ve #E8A24C / #FFA24A practical'larla aydınlatıyor. Bu ışıklar sahnedeki HER yüzeyin hue'sunu turuncu-kırmızıya doğru itiyor. Ten tonu da doğal olarak 20-40° bandında ve sıcak key altında 15°'nin altına iniyor. Yani 350-15° bandını korumak: kapüşonu değil, aynı zamanda sıcak ışık alan tüm duvarları, kirli beji, pası, kanı, ateşi ve karakterlerin yüzlerini koruyacak. Sonuç, belgenin §1'de reddettiği şeyin ta kendisi olur: kontrastı ışık değil post-process yapan, 'filtre uygulanmış' görünen bir kare. Belge zaten adım 1'de (palet polisliği) ve adım 4'te (_DesatExempt float property) bu işi materyal seviyesinde ve nesne bazında doğru şekilde çözmüş — LUT hilesi o iki doğru çözümle rekabet ediyor ve onları bozuyor.

**Yerine:** §2, adım 2'yi şununla değiştir: '2. Hue-seçici desatürasyonu LUT'a GÖMME. LUT hue ile çalışır, nesne ile değil; §3'ün sıcak 3200K key'i ve 2000K sodyumu altında 350-15° bandı sadece kapüşonu değil, ten tonunu, pası, kanı, ateşi ve sıcak ışık alan her duvarı da korur. Sonuç "tek kırmızı" değil, "turuncu filtre" olur.
Doğru araç zaten elinde: doygunluk düşüşü LUT'ta GLOBAL ve ayrımsız uygulanır (§6 tablosundaki Saturation −28 + LUT), kapüşon ise master shader'daki `_DesatExempt` float'ıyla materyal seviyesinde muaf tutulur. Bu, hue'ya değil NESNEYE bağlı bir muafiyettir ve tam olarak istediğin şey budur.
LUT'ta yapılabilecek tek meşru hue işi şudur: 350-15° bandını KORUMAK değil, 30-60° (sarı-yeşilimsi sarı) ve 160-200° (cyan) bantlarını EK OLARAK düşürmek — yani kapüşonla yarışabilecek rakip doygunlukları kesmek. Kırmızıya hiç dokunma.'

---
### D5. §4: 'Kamerayı geometrinin dışına koy, duvarın içinden çek. Ya (a) kamera tarafındaki duvarın mesh'ini sil, ya da (b) duvarı o kameranın Culling Mask'ından hariç bir layer'a al.'

**Sorun:** İkisi de o duvarın gölge ve bounce katkısını da yok ediyor. Duvarı silersen bake'te oda o taraftan açık kalır: lightmap'te sahte bir ışık sızıntısı, reflection probe'da yanlış bir çevre, ve APV'de o duvarın arkasından gelen ışık. Culling Mask'tan çıkarmak daha az yıkıcı ama Culling Mask kameradan çıkarılan nesnenin gölge atmasını da engeller (URP'de culling mask'tan çıkan renderer o kamera için gölge de üretmez), yani odaya giren key ışığının o duvardaki penceresinden gelen gölge deseni kaybolur — ki §3 reçetesinin tamamı o desene dayanıyor.

**Yerine:** §4'teki 'Mesafe ve yükseklik' bölümünde duvar çözümünü şununla değiştir: 'Duvarı SİLME ve Culling Mask'tan ÇIKARMA. Doğru çözüm: duvarı yerinde tut, Static işaretli tut (lightmap, occlusion culling ve reflection probe onu görsün), ve sadece o kamera aktifken duvarın MeshRenderer'ının `shadowCastingMode`'unu `ShadowsOnly` yap. Böylece duvar görünmez olur ama gölgesini atmaya, ışığı engellemeye ve bake'e katkı vermeye devam eder.
Uygulama: `CameraZone` script'ine bir `Renderer[] hideForThisShot` alanı ekle; girişte `r.shadowCastingMode = ShadowCastingMode.ShadowsOnly`, çıkışta `= ShadowCastingMode.On`. Beş satır. Occlusion culling bake'i etkilenmez çünkü mesh sahnede duruyor.
Not: bu, bake edilmiş bir sahnede duvarı silmenin ışık sızıntısı yaratmasının da tek doğru çözümü; APV Min Probe Spacing 1 m'de silinen bir duvarın arkasından gelen sızıntı özellikle görünür olur.'

---
### D6. §5: 'Özellikleri keyword ile değil float property ile yap. Keyword = yeni shader variant = bölünmüş SRP Batcher batch'i = patlayan SetPass sayısı.' + `BAGISIK_Env_Lit`'te 8 açık parametre (Grime, Deta

**Sorun:** Yarı doğru, ama mobilde tehlikeli tarafı atlanmış. Keyword'ler batch'i sadece FARKLI keyword durumundaki materyaller arasında böler; projedeki tüm çevre materyalleri aynı keyword setini kullanıyorsa bölünme olmaz. Buna karşılık float ile lerp'lenen her özellik, o özelliği KULLANMAYAN her pikselde de ALU ve — daha kötüsü — texture sample'ı ödetir. World-space Grime (muhtemelen triplanar, 1-3 sample) + Detail Normal (1 sample) + base albedo/normal/mask (3 sample) = piksel başına 5-7 texture fetch, oyundaki HER opak piksel için, ortalama 2.0x overdraw ile. Belgenin §7 bütçe tablosunda SSAO, Bloom, DoF, Copy Depth için ms rakamı var ama master shader'ın kendisi için SIFIR bütçe var — hâlbuki tile GPU'da GPU süresinin en büyük tek kalemi opak pass'in fragment maliyeti olacak.

**Yerine:** §5'e şu kutuyu ekle: '⚠️ MASTER SHADER'IN KENDİ BÜTÇESİ VAR. §7'de SSAO'ya 0.8-1.5 ms ayırdın ama `BAGISIK_Env_Lit`'e sıfır ayırdın. Oysa oyundaki her opak piksel bu shader'dan geçiyor.
Bütçe: `BAGISIK_Env_Lit` fragment maliyeti, URP/Lit baseline'ının en fazla 1.6 katı olacak. Ölçme yöntemi: LookDev sahnesinde tek bir tam ekran quad'ı önce URP/Lit ile, sonra master shader ile render et, floor cihazda GPU süresi farkını al.
Kurallar:
- Piksel başına toplam texture sample ≤ 5 (albedo, normal, mask, detail normal, grime). Triplanar grime YASAK (3 sample) — grime'ı tek eksenli (dünya Y) projeksiyonla + vertex color maskesiyle yap, ya da doğrudan mask texture'ının A kanalına bake et (zaten §5'te A = detay/grime demişsin — orada tut, ayrı sample alma).
- Sadece iki özellik `shader_feature_local` olsun: `_GRIME_ON` ve `_DETAILNORMAL_ON`. Bu 4 variant demek; SRP Batcher için sorun değil (batch'ler keyword'e göre değil shader variant'ına göre gruplanır ve sen zaten 25-40 materyalle çalışıyorsun). Arka plan / uzak prop'lar ikisini de kapalı kullanır ve bedavaya gelir.
- Geri kalan her şey (AlbedoTint, Desat, ValueClamp, SmoothnessRemap, NormalStrength, DesatExempt) float kalsın — bunlar saf ALU, texture sample değil.
- `GlobalWear` Shader Graph'ta hazır bir "global property" olarak YOK. Bir Custom Function node'u içinde `float _BAGISIK_GlobalWear;` global'ini elle deklare edip `Shader.SetGlobalFloat("_BAGISIK_GlobalWear", x)` ile sür. Bunu Hafta 2'de kur, sonradan eklemek 40 materyali yeniden dokunmak demek.'

---
### D7. §7: sahne başına üçgen hedefi 150.000, ekranda skinned karakter ≤3 (yumuşak tavan 4). §5: Hero LOD0 25.000-35.000 üçgen, ekranda eşzamanlı ≤4. §7/LOD: 'İç mekân için LOD yapma — 12 m odada hiçbir şey

**Sorun:** Üç sayı birbiriyle çelişiyor. 4 × 35.000 = 140.000 üçgen — yani 150.000'lik sahne bütçesinin %93'ü sadece karakterler. Geriye market reyonları, duvarlar, prop'lar için 10.000 üçgen kalıyor ki bu imkânsız. İki kişilik bir diyalog sahnesi bile 70.000 üçgen = bütçenin yarısı. Ayrıca 'iç mekânda LOD yapma' kuralı tam olarak LOD'a en çok ihtiyaç duyulan yeri kapsam dışı bırakıyor: karakterler. §4'ün kendi tablosuna göre karakter 1.2 m'den (yakın plan) 7 m'ye (genel plan) kadar geziyor — 7 m'de bir karakterin ekranda kapladığı yükseklik %50, yani 35.000 üçgenin yarısı bile gereksiz.

**Yerine:** §5 ve §7'deki karakter sayılarını şöyle uyumlaştır: 'Hero LOD0 = 22.000-28.000 üçgen (yakın plan, kamera <2.5 m). Hero LOD1 = %45 (≈11.000), 2.5-6 m. Hero LOD2 = %20 (≈5.000), >6 m ve arka plandaki NPC'ler. NPC LOD0 = 10.000-14.000.
⚠️ §7'deki "iç mekân için LOD yapma" kuralı SADECE ÇEVRE GEOMETRİSİ içindir. KARAKTERLERDE LOD ZORUNLUDUR — §4'ün kamera mesafesi tablosu karakteri 1.2 m ile 7 m arasında gezdiriyor, bu tam olarak LOD'un işe yaradığı aralık. CC5'in Unity Auto Setup'ı LOD dağıtımını zaten otomatik yapıyor; kullan.
Yeni sahne bütçesi (culling sonrası, 150.000 hedefinde): karakterler ≤60.000 (2 hero yakın planda ya da 1 hero + 3 uzak NPC), çevre ≤90.000. Diyalog sahnelerinde ekranda 2 karakter, oynanışta ≤3. "4" rakamını belgeden tamamen kaldır.'

---
### D8. §7: 'Resident doku belleği 250 MB' (lokasyon başına), 5 lokasyon + karakterler; ve 'AAB base module <200 MB — Play limiti'. §10/#18: 'Play Console boyut limitleri için güncel dokümantasyona bak.'

**Sorun:** Bu aritmetik tutmuyor ve belge bunu 'sonra bakılacak' kutusuna atmış. Google Play'in AAB base module sıkıştırılmış indirme limiti 200 MB. Beş lokasyon × ~250 MB resident doku + karakterler + animasyon + ses, sıkıştırılmış olarak bile 200 MB'ın çok üstünde. Yani bu oyun ZORUNLU olarak Play Asset Delivery (ya da Addressables + PAD) kullanacak. Sorun şu: Addressables'a sonradan geçmek, tüm prefab referanslarını, sahne yükleme mimarisini ve materyal/shader referanslarını yeniden düzenlemek demek — tek kişi için haftalar. Ve §10'daki 'Volume Profile pre-warm' notu, Addressables ile birlikte tamamen farklı bir problem haline gelir.

**Yerine:** §11 Hafta 1'e yeni bir madde ekle ve §7'ye şu kutuyu koy: 'İÇERİK MİMARİSİ — HAFTA 1'DE KURULUR, SONRA DEĞİL.
Aritmetik: 5 lokasyon × ~250 MB resident doku + kadro + animasyon, Play'in 200 MB'lık AAB base module limitine sığmaz. Bu bir "ileride optimize ederiz" sorunu değil, bir mimari karardır ve sonradan alınamaz.
Hafta 1'de kur:
- Addressables paketi, her lokasyon = bir Addressable grup.
- Play Asset Delivery entegrasyonu (Unity'nin Android > Play Asset Delivery desteği), lokasyon grupları `install-time` değil `fast-follow` ya da `on-demand`.
- Base module'de KALACAKLAR: boot sahnesi, UI, font/TMP atlası, master shader'lar + shader variant collection, LUT'lar, oyuncu karakteri. Hedef: <120 MB.
- Her lokasyon paketi hedefi: <180 MB sıkıştırılmış.
Bu kurulum boş bir projede yarım gün; 3 lokasyon inşa edildikten sonra iki hafta.'

---
### D9. §3: 'Directional Mode: Directional. Lightmap belleğini ikiye katlar; senin sahne boyutlarında bu sorun değil.' + Lighting Mode: Shadowmask.

**Sorun:** İkiye değil, üçe katlıyor. Directional lightmap ikinci bir texture (yön verisi) ekliyor; Shadowmask ise ÜÇÜNCÜ bir texture (4 kanallı gölge maskesi) ekliyor. Yani atlas seti başına 3 texture. 24 texel/birim + 2048 atlas + 3 texture, belgenin kendi 250 MB doku bütçesinden ciddi bir pay yer ve bu pay §7'nin '100 eşsiz 2K map' hesabında hiç görünmüyor. Ayrıca APV + Shadowmask kombinasyonunun kalitesi topluluk raporlarında tartışmalı — APV shadowmask modunda bake edebiliyor ama dinamik nesnelerdeki gölge maskesi interpolasyonu sorunlu bulunmuş.

**Yerine:** §3'teki Directional notunu düzelt: 'Directional Mode: Directional + Lighting Mode: Shadowmask = atlas seti başına ÜÇ texture (renk + yön + gölge maskesi), iki değil. §7'deki 250 MB doku bütçesine lokasyon başına 15-30 MB lightmap payı ayır ve bunu "100 eşsiz 2K map" hesabından DÜŞ.
Ayrıca Hafta 1 doğrulama listesine ekle: APV + Shadowmask birlikte, dinamik karakter üzerinde. APV'nin shadowmask okuması bazı sürümlerde dinamik nesnelerde bozuk/interpolasyonu kaba bulunmuş. Eğer karakter, statik bir prop'un bake gölgesine girdiğinde gölgeye girmiyorsa (ya da kabaca giriyorsa), APV yerine o sahnede klasik Light Probe Group'a düş — belgede APV'yi zaman tasarrufu diye seçtin, ama yanlış gölgelenen bir karakter zaman tasarrufundan pahalıdır.'

---
### D10. §5: 'Mixamo'yu şunun için kullan: auto-rigging (ücretsiz, ticari kullanım serbest) ve temel locomotion kütüphanesi.' + §7: 'İlk günden ücretsiz ve projede olmalı: Mixamo (rig + locomotion baseline).'

**Sorun:** Mixamo 2026 itibarıyla ayakta ama bakımsız ve güvenilmez. Adobe 2015'ten beri anlamlı bir güncelleme yapmadı, Fuse 2020'de kaldırıldı, ve servis Haziran 2025'ten beri çözülmemiş bir backend authentication arızası yüzünden aralıklı olarak bozuk; Adobe destek kanallarında 'artık desteklenmiyor' olarak tanımlanmış. Belge Mixamo'yu iki kritik yola koyuyor: auto-rig ve tüm locomotion baseline'ı. Bu, çok yıllık bir projenin kritik yolunda, kapanabilecek ve zaten aralıklı çalışan ücretsiz bir servis demek.

**Yerine:** §5'teki 'Karakter kaynağı' bölümüne ekle: '⚠️ MIXAMO BİR TEDARİK RİSKİ. Servis ayakta ama Adobe 2015'ten beri güncellemedi, Fuse 2020'de kaldırıldı ve Haziran 2025'ten beri aralıklı auth arızaları var. Bugün BU HAFTA yap:
1. İhtiyacın olabilecek TÜM locomotion ve idle klipleri (yürüme, koşma, dönüş, dur-kalk, taşıma, tırmanma, tepki) indir ve repoya/yedeğe koy. Bir daha siteye girmek zorunda kalma.
2. Auto-rig için yedeğini kur: Reallusion AccuRig (ücretsiz, masaüstü, CC5 pipeline'ıyla zaten uyumlu) ya da Blender'ın Rigify'ı. CC5 alacaksan zaten kendi iskeletini üretiyorsun — Mixamo auto-rig'e sadece dış kaynak modeller için ihtiyacın olur.
3. §11 Hafta 3-6 dikey diliminin Mixamo'ya bağımlılığını, kliplerin yerel yedeğine bağımlılık olarak yeniden yaz.'

---
### D11. §5 ve §7: 'Reallusion Character Creator 5. Perpetual ~$299... Headshot 3 (2026) fotoğraftan riglenmiş karakter üretiyor.' Toplam bütçe ~$850-1.600.

**Sorun:** $299 perpetual CC5 doğru, ama Headshot o fiyata DAHİL DEĞİL — Headshot Plugin ayrı bir eklenti ve CC 365 Pro aboneliğinde paketleniyor (CC5 Deluxe + Headshot + SkinGen Premium). Belge Headshot'ı 'kadroya birbirinden farklı yüzler vermenin meşru yolu' diye sunuyor, yani planın kadro çeşitliliği ayağı ödenmemiş bir eklentiye dayanıyor. Ayrıca Reallusion Ocak 2026'da iClone ve Character Creator lisanslamasını değiştirdi — belgedeki fiyat/paket yapısı bu tarihten önceki bilgiye dayanıyor olabilir.

**Yerine:** §7'deki satın alma listesini düzelt: 'Character Creator 5 perpetual $299 — AMA Headshot eklentisi bu fiyata dahil değil, ayrı satılıyor ya da CC 365 Pro aboneliğinde (CC5 Deluxe + Headshot + SkinGen Premium) geliyor. Reallusion Ocak 2026'da lisanslamayı değiştirdi; satın almadan önce reallusion.com/plan-and-pricing üzerinden GÜNCEL paketi kontrol et.
Karar noktası: kadroda kaç farklı yüz var? 4-5 karakter için Headshot'a gerek yok — CC5'in kendi morph kütüphanesi ve SkinGen ile yeterince farklılaştırabilirsin. 8+ karakter varsa Headshot'ın maliyeti (ya da bir yıllık CC 365) haklı çıkar. Bütçe satırını "$299 (+ Headshot / CC 365 gerekirse $100-200 daha)" olarak güncelle ve toplam tahmini $850-1.600 yerine $950-1.900 yap.'

---
### D12. §6: 'SSAO Tier A'da AÇIK... Method Interleaved Gradient, Sample Count 4, Downsample AÇIK, Bütçe ~0.8-1.5 ms.'

**Sorun:** URP'nin SSAO Renderer Feature'ının varsayılan kaynağı 'Depth Normals' ve bu, ayrı bir DepthNormals prepass'i zorluyor — yani tüm opak geometriyi ikinci kez çizmek. Tile GPU'da bu, SSAO'nun kendi fragment maliyetinden pahalı olabilir ve belgenin 0.8-1.5 ms tahmininde hiç yok. Belge, decal ayarlarında DepthNormals prepass tehlikesini doğru şekilde uyarıyor (Use Rendering Layers = KAPALI), ama SSAO'da aynı tuzağı fark etmemiş. Ayrıca belge zaten 'Copy Depth Mode = After Opaques' diyor — yani depth zaten kopyalanıyor, prepass'e ihtiyaç yok.

**Yerine:** §6'daki SSAO konfigürasyonuna ekle: 'Source = **Depth** (Depth Normals DEĞİL). Depth Normals kaynağı ayrı bir DepthNormals prepass'i zorlar — tüm opak geometriyi ikinci kez çizmek demektir ve tile GPU'da SSAO'nun kendi maliyetinden pahalıya gelebilir. Depth kaynağı normalleri depth'ten yeniden inşa eder; kalite biraz düşer ama sen zaten yarı çözünürlükte, 4 sample ile, sisin ve grade'in altında çalışıyorsun — fark görünmez.
Doğrulama: Render Graph Viewer'da SSAO açıkken bir DepthNormals pass'i belirdiyse ayar yanlış.'

---
### D13. §4: '2.39:1 letterbox diyalog ve ara sahnelerde. Ama UI çubuğuyla değil — kameranın Viewport Rect'iyle yap. Böylece render edilen alan gerçekten küçülür ve DoF + bloom'da gerçek GPU tasarrufu elde ede

**Sorun:** İki sorun. Birincisi, DoF ve Bloom'un bu planda çalışıp çalışmayacağı zaten belirsiz (bkz. on-tile çelişkisi) — yani gerekçenin yarısı havada. İkincisi ve daha önemlisi: kameranın Viewport Rect'ini 1'den küçük yapmak URP'de intermediate texture'a render'ı zorlayabilir ve tam olarak korumaya çalıştığın on-tile / backbuffer yolunu kırabilir. Yani 'bedava kazanım' diye yazılan şey, belgenin en pahalı varsayımını sessizce iptal edebilir.

**Yerine:** §4'teki letterbox notunu şununla değiştir: 'Letterbox'ı Viewport Rect ile yapmadan ÖNCE Render Graph Viewer'da cihazda doğrula: Viewport Rect < 1 olduğunda intermediate texture beliriyor mu, on-tile yol hâlâ aktif mi? Belirmiyorsa Viewport Rect kullan.
Beliriyorsa letterbox'ı Viewport Rect ile değil, kameranın önüne parentlanmış iki opak siyah quad ile ya da bir UI overlay ile yap. Bu durumda GPU tasarrufu olmaz — ama letterbox zaten kompozisyon aracı, optimizasyon aracı değil. Gerekçeyi "kadraj" olarak yaz, "bedava GPU" olarak değil.'

---
## ➕ EKSİKLER — ana metne eklenecek
### E1. Belge "bounded free walking" oyunu için tam bir görsel yönetim sistemi kuruyor ama karakterin nasıl yürüdüğüne dair tek satır yok: root motion mu, CharacterController mı, joystick büyüklüğü animasyon hızına nasıl bağlanıyor, kitbash geometriyle çarpışma nasıl kuruluyor. §5 "ayak kayması bir numaralı amatör tell'i, root motion kullan" diyor ve orada bırakıyor — oysa root motion ile analog bir sanal joystick birbirine düşman: root motion sabit hızlı klipler üretir, joystick 0-1 arası sürekli bir büyüklük verir.

§5'e yeni alt bölüm: 'HAREKET SİSTEMİ — ROOT MOTION VE JOYSTICK'İ BARIŞTIRMAK
Problem: root motion ayak kaymasını çözer ama sabit klip hızı verir; sanal joystick 0-1 arası sürekli büyüklük verir. İkisini ham haliyle birleştirirsen ya ayak kayar ya da giriş sünger gibi hisseder.
Çözüm (tek doğru mimari):
1. 1D Blend Tree: Idle → Walk → Fast Walk. Bu oyunda KOŞU YOK — Telltale temposu, kapalı mekân, ve koşu klibi eklemek üç ayrı ayak-kayması problemi demek.
2. Animator'da Apply Root Motion AÇIK. Blend Tree'nin parametresi joystick büyüklüğü DEĞİL, joystick büyüklüğünden `SmoothDamp` ile türetilmiş bir `Speed` float'ı (smoothTime 0.12-0.18 s). Ani parmak hareketi ani klip geçişi yapmasın.
3. Kliplerin gerçek root hızlarını Blender/Unity'de ÖLÇ ve Blend Tree eşiklerini o gerçek hızlara koy (örn. Walk = 1.35 m/s ise threshold 1.35). Tahmin etme — bu tek adım ayak kaymasının %80'ini çözer.
4. Dönüş: 90° üstü dönüşlerde yerinde-dönüş (turn-in-place) klibi; altında root motion'ın kendi dönüşü + maksimum 220°/s rotasyon hızı sınırı. Sabit kamerada ani 180° dönüş, §4'ün girdi mandallama kuralıyla birlikte çalışmalı.
5. Çarpışma: karakter CapsuleCollider + CharacterController; root motion'ın delta'sını `CharacterController.Move`'a besle (doğrudan transform'a yazma — kitbash duvarlarından geçer). Çevrede MeshCollider YOK (§5 zaten diyor); oynanabilir alanın sınırı görünmez box collider'lardan oluşan bir "oyun alanı kafesi" olsun ve bu kafes seviyeyle birlikte, prop'lardan bağımsız yazılsın.
6. Foot IK'yı Hafta 3-6 diliminde DEĞİL, sistem oturduktan sonra ekle. Erken eklersen root motion hatalarını maskeler ve neyin bozuk olduğunu göremezsin.'

---
### E2. Oyunun üç ana etkileşiminden biri "hotspot'a dokunarak incele/al/konuş" ama belgede hotspot'un NASIL GÖRÜNDÜĞÜNE dair tek bir görsel karar yok. Üstelik §9 kırmızıyı hotspot'ta yasaklıyor — yani en güçlü dikkat rengi kullanılamaz — ve §1 karenin %40-60'ının neredeyse siyah olmasını, §6 ise 0.28 vignette + 0.28 grain'i zorunlu kılıyor. Yani hotspot'un okunması gereken ortam, okunabilirlik için tasarlanmış en kötü ortam.

§4'ten sonra yeni bölüm: '4B. ETKİLEŞİM GÖRSEL DİLİ
Kırmızı rezerve olduğuna göre hotspot'un dikkat çekme aracı RENK değil, DEĞER ve HAREKET olmak zorunda.
Sistem:
- Hotspot işareti: `#E6E0D6` (§9'daki sıcak kırık beyaz) ince halka, 2 px stroke, dolgu yok. Ekranda çapı ≥ 48 dp (Android dokunmatik hedef minimumu; 44 dp altına asla inme).
- Görünürlük: hotspot sadece karakter etkileşim menzilindeyken (≈2.5 m) belirir, 0.25 s fade-in. Hepsi aynı anda görünmez — §8/#16 "karede en fazla 3 detay" kuralı hotspot'lara da uygulanır: aynı anda ekranda ≤3 hotspot.
- Ayrım: yalnızca hareket ayırır. Halka çok yavaş bir nefes (scale 1.0→1.06, 1.8 s, ease in-out). Parlaklık yanıp sönmesi YOK (grain ile savaşır).
- Okunabilirlik sigortası: halkanın altında 8 px yumuşak, %25 opaklıkta koyu bir gölge halkası. Açık zeminde beyaz halka kaybolur; bu iki katmanlı çözüm her değerde okunur.
- İkonografi: incele / al / konuş için üç ayrı ikon DEĞİL, tek halka + basılınca çıkan etiket. Üç ikon öğrenilmesi gereken bir dil demek; tek halka değil.
- Zorunlu test: floor cihazda, %50 parlaklıkta, aydınlık odada, hastane koridorunun EN KARANLIK karesinde ve benzin istasyonunun en parlak turuncu havuzunda — aynı halka ikisinde de okunuyor mu? §8 sign-off listesine madde olarak ekle.'

---
### E3. Altyazı ve zamanlı seçim UI'ı — bu tür bir oyunun ekranda en çok duran görsel öğesi — sadece iki satırla geçilmiş (font Türkçe glyph kapsamı, `#E6E0D6`). Punto, satır uzunluğu, arka plan plakası, güvenli alan, ve kritik olarak Türkçe metnin İngilizceye göre %20-30 daha uzun olması (seçim butonu taşması) hiç ele alınmamış. Ayrıca zamanlı seçimin sayaç görselinde kırmızı yasak olduğu için o da tasarlanmamış.

§4B'ye devam: 'ALTYAZI VE SEÇİM UI'I
- Altyazı puntosu: telefonda minimum 18 dp, hedef 20-22 dp. Satır uzunluğu ≤ 42 karakter, en fazla 2 satır. Türkçe cümleler İngilizce muadillerinden ortalama %20-30 daha uzun — İngilizceye göre tasarlanmış hiçbir UI şablonunu olduğu gibi kullanma.
- Arka plan plakası: altyazının arkasına %45 opaklıkta, 8 px yumuşak kenarlı koyu bir plaka. "Sadece gölge/outline yeterli" değil — senin görüntün hem çok karanlık hem çok açık bölgeler içeriyor (sodyum havuzu vs. koridor sonu) ve grain açık.
- Renk: `#E6E0D6`. Konuşan kişi adı aynı renkte, %70 opaklıkta, küçük harf.
- Konum: `Screen.safeArea` içinde, alt kenardan ≥ %12. Joystick sol altta, seçim butonları sağda — altyazı ikisinin arasına sıkışmamalı.
- ZAMANLI SEÇİM SAYACI: kırmızı yasak. Sayaç, seçim listesinin sol kenarında dikey bir ince çubuk olarak tükensin, renk `#E6E0D6` → %30 opaklığa doğru sönerek. Ek olarak sayacın son %25'inde çubuk hafifçe kalınlaşsın (renk değil, kalınlık = aciliyet). Ekran titremesi ya da renk değişimi YOK.
- Seçim butonu: 3-4 seçenek, her biri ≤ 2 satır × 34 karakter. Uzun Türkçe seçenekleri kısaltmak yazarın işi değil, TASARIM KISITIDIR — senaryoyu yazarken bu limiti bil.
- TMP atlası: §9'daki Türkçe glyph uyarısını uygula, ama atlas'a şunları da dahil et: ı İ ğ Ğ ş Ş ç Ç ö Ö ü Ü, tipografik tırnaklar (" " ' '), üç nokta (…), uzun tire (—). Atlas'ı Dynamic değil STATIC kur ve build'e gömülü tut — dynamic atlas runtime'da glyph render ederken hitch yapar.'

---
### E4. Belge, karenin %40-60'ının sRGB 12-25 bandında olmasını zorunlu kılıyor ve doğruluk kaynağı olarak "floor cihaz, %50 parlaklık, aydınlık oda"yı seçiyor — ama oyuncunun bu koşulları karşılamayacağı gerçeğiyle hiç ilgilenmiyor. Karanlık bir oyunun standart çözümü olan oyun içi parlaklık/gamma kalibrasyon ekranı belgede yok. OLED ile LCD'nin siyah davranışı arasındaki fark, Android'in otomatik parlaklığı ve "gece modu / göz koruma" filtreleri de yok.

§6'ya yeni alt bölüm: 'PARLAKLIK KALİBRASYONU — KARANLIK BİR OYUNUN ZORUNLU EKRANI
Senin paletinde karenin yarısı sRGB 12-25 bandında. Bu bant:
- OLED'de temiz ayrışır,
- LCD'de gri bir bulamaca döner,
- güneş altındaki bir telefonda TAMAMEN kaybolur,
- Android'in mavi ışık filtresi açıkken sarıya kayar.
Çözüm — ilk açılışta ve Ayarlar'da bir kalibrasyon ekranı:
1. Ekranın ortasında, oyunun gerçek grade'inden geçmiş temsili bir kare (hastane koridoru önerilir — en karanlık set).
2. Üstünde üç adet zar zor görünür logo/şekil: hedef değerleri sRGB 14, 20, 26.
3. Metin: "Ortadaki şekli zar zor görebileceğiniz noktaya ayarlayın. Üstteki görünmemeli, alttaki net olmalı."
4. Kaydırıcı, post-process'e DEĞİL, global Volume'deki Lift/Gamma/Gain'in Gamma'sına ya da Color Adjustments > Post Exposure'a bağlansın (±0.4 EV aralığında sınırla — oyuncunun grade'i bozmasına izin verme).
5. Değeri `PlayerPrefs`'e yaz, her açılışta uygula.
⚠️ Bu, "nice to have" değil. §1'in 2. kuralı ("karenin %40-60'ı neredeyse siyah") kalibrasyon olmadan mağaza yorumlarında "hiçbir şey göremiyorum" olarak geri döner ve bu, karanlık oyunların bir numaralı şikâyetidir.'

---
### E5. Shader variant sayısı ve derleme hitch'leri hiç ele alınmamış. Belge "tek master shader = SRP Batcher verimliliği" diyor (doğru) ama Android'de bir shader variant'ının ekranda İLK göründüğü anda oluşan derleme takılması — bir sinematik kamera kesmesinin ortasında 200-800 ms'lik bir donma — hiçbir yerde yok. §10'da sadece "Volume Profile pre-warm" var, shader pre-warm yok.

§7'ye yeni alt bölüm: 'SHADER VARIANT DİSİPLİNİ VE PRE-WARM
Android'de bir shader variant'ı ekranda ilk kez göründüğünde derlenir. 30 fps'lik bir sinematik kesmenin ortasında bu 200-800 ms'lik bir donmadır ve tam olarak "amatör" okunur.
Zorunlu üç adım:
1. **Variant stripping.** Project Settings > Graphics > Shader Stripping. URP'de kullanmadığın her özelliği kapat (Deferred, Terrain, Decal katmanları vb.). Build log'unda derlenen variant sayısını izle; master shader'ın toplam variant sayısı 200'ü geçiyorsa `shader_feature_local` sayını azalt.
2. **ShaderVariantCollection.** Editörde bir kez oyunun tamamını gez ("Save to asset" ile toplanan variant'lar), çıkan `.shadervariants` asset'ini Preloaded Shaders'a ekle. Yükleme ekranında `ShaderVariantCollection.WarmUp()` çağır.
3. **Yükleme ekranı gerçek olsun.** Yükleme sırasında ısıtılacaklar: shader variant'ları, Volume Profile'lar (§10/#14), o lokasyonun tüm materyalleri, ve diyalog rig'inin kameraları. Yükleme ekranı 2 saniyeden kısa görünüyorsa yapay olarak 2 saniyeye uzat — bir yükleme çubuğu, oyunun ortasındaki bir donmadan sonsuz kez daha iyidir.
Doğrulama: floor cihazda, oyunu KAPAT-AÇ (editörden değil), her yeni sahnenin ilk 10 saniyesini Profiler'da izle. `Shader.Parse` / `CreateGPUProgram` görünüyorsa pre-warm eksik.'

---
### E6. QTE'ler oyunun üç ana etkileşiminden biri ama girdi gecikmesi ve pencere süreleri için tek bir sayı yok. 30 fps (33 ms kare) + Android dokunmatik gecikmesi (tipik 60-120 ms, düşük seviye cihazlarda daha fazla) + termal throttling, bir QTE'yi "adaletsiz" hissettirmenin standart reçetesi. §4 sadece "girdi penceresi sırasında kesme yapma" diyor.

§4'teki QTE bölümüne ekle: 'QTE ZAMANLAMA BÜTÇESİ
30 fps = 33 ms kare. Android dokunmatik-fotona gecikmesi floor cihazlarda tipik olarak 60-120 ms. Yani oyuncu bir prompt'u gördükten sonra senin kodunun dokunuşu duyması 100-150 ms sürüyor — bu ZATEN ödediğin bir vergi.
Sert sayılar:
- **Tap QTE penceresi: ≥ 0,80 s.** Asla 0,6 s'nin altına inme.
- **Swipe QTE: ≥ 1,0 s** (parmağın hareket etmesi gerekiyor).
- **Hold QTE: hedef süre 1,2-2,5 s**, ve tolerans: bırakma anında ±0,15 s af payı.
- **Prompt görünme → pencere açılma arası: 0,15 s okuma payı.** Prompt ve pencere aynı karede başlamasın.
- Pencereleri KARE SAYISIYLA değil, `Time.time` ile ölç. 30 fps'e kilitli bir oyunda kare sayısıyla ölçmek, throttling altında pencereyi sessizce kısaltır.
- **Termal test:** QTE'leri 15 dakikalık soak testinden SONRA oyna. Throttling altında kare süresi 33 ms'den 45 ms'e çıktığında pencere hâlâ adil mi?
- Başarısızlık cömert olsun: ilk kaçırmada "yakın" toleransı (pencere sonrası +0,2 s hâlâ kabul). Oyuncu bunu fark etmez, ama adaletsizlik hissini yok eder.'

---
### E7. Sanat yönetiminin tamamı tek bir doygun kırmızıya dayanıyor ama renk körlüğü hiç düşünülmemiş. Erkeklerin ~%8'inde kırmızı-yeşil renk körlüğü var; protanopide doygun kırmızı koyu kahve/griye düşer ve senin zaten kahverengi-bej orta tonlarından oluşan paletinde AYRIŞMAZ. Ayrıca §3'ün hastane reçetesi aksan olarak yeşil EXIT tabelası kullanıyor — kırmızı-yeşil, mümkün olan en kötü çift.

§2'ye ekle: 'RENK KÖRLÜĞÜ SİGORTASI — AKSAN ASLA TEK BAŞINA RENK OLMASIN
Erkek oyuncuların ~%8'i kırmızı-yeşil ayrımını yapamıyor. Protanopide `#C4342E` kırmızı kapüşon, senin `#5E5850` / `#746C60` orta tonlarınla neredeyse aynı koyu kahveye düşer. Yani oyunun tek imzalı görsel kararı bu oyuncularda TAMAMEN KAYBOLUR.
Çözüm — kırmızı hiçbir zaman tek başına taşıyıcı olmasın, her zaman iki sinyalden biri olsun:
1. **Değer ayrımı zorunlu.** Kapüşonun etrafındaki 1-2 metrelik alan, kapüşonun değerinden en az 3 kademe farklı olsun. §3'ün "kapüşon ya key ışığında ya kendi rim'inde" kuralı zaten bunu yapıyor — bunu tercih değil, ZORUNLULUK olarak yaz.
2. **Siluet ayrımı.** Kapüşon şekli (sivri tepe, omuz hattı) kadroda tekil olsun. Çocuk, siluet olarak kadrodaki başka hiç kimseye benzemesin.
3. **Doğrulama testi:** §8 sign-off listesine yeni madde — "Sahne ekran görüntüsünü bir protanopi simülatöründen geçir (Photoshop > View > Proof Setup > Color Blindness, ya da Coblis). Kapüşon hâlâ karedeki ilk okunan şey mi?" Değilse çözüm renk değil, IŞIK: kapüşona daha güçlü bir rim ya da arkasına daha koyu bir alan.
4. **Hastane reçetesini düzelt:** yeşil EXIT tabelası + kırmızı kapüşon, renk körlüğü için en kötü çift. EXIT tabelasını yeşil bırak (mimari doğruluk) ama onu ASLA kapüşonla aynı karede, benzer ekran boyutunda ve benzer değerde konumlandırma. §4'ün plan listesinde bu bir kadraj kuralı olarak yazılsın.'

---
### E8. §11'deki ilk dikey dilim benzin istasyonu — ve bu iyi bir seçim. Ama benzin istasyonu sahnesinde kırmızı kapüşonlu çocuk yok. Yani oyunun TEK imzalı görsel kararı, tüm belgenin etrafında döndüğü şey, ilk 6-7 haftalık doğrulama diliminde hiç test edilmiyor. Master materyal kütüphanesi, house LUT ve scene template §11 Hafta 7'de o odadan türetiliyor — yani kırmızı aksanın hiç görülmediği bir odadan.

§11 Hafta 3-6'yı düzelt: 'Benzin istasyonu dikey dilimi, KIRMIZI KAPÜŞONLU ÇOCUĞU İÇERMEK ZORUNDA.
Sebep: house LUT (Hafta 7) bu odanın karelerinden yazılacak. Kırmızı aksanın hiç görülmediği karelerden yazılan bir LUT, oyunun tek imzalı kararını hiç test etmemiş demektir — ve LUT yazıldıktan sonra kapüşonun turuncuya kaydığını Ay 8'de keşfetmek, altı ayı geri almak demektir.
Dilime eklenecek üç zorunlu kare:
1. Çocuk, sodyum lambasının turuncu havuzunun İÇİNDE. En riskli kare: 2000K turuncu ışık + kırmızı kumaş = kapüşonun turuncuya düşüp yok olduğu senaryo. Bu kare çalışıyorsa palet çalışıyor.
2. Çocuk, havuzun DIŞINDA, sadece 7500K ay rim'inde. §2 adım 5'in "tersi de bir araçtır" iddiasının testi: kapüşon gölgede kahverengi çamura mı dönüyor, yoksa hâlâ okunuyor mu?
3. Çocuk ve yetişkin, aynı karede, diyalog planında. Kapüşon ekran alanının %3-7'sinde mi?
Bu üç kare geçmeden Hafta 7'ye (artefakt çıkarma) geçme. Bunlar geçmezse düzeltilecek şey LUT değil, kapüşonun albedo'su ve §3'ün ışık reçetesidir — ve o düzeltmeyi ŞİMDİ yapmak istiyorsun.'

---
### E9. Belge her şeyi bake etmeye dayanıyor (Shadowmask + Directional + 24 texel/birim + AO) ama bake SÜRESİ hiçbir yerde bir maliyet kalemi olarak görünmüyor. Tek kişilik bir projede, ışık iterasyonunun her turu tam bir yeniden bake demek; 24 texel/birim + Directional + Shadowmask + AO ile bu, orta seviye bir makinede oda başına saatlerce sürebilir. Bu, belgenin "görsel kaliteyi ışık yapar" tezinin doğrudan iterasyon hızı düşmanı.

§3'e ekle: 'BAKE SÜRESİ BİR BÜTÇE KALEMİDİR
Her ışık ayarı = tam yeniden bake. 24 texel/birim + Directional + Shadowmask + AO ile bu, oda başına saatler demek. Tek kişilik bir projede, günde 2 bake yapabiliyorsan ışık iterasyonun ölmüş demektir.
Zorunlu iki profil — her sahne için İKİ Lighting Settings Asset:
- **`BAKE_Draft`**: Lightmap Resolution 6 texel/birim, Direct 8 / Indirect 8 / Environment 8 sample, AO KAPALI, Directional KAPALI, denoiser kapalı, atlas 512. Hedef: **≤ 3 dakika.** Kompozisyon, ışık yönü, key:fill oranı ve sis kararlarının %90'ını bu profilde ver.
- **`BAKE_Final`**: §3'teki tam ayarlar. Günde bir kez, tercihen gece, çalıştır.
Kural: **Draft'ta karar ver, Final'de doğrula.** Final bake'i bir karar aracı olarak kullanırsan sanat yönetimi değil, bekleme yaparsın.
Ayrıca: `Lightmap Parameters` asset'leri kullan (hero / ikincil / arka plan için üç tane) — §3'ün kademeli texel yoğunluğu ancak böyle sürdürülebilir şekilde uygulanır, obje obje Scale In Lightmap elle ayarlayarak değil.
Hafta 1 ölçümü: ilk market odasını her iki profilde de bake et ve SÜREYİ kaydet. Final bake 40 dakikayı geçiyorsa texel yoğunluğunu 24'ten 16'ya düşür — §12'nin "cihazda 12 vs 24 doğrula" kararına bake süresini de bir kriter olarak ekle.'

---
### E10. Belge "Detroit karakter aydınlatması" hedefini koyuyor ve §3'te Rendering Layer rim light'ı "listedeki en yüksek değerli aydınlatma hamlesi" ilan ediyor — ama sadece bir kez, tek cümlede. §3'ün lokasyon reçeteleri ÇEVRE ışığı için beş ayrı reçete veriyor, karakter ışığı için sıfır. Oysa Detroit görüntüsünün tamamı karakterin nasıl aydınlatıldığıyla ilgili ve satın alınmış çevre kitlerinin içinde bunun hiçbir karşılığı yok.

§3'e yeni alt bölüm: 'KARAKTER IŞIK RİG'İ — LOKASYON BAŞINA REÇETE
Çevre için beş reçete yazdın, karakter için sıfır. Ama "Detroit gibi görünüyor" hissini taşıyan şey çevre değil, karakterin yüzündeki ışık.
Standart rig: bir `CharacterLightRig` prefabı, karaktere parentlanmış, üç ışık — hepsi `Character` Rendering Layer'ında, çevreye HİÇ dokunmuyor:
- **KEY (Spot, gölge YOK):** sahnenin motive edilmiş key yönünden, o sahnenin Kelvin'inde, yoğunluk sahne key'inin %60-80'i. Karakterden 25-35° yanda, 15-25° yukarıda. Bu, bake ışığın yüzde yapamadığı modellemeyi yapar.
- **RIM (Spot, gölge YOK):** kameranın karşı tarafından, sahnenin FILL/ambient renginde (yani soğuk), yoğunluk key'in %25-40'ı. §5'teki Fresnel rim'in tamamlayıcısı — karakteri arka plandan ayıran şey bu.
- **CATCH (çok küçük Point ya da görünmez emissive quad):** sadece göz catchlight'ı için, kamera ekseninin hemen üstünde, yoğunluk minimal, menzil ≤1 m. §5'in (c) maddesinin somut hali.
Lokasyon başına ayar tablosu (Kelvin / key yoğunluk çarpanı / rim rengi):
- Market alacakaranlık: key 3200K ×0,75 / rim `#3A4652`
- Hastane koridoru: key 4300K ×0,60 / rim `#26313A` — ışık havuzları arasında geçerken key'i havuza göre 0'a lerp'le, karanlıkta sadece rim kalsın (en güçlü kare bu)
- Benzin istasyonu: key 2100K ×0,85 / rim 7500K ×0,35 — sıcak/soğuk ayrımı burada maksimum
- Tipi: key 8500K ×0,50, neredeyse yönsüz / rim yok (sis zaten ayırıyor)
- Araştırma tesisi: key 5500K ×0,70, sert / rim 2700K ×0,25 (acil ışıktan)
⚠️ BÜTÇE: §3'ün "en fazla 2 realtime ışık" kuralı bu rig'i KAPSAMAZ mı, kapsar mı — bu bir karardır. Kapsıyorsa el feneri + flicker practical'dan birini feda et. Öneri: KEY + RIM realtime (2 ışık, gölgesiz, tek skinned mesh'e vuruyor, ucuz), CATCH bir emissive quad (0 ışık). Yani karakter rig'i 2 realtime ışık kalemi tüketir ve çevre practical'ları baked'e döner. Bu takas, bu belgenin verdiği en önemli takastır — Detroit görüntüsü buradan gelir.'

---
### E11. Belge "oyun en kötü karesiyle yargılanır" diyor ama oyuncunun göreceği İLK kare mağaza sayfasındaki ekran görüntüsüdür ve bunun için hiçbir plan yok. §8'de thumbnail testi var (iç kalite kontrolü için) ama capture pipeline'ı, key art, mağaza görselleri, fragman çekimi — yani tek kişilik bir projenin "ucuz görünüyor" yargısını gerçekten aldığı yer — hiç ele alınmamış.

§8'e yeni alt bölüm: 'CAPTURE RIG — İLK GÜNDEN KUR, SONA BIRAKMA
Oyuncunun göreceği ilk kare mağaza görselidir. Bu kareyi oyunun bittiği hafta üretmeye çalışırsan, elinde sadece oyun kamerasının verebildiği kareler olur.
Hafta 2'de kur (LookDev ile birlikte, yarım gün):
- **`CaptureCamera` prefabı:** CinemachineCamera, oyun kamerasından bağımsız, serbestçe konumlandırılabilir, Physical lens modunda. Oyun içi kısıtlar (yükseklik, mesafe, ±12° nudge) buna UYGULANMAZ.
- **Yüksek çözünürlük modu:** capture sırasında render scale 1.0, MSAA 4x, SSAO tam çözünürlük, gölge 4096, LOD bias 4. Bunlar oyunda ödenemez ama tek bir kare için bedava. Bir `CaptureSettings` ScriptableObject'i bu değerleri geçici olarak uygulasın ve geri alsın.
- **Grade AYNI KALIR.** §7'nin kuralı burada da geçerli: mağaza görseli oyundan daha iyi grade'lenmiş görünmemeli, yoksa oyuncu aldatılmış hisseder.
- **Kare bütçesi:** oyun boyunca 12-16 "para karesi" belirle ve bunları PLAN LİSTESİNE yaz (§4'ün süreci). Her lokasyon 2-3 tane üretsin. Bu kareler için ekstra dekor, ekstra decal ve elle yerleştirilmiş prop yığını meşrudur — sadece o açıdan iyi görünmeleri yeterli.
- **Dikey format:** Play Store feature graphic ve telefon ekran görüntüleri DİKEY. 19.5:9 dikey kadrajda §4'ün lens ve mesafe tablosu farklı çalışır — capture rig'inde dikey kadrajı da test et.
- Bunları `/Captures` klasöründe versiyonla ve §8/C'deki ekran görüntüsü regresyon setiyle AYNI kamera transformlarını kullan. İki iş tek işe düşer.'

---
### E12. Sürüm kontrolü sadece bir lisans uyarısı olarak ele alınmış ("Assets klasörünü public repoya koyma"). Ama önerilen çözüm — satın alınmış varlıkları .gitignore'lamak — build tekrarlanabilirliğini kırar: altı ay sonra makine değiştiğinde ya da bir dosya bozulduğunda projeyi geri getiremezsin. 3D bir Unity projesinde büyük binary dosya yönetimi (LFS, meta dosyaları, YAML merge) tek kişilik bir projede sessizce projeyi öldürebilecek bir risk.

§7'deki lisans bölümüne ekle: 'SÜRÜM KONTROLÜ — LİSANS SORUNUNU BUILD'İ KIRARAK ÇÖZME
"Satın alınmış varlıkları .gitignore'la" tavsiyesi lisans sorununu çözer ama daha büyük bir sorun yaratır: proje artık tekrarlanabilir değildir. Makine değiştiğinde, disk bozulduğunda ya da iki yıl sonra bir hata düzeltmesi gerektiğinde elinde çalışan bir build yoktur.
Doğru kurulum:
- **PRIVATE repo + Git LFS.** Assets klasörünün TAMAMI repoda, satın alınmış varlıklar dahil. Private repo yeniden dağıtım değildir. GitHub/GitLab private, ya da self-hosted.
- **Git LFS izleme:** `*.psd *.fbx *.png *.tga *.wav *.mp4 *.exr *.hdr *.blend`. LFS'i İLK COMMIT'TEN ÖNCE kur — sonradan geçmişi yeniden yazmak acı verici.
- **Editor ayarları (zorunlu):** Project Settings > Editor > Version Control Mode = **Visible Meta Files**, Asset Serialization Mode = **Force Text**. Bu ikisi olmadan sahne dosyaları binary kalır ve hiçbir değişikliği okuyamazsın.
- **`.gitignore`:** `Library/ Temp/ Obj/ Build/ Logs/ UserSettings/ *.csproj *.sln`. Bunlar dışında hiçbir şeyi ignore etme.
- **Yedek:** repo tek başına yedek değil. Ayda bir, tüm proje klasörünün offline bir kopyasını al (harici disk). LFS kotası dolduğunda ya da hesap kilitlendiğinde tek kurtuluşun bu.
- **Public paylaşım:** itch.io demo, Discord, portfolyo — sadece BUILD paylaş, asla kaynak. §7'nin uyarısı burada geçerli ve doğru.'

---
## 🎯 İLK BEŞ İŞ

Araştırmanın sıraladığı, en yüksek kaldıraçlı beş adım. Sırayı bozma.

**1.** Floor test cihazını satın al (Snapdragon 680 / Adreno 610 veya Mali-G57 sınıfı, örn. ikinci el Redmi Note 12, ~$120-180) ve onu doğruluk kaynağı ilan et. Editörde ya da flagship'te alınan hiçbir ölçüme bir daha güvenme. Aynı sipariste bir orta cihaz (SD 6 Gen 3 / Dimensity 7300, Galaxy A5x sınıfı, ~$180-250) al ama Tier A doğrulaması dışında kullanma.

**2.** Boş bir 6000.5.9f1 URP projesi kur ve pipeline'ı §7'deki baseline'a göre ayarla: Player Settings'te Vulkan-only + min API 30 (OpenGLES3'ü kaldır), URP Asset'te Forward / Depth Priming Disabled / Intermediate Texture Auto / HDR açık 32-bit / Opaque Texture KAPALI / Depth Texture AÇIK / SRP Batcher açık / Render Graph açık (Compatibility Mode kapalı), renderer'da entegre post-processing'i kapat ve on-tile post-processing Renderer Feature'ını ekleyip Tile-Only Mode'u aç. Bootstrap script'i ile 30 fps kilitle. Floor cihaza build et ve Window > Analysis > Render Graph Viewer'ı CİHAZA BAĞLIYKEN açıp on-tile yolun gerçekten aktif olduğunu, beklenmeyen resolve olmadığını doğrula. Bu doğrulanmadan hiçbir görsel iş yapma — tüm grade planı buna dayanıyor.

**3.** İki testi aynı gün yap, çünkü ikisi de pipeline'ın temelini değiştirebilir: (a) NÖTR LUT TESTİ — kusursuz nötr bir 1024x32 strip LUT'u Compression None / Mip Maps kapalı / Wrap Clamp / Filter Bilinear ile import et, Color Lookup'a Contribution 1.0 ile ata; kare görsel olarak birebir aynı kalmalı. Değişiyorsa sRGB checkbox'ını ters çevir ve tekrarla. (b) MSAA + HDR ÇÖKME TESTİ — floor cihazda HDR ve MSAA 2x'i birlikte açıp 15 dakika çalıştır (Adreno 618'de bilinen bir URP çökme raporu var). Sonuçları §12 Karar Defteri'ne yaz.

**4.** İki master Shader Graph'ı yaz — BAGISIK_Env_Lit (Smoothness Remap, Normal Strength 0.6-1.0, Albedo Tint + Desat + Value Clamp 30-240, world-space triplanar grime, paylaşılan 512 detail normal 0.25 m tile, GlobalWear, DesatExempt) ve BAGISIK_Char_Lit (wrapped/half-lambert diffuse + Fresnel rim). Her özelliği shader KEYWORD ile değil FLOAT PROPERTY ile yap — keyword variant üretir, variant SRP Batcher batch'ini böler, SetPass bütçen patlar. Graph Settings'te Fog checkbox'ını işaretle. Aynı hafta 00_LookDev sahnesini kur: %18 gri küre, krom küre, 1.75 m manken, 2.1x0.9 m kapı çerçevesi, 256 px/m checker küpü, ve bir 'kanonik köşe' — mutlak final kalitede tek bir duvar/zemin birleşimi. Palet belgesini (§2 hex listesi) repoya commit et.

**5.** Benzin istasyonu dikey dilimini SADECE ÜCRETSİZ varlıklarla tam kalitede bitir (ambientCG tiling PBR + Poly Haven HDRI + ücretsiz PBR Hospital Horror Pack id 80117 + Mixamo rig). Tek sodyum lamba 2000-2200K, Shadowmask bake, Linear sis #1C2228 Start 8 / End 45, house LUT, master shader'a remaplenmiş materyaller, 6-9 planlık yazılmış shot listesi, içinde yürüyen bir karakter, bir diyalog sahnesi, bir QTE. Floor cihazda 15 dakikalık soak sonrası hedef kare süresini tut. SADECE bu oda bittikten SONRA master materyal kütüphanesini çıkar, house LUT'u onun karelerinden yaz, scene template'i kur ve sign-off checklist'ini yaz — dördü de soyutta icat edilmiş değil, gerçekten işe yaramış bir odadan türetilmiş olsun. Hiçbir şey satın alma; ölçümler bittiğinde satın alma sırasına geç.

---

## 🚫 YAPMAYACAKLARIMIZ

Tek kişilik bir projede bunlara girişmek, yapmamaktan **daha kötü** görünür.

- ACES tonemapping. Doygun kırmızıları clipping'e yaklaşırken turuncuya kaydırır ve çocuğun kırmızı kapüşonunu tam olarak sıcak alacakaranlık key'ini yakaladığı anda — yani para planlarında — öldürür. Neutral + elle yazılmış LUT kullan; ACES kontrastını istiyorsan tonemapper'a değil grade'e yaz. Maliyet farkı sıfır (ikisi de aynı dahili LUT'a bake ediliyor), yani seçim tamamen görüntüye dair.

- Forward+, GPU Resident Drawer ve GPU Occlusion Culling. Unity 6 pazarlamasının en çok cargo-cult edilen tavsiyesi ve bu oyunda aktif olarak kare süresi kaybettirir: Forward+ ~6 eşzamanlı realtime ışığın altında Forward'dan pahalı, GPU Resident Drawer Forward+ ister ve Unity'nin kendi dokümantasyonuna göre GPU iş yükünü artırır ('düşük seviye mobil bu etkiyi daha güçlü hisseder'). Tek odalık, birkaç yüz renderer'lı bir seviye CPU-draw-call-bound değil — GPU-bound. Deferred/Deferred+ de yanlış (tile GPU'da G-buffer bandwidth felaketi).

- URP Asset'te Opaque Texture'ı açmak. StoreAndResolve store action'ı desteklemeyen mobil platformlarda Unity MSAA property'sini runtime'da SESSİZCE yok sayar — aliasing alırsın ve sebebini asla bulamazsın. Kırılgan cam refraction'ı için buna ihtiyacın olurdu; onun yerine baked cubemap + kayan normal distortion, ya da basitçe kirli/buzlu yarı-opak materyal kullan (yağmalanmış bir markette zaten daha doğru).

- Mobilde volumetric fog (raymarched veya froxel — AERO, HAZE, Buto dahil) ve post-process god ray/sun shaft. URP'de native volumetric fog 2026 itibarıyla hâlâ yok; üçüncü parti çözümler orta Adreno'da tam çözünürlükte 2-4 ms ve ancak çeyrek çözünürlükte yaşayabilir. Yerine: legacy Linear fog (pratikte bedava, ve bir renk aracı), unlit additive mesh ışık şaftları (Scene Depth soft fade + Fresnel + kayan noise) ve 150-300 partiküllük toz zerreleri. Baraj/tipi seviyesini bu şekilde kur — orada sis zaten görünümün kendisi.

- Beş full-screen alpha katmanlı gerçek partikül tipisi. 1080x2400 x 0.80 render scale'de tek bir tam ekran alpha-blend katmanı ~1.66 milyon blend edilmiş fragment; beş katman kare başına ~8.3 Mpix blend demek ve Mali-G57/G615 sınıfı bir GPU bunu 30 fps'te yapmaz. Yerine: 2-3 kameraya kilitlenmiş kayan kar kartı + ≤400 gerçek partikül (kameraya yakın, sert boyut sınırıyla) + yarı/çeyrek çözünürlüklü partikül buffer'ı + ağır sis. Aynı kural yanan variller, toz, yağmur ve buhar için de geçerli.

- Sıkı yüz yakın planı (URP Lit'te subsurface scattering yok — mumyalanmış manken etkisi), uzun/gevşek saç (alpha sıralaması mobilde kötü, early-Z'yi kırar), kumaş simülasyonu, gerçekçi su, kalabalık, fluid-sim ateş, dinamik gün döngüsü, yıkım fiziği/ragdoll, ayna/SSR/planar reflection, ve realtime GI. Her biri üç hafta harcayıp oyuncunun 'eksik' değil 'BOZUK' olarak okuduğu bir sonuç üretiyor. Kurgu her birini bedava çözüyor: kapüşon/bere/maske saçı ve yüz maruziyetini birlikte çözüyor, post-apokalips boşluğu meşrulaştırıyor, baraj zaten donmuş.

- Mixamo karakterini kahraman yapmak. Modelleri ~2015 kalitesinde ve indie oyun oynayan herkes tarafından anında tanınıyor; aydınlatmaya ne yaparsan yap ucuz okunur. Mixamo'yu SADECE auto-rigging ve temel locomotion kütüphanesi olarak kullan. Ayrıca Mixamo animasyonu diyalog oyunu için sessizce yanlış — oyunculu, konuşmalı, tereddütlü, yaralı beden dili neredeyse hiç yok, ve Telltale tarzı bir oyunun %70'i ayakta durup konuşan insanlar. Karakterler için Character Creator 5 ($299 perpetual), performans için MoCap Online / Kubold.

- Satın alınmış kitleri kendi materyalleriyle sahneye atmak, ve tek bir pack'i bütün bir oda olarak kullanmak. Her kit smoothness'ı, albedo parlaklığını ve texel yoğunluğunu kendi konvansiyonuna göre yazıyor; üç kit'i orijinal materyalleriyle tek odaya koymak en tanınabilir 'asset flip' sinyali. Import'ta her vendor .mat dosyasını bir editor script'iyle sil ve master shader'dan yeniden yarat. Kit başına 1-2 günlük mobilizasyon geçişi bütçele ve kit'in %60-80'ini sil.

- Beş kalite kademesi bakmak, ve grade'i kademeye göre düşürmek. Tier proliferasyonu tek kişilik projelerin ölüm sebebi. Tam olarak iki tier ship et ve grade'i (tonemapper + LUT + vignette + grain) tier sisteminin DIŞINDA tut — o, oyunun kimliğini tanımlayan şey. Uzamsal efektleri ve render scale'i düşür. Doğru grade'li 0.75 render scale'li bir görüntü, default grade'li native çözünürlüklü bir görüntüden daha iyi görünür.

- Kameranın hiç kadraja almadığı geometriyi detaylandırmak, ve kameraları seviye inşa ederken refleks olarak yerleştirmek. Yarı-sabit kamera, elindeki en büyük yapısal avantaj: 'bir dünyayı iyi göster' sınırsız problemini '~120 planı iyi göster' sınırlı problemine çeviriyor. Tek bir kamera yerleştirmeden önce her seviyeyi numaralı bir shot listesi olarak yaz (plan no, amaç, tip, yükseklik, mesafe, vFOV, önplan, ışık, geçiş). Kameranın görmediği her şey greybox + sis perdesi alır. Bu, sıfır görsel kalite kaybıyla haftalar kazandırıyor.

- 60 fps hedeflemek. Kullanılabilir GPU bütçeni ~20 ms'den ~10 ms'ye indirir, yani 'filmik' ile 'ucuz'u ayıran efektleri (SSAO, decal, DoF, bloom, directional lightmap, soft shadow) tam olarak soymak zorunda kalırsın — ve orta seviye bir cihazda 12 dakika sonra termal throttling seni zaten ~38 fps'e düşürür. Telltale TWD S1 ve Detroit (base PS4) 30'da çıktı. 30 kilidi kare başına görüntü kalitesi bütçeni tam anlamıyla ikiye katlıyor.

- Assets klasörünü public bir GitHub reposuna koymak, ve market kitinden gelen gerçek marka ambalajlarını ship etmek. Birincisi Unity Asset Store EULA'sı altında satın aldığın her varlığın yeniden dağıtımı — repoyu private tut ya da satın alınmış klasörleri .gitignore'la. İkincisi ticari bir oyunda gerçek marka ihlali riski ve post-apokaliptik kurguda immersiyon kırıcı; etiket atlasını yeniden boyamaya bir gün ayır, 6-10 tane Türkçe/jenerik oyun-içi marka icat et, tek 2048 atlasa koy (bonus: tutarlı tipografi güçlü bir dünya kurma ve uyum sinyali).

---

# ANA METİN

> Aşağısı sanat yönetmeninin sentezi. Yukarıdaki düzeltmeler bu metnin ilgili
> maddelerini geçersiz kılar.

---

# BAĞIŞIK — SANAT YÖNETİMİ ANAYASASI
**Unity 6.5 (6000.5.9f1) · URP · Android öncelikli · Tek geliştirici**
Sürüm 1.0 — bu belge tartışmaya açık değil, uygulanmak için yazıldı. Değişiklik yaparsan §12 Karar Defteri'ne yaz.

---

## NASIL KULLANILIR

Bu belgeyi baştan sona bir kere oku, sonra **iki bölümünü açık tut**: §8 (Sahne Kontrol Listesi) ve §7 (Bütçeler). Geri kalanı referans.

Kural şu: **ekran görüntüsüne bakarak kontrol edemediğin bir kural, kural değildir.** Buradaki her madde ya bir sayı ya bir checkbox ya da bir görsel testtir.

---

## 1. GÖRSEL KİMLİK

### Tek paragrafta görüntü

BAĞIŞIK soğuk, ölmüş, nemli bir dünyada geçiyor. Kare neredeyse tamamen doygunluğu alınmış: gri-mavi gölgeler, kirli kahve-bej orta tonlar, hiçbir yerde parlak renk yok. Bu ölü alanın içinde **tek bir sıcak ışık kaynağı** var — batan güneş, sodyum lambası, yanan varil — ve o kaynak sahnenin %10-15'ini aydınlatıp geri kalanını karanlıkta bırakıyor. Karanlık siyaha kırpılmıyor; içinde hâlâ detay var, sadece okumak için gözünü kısman gerekiyor. Havada her zaman bir şey asılı: toz, kül, buhar, kar. Ve bu ölü alanın içinde **tek bir doygun kırmızı** var: çocuğun kapüşonu. Başka hiçbir şey kırmızı değil. Görüntü fotoğraflanmış gibi duruyor, render edilmiş gibi değil — çünkü kontrastı ışık yapıyor, post-process değil.

Referans hattı: *The Road* (2009) rengi, *Children of Men* kadrajı, *The Last of Us Part II* ıslak yüzey davranışı. **Detroit** karakter aydınlatması. **Telltale** plan gramerinde, Telltale'in çizgisel görünümü olmadan.

### Pazarlığa kapalı 12 kural

1. **Karede tek doygun renk vardır ve o kırmızı kapüşondur.** Kadrenin %3-7'si, mutlak tavan %10.
2. **Karenin %40-60'ı neredeyse siyahtır** — ama sRGB 12-25 bandında tutulur, asla 0'a kırpılmaz.
3. **Her key light'ın karede görünen ya da ima edilen bir kaynağı vardır.** Kaynaksız key = amatör tellinin bir numarası.
4. **Key:fill oranı 8:1 ile 16:1 arasındadır.** Düz ambient yok.
5. **Sis her sahnede açıktır** ve rengi o sahnenin baskın ışığından örneklenir. Nötr gri sis yasak.
6. **Satın alınmış hiçbir materyal kendi shader'ıyla build'e girmez.** Import'ta hepsi silinir, master shader'a yeniden bağlanır.
7. **Kamera elle yerleştirilir.** Takip eden kamera istisnadır, kural değil.
8. **Grade (tonemapper + LUT + vignette + grain) her cihazda birebir aynıdır.** Performans için asla düşürülmez.
9. **Kırmızı UI'da, VFX'te, hasar geri bildiriminde, QTE'de, hotspot'ta kullanılmaz.** Rezerve renktir.
10. **Her yeni sahne `BAGISIK_Scene.scenetemplate`'ten klonlanır.** Boş sahneden başlanmaz.
11. **30 fps kilitlidir.** Tartışma yok — §7'de neden.
12. **Doğruluk kaynağı floor cihazdır**, editör değil, flagship telefon değil. 15 dakika ısınmış, kılıfsız, %50 parlaklık, aydınlık oda.

---

## 2. RENK VE PALET

### Palet — repoya commit edilecek hex listesi

**Gölge ailesi** (soğuk, düşük kroma):
`#1B242C` · `#2A3742` · `#38454E`

**Orta aile** (doygunluğu alınmış nötrler, satürasyon %15 altı):
`#4A4A44` · `#5E5850` · `#746C60` · `#8A8175`

**Key / sıcak aile** (alacakaranlık ve practical'lar):
`#E8A24C` · `#F0B27A` · `#C98A52`

**Aksan — SADECE kapüşon:**
`#9B201E` (gölgede / albedo değeri) · `#C4342E` (key ışığında)

**Üçüncül, çok az** (EXIT tabelası, küf, sodyum lamba):
Yeşil `#4E5B43` · Sodyum `#FFA24A`

**Yasaklı:** her doygunlukta magenta ve mor · saf sarı · %40 üstü teal · kapüşon dışında herhangi bir doygun kırmızı.

Kural: her materyalin albedo tint'i bu ailelerden birine indirgenebilmeli. Yeni bir hue eklemek isteyen bir karar, §12'ye yazılır. **Ay 19'da, gece 1'de, satın aldığın bir pack cyan emissive ile geldiğinde seni durduracak tek şey bu liste.**

### Kırmızı kapüşon teknik olarak nasıl çalışır

Bu, oyunun tek imzalı görsel kararı. Yanlış yapılırsa bug gibi görünür. Altı adım, önem sırasıyla:

**1. Önce paletin polisliği, sonra post-process.** Satın aldığın her kit'i doygun kırmızı/turuncu için tara: pas, tuğla, yangın söndürücü, uyarı tabelaları, kan, sıcak ahşap tonları. Bunları master shader'ın `AlbedoTint` + `AlbedoDesat` parametreleriyle doygunluğu alınmış oker/kahveye çek. **Post-process bunu düzeltmez, sadece cilalar.**

**2. Hue-selective desaturation LUT'a gömülür.** DaVinci Resolve'da (ücretsiz) bir Hue vs Saturation eğrisi node'u ekle: **hue 350°-15° bandı %100'de kalır (hatta hafif yükselir), geri kalan her hue %50-65 düşer.** Bunu strip LUT'a bake et. Artık dünya global olarak doygunluğunu kaybederken kapüşon kaybetmiyor — bedava ve her karede.

**3. Kapüşonun albedo'su `#FF0000` DEĞİL.** Bu fiziksel olarak geçerli dielektrik albedo aralığının dışında ve çıkartma gibi görünür. **`#9B201E` = sRGB (155, 32, 30)** kullan. Boyalı kumaş gerçekten böyledir: yüksek kroma, düşük değer. Bu kumaş gibi okunur, emissive gibi değil.

**4. Master shader'da muafiyet, keyword ile değil float ile.** `_DesatExempt` diye bir **float property** (0-1) ekle ve desaturation node'unun sonucunu bununla lerp'le. **Shader keyword KULLANMA** — keyword yeni bir shader variant üretir, variant SRP Batcher batch'ini böler, SetPass bütçen artar. Float property SRP Batcher'ın per-material buffer'ında yaşar, bedava.

**5. Aksan aydınlatılır.** Doygun bir renk gölgede kahverengi çamura döner. Kapüşonun hikâye anı olduğu her planda ya key ışığında olacak ya da kendine ait düşük yoğunluklu bir rim alacak. **Tersi de bir araç:** çocuğu gölgeye yürüttüğünde dünyada kayboluyor. Bu, bedava elde ettiğin anlatısal bir aydınlatma aleti.

**6. Ekran alanı bütçesi %3-7.** %10'u geçtiğinde aksan olmaktan çıkıp palet olur.

### Doygunluk denetimi — her sahnede yapılacak 30 saniyelik test

Debug bir Volume oluştur, `Color Adjustments > Saturation = +100`. Sahneye bak. **Yanan her şey kapüşonla yarışıyor demektir — öldür.** Bunu her sahne sign-off'unda çalıştır.

### Lokasyon başına renk kimliği

| Lokasyon | Kimlik tek cümlede |
|---|---|
| Yağmalanmış market, alacakaranlık | Sıcak kehribar ışık şaftları, buz mavisi reyon gölgeleri |
| Terk edilmiş hastane koridoru | Hastalıklı yeşil-cyan, sert dikey düşüş, uzun boşluk |
| Benzin istasyonu, gece | Soğuk boşlukta tek turuncu ada — aşırı kontrast |
| Donmuş baraj, tipi | Neredeyse tek renk mavi-beyaz, sadece siluetler |
| Araştırma tesisi | Klinik 5000K düzen + tek 2700K acil ışık, temiz mimari / dağınık dekor gerilimi |

---

## 3. IŞIK

### Doktrin: her şey bake, tek motive edilmiş key, karanlık bir bütçe kalemi

Küçük, kapalı, statik seviyeler bake edilmiş aydınlatma için mümkün olan en iyi durumdur. Bunu sonuna kadar sömür.

**Lighting Mode: Shadowmask** (Project Settings > Quality > Shadowmask Mode: **Shadowmask**, *Distance Shadowmask DEĞİL*). Statik geometri tüm mesafede bake gölge alır; sadece dinamik nesneler Shadow Distance içinde realtime gölge alır. Distance Shadowmask her şeyi menzil içinde realtime yapar ve çok daha pahalıdır.

**Lightmapper: Progressive GPU**, denoiser **OIDN**. (OptiX 6.5'te deprecated, 6.7'de kaldırılıyor — OIDN NVIDIA dışı makinelerde de çalışır.) Bake ayarlarında **Ambient Occlusion açık**: Max Distance ~1, Indirect 1, Direct 0.

**Directional Mode: Directional.** Yarı-gerçekçi için pazarlık konusu değil — normal map'lerin bake ışığa tepki vermesini bu sağlar. Lightmap belleğini ikiye katlar; senin sahne boyutlarında bu sorun değil.

**Lightmap Resolution — kademeli, tek bir sayı değil:**
- Kameranın kadraja aldığı hero yüzeyler: **24 texel/birim**
- İkincil yüzeyler: **12 texel/birim**
- Tavan, arka yüzler, görünmeyenler: per-object **Scale In Lightmap = 0.25-0.5** ile texel geri al
- Atlas 2048, compression açık

> **Karar notu:** Android'in resmi rehberi 5-10 texel/birim der ama bu büyük seviyeler varsayar. 12×8 m'lik bir market odası 24 texel/birim'de rahatça bir-iki 2048 lightmap'e sığar. **İlk market odasını hem 12 hem 24'te bake et, bellek ve görüntüyü karşılaştır, sonra standartlaştır.**

**Lightmap Encoding'i kontrol et.** Player Settings > Lightmap Encoding mobilde düşük kaliteli dLDR'a düşebiliyor, bu HDR aralığını kırpar. Derin gölge + parlak practical içeren bir oyunda **Normal Quality (RGBM)** doğru seçim. Cihazda gözle doğrula.

### Işık sayısı — sert bütçe

- **Tam olarak 1 adet Mixed Directional** (alacakaranlık key'i / koridor acil aydınlatması)
- **En fazla 2 realtime** ek ışık (el feneri + 1 flicker practical)
- **Additional Light Shadows: sadece 1 tanesi gölge atsın**, çözünürlük 512
- Point light asla gölge atmaz (6 shadow map yüzü)
- Maliyet sırası ucuzdan pahalıya: Directional < Spot (dar koni = ucuz) < Point

### En yüksek getirili tek numara: Rendering Layer rim light

URP Asset'te **Rendering Layers**'ı aç, renderer'da **Use Rendering Layers**'ı işaretle. Oyuncu karakterine **sadece ona dokunan**, çevreye hiç dokunmayan bir realtime rim/key ışığı ver.

Bu, Detroit tarzı karakter okunabilirliğinin tam olarak elde edilme yöntemi. Tek bir skinned mesh üzerinde tek bir ekstra ışık maliyeti. Çevre çamurlu ve karanlık kalırken karakter net aydınlatılmış olur. **Bu listedeki en yüksek değerli aydınlatma hamlesi.**

> ⚠️ **Karıştırma:** Işıkların Rendering Layers'ı AÇ. Ama **Decal Renderer Feature'ın kendi "Use Rendering Layers" seçeneğini KAPALI bırak** — o, DepthNormals prepass'i zorlar ve tile-based GPU'da özellikle verimsizdir. İki farklı checkbox, ters kararlar.

### Gölge ayarları — küçük seviye avantajı

Genel mobil tavsiyesi "1024 çözünürlük, 2 cascade, 50 m mesafe" der. Sen daha iyisini yapabilirsin çünkü seviyen 12 metre:

| Ayar | İç mekân | Dış mekân (otopark, baraj) |
|---|---|---|
| Shadow Max Distance | **22 m** | **30 m** |
| Cascade Count | **1** | **2** |
| Shadow Resolution | **2048** | **2048** |
| Soft Shadows | **Low** | **Low** |
| Conservative Enclosing Sphere | ON | ON |
| Depth Bias / Normal Bias | 1.0 / 1.0 | 1.0 / 1.0 |

2048 çözünürlük 22 metreye yayıldığında metre başına ~93 shadow texel — genel tavsiyeden hem daha keskin hem daha ucuz.

**Soft Shadows'u kapatma cazibesine direnç göster.** Tile GPU'da gerçekten maliyeti var ama sert PCF'siz gölge kenarı en güvenilir "bu bir mobil oyun" sinyallerinden biri. Low kalitesi 5-tap PCF, yeter.

### Adaptive Probe Volumes (APV) — mobil konfigürasyonu

Elle light probe yerleştirmeyi bir daha yapma. Beş lokasyon ve iterasyonlar boyunca kazandığın zaman gerçek.

- **SH Bands: L1** (L2 belleği ve sampling maliyetini ikiye katlar; sisin ve ağır grade'in altında göremeyeceğin detay için)
- Memory Budget: **Low** · Blending Memory Budget: **Low**
- **Sky Occlusion: KAPALI**
- Min Probe Spacing **1 m** / Max **3 m** (iç mekân) · **1 m / 6 m** (otopark, baraj)
- **Disk Streaming / GPU Streaming: KAPALI** (açık dünya özelliği, sana karmaşıklık dışında bir şey vermez)
- Karakterlerde **per-pixel** sampling, büyük statik prop'larda per-vertex (6.5'te bu ayarın yeri değişmiş olabilir, editörde ara)
- APV compute shader ister → Vulkan zaten şart, tutarlı

APV ayrıca küçük dağınıklık prop'larını lightmap'lemeden bırakmanı sağlar — kitbash sahnelerinde gerçek bir zaman tasarrufu.

### Reflection probe'lar

- Oda başına **1 baked probe** (uzun koridorda 2, blend'li)
- Resolution **128** (küçük dolap/nişlerde 64)
- **Box Projection: AÇIK** — 4 metrelik bir koridorda box projection olmayan probe, mekân sonsuzmuş gibi yansıtır ve her parlak yüzey ucuz plastik olur. **Tek checkbox, oda gerçek mekân gibi okunuyor / okunmuyor farkı.**
- HDR açık, compressed
- Renderer'larda Reflection Probes = **Blend Probes**
- **Diyalog geçen her noktaya baş hizasında (≈1.6 m) bir probe koy** — göz catchlight'ı buradan gelecek (§5)

### Lokasyon başına reçete

Bunlar ilk geçiş değerleri. Cihazda ayarla.

**Yağmalanmış market, alacakaranlık**
- Key: vitrin camından baked directional, **3200 K**, yükseklik **8-12°** (uzun sıyırıcı gölgeler, reyon aralarında ışık şaftları), yoğunluk ~1.3
- Fill: gradient ambient, sky `#3A4652` / ground `#1E2226`, ~0.35
- Practical: 2-3 yarı ölü floresan, 5600 K, baked emissive; 1 tanesi realtime flicker
- **Sis: Linear, `#5E4E42`, Start 6 m / End 40 m**
- İmza: sıcak kehribar şaft + soğuk mavi reyon gölgesi + kırmızı kapüşon

**Terk edilmiş hastane koridoru**
- Key: tavan floresanları **4300 K**, ama **sadece her üçüncüsü yanıyor** → dönüşümlü ışık havuzu / karanlık. Hem atmosfer hem draw call tasarrufu.
- Aksan practical: **yeşil EXIT tabelası** (kapüşonun tamamlayıcısı, onunla yarışmayan hue)
- Fill: çok düşük, soğuk, ~1/16
- **Sis: Linear, `#26313A`, Start 3 m / End 22 m** — koridorun sonu hiçbir zaman çözülmeyecek kadar yoğun
- İmza: hastalıklı yeşil-cyan, sert dikey düşüş, uzun negatif alan

**Benzin istasyonu, gece**
- Key: tek sodyum buharlı kanopi lambası, **2000-2200 K**, sert, tek dar havuz. Havuzun dışı neredeyse siyah.
- Rim: ay ışığı **7500 K**, düşük yoğunluk — siluetleri okunur tutmak için
- **Sis: Linear, `#1C2228`, Start 8 m / End 45 m** (ince)
- İmza: aşırı kontrast, soğuk boşlukta tek turuncu ada. **Saat başına en yüksek dram getirisi olan set.**

**Donmuş baraj / köprü, tipi**
- Key: kapalı gökyüzü kubbesi, **8000-9000 K**, neredeyse yönsüz
- **Sis: Linear, `#C8D2D8`, Start 2 m / End 25 m** — aşırı yoğun
- Düşük kontrast, sadece siluet, rüzgâr yönlü kar
- **Bu sahneyi ERKEN yap.** Pahalı görünmesi en ucuz olan sahne — sis çizim mesafesini öldürüyor ve her şeyi saklıyor. Erken bir "beklediğimden iyi çıktı" sahnesi, tek kişilik bir projede moral açısından kritik.
- ⚠️ Bu seviye ayrı bütçelenir — §7'ye bak.

**Araştırma tesisi**
- Key: klinik practical'lar **5000-6500 K**, sert kenarlı, düzenli
- Monotonluğu kırmak ve göze dinlenecek yer vermek için tek **2700 K** acil/batarya ışığı
- **Sis: Linear, `#2A3138`, Start 5 m / End 30 m** (ince)
- İmza: temiz mimari + dağınık dekor. Gerilim tam olarak bu.

**Beş sahnede de aynı kural:** sis rengi o sahnenin baskın ışığından örneklenir, asla nötr griden değil. Bu tek kural beş çok farklı seti tek bir filmin içinde tutar.

### Sis — teknik notlar

URP'de **volumetric fog YOK** (2026 itibarıyla hâlâ yok; üçüncü parti çözümler var, mobilde kötü bahis). Sis, **Window > Rendering > Lighting > Environment > Fog** altındaki legacy ayarlardan gelir ve URP Lit/SimpleLit shader'ları buna saygı duyar. Per-pixel maliyeti forward shader içinde birkaç ALU — **pratikte bedava.**

**Linear kullan, Exponential Squared değil.** Sebep tek satır: Linear'da End mesafesini kitbash'inin bittiği yerin hemen ötesine koyabilirsin. Bu, sanat yönetimi kontrolü demektir; ExpSq'de o kontrol yok.

İki tuzak:
1. **URP sisi skybox'a uygulanmaz.** Skybox ufuk rengini elle sis rengiyle eşle, yoksa sert bir bant görürsün.
2. **Shader Graph materyalleri, Graph Settings'te Fog checkbox'ı işaretlenmeden sis almaz.** Koridordaki tek bir prop atmosferin dışında yüzer ve bozuk görünür. Master shader'da bunu bir kere işaretle, unut.

Aynı sahne içinde bölgeden bölgeye sis değiştirmek istersen `RenderSettings.fogColor / fogStartDistance / fogEndDistance` üzerinde `Lerp` yapan 15 satırlık bir script yaz — sis Volume override'ı değil.

### Işık şaftları — volumetrik olmadan

Sırayla, maliyet düzeninde:

**1. Mesh ışık şaftları (bunu yap).** Blender'da bir silindir: üst/alt yüzleri sil, üst kısmı daralt. Unlit additive Shader Graph materyali. Kritik node'lar:
- `Scene Depth (Eye)` − screen position depth → **soft depth fade** (şaft zemine sert kesmesin)
- `Fresnel` / view-dot terimi → kenardan bakınca kart kaybolsun, düz geometri olduğu belli olmasın
- Şaft ekseni boyunca yavaş kayan noise texture → yoğunluk değişimi
- Uzunluk boyunca vertex-color/gradient alpha düşüşü

Maliyet: birkaç overdraw'lı transparan pixel. Pratikte bedava, ve plan başına tam kontrol edilebilir — post-process god ray'de olmayan şey.

**2. Toz zerreleri (bunu da yap).** 150-300 partikül, yumuşak additive noktalar, yavaş sürüklenme, şaft hacmine sıkı bağlı, **Soft Particles açık** (depth texture ister). Şaft geometrisinin kendisinden daha ikna edici — havayı "içinde bir şey olan hava" yapan budur.

**3. Radial-blur sun shaft post effect: YAPMA.** ~0.5-1 ms, sadece ışığa yakın bakarken çalışır, ve on-tile post yolunu kırar.

**4. Raymarched volumetrics: YAPMA.** Baseline'a girmez, nokta.

Satın almak istersen: **Volumetric Light Beam** (Tech Salad) tam olarak bu brief için doğru ürün — mesh tabanlı, post-process değil, mobil dostu.

---

## 4. KAMERA VE KADRAJ

### Sürüm gerçeği

**Cinemachine 3.1.7** Unity 6000.5 ile gelir. **Cinemachine 2, Unity 6.5'ten itibaren destek dışı.** Namespace `Cinemachine` → **`Unity.Cinemachine`**. Bulacağın tutorialların yarısı yanlış major sürüm için. İsim haritası:

| CM2 (kullanma) | CM3 (kullan) |
|---|---|
| CinemachineVirtualCamera | **CinemachineCamera** |
| CinemachineTransposer | **CinemachineFollow** |
| CinemachineComposer | **CinemachineRotationComposer** |
| CinemachineFramingTransposer | **CinemachinePositionComposer** |
| CinemachineCollider | **CinemachineDeoccluder** |
| CinemachineConfiner | **CinemachineConfiner3D** |
| CinemachineBlendListCamera | **CinemachineSequencerCamera** |

`Priority` artık int saran bir struct — CM2'ye göre yazılmış scriptler dokunulmak ister.

### Temel kural: tripod, takip kamerası değil

**Bu belgedeki en önemli tek fikir:** Telltale ve Detroit'in sinematik görüntüsü takip kamerasından gelmiyor. **Elle yerleştirilmiş bir plan listesinden** geliyor. Yürüdükçe devreden çıkan, çoğu sabit, yazılmış kamera kurulumları zinciri.

Planlarının **%80'i** şöyle:
- `CinemachineCamera`, **Body bileşeni SİLİNMİŞ** (Do Nothing) — konumu Transform ile elle veriyorsun
- Aim = **CinemachineRotationComposer**
- Tracking Target = karakterin **göğüs hizasına** parentlenmiş boş bir GameObject (**root'a DEĞİL** — root'a nişan alırsan oyuncu yaklaştıkça kamera aşağı eğilir ve güvenlik kamerası gibi okunur)

**Rotation Composer değerleri:**

| Parametre | Değer | Neden |
|---|---|---|
| Screen Position X / Y | 0 / **0.15** | Özne merkezin biraz üstünde; alt üçte birdeki dokunmatik UI'dan uzak |
| Dead Zone X / Y | **0.35 / 0.45** | **En kritik parametre.** Küçük dead zone = her karede mikro düzeltme = "oyunumsu" titreme. 0.35+ = kamera duruyor, sonra salınıyor = "operatörlü" |
| Soft Zone X / Y | 0.9 / 0.8 | |
| Damping Horizontal / Vertical | **1.4 / 1.8** | Asimetrik — gerçek operatörler böyle davranır |
| Lookahead Time / Smoothing | 0.25 / 5 | |

### Takip gerektiğinde

Koridorlar ve uzun yürüyüşler için:
- Body = **CinemachineFollow**
- Binding Mode = **World Space** (**Lock To Target DEĞİL** — o, kamerayı karakterle döndürür ve sabit-sinematik hissini anında yok eder)
- Follow Offset ≈ (0, 1.9, −5.5)
- Damping (2.5, 1.5, 3.0)
- Aim = yine RotationComposer

**Asla kullanma: CinemachineThirdPersonFollow, CinemachineOrbitalFollow.** Biri omuz-üstü nişancı rig'i, diğeri sağ analog varsayıyor. İkisi de oyunu "generic Unity third-person template" gibi gösterir, sanatın ne kadar iyi olursa olsun.

**Sürekli Perlin noise el kamerası sarsıntısı KULLANMA.** Fragmanda harika, 30 cm mesafedeki telefonda mide bulandırır ve her dokuyu titretir. İstersen sadece sinematik planlarda, amplitude ≤0.12 / frequency ≤0.4.

### Lens — telefon en-boy oranı için hesaplanmış

Unity'nin FOV'u **dikeydir**. 19.5:9 telefonda yatayda (aspect 2.167) karşılıklar:

| vFOV | Yatay | 35mm karşılığı | Kullanım |
|---|---|---|---|
| **12-14°** | 26-30° | **68-95 mm** | **Duygusal yakın plan** — burun distorsiyonu yok |
| **18-20°** | 38-42° | **47-52 mm** | **Diyalog ikili / omuz plan** |
| **20-24°** | 42-50° | **39-47 mm** | **Genel plan / ölçek** — uzun lens katmanları üst üste sıkıştırır, barajı anıtsal yapar |
| **28°** | 57° | **≈33 mm** | **Oyun içi yürüme** — mekânı gösterir, karakteri bozmaz |
| **35° üstü** | 69°+ | 26 mm altı | **YASAK** (karakter planında). Yüzler bombeleşir, webcam gibi olur |

> **Karar notu:** İki araştırmacı burada çelişti — biri "yatay 45-60° kilitle", diğeri "vFOV 28-32°" dedi. **vFOV 28° = yatay 57°**, ikisinin kesişimi. Oyun içi lens bu. Karar verildi.

**Cinemachine 3'te LensSettings > ModeOverride = Physical** yap, Sensor Size = Super 35 (24.89 × 18.66 mm). Artık gerçek milimetre ile yazıyorsun ve plan listen gerçek bir kamera raporu gibi okunuyor.

**En-boy koruması** (tablet / katlanabilir cihaz için — bu, mağaza yorumlarında öğrenilecek bir hata):

```csharp
// 4:3 tablette vFOV 28° yatayda sadece 37° görür — kompozisyonun çöker.
if (cam.aspect < 1.7f)
    lens.FieldOfView = Camera.HorizontalToVerticalFieldOfView(57f, cam.aspect);
// Hedef yataylar: 57° oyun, 42° diyalog, 30° yakın plan
```

`Screen.safeArea`'ya UI için uy, **3D kadraja uygulama.** Çentik ve yuvarlak köşeler dış %3-5'i yiyor — **yüzleri ve kırmızı kapüşonu her kenardan en az %8 içeride tut.**

### Mesafe ve yükseklik — küçük oda için sert sayılar

1.75 m'lik bir yetişkinin ekran yüksekliği kaplaması, vFOV 28°'de:

| Mesafe | Ekran yüksekliği |
|---|---|
| 2 m | %175 |
| 3 m | %117 |
| 4 m | %88 |
| **4.5-7 m** | **%70-45 ← HEDEF** |
| 10 m | %35 |

Telefonda okunur bir oyun planı **%45-70** ister. Yani: **vFOV 28-32°, karakterden 4.5-7 m.**

4×5 m'lik bir market koridorunda bu mesafe **odanın içinde yok**. Çözüm — Resident Evil ve Telltale'in tam olarak yaptığı şey: **kamerayı geometrinin dışına koy, duvarın içinden çek.**

Uygulama: odanın dışına 1.5 m boşluk bırak, kamerayı oraya koy, ve ya (a) kamera tarafındaki duvarın mesh'ini sil, ya da (b) duvarı o kameranın **Culling Mask**'ından hariç bir layer'a al (Culling Mask bir Unity Camera property'si, dolayısıyla zone geçişinde küçük bir scriptle sür).

**Kamera yükseklikleri:**

| Yükseklik | Anlam | Ne zaman |
|---|---|---|
| **1.55-1.65 m** | Göz hizası, nötr — karakter senin eşitin | Varsayılan |
| **1.10-1.25 m** | **Çocuk göz hizası** | **Kırmızı kapüşonlu çocuk duygusal özne olduğu her an.** Telltale'in Clementine ile kullandığı en güçlü empati aleti |
| 0.5-0.9 m | Alçak açı, tehdit | Enfekte yaklaşırken |
| 2.6-4.0 m, 20-30° aşağı | Kırılganlık, karakter küçük ve açıkta | Donmuş baraj, araştırma tesisi |

**Oyun içi eğim 5-12° aşağı.** 20°'yi geçen her şey CCTV gibi okunur, sinema gibi değil.

### Kesme mi geçiş mi — kural

**CinemachineBrain:** Default Blend = **Ease In Out, 0.7 s** · Update Method = **Smart Update** · Blend Update = **Late Update**. Plan-bazlı istisnalar için bir **CinemachineBlenderSettings** asset'i kur (isimle eşleşir, en spesifik kural kazanır).

> **Kural: iki kamera aynı tarafta ve aralarında <40° yaw farkı varsa BLEND. >40° ise KES (blend süresi 0.0).**

Geniş açı değişiminde uzun blend, Unity projelerindeki en gürültülü "öğrenci filmi" artefaktı — duvarların içinden geçer ve dünyanın bir diorama olduğunu ele verir.

İki film kuralına da uy:
- **30 derece kuralı:** 30°'den az farklı iki açı arasındaki kesme, jump cut / glitch gibi okunur. Ya ≥30° taşı ya da kesme.
- **180 derece çizgisi:** İki karakterden (ya da koridor ekseninden) geçen bir çizgi seç ve **tüm kameraları bir tarafta tut**, yoksa oyuncunun yürüme yönü ekranda ters döner.

Faydalı blend hint'leri: `ScreenSpaceAimWhenTargetsDiffer` (blend ortasında LookAt hedefi değişiyorsa — özne karede kayar, kamera dünyanın içinden savrulmaz), `InheritPosition` (sinematik kamera oyun kamerasının bulunduğu yerden başlasın — kesintisiz push-in).

### Bölge geçişi ve CPU

Her küçük seviyeyi **3-8 yazılmış kamera kurulumu** olarak inşa et. Her bölgeye bir `BoxCollider (isTrigger)` koy ve **priority ile** geçiş yap (Unity Camera'yı enable/disable ederek değil).

15 satırlık bir `CameraZone` MonoBehaviour yeterli: girişte `cam.Priority = 20`, çıkışta `= 10`. (Alternatif: paketle gelen `CinemachineTriggerAction` extension'ı, PriorityBoost modunda, layer mask ile filtrelenmiş.)

**Kritik performans kuralı:** Enabled bir CinemachineCamera, live olmasa bile her karede çözülür. **Aynı anda en fazla 4 kamera enabled olsun** — mevcut bölge, komşuları, ve yaklaşan diyalog rig'i. Gerisini zone script'i kapatsın.

Trigger kutularını **hareket yönünde 0.5 m üst üste bindir** ki devir, oyuncu yeni planın ihtiyaç duyduğu geometriye ulaşmadan olsun.

### Diyalog — tek prefab, kırk konuşma

Diyalog kamerasını konuşma başına yazma. **Bir `DialogueRig` prefabı yap**, dört CinemachineCamera içersin (hepsinin Body'si silinmiş, rig root'una göre elle yerleştirilmiş):

| Kamera | Lens | Mesafe | Kullanım |
|---|---|---|---|
| **A — İkili plan** | vFOV 20° | ~4 m, göz hizası | Sessizlik, tepki |
| **B — Omuz plan (K1)** | vFOV 18° | ~1.8 m | Karakter 1 konuşurken |
| **C — Omuz plan (K2)** | vFOV 18° | ~1.8 m, çizginin diğer yanı | Karakter 2 konuşurken |
| **D — Yakın plan** | vFOV 12-14° | ~1.2 m | **Seçim çıktığı an** |

Omuz planlarda kamerayı göz hattından **15-20° kaydır** — tam karşıdan omuz planı vesikalık gibi görünür.

Prefabı sahneye at, iki Tracking Target'ı karakterlerin head bone'larına ver, **rig'in tamamını 180° çizgisinin doğru tarafına döndür**, bitti.

Kesme düzeni: B↔C arası **0.0 s sert kesme**, D'ye **0.5 s Ease In Out**. Diyaloğa girerken A kamerasına `InheritPosition` ver — oyun kamerasının bulunduğu yerden kayarak gelir, kesme olmaz.

Yazılmış hareket gereken anlarda **Timeline + CinemachineTrack + CinemachineShot** klipleri kullan; klip çakışma bölgesi zaten blend'dir, yani planları bir kurgu programındaki gibi kesiyorsun. Kısa anlar için `CinemachineSequencerCamera` daha hafif.

### Güvenlik ağları

**ClearShot + CinemachineShotQualityEvaluator.** Kitbash edilmiş bir mekânda oyuncunun tam olarak nerede duracağını öngöremezsin. 3-4 sabit kamerayı bir `CinemachineClearShot` altına park et, çocuklara **CinemachineShotQualityEvaluator** ver (Deoccluder DEĞİL). Optimal Target Distance = 5 m ver ki sadece engelsiz olanı değil, **doğru kadrajlı olanı** seçsin. ClearShot'ta Activate After ≈ 0.4 s, Min Duration ≈ 2 s — açılar arasında zıplamasın.

> Üç extension'ın farkı: **Deoccluder** kamerayı fiziksel olarak kaydırır (sabit sinematik kamerada tam olarak istemediğin şey). **Decollider** sadece geometrinin içine girmesini engeller. **ShotQualityEvaluator** hiçbir şeyi hareket ettirmeden puanlar — senin istediğin bu.

**CinemachineConfiner3D**, Slowing Distance 1.0-2.0 m, **sadece takip kameralarında.** Kameranın seviyeden çıkıp kitbash'in arkasını göstermesi, bounded-walking bir oyunda en immersiyon-yıkıcı bug sınıfı; bu bileşen o sınıfın tamamını sürükle-bırak ile siler.

### Sanal joystick + sabit kamera yön kaybı — üç çözüm birlikte

Bu, sabit kameralı oyunların bilinen katili: oyuncu koridorda çubuğu yukarı tutuyor, yeni kamera ters yönden devralıyor, karakter anında duvara doğru dönüyor.

**1. Girdi mandallama (input latching).** Kamera değiştiğinde çubuk vektörünü **hemen** yeni kameranın eksenine yeniden yansıtma. Parmak ekranda kaldığı ve çubuk yönü kesme anındaki yönden ±45° içinde kaldığı sürece **eski kameranın eksenini kullanmaya devam et.** Parmak kalkınca ya da 45°'den fazla sapınca yeniden yansıt. ~20 satır kod.

**2. Kesme açılarını kendin sınırla.** Ardışık bölgeler arasında yaw farkı **90°'yi geçmesin**, ve bağlı iki bölge arasında **180° çizgisini asla geçme.** Seviye gerçekten bir ters dönüş istiyorsa, onu bir kapı eşiğine ya da oyuncunun yürümediği bir duraklamaya koy.

**3. Ekran yönü sürekliliği.** Karakter A planında sağdan çıkıyorsa, B planına **soldan** girmeli. Bu bir film kuralı ve aynı zamanda tam olarak joystick'i tutarlı hissettiren kural.

**Joystick:** dead zone 0.15-0.20, **kayan orijin** (başparmak ekranın sol yarısında nereye inerse çubuk orada doğar). Sabit konumlu ekran çubuğu, dokunmatikte ilk üç şikâyetten biri.

### Oyuncuya küçük bir kamera payı ver

Detroit tam kilit yapmaz; oyuncunun yazılmış kurulumun içinde sınırlı bakınma özgürlüğü var. Bu, sabit kameranın deli gömleği gibi hissettirmesini engelleyen şey.

Dokunmatikte: **ekranın sağ yarısında kaydırma** → live kameraya ek yaw/pitch. **±12° yaw, ±8° pitch ile sınırla**, parmak kalkınca ~1.2 s'de `SmoothDamp` ile sıfıra dön. `CinemachineCameraOffset` extension'ı ile sür.

İkinci yarısı: önemli bir alternatif açısı olan odalara ikinci bir kamera yaz ve küçük bir "görüş değiştir" butonu ile 0.0 s kesmeyle priority takas et.

**Serbest orbit kamera YAPMA.** Serbest orbit, her duvarı ve tavanı sunulabilir yapmanı zorunlu kılar — tek kişilik bir ekibin ödeyemeyeceği maliyet tam olarak budur.

### QTE ve darbe kamerası

**Girdi penceresi sırasında ASLA kesme.** QTE şöyle yazılır: prompt çıkmadan **0.3-0.5 s önce** plana kes (0.0 s), **tüm girdi penceresi boyunca tek kamera tut**, çözümde kes.

Darbe için: vuran nesneye **CinemachineImpulseSource**, kameraya **CinemachineImpulseListener** extension'ı. Impulse dünya-uzayı olayından mesafe düşüşüyle yayılır ve Perlin noise'dan çok daha ikna edicidir çünkü **darbe şekillidir**.
- Source: Impulse Definition = Uniform, Duration **0.22 s**, Amplitude Gain **0.5-0.9**, Custom Shape "Recoil" veya "Bump"
- Listener: Amplitude Gain 1.0, Reaction Settings Amplitude 0.3 / Frequency 0.5 (kamera oturur, çarpmaz)

**FOV yumruğu:** başarılı darbede live kameranın `Lens.FieldOfView` değerini **0.12 s'de 3-5° daralt, 0.4 s'de geri aç.** Bedava, ve standart sinematik "isabet etti" işareti.

Başarısızlık dalında: 0.6 s Ease-In-Out ile daha geniş ve daha yüksek bir kameraya geçiş — hikâyenin sönmesi gibi okunur. **Kamera davranışının kendisi duyguyu taşır.**

### Bedava kazanımlar

**2.39:1 letterbox** diyalog ve ara sahnelerde. Ama UI çubuğuyla değil — **kameranın Viewport Rect'iyle** yap. Böylece render edilen alan gerçekten küçülür ve en efekt-yoğun anlarında (DoF + bloom) gerçek GPU tasarrufu elde edersin.

**CinemachineStoryboard** extension'ı: bir film karesini (The Road, Children of Men, The Last of Us) düşük alpha ile kameranın üstüne koy ve kadrajını ona eşle. Sinematograf olmayan biri için profesyonel kompozisyona ulaşmanın en hızlı yolu, ve pakette hazır geliyor.

**Kendi referansını telefonla çek.** Gerçek bir koridorda yukarıdaki yüksekliklerde on dakika çekim, herhangi bir tutorialdan fazla içgüdü düzeltir.

### Süreç: seviye değil, plan listesi yaz

**Tek bir kamera yerleştirmeden önce** her seviyeyi bir tabloda numaralı plan listesi olarak yaz:

`Plan no · Amaç (tanıtım/geçiş/keşif/diyalog/tepki) · Tip (tripod/takip) · Yükseklik (m) · Mesafe (m) · vFOV (°) · Önplanda ne var · Işık nereden · Geçiş (kesme/blend, süre)`

Alacakaranlıkta yağmalanmış bir market **6-9 plan** olmalı. Sonra kameraları listeye göre inşa et.

**Thumbnail geçişi:** her planı 200 px genişliğinde dışa aktar ve hepsini tek sayfaya diz. Thumbnail boyutunda birbirinden ayırt edilemeyen planlar, telefonda okunmayacak planlardır. Ve neredeyse aynı gri karelerden oluşan bir sayfa, tam olarak kaçınmaya çalıştığın "ucuz görünüyor" hissinin teşhisidir.

---

## 5. MATERYAL VE DOKU

### Uyum sistemi — bu bölüm bu belgedeki en yüksek değerli iş

Satın alınmış varlıklar dört ölçülebilir sebepten uyumsuz görünür: **tutarsız smoothness, tutarsız albedo değeri/doygunluğu, tutarsız texel yoğunluğu, tutarsız aydınlatma.** Her birini varlık-varlık değil, **sistematik** çöz.

### Tam olarak iki shader

**`BAGISIK_Env_Lit`** — oyundaki her opak çevre nesnesinde. URP Lit tabanlı Shader Graph. Açık parametreler:

| Parametre | Amaç |
|---|---|
| **Smoothness Remap (min, max)** | **En önemli kontrol.** Kit'ler 0.05-0.95 arası smoothness ile gelir, ortak konvansiyon yok. Proje geneli bir aralığa kelepçelemek yirmi farklı kit'i anında tek ışık tepkisinde birleştirir |
| **Normal Strength** | 0.6-1.0'a kelepçele — bir pack bağırıyor, diğeri düz |
| **Albedo Tint + Desat** | Dokuları yeniden boyamadan motorda palete çek |
| **Albedo Value Clamp** | sRGB 30-240'a kelepçele (gerçek materyaller bunu aşmaz) |
| **World-space Grime** | Yukarıdan projekte edilen toz (dünya normali yukarı bakanlarda) + aşağıdan tırmanan kir (dünya yüksekliğine göre). **Bu tek node grubu, satın alınmış bir sandalye ile satın alınmış bir duvarı on yıldır aynı odada duruyormuş gibi gösteren şey** |
| **Detail Normal** | Ortak 512 detay-noise normal, ~0.25 m'de tile'lı. Mikro-yüzey tepkisini birleştirir, düşük çözünürlüklü prop'ları yakın planda kurtarır |
| **GlobalWear** | Shader Graph Global, tek scriptten sürülür — tüm oyunu tek slider'la yaşlandır |
| **DesatExempt** | §2'deki kırmızı muafiyeti |

**`BAGISIK_Char_Lit`** — deri, kumaş, göz. Float switch'lerle tek graph:
- Deri için **wrapped / half-lambert diffuse**: NdotL'yi [−1,1]'den [0,1]'e remap et ve wrap terimini sıcak kırmızı-turuncuya boya. **~5 ekstra ALU, plastiği ete çevirir.** URP Lit'te subsurface scattering yok; bu, mobilde ödeyebileceğin tek yaklaşım.
- Rim (Fresnel), sahne ambient'ının soğuk tonunda. Karakteri arka plandan ayıran şey.

> **Sadece iki shader.** Beş değil (SRP Batcher'ı öldürür), bir değil (karakter ile duvar aynı muameleyi göremez). URP/Lit sadece geçici import stub'ı olarak kalır.
>
> ⚠️ **Özellikleri keyword ile değil float property ile yap.** Keyword = yeni shader variant = bölünmüş SRP Batcher batch'i = patlayan SetPass sayısı.

### Import disiplini — kit başına 1-2 gün, pazarlıksız

1. **Her satın alınmış `.mat` dosyasını sil.** Küçük bir editor script'i ile master shader'dan yeniden yarat, kit'in dokularına işaret et. Vendor'ın `.mat` dosyası asla ship edilmez.
2. Android platform sekmesinde **Max Size override** — 1024 çevre, 512 küçük prop, 2048 sadece gerçek hero yüzeyler.
3. **1 birim = 1 metre** doğrula, FBX Scale Factor'ü kit başına düzelt.
4. Texel yoğunluğu hedeften **2×'den fazla sapan her duvarı yeniden UV'le.** (Blender + TexTools)
5. Hero mesh'leri decimate et, dış mekân için LOD üret (LOD0/1/2 ≈ %100/40/15).
6. Mesh collider'ları box/capsule ile değiştir.
7. **Kit'in %60-80'ini sil.** Kit'ler ihtiyacın olmayan çeşitlilikle gelir; kullanılmayan her mesh build boyutu ve zihinsel yük.

**Ve asla tek bir pack'i bütün bir oda olarak kullanma.** Tanınabilir pack'ler anında teşhis edilir. Mimari bir pack'ten, prop'lar üç ayrı pack'ten, hepsi senin master materyalinden ve senin grade'inden geçerek.

### Texel yoğunluğu — hem uyum hem bütçe aracı

Tek bir sayı seç ve **±2× içinde** zorla:
- **Mimari: 256 px/m**
- **Hero prop (oyuncunun dokunduğu, kameranın yaklaştığı): 512 px/m**
- Arka plan / hiç yaklaşılmayan: 128 px/m

LookDev sahnesinde hedef yoğunlukta bir **checker küpü** tut ve import ettiğin her varlığın yanına koy.

**En büyük tek kazanç:** satın aldığın kit'lerin unique-unwrap edilmiş duvarlarını **senin tiling materyalin + senin trim sheet'in** üzerine taşı. Lokasyon başına Blender'da bir-iki günlük UV işi; bir kit'in doku ayak izini tipik olarak %60-80 düşürür ve aynı anda diğer kit'lerinle eşleştirir.

### Smoothness — plastik karşıtı tablo

Bir pack'te her şey 0.6'nın üstündeyse, **plastik görüntüsü tam olarak budur** ve remap tek geçişte düzeltir.

| Yüzey | Smoothness |
|---|---|
| Beton | 0.05-0.15 |
| Boyalı alçıpan | 0.15-0.25 |
| Ham ahşap | 0.15-0.25 |
| Kumaş | 0.10-0.20 |
| Paslı metal | 0.10-0.20 |
| Yıpranmış muşamba / vinil | 0.30-0.45 |
| Boyalı metal | 0.35-0.50 |
| Islak asfalt | 0.55-0.70 |
| Cam | 0.90+ |

**Büyük düz yüzeylerde asla 0.85'i geçme** — hiçbir MSAA ayarının düzeltemeyeceği specular titremesi alırsın.

**Islak yüzeyler dostun:** zeminlerde ve asfaltta hafifçe yükseltilmiş smoothness, practical'lardan bedava yansıyan ışık şeritleri verir ve pahalı okunur.

**Albedo yasası:** dielektrikler sRGB **50-200**. Hiçbir şey saf siyah, hiçbir şey saf beyaz. Metalik 0 ya da 1, arada bir şey yok. En yaygın import hatası albedo'nun fazla koyu olması — bu bounce ışığı aç bırakır ve bake'i bozar.

### Doku sıkıştırma — ASTC, sınıf sınıf

ASTC blokları hep 128 bit, yani bits-per-pixel blok boyutuyla belirleniyor: 4×4 = 8.00 · 5×5 = 5.12 · **6×6 = 3.56** · 8×8 = 2.00 · 10×10 = 1.28 · 12×12 = 0.89.

**Sadece ASTC ship et.** Vulkan destekleyen her Android cihaz destekliyor; ETC2 varyantı bakma.

| Varlık sınıfı | Format |
|---|---|
| Hero karakter yüz + ten albedo | **ASTC 5×5** (ten geçişlerindeki banding 6×6'da görünür) |
| Hero karakter gövde albedo, tüm diğer albedo | **ASTC 6×6** |
| Normal map — çevre | ASTC 6×6 |
| Normal map — karakter yüzü | ASTC 5×5 (yüzde yüzme görürsen o tek map'i 4×4 yap, 2.5 MB'ı bir kere karşılarsın) |
| Packed mask (AO/rough/metal/detay) | **ASTC 8×8** — düşük frekanslı veri, kimse AO artefaktı görmedi |
| Emissive | ASTC 8×8 |
| Uzak / arka plan / LOD albedo | ASTC 8×8 |
| Skybox / uzak fon | ASTC 10×10 veya 12×12 |
| **UI, altyazı atlası, ikon, SDF text** | **ASTC 4×4 veya sıkıştırmasız.** Altyazıyı asla 6×6 ile sıkıştırma — oyunun çoğunda ekranda duran şey o |

**Kanal paketleme:** RGBA mask'ta **R = AO, G = roughness, B = metallic, A = detay/grime.** En yüksek frekanslı veriyi **G**'ye koy — yeşil çoğu sıkıştırıcıda en fazla biti alır. Bu, materyal başına 4 map'ten 3 map'e iner — **doğrudan %25 doku belleği kesintisi.**

**Mipmap:** her 3D varlıkta açık, her zaman. UI'da kapalı. Tiling çevre dokularında **mip filter = Kaiser** — uzakta bulanıklaşma hissini gözle görülür azaltır.

**Kaynak dosyaları 4K import et**, Android override ile küçült. Kaynak dosyaları asla küçültme. Yani 4K doku ile gelen bir kit **sorun değil, fırsat**. Sorun hiçbir zaman çözünürlük değil — **eşsiz materyal SAYISI.**

### Çözünürlük tablosu

| Varlık | Çözünürlük |
|---|---|
| Hero karakter kafa/yüz | 2048 (albedo, normal), 1024 mask |
| Hero karakter gövde/kıyafet | 2048 albedo + normal, 1024 mask — mümkünse tüm gövde tek materyal |
| İkincil / NPC karakter | 1024 (hepsi) |
| Hikâye-kritik prop (kapüşon, fotoğraf, kartlı geçiş, ilaç kutusu) | 1024, nadiren 2048 |
| Sıradan prop (sandalye, kasa, konserve, serum askısı) | 512 — ideali 8-15 tanesini tek 2048 sayfaya atlasla, tek materyal |
| Tiling duvar/zemin/tavan | 1024, ~2 m'de tekrar + lokasyon başına **bir 2048 trim sheet** (tüm kenarlar, borular, kartonpiyerler, tabela şeritleri) |
| Arka plan / hiç yaklaşılmayan | 256-512 |
| Decal | 512-1024, atlaslı |

Atlas'ta bölgeler arasında **2-4 px gutter** bırak, yoksa mipmap taşırır. Mobilde atlas çıktısını 2048'de sınırla.

### Decal — kitleri "satın alınmış" olmaktan çıkaran şey

**URP Decal Renderer Feature**, Technique = **Screen Space** (DBuffer DEĞİL — MRT ister ve tile bandwidth'ini havaya uçurur), Normal Blend = **None** (en ucuz), **Use Rendering Layers = KAPALI** (DepthNormals prepass'i zorlar).

**Lokasyon başına 30-60 decal yaz, tek 2048 atlasta. Herhangi bir karede ≤20 görünür** (Tier B'de ≤8).

Ne çizeceksin: raf altı kir, hastane duvarlarından akan su lekeleri, boru eklerinde pas halkaları, otoparkta lastik izleri, kan, is, küf, yırtık afişler, sprey boya.

**Düz zemindeki tamamen statik kir için** gerçek decal feature yerine **unlit offset quad** kullan — bir draw call, sıfır ekstra pass.

Bu, hiçbir şey modellemeyen bir geliştiricinin elindeki **en yüksek sanat yönetimi kaldıracı.** Modüler bir kit'in modüler okunmasını durduran şey budur.

### SSAO — kararı verdim

> **Tier A'da AÇIK, Tier B'de KAPALI.**
>
> İki araştırmacı çelişti: biri "SSAO satın alınmış kitleri yere oturtan iki efektten biri", diğeri "atla, AO'yu lightmap'e ve mask'e bake et". **İkisi de kısmen haklı — sınır statik/dinamik ayrımında.** Bake AO statik-statik teması çözüyor. Ama **dinamik karakteri zemine oturtamaz.** Tier B bunun yerine sahte blob/planar contact shadow alır.

Tier A konfigürasyonu: Method **Interleaved Gradient**, Sample Count **4** (4→8 maliyeti tam iki katına çıkarır), **Downsample AÇIK** (yarı çözünürlük), Radius **0.3**, Intensity **0.7-0.9**, Falloff Distance 20-30, Blur Quality **Medium**. Bütçe ~0.8-1.5 ms.

### Depth Texture: AÇIK · Opaque Texture: KAPALI

**Depth Texture açık.** SSAO, Gaussian DoF, soft particle (toz/sis kartları için şart), screen-space decal ve her depth-fade shader'ı için gerekli. **Copy Depth Mode = After Opaques.** Maliyet ~0.3-0.8 ms — kabul et, dört efekt açıyor.

**Opaque Texture KAPALI.** Tam çözünürlüklü renk kopyası; ekstra bandwidth, ekstra bellek, ve **StoreAndResolve store action'ı desteklemeyen mobil platformlarda Unity MSAA property'sini runtime'da SESSİZCE YOK SAYAR.** Bu, URP mobil projelerindeki en yaygın görünmez MSAA katili — insanlar günlerce debug eder.

Kırılgan cam (market dondurucu kapıları, hastane pencereleri) için refraction'a ihtiyacın olurdu; onu **baked cubemap + kayan normal distortion'lı unlit transparan shader** ile ya da basitçe **kirli/buzlu yarı-opak materyal** ile taklit et. Alacakaranlıkta yağmalanmış bir markette kirli cam zaten daha doğru.

**Depth Priming Mode: Disabled.** (Unity'nin açık rehberi: Auto/Forced sadece PC ve konsol için; tile GPU'da kayıp.)

### Karakter — üç ucuz sistem, plastikten insana

**GÖZLER — bir numaralı tell.** URP Lit'te kornea refraction yok ve mobilde parallax göz shader'ı ödeyemezsin. Okunan şey **catchlight**:
- (a) Gözlere kendi küçük materyalini ver, smoothness 0.9-1.0, metallic düşük
- (b) Diyalog geçen her yere **baş hizasında (≈1.6 m) baked, box-projected, 64-128 reflection probe** koy — göz yansıtacak bir şey bulsun
- (c) Sahnede parlak kaynak yoksa, kadraj dışına **küçük görünmez emissive quad** koy, sadece catchlight üretmek için
- (d) Göz köşelerine albedo'da hafif occlusion koyulaştırması ekle

Ölü, mat gözler insanların "bu asset gibi duruyor" demesine sebep olan şeydir.

**SAÇ — iki numaralı tell ve teknik olarak tehlikeli olanı.** Alpha-test saç kartları tile-based mobil GPU'da early-Z'yi kırar ve gerçek bir overdraw maliyetidir.

**Stratejik cevap: bu post-apokaliptik bir kış hikâyesi. Kapüşon, bere, şapka, toplanmış saç ve kısa saç anlatısal olarak doğal ve problemi tamamen ortadan kaldırıyor.** Kırmızı kapüşon zaten en önemli karakterinde bunu yapıyor. **Bunu tüm kadroya bilinçli olarak yay.**

Mecbur kalırsan: **Alpha CLIP** (blend asla — sıralama ve tam overdraw zorlar), ≤400 kart, 1024 atlas, AlphaTest queue.

**TEN:** yukarıdaki wrapped diffuse + kulaklara, burun kanatlarına, parmak eklemlerine ve parmak aralarına albedo'ya gömülmüş hafif sıcaklık. **Mobilde screen-space SSS denemeye kalkma.**

### Karakter bütçeleri

| Kalem | Bütçe |
|---|---|
| Hero LOD0 | **25.000-35.000 üçgen** |
| NPC | 12.000-18.000 |
| Kemik | **≤80** dahil parmaklar (Mixamo standardı 65, CC Game Base 72) |
| Materyal | 1 (gövde+kafa atlaslı) + 1 saç + 1 küçük göz |
| Doku | 2048 albedo (5×5) + 2048 normal (5×5) + 1024 mask (8×8) + 1024 saç (6×6) ≈ **8 MB** |
| Ekranda eşzamanlı | **≤4** (25-35k'da) |

SkinWeights: kafa ve ellerde **4 kemik**, gövdede **2** (gövdede görünmez, standart takas).

**Her karakterde zorunlu ayarlar:** Animator Culling Mode = **Based on Renderers** · SkinnedMeshRenderer **Update When Offscreen = KAPALI** · Model Import > **Optimize Game Objects AÇIK**, sadece gerçekten gereken kemikler expose (silah eli, head-look hedefi).

**GPU Skinning: KAPALI bırak.** Bazı Android cihazlarda performansı ciddi düşürdüğüne dair dokümante edilmiş regresyonlar var. Varsayılan CPU skinning; kendi cihazlarında A/B test etmeden güvenme.

### Karakter kaynağı

**Mixamo'yu karakter kaynağı olarak KULLANMA.** Modelleri ~2015 kalitesinde ve indie oyun oynayan herkes tarafından anında tanınıyor — "Ely", "Vanguard", "Remy" görsel olarak Unity default capsule'ünün eşdeğeri. Kahramanının Mixamo olması, aydınlatmaya ne yaparsan yap ucuz okunur.

**Mixamo'yu şunun için kullan:** auto-rigging (ücretsiz, ticari kullanım serbest) ve **temel locomotion kütüphanesi**.

**Karakterler için: Reallusion Character Creator 5.** Perpetual ~**$299** (Deluxe başlangıç paketi $329 civarı promosyonlarla; CC 365 aboneliği $99/yıl — **3 yılı geçen bir projede perpetual kazanır**). Neden:
- Unity Auto Setup pipeline'ı shader atamasını, iskelet eşlemesini ve LOD dağıtımını otomatik yapıyor
- "Game Base" dönüşümü yüz animasyonunu koruyarak poligonu ciddi düşürüyor; subdivision kontrolüyle tam olarak 25-35k aralığına inebiliyorsun
- **ARKit uyumlu 52 blendshape** export ediyor
- **Headshot 3** (2026) fotoğraftan riglenmiş karakter üretiyor — kadroya jenerik olmayan, birbirinden farklı yüzler vermenin meşru yolu
- Ticari kullanım royalty-free (satın aldığın içerik öğelerinin Extended License'ını ayrıca kontrol et)

**Bu, tek kişinin tutarlı, yarı-gerçekçi, birbirine benzemeyen, yüz animasyonu yapılabilir bir kadro elde etmesinin tek gerçekçi yolu.** Alternatif — Asset Store'dan kopuk karakter modelleri almak — kadronun beş farklı oyundan gelmiş gibi görünmesini garantiler; korktuğun başarısızlık modu tam olarak bu.

**Daz Genesis: kullanma** (game export topolojisi ve lisanslama karışık, per-character lisansı pahalı). **Synty POLYGON: kullanma** (mükemmel pack'ler, tamamen yanlış stil). **MetaHuman: Unity'de yok**, plan yapma.

### Animasyon

**Mixamo animasyonu diyalog oyunu için sessizce yanlış.** Mixamo'da neredeyse hiç oyunculu, konuşmalı, tereddütlü, yaralı ya da duygusal beden dili yok — döngülenebilir oyun cycle'ları kütüphanesi. **Telltale tarzı bir oyunun %70'i ayakta durup konuşan insanlar.**

Satın al: **MoCap Online** (perpetual lisans, abonelik yok, 2500+ animasyon, native Unity formatı) ve/veya **Kubold** (~$60'a ~130 animasyonluk setler, ağırlık ve okunabilirlik açısından çok iyi bilinir). Reallusion ActorCore büyük ama aylık abonelik — çok yıllık yarı-zamanlı bir projede perpetual tercih et.

**Ayak kayması (foot sliding) bir numaralı amatör animasyon tell'i, modelle ilgili her şeyin önünde.** Yürüyüş için **root motion** kullan.

Ucuzdan pahalıya kalite basamakları:
1. Statik idle üzerine **tek bir additive nefes katmanı** — "manken"den "insan"a en ucuz yükseltme
2. **2-3 idle varyantı, rastgele offset'le girilen** — iki karakter asla senkron nefes almasın
3. Klip geçişleri **0.15-0.25 s crossfade** (T-pose zıplamalarını öldürür)
4. **Foot IK** geçişi
5. **Her büyük seçimin etrafındaki 5-10 saniyeyi elle key'le.** Oyuncunun gerçekten baktığı tek anlar bunlar.

**Yüz:** ARKit 52 blendshape standardı. **Kafayı ayrı, daha düşük vertex'li bir mesh olarak tut** ki blendshape maliyeti sınırlı kalsın; aynı anda ≤20 shape aktif.

**Lipsync:** **uLipSync** (ücretsiz, MIT, MFCC tabanlı, Burst derlenmiş, runtime) veya **Rhubarb Lip Sync** (ücretsiz, offline, build zamanında AnimationClip'e bake → runtime maliyeti sıfır). Tek kişi için Rhubarb + bake mimarisi doğru olan.
⚠️ **İkisi de İngilizce fonetiği üzerine eğitilmiş. Türkçe VO ile gerçek bir klip üzerinde test etmeden pipeline'ı buna kurma.**

Ve üç ucuz "canlı" sistemi: **rastgele göz kırpma** (doğal dağılımla), **göz sakkadları** (bir look-target ile), **sınırlı head-look-at.** Bunlar %80'i alır. **Replik başına kaş/duygu key'lemeye kalkma** — iyi lipsync ile ölü gözler arasındaki uncanny boşluk, stilize kısıtlamadan daha kötüdür.

---

## 6. POST-PROCESS

### Unity 6.5'in kilidi açtığı şey

Unity 6.5, mobilde on-tile post-processing'in geldiği ilk sürüm. Efektler **tile GPU'nun on-chip belleğindeyken** uygulanıyor, framebuffer'ı sistem belleğine gidip getirme maliyeti olmadan. **Unity'nin tarihsel olarak "mobilde HDR render etmeyin" demesinin sebebi bandwidth'ti; 6.5 bunu tersine çeviriyor.**

**Bu oturumda doğrulanmış tile-uyumlu efekt listesi:**
`Channel Mixer · Color Adjustments · Color Curves · **Color Lookup** · Film Grain · Lift Gamma Gain · Shadows Midtones Highlights · Split Toning · Tonemapping · Vignette · White Balance`

> **Bu, planın kilit taşı ve araştırmacılardan birinin en büyük açık sorusuydu: Color Lookup tile-uyumlu.** Yani **kalıcı grade'in tamamı — tonemapper, LUT, split toning, vignette, grain — tile belleğinde çalışıyor ve pratikte bedava.** Bu, "Unity projesi gibi görünüyor" ile "film gibi görünüyor" arasındaki fark, ve 6.0 LTS yerine 6.5'te olmanın sebebi.

**Gereksinimler:** Unity 6.5+, Vulkan, Render Graph açık (Compatibility Mode KAPALI), on-tile post-processing Renderer Feature eklenmiş, ve renderer üzerindeki **entegre URP post-processing KAPALI**. **Tile-Only Mode'u aç** — bu, kamera/renderer/URP Asset konfigürasyonun tile yolunu sessizce bozmadığını doğrular. Kapalıysa on-tile PP, görsel olarak aynı ama bandwidth kazandırmayan texture-sampling fallback'ine sessizce düşer.

**Tile yolunu KIRAN efektler** (komşu pixel okurlar, tile belleğinden resolve zorlarlar): **Bloom, Depth of Field, Motion Blur, Chromatic Aberration.**

### Global Volume — her zaman açık, tile'da, pratikte bedava

| Override | Değer |
|---|---|
| **Tonemapping** | **Neutral** |
| **White Balance** | Temperature **−12**, Tint **+4** |
| **Color Adjustments** | Post Exposure 0.0 · Contrast **+15** · Saturation **−28** · Color Filter **beyaz (#FFFFFF)** |
| **Shadows Midtones Highlights** | Shadows (0.93, 0.98, 1.10) · Midtones (1.0, 1.0, 1.0) · Highlights (1.06, 1.01, 0.94) · Shadow Limits 0 / 0.3 · Highlight Limits 0.55 / 1.0 |
| **Color Lookup** | House LUT (`BASE_desat`), Contribution **0.85** |
| **Vignette** | Intensity **0.28** · Smoothness **0.45** · Rounded **kapalı** · Renk saf siyah |
| **Film Grain** | Type **Thin 1** · Intensity **0.28** · Response **0.8** |

Soğuk gölge / sıcak highlight ayrımı (Shadows Midtones Highlights satırı) **filmik görüntünün kendisidir** ve hiçbir şeye mal olmaz.

> **Karar: Color Filter global Volume'de nötr kalır.** Lokasyon renklendirmesi LUT'ta yaşar. Tek doğruluk kaynağı, tek asset, versiyon kontrolünde izlenebilir. İki araştırmacı per-lokasyon Color Filter önerdi; **reddettim** — iki yerde renk ayarı yapmak altı ay içinde tutarsızlığa dönüşür.

> **Karar: Tonemapping = Neutral, ACES DEĞİL.** Bir araştırmacı ACES önerdi, üçü Neutral. **Neutral kazandı ve sebep kırmızı kapüşon:** ACES'in tone curve'ünde bilinen bir hue kayması var — doygun kırmızıları clipping'e yaklaşırken turuncuya çeker. Sanat yönetiminin tamamı **tek** bir doygun aksana bağlı, ve ACES o kapüşonu **tam olarak sıcak alacakaranlık key'ini yakaladığı anda**, yani para planlarında turuncu yapar. Neutral sadece aralık remap'i yapar. ACES kontrastı istiyorsan onu tonemapper'a değil **grade'ine** yaz.
>
> Not: URP'de tonemapping, white balance, color adjustments, channel mixer, split toning, shadows/midtones/highlights, lift-gamma-gain ve color curves **hepsi tek bir dahili 32³ LUT'a bake ediliyor** (LutBuilderHdr pass'i) ve uber pass'te tek texture lookup olarak uygulanıyor. Yani **ACES vs Neutral arasında runtime maliyet farkı esasen sıfır.** Görüntüye göre seç, performansa göre değil.

### Lokasyon Volume'leri — sadece iki şeyi değiştirirler

Her lokasyon sahnesinde daha yüksek Priority'li lokal bir Volume:
1. **Color Lookup texture'ını değiştirir** (`WARM_dusk`, `CLINICAL_hospital`, `SODIUM_gasstation`, `COLD_blizzard`, `FACILITY`)
2. **Bloom'u açar/kapatır** (aşağıya bak)

Başka hiçbir şeyi override etmez. Bu kural, altı ay sonra "bu sahne neden farklı görünüyor" sorusunu ortadan kaldırır.

### Bütçelenmiş ekstralar — bilinçli açılır, tile yolunu kırarlar

**Bloom** — Threshold **1.1** · Intensity **0.4** · Scatter **0.65** · Clamp 20 · High Quality Filtering **KAPALI** · Downscale **Quarter** · Max Iterations **4**. Bütçe ~0.5-1.2 ms.
> **Karar:** Bloom **global değil, lokasyon Volume'ünde.** Market, hastane, benzin istasyonu ve araştırma tesisinde AÇIK (practical'lar atmosferin çoğunu taşıyor). **Tipide KAPALI** (nokta kaynak yok, sis işi yapıyor). Floor cihazda 1.2 ms'yi geçerse Tier B'de tamamen kapan.

**Depth of Field** — **sadece Gaussian.** Start = özne mesafesi + 1.5 m · End = Start + 6 m · Max Radius **0.8** (**asla 1.0'ı geçme** — under-sampling artefaktı, telefonda sürünen bloklu kenar olarak görünür) · High Quality Sampling kapalı. Bütçe ~0.7-1.5 ms.
> **Volume weight'i sadece kamera diyalog/yakın plan state'ine girdiğinde 0→1 blend et. Yürürken tamamen kapalı.**
>
> ⚠️ **Tuzak: Cinemachine'in Focus Tracking'i Gaussian ile ÇALIŞMAZ.** `CinemachineVolumeSettings`, `dof.focusDistance` yazar ve o alan URP'de **sadece Bokeh modunda** var. Sessizce hiçbir şey yapar ve bir öğleden sonranı yer. **Gaussian'da Start/End'i kamera bölgesi başına elle yaz** ve per-camera Volume Profile'a koy.
>
> **Bokeh: yasak.** Pahalı mod, ve flagship test cihazında maliyeti göremezsin.

**Lens Flare (SRP)** data-driven bileşeni — alacakaranlık güneşinde, hastane tavan lambalarında, benzin istasyonu kanopi lambasında. Birkaç quad + bir occlusion query. **Ucuz, çok yüksek sinematik değer. Kullan.**

### Yasak liste

| Efekt | Neden |
|---|---|
| **Chromatic Aberration** | Uber pass'te çoklu bağımlı texture okuması, tile yolunu kırar, ve <1.0 render scale'de lens karakteri değil bulanıklık gibi okunur. Maks 0.05, sadece ara sahne |
| **Lens Distortion** | Tüm kare boyunca bağımlı texture okuması (tile texture cache için felaket) **VE dokunmatik hotspot'larının ekran konumlarını oyuncunun gördüğüne göre kaydırır** |
| **Motion Blur** | URP'ninki kamera-only, motion vector ister, dokunmatik joystick kamerasıyla bulaşır ve mide bulandırır. Sadece yazılmış sinematik kamera hareketlerinde ≤0.15 / Quality Low |
| **Panini Projection** | Kapalı |
| **Screen Space Lens Flare** | Tam ekran, mobilde atla |
| **SMAA** | 3 pass, ağır edge/blend lookup. Mobilde Unity'nin kendi ifadesiyle **FXAA en az kaynak tüketen seçenek** |
| **Auto Exposure** | Elle yazılmış grade'i sürekli yeniden normalize ederek yok eder. **Sabit exposure, nokta** |
| **Custom shader'da procedural noise grain** | Adreno 540'ta 0.8 ms+ ölçüldü. **Unity'nin Film Grain'i önceden hesaplanmış grain texture'ı kullanır** — onu kullan |

> **Karar: Film Grain açık ama düşük (0.28).** Bir araştırmacı "grain telefon ekranıyla ve video sıkıştırmasıyla savaşır, kaçın" dedi, diğeri "her zaman açık, banding'i öldürür" dedi. **Çözüm yoğunlukta:** 0.28'de grain doku olarak okunur, gürültü olarak değil. Ve senin paletinde bu bir zorunluluk — HDR Precision 32-bit (R11G11B10_UFloat) zayıf precision'a sahip ve senin palet **doygunluğu alınmış soğuk gölgeler ve sis**, yani mümkün olan en kötü banding senaryosu. Grain o banding'i dither'lar.

### LUT üretimi — tam iş akışı

1. URP Asset > Post-processing > **Grading Mode = High Dynamic Range**, **LUT size = 32.** Strip LUT'un **1024×32** olmalı.
2. Neutral tonemapping ile, grade'siz, temsili bir **cihaz ekran görüntüsü** al.
3. Photoshop / Affinity'de o ekran görüntüsünün köşesine nötr bir 1024×32 LUT strip yapıştır.
4. **DaVinci Resolve'da (ücretsiz) sadece GLOBAL araçlarla grade et:** curves, lift/gamma/gain, HSL, Hue vs Sat. **Mask yok, lokal düzeltme yok** — LUT'a bake olamazlar.
5. Grade edilmiş strip'i kırp, Unity'ye import et.

**Import ayarları** (klasik hata noktası): Texture Type **Default** · Compression **None** · Mip Maps **KAPALI** · Wrap **Clamp** · Filter **Bilinear** · Max Size 1024 · Aniso 0.

> ⚠️ **sRGB checkbox'ı iki araştırmacı arasında çelişkili.** Tahmin etme — **nötr LUT testi ile çöz:** kusursuz nötr bir 1024×32 strip import et, Contribution 1.0 ile ata. **Kare görsel olarak birebir aynı kalmalı.** Değişiyorsa ayar yanlış; sRGB'yi tersine çevir ve tekrar test et. İki dakikalık kesin cevap.

**Altı LUT üret, hepsi 1024×32** (aynı boyut olmazsa Volume blending çalışmaz): `BASE_desat` (house look) + beş lokasyon varyantı.

Her LUT'ta §2'deki hue-selective desaturation node'u var — kırmızı korunuyor, geri kalan düşüyor.

### Doğrulama — cihazda, editörde değil

**Window > Analysis > Render Graph Viewer**'ı **cihaza bağlıyken** aç. Bakacakların:
- (a) mümkün olan **en az sayıda native render pass**
- (b) **beklenmeyen resolve/copy yok**
- (c) post-processing'in gerçekten on-tile yolda olduğunun teyidi (framebuffer-input kullanımı)

**Eklediğin her Renderer Feature'dan sonra kontrol et, projenin sonunda değil.** SSAO, Decals, custom fullscreen pass — her biri bir intermediate texture zorlayıp birleşmiş bir pass'i bölebilir. **1080p'de kazara eklenen tek bir tam ekran resolve, tüm renk grade'inden pahalıdır**, ve bunu görmenin tek yolu Viewer.

---

## 7. BÜTÇELER

### Kare bütçesi — 30 fps kilitli

```csharp
[RuntimeInitializeOnLoadMethod(RuntimeInitializeLoadType.BeforeSceneLoad)]
static void Boot() {
    QualitySettings.vSyncCount = 0;
    Application.targetFrameRate = 30;
}
```

**30 fps = 33.3 ms duvar saati. Ama 33 ms'nin hepsini harcama.** Android'de sürekli termal throttling 10-15 dakika oyundan sonra tepe GPU performansının **%30-50'sini** alıyor.

| Kalem | Bütçe (floor cihazda **SOĞUK** ölçülmüş) |
|---|---|
| GPU | **≤20 ms** |
| CPU ana thread | **≤14 ms** |
| Render thread | ≤5 ms |
| Termal pay | ~%35 |

**60 fps kovalamak bu belgedeki her şeyi imkânsız kılar.** 60'ta ~10 ms kullanılabilir GPU zamanın olur ve "filmik" ile "ucuz"u ayıran efektleri (SSAO, decal, DoF, bloom, directional lightmap) tam olarak soymak zorunda kalırsın. Seçim tabanlı bir anlatı yürüyüşünün 60 fps'e sıfır ihtiyacı var. Telltale TWD S1 30'da çıktı, Detroit base PS4'te 30'da çıktı. **30 kilidi, kare başına görüntü kalitesi bütçeni tam anlamıyla ikiye katlıyor — elindeki en büyük tek "görsel kalite" kararı bu.**

**Adaptive Performance** paketini + Android/Samsung provider'ını ekle ve bir scaler'ı **URP render scale**'e bağla. Termal uyarı geldiğinde kare düşürmek yerine zarifçe render scale düşür. **Oyuncu 1.0'dan 0.85'e düşüşü, 30 fps'ten 19 fps'e düşüş kadar fark etmez.** Ve senin görüntün buna uygun: karanlık ve sis taşıyorsa render scale düşüşü neredeyse görünmez; ince specular detay ve bloom taşıyorsa görünür.

### Sahne başına sert sayılar

| Metrik | Hedef | Yumuşak tavan | Sert tavan |
|---|---|---|---|
| Üçgen (culling sonrası) | **150.000** | 250.000 | 300.000 |
| Draw call | **80-120** | 150 | — |
| **SetPass call** | **<50** | 80 | — |
| **Sahnedeki eşsiz materyal** | **25-40** | — | — |
| Overdraw (ortalama) | **<2.0×** | — | — |
| Resident doku belleği | **250 MB** | 350 MB | — |
| Toplam process RAM | **<1.0 GB** | 1.2 GB | — |
| Ekranda skinned karakter | ≤3 | 4 | — |
| Karede görünür decal | **≤20** (Tier B: 8) | — | — |
| Enabled CinemachineCamera | **≤4** | — | — |
| `CinemachineBrain.LateUpdate` | **≤0.3 ms** | — | — |
| Kamera + post sistemi toplam | **≤2 ms** | — | — |
| AAB base module | **<200 MB** | — | Play limiti |

**SetPass ve eşsiz materyal sayısı en çok denetlemen gereken ikisi.** SetPass, shader/render-state yüklemesi ve mobilde genellikle draw'ın kendisinden pahalı. **Nesne sayısıyla değil, materyal ve shader variant sayısıyla ölçekleniyor** — ve tam olarak üç kit'i tek seviyeye attığında patlayan sayı bu.

### Doku belleği dönüşümü — soyut korkuyu checklist'e çevir

Mipmap'li tek bir 2048 map:
- ASTC **6×6 = 2.49 MB**
- ASTC 4×4 = 5.59 MB
- 1024 @ 6×6 = **0.62 MB**

**250 MB ≈ 100 eşsiz 2K/6×6 map ≈ materyal başına 3 map ile 33 eşsiz 2K materyal.** Lokasyon başına gerçek varlık bütçen bu, ve tiling/trim materyallerin neden unique-unwrap hero geometriden daha önemli olduğunun sebebi de bu.

**Mipmap Streaming'i başlangıçta açma.** Bütçeyi gerçekten aştığında acil valf olarak kullan. Yarı-sabit kameralı, hep aynı odayı gören bir oyunda açık dünyaya kıyasla çok daha az kazandırır ve sinematik kamerada mip-pop riski getirir.

### Efekt maliyeti tahminleri — ölç, güvenme

Bunlar 1080p'de orta Adreno için genel pratisyen tahminleri, **senin içeriğinde ölçüm değil:**

| Efekt | Tahmin |
|---|---|
| SSAO (yarı çözünürlük, 4 sample) | 0.8-1.5 ms |
| Bloom (Quarter, 4 iterasyon) | 0.5-1.2 ms |
| Gaussian DoF | 0.7-1.5 ms |
| Copy depth | 0.3-0.8 ms |
| STP | 1-2 ms |
| MSAA 4× (tile GPU'da) | GPU'nun ~%10-15'i — *ama alpha-test geometri (tel örgü, raf teli) bunu ciddi artırır* |

Bütçeyi taahhüt etmeden önce Unity Profiler + Snapdragon Profiler / Arm Performance Studio ile **cihazda ölç.**

### URP Asset — tam baseline

**Rendering**
- Rendering Path: **Forward** (Forward+ DEĞİL)
- Depth Priming Mode: **Disabled**
- Intermediate Texture: **Auto**
- Store Actions: **Discard**

**Quality**
- HDR: **AÇIK** · HDR Precision: **32 Bit** (R11G11B10_UFloat — 64-bit'in yarısı bandwidth)
- Anti Aliasing (MSAA): Tier A **kapalı** (STP var) / Tier B **2×**
- Render Scale: Tier A **0.75** / Tier B **0.80**
- Upscaling Filter: Tier A **Spatial-Temporal Post-processing (STP)** / Tier B **FXAA**
- LOD Cross Fade: kapalı

**Lighting**
- Main Light: Per Pixel, Cast Shadows açık, Shadow Resolution **2048**
- Additional Lights: **Per Pixel**, Max **4** per object (Forward'ın per-object limiti 8; 4 kapalı odalarda fazlasıyla yeter ve döngüyü yarıya indirir)
- Additional Light Shadows: **AÇIK, çözünürlük 512**, ve sadece **BİR** ışık gerçekten gölge atsın
- Reflection Probe Blending: **AÇIK**
- Reflection Probe Box Projection: **AÇIK**
- **Rendering Layers: AÇIK** (karakter rim light'ı için)

**Shadows** — §3'teki tablo.

**Post-processing**
- Grading Mode: **High Dynamic Range** · LUT size **32**

**Advanced**
- SRP Batcher: **AÇIK**
- Render Graph: **AÇIK** (Compatibility Mode kapalı)

**Player Settings**
- Graphics APIs: **sadece Vulkan** (OpenGLES3'ü kaldır)
- Minimum API Level: **30**
- Lightmap Encoding: **Normal Quality (RGBM)** — mobilde dLDR'a düşmediğini doğrula

> **Karar: Vulkan-only.** On-tile PP, STP, APV per-pixel ve compute'un hepsi Vulkan'a bağlı. Vulkan tek code path veriyor, GLES3 variant patlamasını önlüyor, ve ARM'ın ölçümlerine göre GLES'e kıyasla %10-12 daha az güç tüketiyor — **yani daha az ısı, yani daha az throttling.** Aktif Android cihazların ~%85+'ı destekliyor. Analytics gerçek GLES-only oyuncu gösterirse o zaman soyulmuş bir tier ekle, önce değil.

### Forward+ / GPU Resident Drawer — kullanma

**Forward+**, per-object ışık listelerini clustered light-culling pass'i ile değiştiriyor. O clustering'in her karede sabit bir kurulum maliyeti var, yani Forward+ **ancak ~6+ eşzamanlı realtime ışıktan sonra** kazanıyor. Senin sahnelerin baked lightmap + 1 directional + 1-3 practical. **Forward'ın alanı, tam ortası.**

**GPU Resident Drawer** Forward+ ister VE compute ister. **GPU Occlusion Culling** GPU Resident Drawer ister. İkisi de binlerce tekrarlı instance'lı, CPU-bound sahneleri hedefliyor. **Unity'nin kendi dokümantasyonu açık: GPU performansını iyileştirirken GPU iş yükünü hafifçe artırıyor, ve "düşük seviye mobil veya VR platformları bu etkiyi daha güçlü hissediyor."** Sen orta seviye Android telefonda GPU-bound'sun. Tek bir oda, birkaç yüz renderer ile CPU-draw-call-bound değil. Her karede Forward+ clustering vergisini ödeyip ihtiyacın olmayan bir CPU tasarrufu satın alırdın.

**Deferred / Deferred+ de yanlış** — G-buffer yazımları tile-based mobil GPU'da bandwidth felaketi.

CPU kazancını bunun yerine **satın alınmış kitleri az sayıda ortak URP/Lit shader variant'ına yeniden materyalleştirerek** al, ki SRP Batcher gerçekten batch'leyebilsin.

### LOD, occlusion, batching — bu oyun için doğru sıralama

Standart mobil optimizasyon tavsiyesinin yarısı açık dünya oyunları için yazılmış ve senin zamanını aktif olarak harcıyor. Doğru sıra:

1. **SRP Batcher — EN YÜKSEK değer, her zaman açık.** Draw call sayısını azaltmaz; materyal property'lerini kalıcı GPU buffer'larında cache'ler ve **aynı shader variant'ını paylaşan** nesnelerde per-draw SetPass maliyetini yok eder. Verimliliği **ne kadar az farklı shader kullandığının** fonksiyonu — tek-master-shader kuralının bir performans kararı olmasının sebebi tam olarak bu. Frame Debugger'da nesnelerin "SRP Batch" node'larına düştüğünü doğrula.

2. **Occlusion Culling — YÜKSEK değer, ama sadece doğru seviyede.** Unity'nin kendi dokümantasyonu "koridorlarla bağlanmış odalar"ı ders kitabı örneği olarak adlandırıyor. **Hastane koridorun tam olarak bu — bake et.** Benzin istasyonu, otopark (çoğunlukla açık) ve tek bir oda (hiçbir şey hiçbir şeyi kapatmıyor) için neredeyse hiçbir şey kazandırmaz — bake süresini harcama. Smallest Occluder ≈ duvar kalınlığın (~0.5 m), Smallest Hole ≈ 0.25 m.

3. **Static Batching — İYİ değer.** Hareket etmeyen tüm geometriyi Static işaretle. Bellek maliyeti var ama tek küçük odada en kötü ihtimalle onlarca MB, ödeyebilirsin.

4. **LOD — DÜŞÜK değer, ve bu insanları şaşırtıyor.** 12×12 m'lik bir odada her şey kameradan 15 m içinde ve her nesne LOD0'da. **LOD'u sadece otopark, benzin istasyonu dışı ve baraj/köprü için yap.** İç mekânda o zamanı bunun yerine **`Camera.layerCullDistances`**'a harca: küçük dağınıklığı (kutular, kâğıtlar, kablolar, vidalar) bir `SmallProps` layer'ına koy ve 12-15 m'de cull et. **İki satırlık script, bu geometri için LOD'u yener.**

5. **GPU Instancing — DÜŞÜK-ORTA.** İnsanların yanlış bildiği etkileşim: **SRP Batcher aynı materyal için GPU instancing'in önüne geçer.** 150 draw call'un altında önemi yok. Gerçekten çok sayıda tek mesh olduğunda (raftaki 200 aynı konserve) `Graphics.RenderMeshInstanced` açık yol.

6. **Dynamic Batching — KAPALI bırak.** CPU-pahalı, minik mesh'lerle sınırlı, SRP Batcher altında büyük ölçüde geçersiz.

### Tam olarak iki kalite kademesi

Tek kişi beş URP asset'i bakamaz. **İkisi**, boot'ta basit bir cihaz kontrolüyle seçilir (`SystemInfo.graphicsDeviceName` tier listesi + ilk açılışta 10 saniyelik benchmark).

| | **Tier A** (Adreno 710+ / Mali-G68+ / Xclipse) | **Tier B** (Adreno 610 / Mali-G57 sınıfı) |
|---|---|---|
| Render Scale | **0.75 + STP** | **0.80 + MSAA 2× + FXAA** |
| HDR on-tile | Açık, 32-bit | Açık, 32-bit |
| SSAO | Yarı çözünürlük, 4 sample | **KAPALI** (blob contact shadow) |
| Decal (görünür) | 20 | 8 |
| Bloom | Açık | Downscale Quarter / 3 iterasyon |
| DoF | Diyalogda Gaussian | **KAPALI** |
| Shadow Distance | 30 m | 20 m |
| Cascade | 1 | 1 |
| Shadow Resolution | 2048 | 1024 |
| Soft Shadows | Low | Low |
| **Grade** | **AYNI** | **AYNI** |

> **Grade asla düşürülmez** çünkü oyunun kimliğini tanımlayan şey o. **Uzamsal efektleri ve çözünürlüğü düşür.** Doğru grade'li 0.75 render scale'li bir görüntü, default grade'li native çözünürlüklü bir görüntüden daha iyi görünür.

Ayarlarda manuel bir kalite anahtarı da aç — flagship'i olan oyuncular yukarı çıkmak isteyecek, ve sana hiçbir şeye mal olmuyor.

### Tipi seviyesi — ayrı bütçelenir

**Tam ekran transparanlık, orta seviye bir mobil GPU'yu yok etmenin en hızlı yolu, ve tipi bundan başka bir şey değil.**

Aritmetik: 1080×2400 × 0.80 render scale'de **tek** bir tam ekran alpha-blend katmanı ≈ **1.66 milyon blend edilmiş fragment.** Ekranı kaplayan beş kar partikül katmanı = opak pass'in üstüne kare başına **8.3 Mpix blend.** Mali-G57'de bu olmaz.

Bunun yerine:
- **≤400 gerçek partikül**, kameraya yakın küçük bir hacimde, sert boyut sınırıyla
- **2-3 kameraya kilitlenmiş kayan kar KARTI** (kameraya parentlanmış quad, kayan noise texture). Bu, 20 değil **2-3 tam ekran blend** maliyeti
- Partikül sistemini **yarı veya çeyrek çözünürlüklü buffer'a** render edip yukarı composite et. Kar yüksek frekanslı ve düşük kontrastlı; kimse anlamaz
- Mümkün olan yerde **Alpha CLIP**, alpha blend değil
- **Fırtına işinin çoğunu mesafe sisi yapsın** — bedava ve tipi gibi okunuyor
- Ölçmeden **soft particle kullanma** (fragment başına depth texture sample'ı)
- **Bu seviyeyi özellikle Rendering Debugger'ın overdraw görünümüyle profille** ve her yerdeki gibi 2.0× ortalamada tut

**Aynı uyarı şunlar için de geçerli:** yanan variller, hastanedeki toz zerreleri, yağmur, buhar, ve her lens-flare / god-ray efekti.

### Satın alma bütçesi ve — daha önemlisi — SIRA

**ÖNCE TEST CİHAZINI AL.** Bu listedeki en önemli satın alma.
- **Floor cihaz** (Snapdragon 680 / Adreno 610 veya Mali-G57 sınıfı — Redmi Note 12 tipi): ~$120-180 ikinci el. **Doğruluk kaynağın bu.**
- **Orta cihaz** (Snapdragon 6 Gen 3 / Dimensity 7300 / Adreno 810 / Mali-G615, Galaxy A5x sınıfı): ~$180-250.

**Sıra:**

1. **Hiçbir şey satın almadan önce: TEK ODA DİKEY DİLİMİ.** Ücretsiz hastane pack'i (`PBR - Hospital Horror Pack`, id 80117, bedava) + bir Mixamo karakteri + master shader + bir baked lighting + bir LUT. Gerçek telefonuna build et. Üçgen, draw call, SetPass, doku belleği, kare süresi ölç. **Artık senin sayılarının senin cihazında doğru olup olmadığını biliyorsun.**
2. **Sonra Character Creator 5** ($299 perpetual) ve gerçek protagonistini + çocuğu yap. Kadro en uzun yaşayacağın şey ve Telltale tarzı bir oyunu taşıyan şey.
3. **Sonra çevre kitleri** — ve **mümkün olan en az yayıncıdan, ideali iki.** Tek yayıncının kitleri zaten yarı yarıya birbirine uyuyor. İndirimde $150-400.
4. **Sonra animasyon pack'leri**, senaryonun hangi performansları istediğini bildiğinde. $200-400.
5. Decal + moloz + VFX pack'leri: $100-200.

**Toplam: ~$850-1.600 varlık + ~$300-430 cihaz**, projeye yayılmış.

**İlk günden ücretsiz ve projede olmalı:** **Poly Haven** (CC0 HDRI — elindeki en büyük bedava aydınlatma kalitesi kazancı, her lokasyonun skybox ve reflection probe kaynağı) · **ambientCG** (CC0 tiling PBR — tüm duvar/zemin/beton/pas kütüphanen; CC0 olduğu için serbestçe yeniden renklendirip eşleştirebilirsin) · **Mixamo** (rig + locomotion baseline) · **Blender** · **DaVinci Resolve** (LUT) · **uLipSync** · **PureRef** (referans panosu).

**Asla tam fiyata alma.** Unity yılda birkaç kez derin indirim yapıyor (Black Friday ~%70'e kadar, bahar/yaz indirimleri, haftalık Publisher of the Week ~%50, Mega Bundle'lar). Bir wishlist tut, bir ay mağazayı izle.

### Kit satın alırken mağaza sayfasından ne okunur — 15 dakikalık kontrol

1. **Render pipeline etiketleri.** URP yazıyorsa güvenli. Sadece Built-In = kit başına bir öğleden sonra yeniden materyalleştirme. Sadece HDRP ekran görüntüleri = pazarlama görselleri ne alacağın konusunda yalan söylüyor.
2. **"Technical Details" bloğu.** Açık poligon aralıkları ve doku çözünürlükleri istiyorsun. Bir yayıncı poligon sayısını söylemiyorsa, **cevap zaten bu.**
3. **Modüler ve grid üzerinde mi?** 1 m veya 2 m'ye snap eden duvar/zemin, eşleşen köşe/kapı/pencere parçaları. Modüler olmayan kitler ("tek parça hazır hastane") kendi tasarladığın bir seviye için neredeyse işe yaramaz ve her duvar için unique-unwrap 4K doku taşırlar.
4. **Tiling materyal ve trim sheet ile geliyor mu?** Nadir, ve iki katı fiyatı hak eder. Lokasyon başına 30 MB ile 300 MB arasındaki fark.
5. **Prefab / mesh oranı.** Yüksek oran = yayıncı varyasyonları kendisi kurmuş = kitbash zamanından tasarruf.
6. **Son güncelleme tarihi.** 2022.3 LTS veya 6000.x listelemeli. 2019'dan beri dokunulmamış bir kit Standard shader materyalleri ve muhtemelen bozuk import ayarları taşır.
7. **Yayıncının diğer pack'leri.** **Uyum için en yüksek değerli kriter ve kimse bundan bahsetmiyor:** 10 gerçekçi kit'ten oluşan tutarlı bir kataloğu olan bir yayıncı, tüm oyununu üzerine kurabileceğin yayıncıdır çünkü kitler birbirine uyacak.

**Kırmızı bayraklar:** albedo'ya bake edilmiş AO/aydınlatma (senin ışığınla savaşır ve asla geri alınamaz) · ağır alpha-test yaprak/kablo/tel örgü · tek bir lighting setup'a bağlı vertex-color materyaller · tek parça 200k üçgen "hero" mesh'ler · belli ki offline render edilmiş ya da HDRP volumetrics/raytracing kullanan ekran görüntüleri · sadece tek bir kamera açısından iyi görünen demo sahneler.

**Belirleyici OLMAYAN, insanların sandığının aksine: 4K kaynak doku.** Import'ta Max Size'ı platform bazında override ediyorsun. **4K kaynak İYİ** — seçme şansın var demek. Sorun hiçbir zaman çözünürlük değil, eşsiz materyal SAYISI.

**Değerlendirilecek ipuçları** (mağaza sayfaları bu oturumda açılamadı — poligon sayısı, doku sayısı, URP desteği ve güncellik kendin doğrula):
- Hastane: `Modular Abandoned Hospital` — **Hivemind** (id 276792), modüler + geniş decal kütüphanesi · `The Horror Hospital` — Art Equilibrium (id 310180)
- Post-apokaliptik: `Post Apocalyptic Town` — **Hivemind** (id 288838) — **aynı yayıncı, uyum hamlesi bu** · `Post Apocalyptic Modular Environment Pack` — Sector4 (id 330426)
- Market: `Modern Supermarket` (id 186122) · `Grocery store - parking and supermarket` (id 224033) — **otoparkı da içeriyor**, senin ayrı bir lokasyon olarak ihtiyacın olan şey
- Ücretsiz test için: `PBR - Hospital Horror Pack` — DNK_DEV (id 80117)

### Lisans tuzakları

- **Asset Store EULA — tek kişi = single-entity lisansı doğru.** Ama projeye bir freelance animatör dokunduğu anda multi-entity gerekir ya da kendi seat'i olmalı.
- **"Extension Assets"** (araçlar, editor eklentileri) **kişi başı seat** ister. Sanat pack'leri normalde bu kısıtlamaya girmez, araçlar girer.
- ⚠️ **Assets klasörünü içeren PUBLIC bir GitHub reposu, içindeki her satın alınmış varlığın YENİDEN DAĞITIMIDIR.** Repoyu private tut, ya da satın alınmış varlık klasörlerini `.gitignore`'la ve ayrı bir private depoda sakla. Aynısı public Discord'a ya da itch.io "source" build'ine yüklemek için de geçerli.
- **Mixamo:** ücretsiz, royalty-free, sınırsız ticari. Ham dosyaları yeniden paketleyip dağıtamazsın. Oyunun içinde ship etmek serbest.
- **CC0** (Poly Haven, ambientCG): her amaç, atıf gerekmez. Mümkün olan en güvenli.
- **Sketchfab:** model bazında kontrol et. Çoğu CC-BY (atıf ZORUNLU), bazıları ticari-olmayan (kullanılamaz).
- ⚠️ **MARKA TUZAĞI — yağmalanmış marketin için doğrudan geçerli.** Market ve bakkal asset pack'leri rutin olarak gerçek markaların ince parodisi ya da doğrudan kopyası olan ambalajlarla geliyor: mısır gevreği kutuları, gazoz kutuları, cips paketleri, ilaç kutuları. Bunları ticari bir oyunda ship etmek gerçek bir marka ihlali riski ve post-apokaliptik bir kurguda immersiyon kırıcı.
  **Somut çözüm: etiket atlasını yeniden boyamaya bir gün ayır.** 6-10 tane oyun-içi marka icat et, Türkçe ya da kasten jenerik yap, tek bir 2048 atlasa koy. **Bonus: tutarlı tipografiye sahip icat edilmiş markalar güçlü bir dünya kurma ve uyum sinyali** — satın alınmış bir market kitini yazılmış göstermenin en ucuz yollarından biri.
- **AI üretimi içerik:** Unity yayıncılardan mağaza sayfasında bunu beyan etmelerini istiyor. AI üretimi varlıklardaki haklar birkaç ülkede belirsiz. **Satmayı düşündüğün bir oyun için o beyanı taşımayan pack'leri tercih et.**

---

## 8. SAHNE KURULUM KONTROL LİSTESİ

### LookDev kiti — bunları bir kere yap, sonsuza kadar kullan

**A. `00_LookDev` sahnesi:**
- %18 gri küre, krom küre, fiziksel renk kartı düzlemi (exposure/grade referansı)
- **1.75 m manken** ve **2.1 × 0.9 m kapı çerçevesi** (ölçek referansı — kitlerin kapıları 2.05 ile 2.4 arasında değişiyor)
- Hedef yoğunlukta **texel density checker küpü**
- **Kanonik köşe:** tek bir duvar/zemin/tavan birleşimi, mutlak final kalitede — süpürgelik, temas çizgisinde kir decal'i, bir çatlak, bir sıyrık, bir prop. **Kalite çıtan bu.** Yeni bir sahne doğru görünmediğinde onun köşesini bunun yanına koy.
- Tüm master materyal kütüphanesi küre/düzlem olarak dizili

**B. `BAGISIK_Scene.scenetemplate`** — önceden bağlanmış: global Volume + house profile · sis açık, placeholder renkle · Kelvin modda directional key · **FOV kilitli** Cinemachine kamera prefabı · APV volume. **Her yeni seviye bu dosyadan başlar.**

**C. Ekran görüntüsü regresyonu.** Sabit kamera transformlarından alınmış kanonik ekran görüntüleri klasörü tut. Her global değişiklikten sonra (LUT düzenlemesi, lightmap yeniden bake, master shader değişikliği) **aynı kareleri tekrar çek ve yan yana karşılaştır.** Hiçbir checklist maddesinin adlandırmadığı yavaş kaymayı yakalayan şey bu.

**D. Referans panosu.** Lokasyon başına **6-9 görsel**, PureRef'te, repoya commit edilmiş. Pinterest değil — sabit, küçük, seçilmiş bir set. **Kısıtlama bütün mesele.**

### Kurulum listesi (sahne oluştururken)

1. `BAGISIK_Scene.scenetemplate`'ten klonlandı
2. Global Volume + house profile mevcut, Priority 0
3. Lokasyon Volume'ü var, doğru LUT atanmış, Priority 10
4. **Plan listesi yazıldı** (tek bir kamera yerleştirilmeden önce)
5. Sis: Linear, rengi key light'tan örneklendi, Start/End reçeteye göre
6. Skybox ufuk rengi sis rengiyle eşleştirildi
7. Directional key **Kelvin modunda**, **Mixed**, ve **karede görünen bir kaynağı var**
8. APV volume oynanabilir alanı kaplıyor: L1, Low/Low, Sky Occlusion kapalı, streaming kapalı
9. Oda başına reflection probe: 128, **Box Projection açık**, baked
10. Diyalog noktalarında baş hizasında (≈1.6 m) probe var (göz catchlight'ı)
11. Karakter rim light'ı **kendi Rendering Layer'ında**
12. Shadow Distance 22 (iç) / 30 (dış), Cascade 1 / 2
13. Hareketsiz tüm geometri **Static** işaretli
14. Occlusion culling **sadece** koridor tipi seviyede bake edildi
15. Küçük dağınıklık `SmallProps` layer'ında, `layerCullDistances` 12-15 m

### Sign-off listesi (sahne "bitti" denmeden önce)

**Işık ve renk**
1. Sis rengi key light'tan örneklendi mi? (gri sis = motor default'u)
2. Key light Kelvin'de mi, RGB picker'da değil?
3. Key:fill oranı ≥8:1 mi?
4. Her ışığın görünen/ima edilen kaynağı var mı?
5. House LUT + doğru lokasyon varyantı uygulandı mı?
6. **Saturation +100 debug geçişi:** kapüşon dışında yanan bir şey var mı?
7. Kırmızı aksan karenin ≤%7'si mi, ve başka hiçbir şey kırmızı değil mi?
8. **Greyscale testi:** tam doygunluğu al. **Üç net değer grubu** çıkıyor mu ve özne karedeki en parlak ya da en koyu şey mi? Hepsi aynı orta gri bulamaçtaysa sorun kontrast, renk değil — hiçbir grade bunu kurtarmaz.
9. Gölgeler sRGB 12-25'te tutuluyor mu, 0'a kırpılmıyor mu? Highlight'lar 245 altında mı (practical kaynaklar hariç)?

**Materyal**
10. Satın alınmış pack'in orijinal shader'ını kullanan **sıfır** materyal var mı?
11. Smoothness değerleri §5'teki yüzey sınıfı tablosunda mı?
12. Texel yoğunluğu hedeften ±2× içinde mi?
13. Her prop'un yere teması var mı (gölge + temas kiri)?
14. Tek karede aynı rotasyon ve ölçekte tekrarlanan prop var mı? (±3-7° Y rotasyon, ±%5 ölçek)

**Kompozisyon**
15. Her kamera pozisyonundan **üç katman** var mı (önplan engelleyici / orta / arka)?
16. Karede **okumak isteyen en fazla 3 detay** var mı? (fazlası göz vazgeçer, "dağınıklık" olur)
17. Zemin düzleminin **≥%30'u boş** mu? (yürüyüş okunsun, joystick geometriyle savaşmasın)
18. Odada **başka hiçbir yerde olmayan bir hero prop** var mı?
19. **Siluet geçişi:** sahneyi düz siyah şekiller olarak gör. Siluet sıkıcıysa prop eklemek kurtarmaz — blocking yanlış.
20. **Thumbnail geçişi:** 200 px genişlikte hâlâ neyin ne olduğu ve nereye gidileceği anlaşılıyor mu?
21. **Flip testi:** kareyi yatay aynala. Körleştiğin kompozisyon hataları anında ortaya çıkar.
22. Dokunmatik UI hesaba katıldı mı? (joystick ve seçim promptları alt üçte biri yiyor)

**Teknik**
23. Bake yapıldı, bütçe dışı realtime ışık yok mu?
24. Enabled CinemachineCamera sayısı ≤4 mü?
25. **Gerçek telefonda, %50 parlaklıkta, aydınlık bir odada** doğrulandı mı?
26. **15 dakikalık termal soak sonrası** kare süresi hâlâ bütçede mi?
27. Render Graph Viewer'da cihazda: beklenmeyen resolve yok, on-tile yol aktif mi?

### Haftalık ritim

Greyscale + thumbnail + flip + squint testleri toplamda haftada **on dakika** ve tam olarak bir sanat yönetmeninin senin sahneni incelerken yapacağı şey. **Çok yıllık tek kişilik bir projede kendi işine kör olursun; bu testler bir insana ihtiyaç duymadan gözünü dışsallaştırır.**

---

## 9. YAPMAYACAKLARIMIZ

Bunlarda başarısız olmak, hiç denememekten **daha kötü** görünür.

### Rendering

| Yapma | Neden |
|---|---|
| **ACES tonemapping** | Doygun kırmızıları clipping'e yakın turuncuya kaydırır — sanat yönetiminin tek kasıtlı rengini, tam da aydınlandığı anda öldürür |
| **Forward+ / GPU Resident Drawer / GPU Occlusion Culling** | En çok cargo-cult edilen Unity 6 tavsiyesi. ~6 ışığın altında Forward+ Forward'dan pahalı; GRD Forward+ ister ve tek odalık, CPU-bound olmayan seviyede hiçbir şey satın almaz |
| **Deferred / Deferred+** | Tile-based mobil GPU'da G-buffer bandwidth felaketi |
| **Opaque Texture açık** | StoreAndResolve olmayan mobil platformlarda **MSAA'yı sessizce runtime'da yok saydırır.** URP mobilde günlerce debug edilen bir numaralı görünmez hata |
| **Depth Priming Auto** | PC/konsolda kazanç, tile GPU'da kayıp. Disabled |
| **Bokeh Depth of Field** | Pahalı mod, ve flagship'te maliyetini göremezsin. Gaussian |
| **Chromatic Aberration / Lens Distortion / Motion Blur / Panini / SMAA** | §6'daki tablo. Lens Distortion ayrıca **hotspot'larının ekran konumlarını kaydırır** |
| **Auto exposure** | Elle yazılmış grade'i sürekli yeniden normalize eder |
| **Custom shader'da procedural noise grain** | Adreno 540'ta 0.8 ms+. Unity'nin precomputed Film Grain'ini kullan |
| **Raymarched volumetric fog** | URP'de native yok, mobilde kötü bahis. Mesh şaft + legacy sis |
| **Realtime GI / SSR / planar reflection / ayna** | Baked reflection probe + box projection, nokta. (Real-time GI 6.7 LTS'de geliyor — plan yapma) |
| **Varsayılan 50 m shadow distance + 4 cascade** | 12 m'lik bir odada var olmayan geometriyi kaplamak için 4 shadow map render'ı harcıyorsun |
| **Sert kenarlı gölgeler** (Soft Shadows kapalı) | En güvenilir "bu bir mobil oyun" sinyallerinden biri. Low kalitede tut |

### İçerik

| Yapma | Neden |
|---|---|
| **Yakın planda gerçekçi ten** | URP Lit'te SSS yok; sıkı yakın planda mumyalanmış manken gibi okunur. **Bu projedeki en yüksek riskli plan tipi.** Omuz/orta plan tercih et, güçlü yan key kullan, kenarı rim'le, ve kurguyu sömür: maske, atkı, kapüşon, tipide nefes buharı, kir ve kan yüzü meşru olarak parçalar ve **bedava anlatısal doku** |
| **Uzun / gevşek saç** | Alpha sıralaması mobilde kötü. Bere, kapüşon, kask, kısa saç, toplanmış saç — post-apokaliptik kış bunu tamamen doğal kılıyor |
| **Kumaş simülasyonu** | Kıyafeti rijit geometri olarak modelle. En fazla ceket eteğinde 2-3 elle yazılmış jiggle kemiği |
| **Gerçekçi su** | Islak zemin (roughness + normal + koyulaştırılmış albedo), decal su birikintileri, statik buz. **Barajın zaten donmuş — bunu kucakla** |
| **Kalabalık** | Ekranda maks 2-4 karakter. **Post-apokalips boşluğu her türden daha iyi meşrulaştırıyor** |
| **Fluid-sim ateş / volumetrik duman** | Flipbook VFX + titreyen ışık + additive şaft mesh'i |
| **Dinamik gün döngüsü** | Sahne başına sabit, baked. Alacakaranlık alacakaranlık kalır. Bake'in mümkün olmasının sebebi de bu |
| **Yıkım fiziği / ragdoll** | Önceden yazılmış yıkık durumlar, animasyonlu ölümler |
| **Geniş manzaralar** | Her geniş görüş bir çizim mesafesi ve detay yoksulluğu testi. Baraj tek maruziyet, tipi sisi çözüyor |
| **Replik başına yüz duygu key'i** | İyi lipsync + ölü gözler arasındaki uncanny boşluk, stilize kısıtlamadan kötü. **uLipSync/Rhubarb + göz kırpma + sakkad + head-look = %80** |
| **Mixamo karakterini kahraman yapmak** | Indie oyun oynayan herkes tarafından anında tanınır |
| **İç mekân için LOD** | 12 m odada hiçbir şey LOD0'ı geçmez. O zamanı `layerCullDistances`'a harca |
| **Dynamic Batching** | CPU-pahalı, SRP Batcher altında geçersiz |

### Kamera ve UI

| Yapma | Neden |
|---|---|
| **ThirdPersonFollow / OrbitalFollow** | Nişancı rig'leri, kamerayı karaktere ya da olmayan bir sağ analoğa bağlarlar |
| **Binding Mode = Lock To Target** | Kamera karakterle döner, dünya arkada döner, sabit-kamera sineması yok olur |
| **Fixed sinematik kamerada Deoccluder** | Kamerayı fiziksel olarak kaydırır — kilitli planın yapmaması gereken tam olarak o hareket. ShotQualityEvaluator + ClearShot |
| **Serbest orbit kamera** | Her duvarı ve tavanı sunulabilir yapmayı zorunlu kılar — ödeyemeyeceğin maliyet |
| **QTE girdi penceresi sırasında kesme** | Oyuncu prompt'u kaybeder ve oyunu suçlar |
| **Sürekli açık el kamerası Perlin noise'u** | 30 cm'deki telefonda mide bulandırır, her dokuyu titretir |
| **UI'da, VFX'te, hasar geri bildiriminde, hotspot'ta KIRMIZI** | Aksan cihazını kaybedersin |
| **Saf beyaz (#FFFFFF) metin** | Doygunluğu alınmış soğuk bir görüntüde debug overlay gibi okunur. **`#E6E0D6` sıcak kırık beyaz** |
| **Türkçe glyph kapsamı olmayan font** | Birçok display/grunge font sessizce **ı İ ğ Ğ ş Ş** taşımıyor. TMP kelime ortasında tofu ya da başka bir yüze fallback yapar. **TMP atlasını kurmadan ÖNCE kapsamı doğrula** ve atlası tam Türkçe aralığını + tipografik tırnak + üç nokta içeren açık bir karakter setiyle kur. Kendi dilinde tofu, hiçbir render kalitesinin telafi edemeyeceği bir güvenilirlik kaybı |
| **Cinemachine 2 API isimleri** | 6.5'ten itibaren destek dışı, namespace ve bileşen isimlerinin yarısı değişti |
| **60 fps kovalamak** | Görüntü kalitesi bütçeni yarıya indirir, ısıyı ikiye katlar, ve 12 dakika sonra zaten ~38 fps'e düşersin |
| **Beş kalite kademesi** | Tek kişilik projelerin ölüm sebebi. İki |

### Süreç

| Yapma | Neden |
|---|---|
| **Bilinen kamera pozisyonları yerine tüm seviyeyi detaylandırmak** | Elindeki en büyük işgücü tasarrufu. Kameranın hiç kadraja almadığı her şey greybox + sis perdesi alır, sıfır görsel kalite kaybıyla |
| **Prop'ları eşit dağıtmak** | Eşit dağılım prosedürel gürültü gibi okunur. **3-5 prop'luk hikâye kümeleri** halinde dizil, aralarında boşluk bırak. Her odaya üç soru sor: **kim buradaydı, ne istiyordu, neden başaramadı?** Cevap vermiyorsa dekorasyondur, hikâye anlatımı değil |
| **Tek bir pack'i bütün bir oda olarak kullanmak** | Anında teşhis edilir |
| **Editörde ya da flagship'te test etmek** | Ve sadece soğuk test etmek. **10-15 dakika sonra sürekli performans %30-50 düşük** |
| **Sahne #1 ile sahne #6'yı farklı beceri seviyelerinde ve ortak template olmadan yapmak** | Eşitsiz kalite, tek kişilik 3D projelerin amatör okunmasının bir numaralı sebebi. **Oyun en kötü karesiyle yargılanır** |
| **Assets klasörünü public repoya koymak** | Satın aldığın her varlığın yeniden dağıtımı |

---

## 10. CİHAZDA DOĞRULANACAKLAR

Bunların hiçbirine güvenme — **floor cihazında doğrula.** Belirsizlik, bu belgedeki tavsiyeden değil, bu oturumda Unity dokümantasyon sitesinin egress proxy tarafından bloklanmış olmasından geliyor.

| # | Doğrulanacak | Nasıl |
|---|---|---|
| 1 | **On-tile PP gerçekten devrede mi** | Render Graph Viewer, **cihazda**. Tile-Only Mode açıkken validation uyarısı almalısın. Renderer Feature'ın 6000.5.9f1'deki tam adını ve inspector konumunu editörde teyit et |
| 2 | **LUT'un sRGB checkbox'ı** | Nötr LUT testi (§6). İki dakika, kesin cevap |
| 3 | **MSAA + HDR düşük seviye Adreno'da** | **Adreno 618'de URP'de HDR ve MSAA birlikte açıkken çökme raporu var.** Tüm pipeline buna bağımlı hale gelmeden ERKEN test et |
| 4 | **MSAA + on-tile PP etkileşimi** | MSAA sadece backbuffer'da destekleniyor, intermediate buffer'da değil; on-tile PP framebuffer fetch ile backbuffer'a yazıyor. Tier B'nin MSAA 2×'i on-tile yolu kırıyor mu? |
| 5 | **STP vs MSAA** | 0.75 + STP'yi 0.85 + MSAA 2× + FXAA'ya karşı test et. **Hem kare süresini HEM hareket halindeki algılanan keskinliği** karşılaştır. Yavaş sinematik kameranın ghosting'i bastırması gerekir ama **bu görülmeli, varsayılmamalı.** STP'nin orta seviye Android'de bağımsız benchmark'ı yok — bu, listedeki en büyük açık soru |
| 6 | **Lightmap 12 vs 24 texel/birim** | Bir market odasını her ikisinde bake et, bellek ve görüntü karşılaştır, sonra standartlaştır |
| 7 | **Lightmap Encoding Android default'u** | dLDR'a düşüyorsa RGBM'e al, cihazda gözle karşılaştır |
| 8 | **APV vs klasik Light Probe Group maliyeti** | APV'de performans regresyonu bildiren topluluk raporları var. Taahhüt etmeden profille |
| 9 | **GPU Skinning A/B** | Varsayılan CPU. Dokümante edilmiş Android regresyonlarının 6.5'te çözülüp çözülmediği belirsiz |
| 10 | **Koyu mavi gradyanlarda banding** | Grain çözmüyorsa **HDR Precision'ı sadece Tier A'da 64-bit** yap |
| 11 | **15 dakikalık termal soak** | Kılıfsız, %50 parlaklık, aydınlık oda. **Herhangi bir telefon iki dakika iyi sayı verir** |
| 12 | **Tipi overdraw'ı** | Rendering Debugger overdraw görünümü, 2.0× ortalama sınırı |
| 13 | **Türkçe VO'da viseme doğruluğu** | Rhubarb/uLipSync İngilizce fonetiği üzerine eğitilmiş. Pipeline'ı kurmadan gerçek bir klip test et |
| 14 | **Volume Profile geçişinde hitch** | Per-shot profile ilk aktivasyonunda takılma varsa **seviye yüklenirken tüm profilleri pre-warm et** |
| 15 | **Bloom / DoF / SSAO gerçek ms maliyeti** | §7'deki tüm ms rakamları tahmin. Snapdragon Profiler / Arm Performance Studio ile kendi içeriğinde ölç |
| 16 | **Variable Rate Shading** | Unity 6.5'in Android GPU'larda URP üzerinden VRS sunup sunmadığı doğrulanamadı. **Ağır çevresel sis ve vignette'i olan bir oyun için potansiyel olarak büyük bedava kazanç** — kontrol etmeye değer |
| 17 | **CC5 Game Base poligon/kemik sayıları** | 5.6k poly / 72 bone rakamları CC3/CC4 için dokümante; CC5'e aynen taşındığı doğrulanamadı. ARKit blendshape export'unun Unity 6.5 URP'de davranışı da |
| 18 | **Play Console boyut limitleri 2026** | AAB base module 200 MB ve Play Asset Delivery pack limitleri için güncel dokümantasyona bak |

---

## 11. İLK OTUZ GÜN

Sıra önemli. Her adım bir sonrakini bilgilendiriyor.

**Hafta 1 — Ölçüm altyapısı.** Floor cihazı al. Boş bir 6000.5.9f1 URP projesi kur: Vulkan-only, Forward, HDR 32-bit, Render Graph açık, on-tile PP Renderer Feature + Tile-Only Mode, entegre post kapalı. Bootstrap script'i (30 fps). Cihaza build et. **Render Graph Viewer'da on-tile yolun aktif olduğunu doğrula.** Nötr LUT testini yap. MSAA+HDR çökme testini yap.

**Hafta 2 — Master shader ve LookDev.** `BAGISIK_Env_Lit` ve `BAGISIK_Char_Lit` Shader Graph'larını yaz. `00_LookDev` sahnesini kur (gri küre, manken, kapı, checker küpü). Materyal kütüphanesini dizil. Palet belgesini yaz ve repoya commit et.

**Hafta 3-6 — Benzin istasyonu dikey dilimi.** Beş lokasyonun **en küçüğü, en kontrastlısı, en karanlığı ve pahalı görünmek için en az varlık gerektireni.** Ücretsiz varlıklarla, tam kalitede: bake edilmiş aydınlatma, LUT, remaplenmiş materyaller, bitirilmiş dekor, içinde yürüyen bir karakter, bir diyalog sahnesi, bir QTE, floor cihazda hedef kare hızında.

**Hafta 7 — Artefaktları çıkar.** Sadece o oda **bittiğinde**: master materyal kütüphanesini çıkar, house LUT'u onun karelerinden yaz, scene template'i kur, sign-off checklist'ini yaz. **Dördü de soyutta icat edilmiş değil, gerçekten işe yaramış bir odadan türetilmiş.**

Sonra kalan lokasyonlar **keşif değil, uygulama.** İlk odayı altıncının 3-5 katı zamanla bütçele.

**İkinci erken sahne: tipi/baraj.** Ters sebeple — sis işin çoğunu yapıyor, yani orantısız etkileyici bir sonucu hızlı veriyor. **Tek kişilik bir projede beklenenden iyi çıkan en az bir erken sahneye ihtiyacın var.**

---

## 12. KARAR DEFTERİ

Araştırmacılar çeliştiğinde ne karar verdim ve neden. **Buradaki bir kararı değiştirirsen satırı güncelle ve tarih at.**

| Konu | Çelişki | **KARAR** | Sebep |
|---|---|---|---|
| Tonemapper | 3 lens Neutral, 1 lens ACES | **Neutral** | ACES doygun kırmızıyı turuncuya kaydırıyor; kapüşon tüm kimlik |
| SSAO | "En yüksek değerli 2 efektten biri" vs "atla, AO'yu bake et" | **Tier A açık, Tier B kapalı** | Bake AO statik-statik teması çözer, **dinamik karakteri zemine oturtamaz.** Tier B blob shadow alır |
| Depth of Field | "Kullan" vs "mobilde pratikte yok, sisle taklit et" | **Sadece Gaussian, sadece diyalog state'inde** | Sabit kadrajlı, statik sahnede ödenebilir; yürürken kapalı. Bokeh yasak |
| Bloom | Global vs per-shot lüks | **Lokasyon Volume'ünde**, tipide kapalı | Tile yolunu kırıyor; bilinçli aç, bütçesi ölçülmüş |
| Sis modu | Exponential Squared vs Linear | **Linear** | End mesafesini kitbash'inin bittiği yerin hemen ötesine koyabiliyorsun. Sanat yönetimi kontrolü |
| Lightmap çözünürlüğü | 5-10 vs 20-40 vs 25-40/10-15 texel/birim | **24 hero / 12 ikincil / Scale In Lightmap ile geri kalan** | Küçük seviye avantajı gerçek, ama flat 30 israf. Cihazda 12 vs 24 doğrula |
| Directional lightmap | "Pazarlıksız" vs "ölç" | **Açık** | Yarı-gerçekçilik normal map'lerin bake ışığa tepki vermesine bağlı |
| Oyun içi FOV | Yatay 45-60° vs vFOV 28-32° | **vFOV 28° (= yatay 57°)** | İki aralığın kesişimi; telefon en-boyu için hesaplanmış |
| Film Grain | "Kaçın" vs "her zaman açık" | **Açık, 0.28** | Çelişki yoğunlukta. R11G11B10 + soğuk gradyan paletinde banding'e karşı zorunlu |
| Renk filtresi | Per-lokasyon Color Filter vs sadece LUT | **Sadece LUT** | Tek doğruluk kaynağı, versiyon kontrollü, altı ay sonra kaymaz |
| Shader sayısı | "Tek master shader" | **İki** (çevre + karakter) | Karakter wrapped-diffuse ve rim istiyor; duvar istemiyor. 2 variant SRP Batcher için sorun değil |
| Referans cihaz | SD 680/Adreno 610 vs SD 6 Gen 3/Adreno 810 | **İkisini de al, floor'da profille** | Floor doğruluk kaynağı, orta cihaz Tier A doğrulaması |
| Grafik API | Vulkan-only vs Vulkan + GLES fallback | **Vulkan-only, min API 30** | On-tile PP, STP, APV per-pixel hepsi buna bağlı; %10-12 daha az güç = daha az throttling |
| Kamera nudge | Sabit kilit vs oyuncu payı | **±12° yaw / ±8° pitch, 1.2 s'de geri** | Detroit'in çözümü. Kompozisyonu korur, oyuncuya ajans verir, önplan engelleyicisinin arkasına bakma imkânı |

---

*Bu belgeyi değiştiren her karar §12'ye yazılır. Yazılmamış karar, altı ay sonra hatırlanmayan karardır.*