import pandas as pd
import requests
import concurrent.futures
import time
from datetime import datetime
from deep_translator import GoogleTranslator

# tqdm (İlerleme Çubuğu) kütüphanesi kontrolü
try:
    from tqdm import tqdm
except ImportError:
    def tqdm(iterable, **kwargs): return iterable

# ==========================================
# ⚙️ AYARLAR (BURAYI DOLDURMALISIN)
# ==========================================
API_KEY = "BURAYA_KENDI_API_ANAHTARINI_YAZ"
PLACE_ID = "BURAYA_MEKAN_PLACE_ID_YAZ"

DOSYA_ADI = "google_yorumlari.csv"
MAX_YORUM_LIMITI = 1000  # Güvenlik için limit (İstersen 4000 yap)

# ==========================================
# 1. MODÜL: GOOGLE API VERİ ÇEKİCİ (FETCHER)
# ==========================================
def google_yorumlari_getir(api_key, place_id, limit):
    print(f"📡 Google API'ye bağlanılıyor... (Hedef: {limit} yorum)")
    
    reviews = []
    # Google API URL'i (Sadece yorumları ve yazarı istiyoruz)
    url = f"https://maps.googleapis.com/maps/api/place/details/json?place_id={place_id}&fields=reviews&key={api_key}&language=tr"
    
    page_count = 0
    
    while True:
        try:
            response = requests.get(url).json()
            
            # Hata kontrolü
            if 'error_message' in response:
                print(f"❌ API Hatası: {response['error_message']}")
                break
                
            if 'result' in response and 'reviews' in response['result']:
                yeni_gelenler = response['result']['reviews']
                reviews.extend(yeni_gelenler)
                page_count += 1
                print(f"   -> Sayfa {page_count} çekildi. (Toplam: {len(reviews)} yorum)")
            
            # Limit kontrolü
            if len(reviews) >= limit:
                print("🛑 İstenen limite ulaşıldı.")
                break

            # Pagination (Sonraki Sayfa) Kontrolü
            if 'next_page_token' in response:
                token = response['next_page_token']
                
                # ÖNEMLİ: Google, token oluştuktan sonra aktif olması için 
                # kısa bir süre beklemeyi şart koşar. Beklemezsek "Invalid Token" hatası alırız.
                time.sleep(2) 
                
                url = f"https://maps.googleapis.com/maps/api/place/details/json?pagetoken={token}&key={api_key}"
            else:
                print("✅ Tüm sayfalar tarandı.")
                break
                
        except Exception as e:
            print(f"⚠️ Bir hata oluştu: {e}")
            break
            
    # Listeyi DataFrame'e çevir
    df = pd.DataFrame(reviews)
    
    # Sütun isimlerini bizim sisteme uydur (Normalization)
    if not df.empty:
        df = df.rename(columns={
            'author_name': 'author_title',
            'rating': 'review_rating',
            'text': 'review_text',
            'time': 'timestamp' # Geçici isim
        })
        # Unix Timestamp'i datetime'a çevir
        df['review_datetime_utc'] = pd.to_datetime(df['timestamp'], unit='s')
        
    return df

# ==========================================
# 2. MODÜL: İŞLEME VE ÇEVİRİ MOTORU (WORKER)
# ==========================================
def tekil_satir_isle(row):
    translator = GoogleTranslator(source='auto', target='tr')
    
    keywords = {
        'Yemek': ['yemek', 'lezzet', 'soğuk', 'tuzlu', 'bayat', 'food', 'taste', 'et', 'tavuk', 'pişmemiş'],
        'Personel': ['garson', 'personel', 'kaba', 'ilgisiz', 'yavaş', 'staff', 'waiter', 'hizmet', 'suratsız'],
        'Temizlik': ['kirli', 'pis', 'böcek', 'hijyen', 'dirty', 'clean', 'kokuyor', 'tozlu'],
        'Fiyat': ['pahalı', 'fiyat', 'hesap', 'expensive', 'price', 'lüzumsuz', 'kazık'],
        'Ortam': ['gürültü', 'ses', 'müzik', 'dar', 'karanlık', 'atmosphere', 'mekan', 'basık']
    }

    text = str(row['review_text'])
    puan = row['review_rating']
    
    ceviri = text
    
    # Optimizasyon: Sadece düşük puanlı ve dolu yorumları çevir
    if puan <= 3 and len(text) > 3:
        try:
            # Google Translate IP ban yememek için minik gecikme (Throttling)
            # time.sleep(0.1) 
            ceviri = translator.translate(text)
        except:
            ceviri = text 
    
    assigned_cat = "Diğer / Belirsiz" if puan <= 3 else "-"
    
    if puan <= 3:
        lower_text = ceviri.lower()
        for cat, keys in keywords.items():
            if any(k in lower_text for k in keys):
                assigned_cat = cat
                break
        
    return {
        'review_text_tr': ceviri,
        'category': assigned_cat
    }

# ==========================================
# ANA İŞLEM (PIPELINE)
# ==========================================
def main():
    print("🚀 API ENTEGRASYON MODU BAŞLATILIYOR...")
    
    # 1. API'den Ham Veriyi Çek
    if API_KEY == "BURAYA_KENDI_API_ANAHTARINI_YAZ":
        print("❌ HATA: Lütfen kodun başındaki API_KEY ve PLACE_ID alanlarını doldur!")
        return

    ham_df = google_yorumlari_getir(API_KEY, PLACE_ID, MAX_YORUM_LIMITI)
    
    if ham_df.empty:
        print("⚠️ Hiç veri çekilemedi. API Key veya Place ID hatalı olabilir.")
        return

    print(f"\n🔄 {len(ham_df)} adet yorum işleniyor ve çevriliyor (Multithreading)...")
    
    rows = ham_df.to_dict('records')
    results = []
    
    # 2. Veriyi İşle (Parallel Processing)
    with concurrent.futures.ThreadPoolExecutor(max_workers=10) as executor:
        futures = {executor.submit(tekil_satir_isle, row): row for row in rows}
        
        for future in tqdm(concurrent.futures.as_completed(futures), total=len(rows), unit="yorum"):
            results.append(future.result())
    
    # 3. Sonuçları Birleştir
    processed_df = pd.DataFrame(results)
    ham_df['review_text_tr'] = processed_df['review_text_tr']
    ham_df['category'] = processed_df['category']
    
    # Gereksiz sütunları temizle
    if 'timestamp' in ham_df.columns:
        ham_df = ham_df.drop(columns=['timestamp'])
        
    # 4. Kaydet
    print(f"💾 Veriler '{DOSYA_ADI}' dosyasına kaydediliyor...")
    ham_df.to_csv(DOSYA_ADI, index=False)
    print("✅ İŞLEM TAMAMLANDI! Şimdi 'py -m streamlit run dashboard.py' komutunu çalıştırabilirsin.")

if __name__ == "__main__":
    main()