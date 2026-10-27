# Google E-Tablo Kurulumu (Yorum Veritabanı)

Uygulama yorumları artık Google E-Tablo'da saklıyor. Streamlit Cloud, uygulama
uyuyunca ya da yeniden açılınca kendi içindeki dosyaları siliyordu; tablo silinmez.

Kurulum bir kere yapılır, yaklaşık 15 dakika sürer. Kurulum bitene kadar uygulamada
**"Google E-Tablo ayarı bulunamadı"** uyarısı görünür; bu normaldir.

> ⚠️ Bu depo herkese açıktır. Bu dosyaya ya da depoya **asla** şifre, JSON anahtarı
> veya tablo adresi yazmayın. Gizli bilgiler yalnızca Streamlit Secrets'a girilir.

---

## Bölüm 0 — Google hesabında 2 adımlı doğrulamayı açın

Google, 2 Ekim 2026'dan beri Google Cloud'u kullanmak için 2 adımlı doğrulama istiyor.
Google Cloud'da **"Google Cloud access blocked"** yazısı görüyorsanız önce bunu yapın.

1. https://myaccount.google.com/signinoptions/two-step-verification adresini açın
   (ya da Google Cloud'daki mavi **Enable MFA** butonuna basın).
2. Telefon numaranızla 2 adımlı doğrulamayı açın.
3. Birkaç dakika bekleyip Google Cloud sayfasını yenileyin. "access blocked" yazısı gitmeli.

## Bölüm 1 — Tabloyu oluşturun

1. https://sheets.new adresini açın (işletmenin Google hesabıyla).
2. Sol üstteki "Adsız e-tablo" yazısına tıklayıp adını **Yorum Veritabanı** yapın.
3. Adres çubuğundaki bağlantıyı kopyalayıp bir Not Defteri'ne yapıştırın.
   Bu, **tablo adresi**dir.

Tabloya elle hiçbir şey yazmanıza gerek yok. Uygulama gereken sayfaları kendisi açar:

| Sayfa | Ne var? |
|---|---|
| Google Yorumları | Google tarafında Excel/CSV ile yüklenen yorumlar (analize katılır) |
| Google API Yorumları | "VERİLERİ ÇEK" ile gelen yorumların arşivi (yalnızca saklanır) |
| TripAdvisor Yorumları | TripAdvisor Excel'iyle yüklenen yorumlar |

## Bölüm 2 — Google Cloud'da "robot hesap" (servis hesabı) açın

1. https://console.cloud.google.com adresini açın.
   - Üstte **"Start your Free Trial"** teklifi çıkarsa **Dismiss** deyin. Kart bilgisi
     girmeyin; tablo bağlantısı ücretsizdir.
2. **Yeni proje açın:** Üstteki **Select a project** kutusuna basın → sağ üstte
   **New project** → Project name: `yorum-veritabani` → **Create**.
   30 saniye bekleyip üstteki kutudan `yorum-veritabani` projesini seçin.
   (Listede "Default Gemini Project" gibi başka projeler olması sorun değil.)
3. **Google Sheets API'yi açın:**
   https://console.cloud.google.com/apis/library/sheets.googleapis.com adresini açın,
   üstte `yorum-veritabani` seçili olsun, **Enable / Etkinleştir**'e basın.
4. **Robot hesabı oluşturun:**
   https://console.cloud.google.com/iam-admin/serviceaccounts adresini açın →
   **+ Create service account / Hizmet hesabı oluştur** → ad: `yorum-veritabani` →
   **Create and continue / Oluştur ve devam et** → hiçbir şey seçmeden **Done / Bitti**.
5. **Anahtar dosyasını indirin:** Listede yeni hesaba tıklayın → üstten
   **Keys / Anahtarlar** sekmesi → **Add key → Create new key / Anahtar ekle → Yeni anahtar
   oluştur** → **JSON** → **Create / Oluştur**. Bilgisayara bir `.json` dosyası iner.
   - ⚠️ Bu dosya bir şifredir. Kimseyle paylaşmayın, GitHub'a koymayın, e-postayla
     göndermeyin.
   - "Key creation is disabled / anahtar oluşturma engellendi" gibi bir mesaj çıkarsa:
     hesabın güvenlik kuralı engelliyordur. Mesajı not alın, birlikte çözeriz.

## Bölüm 3 — Tabloyu robot hesapla paylaşın

1. İnen `.json` dosyasına sağ tıklayın → **Birlikte aç → Not Defteri**.
2. `"client_email"` satırındaki adresi kopyalayın (sonu `...iam.gserviceaccount.com`).
3. Bölüm 1'deki tabloda sağ üstteki **Paylaş**'a basın → adresi yapıştırın →
   yetki **Düzenleyen** → **Gönder**.

## Bölüm 4 — Bilgileri Streamlit'e girin

1. https://share.streamlit.io adresinde uygulamanın yanındaki **⋮ → Settings → Secrets**
   bölümünü açın.
2. Kutunun **en üstüne** (mevcut satırların üstüne) aşağıdakini yazın:
   - Tırnakların arasına: Bölüm 1'deki tablo adresi.
   - İki `'''` arasına: Not Defteri'nde açtığınız JSON dosyasının **tamamı**
     (Ctrl+A, Ctrl+C, Ctrl+V). Hiçbir yerini değiştirmeyin.

   ```toml
   GSHEETS_URL = "tablo adresi"
   GSHEETS_SERVICE_ACCOUNT_JSON = '''
   JSON dosyasının tamamı
   '''
   ```

3. Dikkat:
   - Adlar büyük harfle, birebir aynı yazılmalı.
   - Bu iki satır, köşeli parantezli başlıkların (`[...]`) **üstünde** durmalı.
   - Kapanış `'''` ayrı bir satırda olmalı.
4. **Save changes**'e basın. Kırmızı bir hata (ör. "Invalid TOML") çıkarsa kayıt
   **yapılmamıştır**; genellikle `'''` işaretlerinden biri eksiktir.

## Bölüm 5 — Kontrol

1. Yaklaşık 1 dakika bekleyin, uygulamayı yenileyip giriş yapın.
2. **TripAdvisor Yorumları** sekmesine geçin. Kırmızı uyarı yoksa bağlantı çalışıyor.
3. TripAdvisor Excel'inizi seçip **Trip DB'ye Kaydet**'e basın.
4. **Google Yorumları** sekmesinde tesis seçip **VERİLERİ ÇEK**'e basın.
5. Tabloyu açın: "TripAdvisor Yorumları" ve "Google API Yorumları" sayfaları ve içlerinde
   yorumlar görünmeli.

### Uygulamadaki uyarılar ne demek?

| Uyarı | Ne yapılmalı? |
|---|---|
| Google E-Tablo ayarı bulunamadı | Bölüm 4 yapılmamış ya da kaydedilmemiş. Mesajda "[...] başlığının altında kalmış" veya "... yazılmış, ... olmalı" diyorsa onu düzeltin. |
| GSHEETS_SERVICE_ACCOUNT_JSON okunamadı | JSON `'''` işaretlerinin arasına tam yapıştırılmamış. |
| GSHEETS_SERVICE_ACCOUNT_JSON eksik ya da bozuk | JSON dosyasının bir kısmı kopyalanmamış. Tamamını yeniden yapıştırın. |
| Tablo açılamadı … paylaşıldığından emin olun | Bölüm 3 yapılmamış ya da tablo adresi yanlış. |
| Google E-Tablo'ya bağlanılamadı … Google Sheets API açık olmalı | Bölüm 2.3 yapılmamış ya da API başka projede açılmış. |
| '…' sayfasında şu sütun başlıkları bulunamadı | Tablonun ilk satırındaki başlıklar değiştirilmiş. Eski hâline getirin. |

---

## Bilinmesi gerekenler

- Tablonun **ilk satırındaki başlıkları değiştirmeyin**. Sütunların yerini değiştirmek
  sorun değil.
- Tabloda elle yapılan değişiklikler uygulamada en geç **10 dakika** sonra görünür.
- Tablo ilk açıldığında boştur. Elinizdeki Excel dosyalarını **bir kez** yeniden yükleyin.
- Uygulamadaki **DB'yi Temizle / Trip DB Temizle** butonları tablodaki ilgili sayfayı boşaltır.
- Uygulamayı kendi bilgisayarınızda çalıştırırken aynı iki satırı
  `ömer/src/.streamlit/secrets.toml` dosyasına yazın (bu dosya GitHub'a gitmez).
- Kodda ilgili yerler: `ömer/src/sheets_db.py` (tablo bağlantısı) ve `ömer/src/main.py`
  içindeki `db_...` fonksiyonları.
