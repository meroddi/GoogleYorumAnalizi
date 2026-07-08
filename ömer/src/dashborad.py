import streamlit as st
import pandas as pd
from io import BytesIO
import os

# ==========================================
# 1. AYARLAR
# ==========================================
st.set_page_config(page_title="Google Yorum Dashboard", page_icon="🚀", layout="wide")
DOSYA_ADI = "google_yorumlari.csv"

# ==========================================
# 2. YARDIMCI FONKSİYONLAR
# ==========================================
def tarih_etiketi_olustur(tarih_obj, mod):
    # Tarih objesi string gelirse dönüştür
    if isinstance(tarih_obj, str):
        tarih_obj = pd.to_datetime(tarih_obj)
        
    aylar = {1: 'Ocak', 2: 'Şubat', 3: 'Mart', 4: 'Nisan', 5: 'Mayıs', 6: 'Haziran',
             7: 'Temmuz', 8: 'Ağustos', 9: 'Eylül', 10: 'Ekim', 11: 'Kasım', 12: 'Aralık'}
    
    if mod == "Günlük":
        return f"{tarih_obj.day} {aylar[tarih_obj.month]} {tarih_obj.year}"
    elif mod == "Haftalık":
        hafta_no = (tarih_obj.day - 1) // 7 + 1
        if hafta_no > 4: hafta_no = 4
        return f"{aylar[tarih_obj.month]} {hafta_no}. Hafta"
    elif mod == "Aylık":
        return f"{aylar[tarih_obj.month]} {tarih_obj.year}"
    return str(tarih_obj)

def excel_indir(df, puan_tablosu):
    output = BytesIO()
    with pd.ExcelWriter(output, engine='xlsxwriter') as writer:
        df.to_excel(writer, sheet_name='Tum_Veriler', index=False)
        puan_tablosu.to_excel(writer, sheet_name='Puan_Analizi')
    return output.getvalue()

# ==========================================
# 3. ARAYÜZ
# ==========================================
st.title("📊 Google Yorum Analiz Paneli (Live Dashboard)")

# Veri Kontrolü
if not os.path.exists(DOSYA_ADI):
    st.error(f"⚠️ '{DOSYA_ADI}' dosyası bulunamadı! Lütfen önce 'veri_hazirla.py' scriptini çalıştırın.")
    st.stop()

# Veriyi Hızlıca Oku
try:
    df = pd.read_csv(DOSYA_ADI)
    # Tarih sütunu kontrolü
    if 'review_datetime_utc' in df.columns:
        df['review_datetime_utc'] = pd.to_datetime(df['review_datetime_utc'])
    else:
        st.error("CSV dosyasında tarih sütunu bulunamadı. Veri hatalı olabilir.")
        st.stop()
except Exception as e:
    st.error(f"Dosya okunurken hata oluştu: {e}")
    st.stop()

# METRİKLER
c1, c2, c3 = st.columns(3)
c1.metric("Toplam Yorum", len(df))
c2.metric("Ortalama Puan", f"{df['review_rating'].mean():.2f}")
c3.metric("Kötü Yorumlar (≤3)", len(df[df['review_rating'] <= 3]))

st.divider()

# FİLTRELER
col_secim, _ = st.columns([1, 4])
with col_secim:
    zaman_modu = st.selectbox("⏳ Zaman Analizi:", ["Haftalık", "Günlük", "Aylık"])

# ANALİZ MOTORU
df_analiz = df.copy()
df_analiz = df_analiz.sort_values('review_datetime_utc', ascending=False)
df_analiz['Zaman'] = df_analiz['review_datetime_utc'].apply(lambda x: tarih_etiketi_olustur(x, zaman_modu))

puan_tablosu = df_analiz.groupby(['Zaman', 'review_rating'], sort=False).size().unstack(fill_value=0)

# Sütun düzenleme (1-5 arası eksikleri tamamla)
for i in ["1", "2", "3", "4", "5", 1, 2, 3, 4, 5]: # String veya int olabilir
    if i not in puan_tablosu.columns: 
        # Sadece int olanları ekle (temizlik)
        if isinstance(i, int): puan_tablosu[i] = 0

# Sadece sayısal sütunları alıp sıralayalım
mevcut_kolonlar = [c for c in puan_tablosu.columns if str(c) in ["1","2","3","4","5"]]
puan_tablosu = puan_tablosu[mevcut_kolonlar]

# İsimlendirme
puan_tablosu.columns = [f"{c} Yıldız" for c in puan_tablosu.columns]
puan_tablosu['Toplam'] = puan_tablosu.sum(axis=1)

# TABLAR
tab1, tab2, tab3 = st.tabs(["📄 Puan Tablosu", "🚨 Şikayet Detayları", "📋 Tüm Veriler"])

with tab1:
    st.dataframe(puan_tablosu, use_container_width=True)

with tab2:
    st.subheader("Filtrelenmiş Şikayetler")
    sikayet_df = df_analiz[df_analiz['review_rating'] <= 3].copy()
    
    if not sikayet_df.empty:
        cats = ["Tümü"] + list(sikayet_df['category'].unique())
        secilen_cat = st.selectbox("Kategoriye Göre Süz:", cats)
        
        if secilen_cat != "Tümü":
            sikayet_df = sikayet_df[sikayet_df['category'] == secilen_cat]
            
        view_df = sikayet_df[['Zaman', 'author_title', 'review_rating', 'review_text_tr', 'review_text', 'category']]
        st.dataframe(view_df, use_container_width=True, hide_index=True)
    else:
        st.success("Şikayet kaydı yok.")

with tab3:
    st.dataframe(df_analiz, use_container_width=True)

# EXCEL İNDİRME
st.divider()
excel_data = excel_indir(df_analiz, puan_tablosu)
st.download_button(
    label="📥 Raporu İndir",
    data=excel_data,
    file_name="Analiz_Raporu.xlsx",
    mime="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    type="primary"
)