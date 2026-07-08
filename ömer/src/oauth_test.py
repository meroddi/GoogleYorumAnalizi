import streamlit as st

# --- basit giriş programı ---
# kullanıcı/giriş -> token eşlemesi
USER_TOKENS = {
    "admin": (
        "your-client-id",        # gerçek CLIENT_ID
        "your-client-secret",    # gerçek CLIENT_SECRET
        "your-refresh-token",    # gerçek REFRESH_TOKEN
    ),
    # başkaları ekleyin
}

if "logged_in" not in st.session_state:
    st.session_state.logged_in = False

if not st.session_state.logged_in:
    st.title("🔒 Giriş Ekranı")
    username = st.text_input("Kullanıcı adı")
    password = st.text_input("Şifre", type="password")
    if st.button("Giriş"):
        if username in USER_TOKENS and password == "12345":
            st.session_state.logged_in = True
            # tokenları sessiona ekle
            cid, csecret, rtoken = USER_TOKENS[username]
            st.session_state.oauth_client_id = cid
            st.session_state.oauth_client_secret = csecret
            st.session_state.oauth_refresh_token = rtoken
            st.experimental_rerun()
        else:
            st.error("Kullanıcı adı veya şifre yanlış")
    st.stop()

# main kodu aşağıya gelecek
import streamlit as st
import pandas as pd
import requests
import random
import concurrent.futures
import time
import re
from datetime import datetime, timedelta
from io import BytesIO
from typing import Dict, List, Optional, Tuple
import os

# ==========================================
# 1. AYARLAR
# ==========================================
st.set_page_config(page_title="Google Yorum Analizi", page_icon="📊", layout="wide")

# ==============================
# OAUTH BİLGİLERİ (KULLANICI GİRİŞİ)
# ==============================
with st.sidebar:
    st.markdown("## 🔑 OAuth Bilgileri")
    st.caption("Google yorumlarını çekmek için gerekli.")

    # Session state init
    if "oauth_client_id" not in st.session_state:
        st.session_state.oauth_client_id = ""
    if "oauth_client_secret" not in st.session_state:
        st.session_state.oauth_client_secret = ""
    if "oauth_refresh_token" not in st.session_state:
        st.session_state.oauth_refresh_token = ""

    # if tokens already filled (from login), show notice instead of inputs
    if st.session_state.oauth_client_id and st.session_state.oauth_client_secret and st.session_state.oauth_refresh_token:
        st.info("Tokenlar giriş bilgilerinden otomatik yüklendi.")
    else:
        st.session_state.oauth_client_id = st.text_input(
            "CLIENT_ID",
            value=st.session_state.oauth_client_id,
            type="password"
        )
        st.session_state.oauth_client_secret = st.text_input(
            "CLIENT_SECRET",
            value=st.session_state.oauth_client_secret,
            type="password"
        )
        st.session_state.oauth_refresh_token = st.text_input(
            "REFRESH_TOKEN",
            value=st.session_state.oauth_refresh_token,
            type="password"
        )

CLIENT_ID = st.session_state.oauth_client_id
CLIENT_SECRET = st.session_state.oauth_client_secret
REFRESH_TOKEN = st.session_state.oauth_refresh_token


def load_css(path: str):
    base_dir = os.path.dirname(os.path.abspath(__file__))
    css_path = os.path.join(base_dir, path)
    if os.path.exists(css_path):
        with open(css_path, "r", encoding="utf-8") as f:
            st.markdown(f"<style>{f.read()}</style>", unsafe_allow_html=True)


load_css("styles.css")

# ==========================================
# 2. MOTOR (VERİ ÇEKME & İŞLEME)
# ==========================================

def get_headers(access_token: str) -> Dict[str, str]:
    return {"Authorization": f"Bearer {access_token}"}


def access_token_from_refresh_token(
    client_id: str,
    client_secret: str,
    refresh_token: str
) -> Tuple[Optional[str], Optional[str]]:
    """Refresh token -> access token çevirir.
    Returns: (access_token, error_message)
    """
    try:
        url = "https://oauth2.googleapis.com/token"
        data = {
            "client_id": client_id,
            "client_secret": client_secret,
            "refresh_token": refresh_token,
            "grant_type": "refresh_token",
        }
        r = requests.post(url, data=data, timeout=30)
        js = r.json()
        if r.status_code != 200:
            return None, js.get("error_description") or js.get("error") or str(js)
        return js.get("access_token"), None
    except Exception as e:
        return None, str(e)


def list_accounts(access_token: str) -> List[Dict]:
    url = "https://mybusinessaccountmanagement.googleapis.com/v1/accounts"
    resp = requests.get(url, headers=get_headers(access_token), timeout=30).json()
    return resp.get("accounts", [])


FIXED_ACCOUNT_LABEL = "Meroddi Hotels"


def find_fixed_account_name(accounts: List[Dict]) -> Optional[str]:
    target = FIXED_ACCOUNT_LABEL.casefold()
    for account in accounts:
        account_label = (account.get("accountName") or "").strip()
        if account_label.casefold() == target and account.get("name"):
            return account.get("name")

    for account in accounts:
        searchable = f"{account.get('accountName', '')} {account.get('name', '')}"
        if target in searchable.casefold() and account.get("name"):
            return account.get("name")

    return None


def list_locations(access_token: str, account_name: str, page_size: int = 100) -> List[Dict]:
    """Business Information API ile lokasyonları sayfalamalı çeker."""
    all_locs: List[Dict] = []
    page_token = None
    base_url = f"https://mybusinessbusinessinformation.googleapis.com/v1/{account_name}/locations"
    while True:
        params = {
            "pageSize": page_size,
            "readMask": "name,title,storeCode,metadata",
        }
        if page_token:
            params["pageToken"] = page_token
        js = requests.get(base_url, headers=get_headers(access_token), params=params, timeout=60).json()
        all_locs.extend(js.get("locations", []))
        page_token = js.get("nextPageToken")
        if not page_token:
            break
    return all_locs


def yorumlari_getir_business_api(account_name: str, location_name: str, access_token: str) -> pd.DataFrame:
    all_reviews: List[Dict] = []
    page_token = None
    base_url = f"https://mybusiness.googleapis.com/v4/{account_name}/{location_name}/reviews"

    status_text = st.sidebar.empty()
    bar = st.sidebar.progress(0)
    sayfa = 0

    while True:
        status_text.text(f"Veriler çekiliyor... Sayfa: {sayfa + 1}")
        params = {"pageSize": 50}
        if page_token:
            params["pageToken"] = page_token

        try:
            resp = requests.get(base_url, headers=get_headers(access_token), params=params, timeout=60).json()
            if "reviews" in resp:
                all_reviews.extend(resp["reviews"])
                sayfa += 1
                bar.progress(min(sayfa * 5, 90))

            if "nextPageToken" in resp:
                page_token = resp["nextPageToken"]
                time.sleep(0.1)
            else:
                break
        except Exception:
            break

    bar.empty()
    status_text.success(f"✅ {len(all_reviews)} yorum çekildi.")

    processed: List[Dict] = []
    star_map = {
        "STAR_RATING_UNSPECIFIED": 0,
        "ONE": 1, "TWO": 2, "THREE": 3, "FOUR": 4, "FIVE": 5
    }

    for r in all_reviews:
        rating = star_map.get(r.get("starRating", "ONE"), 0)
        processed.append({
            "author_title": r.get("reviewer", {}).get("displayName", "Gizli Kullanıcı"),
            "review_rating": rating,
            "review_text": r.get("comment", ""),
            "review_datetime_utc": r.get("createTime", ""),
        })

    return pd.DataFrame(processed)


def fetch_reviews_for_locations(
    access_token: str,
    account_name: str,
    locations: List[Dict],
    parallel: bool = False
) -> pd.DataFrame:
    """Seçilen birden fazla location için tüm yorumları çeker ve tek DF döndürür."""
    loc_pairs = []
    for loc in locations:
        loc_name = loc.get("name")
        title = loc.get("title") or loc_name
        store_code = loc.get("storeCode")
        if loc_name:
            loc_pairs.append((loc_name, title, store_code))

    if not loc_pairs:
        return pd.DataFrame()

    sidebar_status = st.sidebar.empty()
    sidebar_bar = st.sidebar.progress(0)

    results: List[pd.DataFrame] = []

    def _one(loc_name: str, title: str, store_code: Optional[str]):
        df_loc = yorumlari_getir_business_api(account_name, loc_name, access_token)
        if not df_loc.empty:
            df_loc.insert(0, "location_title", title)
            df_loc.insert(1, "location_name", loc_name)
            df_loc.insert(2, "store_code", store_code)
        return df_loc

    if parallel and len(loc_pairs) > 1:
        max_workers = min(5, len(loc_pairs))
        with concurrent.futures.ThreadPoolExecutor(max_workers=max_workers) as ex:
            futs = {ex.submit(_one, a, b, c): (a, b) for a, b, c in loc_pairs}
            done = 0
            for fut in concurrent.futures.as_completed(futs):
                done += 1
                sidebar_status.text(f"Yorumlar çekiliyor: {done}/{len(loc_pairs)}")
                sidebar_bar.progress(int(done / len(loc_pairs) * 100))
                try:
                    results.append(fut.result())
                except Exception:
                    pass
    else:
        for i, (loc_name, title, store_code) in enumerate(loc_pairs, start=1):
            sidebar_status.text(f"Yorumlar çekiliyor: {i}/{len(loc_pairs)}")
            sidebar_bar.progress(int(i / len(loc_pairs) * 100))
            try:
                results.append(_one(loc_name, title, store_code))
                time.sleep(0.05)
            except Exception:
                pass

    sidebar_bar.empty()
    sidebar_status.empty()

    if not results:
        return pd.DataFrame()
    return pd.concat(results, ignore_index=True)


def tarih_etiketi_olustur(tarih_obj, mod: str) -> str:
    if isinstance(tarih_obj, str):
        try:
            tarih_obj = pd.to_datetime(tarih_obj)
        except Exception:
            return "-"

    aylar = {
        1: "Ocak", 2: "Şubat", 3: "Mart", 4: "Nisan",
        5: "Mayıs", 6: "Haziran", 7: "Temmuz", 8: "Ağustos",
        9: "Eylül", 10: "Ekim", 11: "Kasım", 12: "Aralık"
    }

    if mod == "Günlük":
        return f"{tarih_obj.day} {aylar[tarih_obj.month]} {tarih_obj.year}"

    elif mod == "Haftalık":
        baslangic = tarih_obj - timedelta(days=tarih_obj.weekday())
        bitis = baslangic + timedelta(days=6)
        return (
            f"{baslangic.day:02d} {aylar[baslangic.month][:3]} - "
            f"{bitis.day:02d} {aylar[bitis.month][:3]} {bitis.year}"
        )

    elif mod == "Aylık":
        return f"{aylar[tarih_obj.month]} {tarih_obj.year}"

    return str(tarih_obj)


def demo_veri_uret() -> pd.DataFrame:
    isimler = ["Jean (Fr)", "Ivan (Ru)", "Ahmed (Ar)", "Ayşe (Tr)", "John (En)", "Zeynep S."]
    yorumlar_iyi = ["Yemekler harikaydı", "The service was fast", "Mükemmel", "Tavsiye ederim"]
    yorumlar_kotu = ["Yemekler soğuk geldi", "Personel kabaydı", "Hijyen sıfır", "Çok pahalı"]
    data = []
    bugun = datetime.now()
    for _ in range(150):
        if random.random() > 0.3:
            puan = random.choice([4, 5])
            yorum = random.choice(yorumlar_iyi)
        else:
            puan = random.choice([1, 2, 3])
            yorum = random.choice(yorumlar_kotu)
        tarih = bugun - timedelta(days=random.randint(0, 365))
        data.append({
            "author_title": random.choice(isimler),
            "review_rating": puan,
            "review_text": yorum,
            "review_datetime_utc": tarih
        })
    return pd.DataFrame(data)


def analiz_et(df: pd.DataFrame, zaman_modu: str, date_range=None) -> Tuple[pd.DataFrame, pd.DataFrame]:
    """Zaman bazlı yıldız dağılım tablosu + ham veri döndürür.
    Ek olarak df_copy içine 'period_start' (sıralanabilir dönem anahtarı) kolonunu ekler.
    """
    df_copy = df.copy()
    df_copy["review_datetime_utc"] = pd.to_datetime(df_copy["review_datetime_utc"], utc=True, errors="coerce").dt.tz_convert(None)
    df_copy = df_copy.dropna(subset=["review_datetime_utc"])

    if date_range and len(date_range) == 2 and date_range[0] and date_range[1]:
        start_date, end_date = date_range
        start_dt = pd.to_datetime(start_date)
        end_dt = pd.to_datetime(end_date) + pd.Timedelta(days=1) - pd.Timedelta(seconds=1)
        df_copy = df_copy[(df_copy["review_datetime_utc"] >= start_dt) & (df_copy["review_datetime_utc"] <= end_dt)].copy()
        if df_copy.empty:
            return pd.DataFrame(), df_copy

    dt = df_copy["review_datetime_utc"]
    if zaman_modu == "Günlük":
        df_copy["period_start"] = dt.dt.floor("D")
    elif zaman_modu == "Haftalık":
        df_copy["period_start"] = (dt - pd.to_timedelta(dt.dt.weekday, unit="D")).dt.floor("D")
    else:
        df_copy["period_start"] = dt.dt.to_period("M").dt.to_timestamp()

    df_copy = df_copy.sort_values("review_datetime_utc", ascending=False)
    df_copy["Zaman"] = df_copy["review_datetime_utc"].apply(lambda x: tarih_etiketi_olustur(x, zaman_modu))

    analiz_tablosu = df_copy.groupby(["Zaman", "review_rating"], sort=False).size().unstack(fill_value=0)

        # Eksik sütunları tamamla (1'den 5'e)
    for i in [1, 2, 3, 4, 5]:
        if i not in analiz_tablosu.columns:
            analiz_tablosu[i] = 0

    # Sütunları sırala ve isimlendir
    analiz_tablosu = analiz_tablosu[[1, 2, 3, 4, 5]]
    analiz_tablosu.columns = [f"{c} Yıldız" for c in analiz_tablosu.columns]

    # Toplam yorum
    analiz_tablosu["TOPLAM YORUM"] = analiz_tablosu.sum(axis=1)

    # ✅ Dönem bazlı ortalama puan
    ort_puan = df_copy.groupby("Zaman")["review_rating"].mean()
    analiz_tablosu["ORT. PUAN"] = ort_puan.reindex(analiz_tablosu.index).round(2)

    # ✅ GENEL TOPLAM satırı (yıldız sütunları + toplam yorum toplanır)
    cols_yildiz = [c for c in analiz_tablosu.columns if c.endswith("Yıldız")]
    genel_toplam = analiz_tablosu[cols_yildiz + ["TOPLAM YORUM"]].sum(axis=0)

    analiz_tablosu.loc["GENEL TOPLAM", cols_yildiz] = genel_toplam[cols_yildiz].values
    analiz_tablosu.loc["GENEL TOPLAM", "TOPLAM YORUM"] = float(genel_toplam["TOPLAM YORUM"])

    # ✅ GENEL ortalama puan
    analiz_tablosu.loc["GENEL TOPLAM", "ORT. PUAN"] = round(float(df_copy["review_rating"].mean()), 2)

    return analiz_tablosu, df_copy



def _normalize_text(s: str) -> str:
    s = (s or "").lower()
    s = re.sub(r"https?://\S+|www\.\S+", " ", s)
    s = re.sub(r"[^\w\sçğıöşüÇĞİÖŞÜ]", " ", s)
    s = re.sub(r"\s+", " ", s).strip()
    return s


def tekrar_eden_sikayetler(df: pd.DataFrame, top_n: int = 12, min_count: int = 3) -> pd.DataFrame:
    """Kötü yorumlarda tekrar eden kelime/ifade (bigrams) tespiti."""
    if df is None or df.empty:
        return pd.DataFrame()

    bad = df[df["review_rating"] <= 3].copy()
    if bad.empty:
        return pd.DataFrame()

    stop = set([
        "ve", "veya", "ama", "çok", "daha", "bir", "bu", "şu", "o", "de", "da", "ile", "için", "gibi", "kadar", "her",
        "the", "and", "or", "but", "very", "more", "a", "an", "to", "of", "in", "on", "for", "with", "is", "was", "were",
        "it", "we", "they", "you", "i", "my", "our", "your", "at", "as"
    ])

    texts = bad["review_text"].fillna("").astype(str).tolist()

    counts: Dict[str, int] = {}
    for t in texts:
        t = _normalize_text(t)
        if not t:
            continue
        tokens = [tok for tok in t.split() if tok and tok not in stop and len(tok) > 2]
        for i in range(len(tokens) - 1):
            bg = tokens[i] + " " + tokens[i + 1]
            counts[bg] = counts.get(bg, 0) + 1

    if not counts:
        return pd.DataFrame()

    out = (
        pd.DataFrame([{"ifade": k, "adet": v} for k, v in counts.items()])
        .sort_values(["adet", "ifade"], ascending=[False, True])
    )
    out = out[out["adet"] >= min_count].head(top_n).reset_index(drop=True)
    return out


def excel_indir(df: pd.DataFrame, puan_tablosu: pd.DataFrame) -> bytes:
    """AI'sız Excel çıktısı:
    - Tum_Veriler
    - Puan_Analizi
    - KPI_Ozet
    """
    output = BytesIO()

    toplam = len(df)
    ort_puan = float(df["review_rating"].mean()) if toplam else 0.0
    kotu = int((df["review_rating"] <= 3).sum()) if toplam else 0
    kotu_oran = (kotu / toplam) if toplam else 0.0

    kpi = pd.DataFrame([{
        "Toplam Yorum": toplam,
        "Ortalama Puan": round(ort_puan, 2),
        "Kötü Yorum": kotu,
        "Kötü Oran (%)": round(kotu_oran * 100, 1),
    }])

    with pd.ExcelWriter(output, engine="xlsxwriter") as writer:
        df_copy = df.copy()
        if "review_datetime_utc" in df_copy.columns:
            df_copy["review_datetime_utc"] = df_copy["review_datetime_utc"].astype(str)

        df_copy.to_excel(writer, sheet_name="Tum_Veriler", index=False)
        puan_tablosu.to_excel(writer, sheet_name="Puan_Analizi", index=True)
        kpi.to_excel(writer, sheet_name="KPI_Ozet", index=False)

    return output.getvalue()


# ==========================================
# 3. ARAYÜZ
# ==========================================
st.title("📊 Google Yorum Analiz Paneli")

if "data_frame" not in st.session_state:
    st.session_state.data_frame = None

df_loaded = st.session_state.get("data_frame") is not None

# Varsayılanlar
access_token = None
err = None

# =========================
# A) VERİ YOKKEN: KURULUM
# =========================
if not df_loaded:
    st.sidebar.markdown("## 🧭 Kontrol Paneli")
    st.sidebar.caption("Adım adım ilerleyin")
    st.sidebar.divider()

    # ① Veri Modu
    st.sidebar.markdown('<div class="sb-card">', unsafe_allow_html=True)
    st.sidebar.markdown('<div class="sb-title">① Veri Modu</div>', unsafe_allow_html=True)
    mod = st.sidebar.radio(
        "Çalışma tipi",
        ["Gerçek Veri (API)", "Demo Modu"],
        label_visibility="collapsed"
    )
    st.sidebar.markdown('<div class="sb-muted">Demo ile test, API ile canlı çekim.</div>', unsafe_allow_html=True)
    st.sidebar.markdown("</div>", unsafe_allow_html=True)

    # ② Veri Kaynağı + OAuth (API modunda)
    st.sidebar.markdown('<div class="sb-card">', unsafe_allow_html=True)
    st.sidebar.markdown('<div class="sb-title">② Veri Kaynağı</div>', unsafe_allow_html=True)

    if mod == "Demo Modu":
        if st.sidebar.button("🚀 Demo Verileri Yükle", use_container_width=True):
            raw_df = demo_veri_uret()
            st.session_state.data_frame = raw_df
            st.sidebar.success("✅ Demo verileri yüklendi!")
            st.rerun()
    else:
        st.sidebar.info("Google yorumlarını çekmek için OAuth gerekir (Bearer token).")
        token, err = access_token_from_refresh_token(CLIENT_ID, CLIENT_SECRET, REFRESH_TOKEN)
        if not err:
            access_token = token

    st.sidebar.markdown("</div>", unsafe_allow_html=True)

    # ③ Sistem Durumu
    st.sidebar.markdown('<div class="sb-card">', unsafe_allow_html=True)
    st.sidebar.markdown('<div class="sb-title">🛰️ Sistem Durumu</div>', unsafe_allow_html=True)

    if mod == "Demo Modu":
        st.sidebar.markdown("✅ Demo Modu Aktif")
    else:
        if err:
            st.sidebar.markdown("❌ OAuth Bağlantısı Yok")
            st.sidebar.caption(f"Hata: {err}")
        else:
            st.sidebar.markdown("✅ OAuth Bağlandı")

    st.sidebar.markdown("🟡 Veri Yüklenmedi")
    st.sidebar.markdown("</div>", unsafe_allow_html=True)

    # ④ İşletme & Tesis + ⑤ Veri Çek (sadece API)
    selected_account_name = None
    selected_locations: List[Dict] = []
    parallel_fetch = False

    if mod != "Demo Modu":
        st.sidebar.markdown('<div class="sb-card">', unsafe_allow_html=True)
        st.sidebar.markdown('<div class="sb-title">③ İşletme & Tesis</div>', unsafe_allow_html=True)

        if access_token:
            try:
                accounts = list_accounts(access_token)
                if accounts:
                    selected_account_name = find_fixed_account_name(accounts)
                    if selected_account_name:
                        locs = list_locations(access_token, selected_account_name)
                        if locs:
                            loc_options = {f"{l.get('title','(isimsiz)')} — {l.get('name')}": l for l in locs}
                            default_labels = list(loc_options.keys())

                            chosen = st.sidebar.multiselect(
                                "Tesis(ler) (Location)",
                                options=list(loc_options.keys()),
                                default=default_labels
                            )
                            selected_locations = [loc_options[c] for c in chosen]
                            parallel_fetch = st.sidebar.checkbox("Hızlı çek (paralel)", value=False)
                        else:
                            st.sidebar.warning("Bu account altında tesis bulunamadı.")
                    else:
                        st.sidebar.warning("Meroddi Hotels işletme hesabı bulunamadı.")
                else:
                    st.sidebar.warning("Accounts listesi boş. Yetki/erişim problemi olabilir.")
            except Exception:
                st.sidebar.warning("Accounts/locations okunamadı. Token scope veya yetkiler eksik olabilir.")
        else:
            st.sidebar.warning("OAuth token yok veya alınamadı.")

        st.sidebar.markdown("</div>", unsafe_allow_html=True)

        # İşlem kartı
        st.sidebar.markdown('<div class="sb-card">', unsafe_allow_html=True)
        st.sidebar.markdown('<div class="sb-title">🚀 İşlem</div>', unsafe_allow_html=True)

        veri_cek = st.sidebar.button("🌍 VERİLERİ ÇEK", use_container_width=True)

        if veri_cek:
            if not access_token:
                st.sidebar.warning("OAuth token yok.")
            elif not selected_account_name:
                st.sidebar.warning("Account seçilemedi.")
            elif not selected_locations:
                st.sidebar.warning("En az 1 tesis seçiniz.")
            else:
                raw_df = fetch_reviews_for_locations(
                    access_token,
                    selected_account_name,
                    selected_locations,
                    parallel=parallel_fetch
                )
                if not raw_df.empty:
                    st.session_state.data_frame = raw_df
                    st.sidebar.success("✅ Veriler yüklendi")
                    st.rerun()
                else:
                    st.sidebar.error("Hiç yorum bulunamadı (veya yetki yok).")

        st.sidebar.markdown("</div>", unsafe_allow_html=True)

# =========================
# B) VERİ VARKEN: FİLTRE/AYAR
# =========================
else:
    st.sidebar.markdown("## 🧭 Kontrol Paneli")
    st.sidebar.caption("Filtreleyip analiz edin")
    st.sidebar.divider()

    df_sidebar: pd.DataFrame = st.session_state.data_frame

    # Sistem Durumu
    st.sidebar.markdown('<div class="sb-card">', unsafe_allow_html=True)
    st.sidebar.markdown('<div class="sb-title">🛰️ Sistem Durumu</div>', unsafe_allow_html=True)
    st.sidebar.markdown("🟢 Veri Hazır")
    st.sidebar.markdown("</div>", unsafe_allow_html=True)

    # Filtreler
    st.sidebar.markdown('<div class="sb-card">', unsafe_allow_html=True)
    st.sidebar.markdown('<div class="sb-title">🎛️ Filtreler</div>', unsafe_allow_html=True)

    min_star, max_star = st.sidebar.slider("Yıldız aralığı", 1, 5, (1, 5))
    keyword = st.sidebar.text_input("Yorum içinde ara (keyword)", value="")

    loc_choice = None
    if "location_title" in df_sidebar.columns:
        locs = sorted(df_sidebar["location_title"].dropna().unique().tolist())
        loc_choice = st.sidebar.multiselect("Tesis", options=locs, default=locs)

    st.sidebar.markdown("</div>", unsafe_allow_html=True)

    c1, c2 = st.sidebar.columns(2)
    with c1:
        if st.sidebar.button("🔄 Yenile", use_container_width=True):
            st.rerun()
    with c2:
        if st.sidebar.button("🧹 Veriyi Sıfırla", use_container_width=True):
            st.session_state.data_frame = None
            st.rerun()

    st.session_state["flt_min_star"] = min_star
    st.session_state["flt_max_star"] = max_star
    st.session_state["flt_keyword"] = keyword
    st.session_state["flt_locs"] = loc_choice

# ---- ANA EKRAN ----
df = st.session_state.data_frame

if df is not None and not df.empty:
    # Filtre uygula
    min_star = st.session_state.get("flt_min_star", 1)
    max_star = st.session_state.get("flt_max_star", 5)
    keyword = st.session_state.get("flt_keyword", "")
    loc_choice = st.session_state.get("flt_locs", None)

    df = df.copy()
    df = df[(df["review_rating"] >= min_star) & (df["review_rating"] <= max_star)]

    if "location_title" in df.columns and loc_choice:
        df = df[df["location_title"].isin(loc_choice)].copy()

    if keyword and keyword.strip():
        k = keyword.strip().lower()
        df = df[df["review_text"].fillna("").astype(str).str.lower().str.contains(k)].copy()

    # Üst metrikler
    c1, c2, c3 = st.columns(3)
    c1.metric("Toplam Yorum", len(df))

    puan = df["review_rating"].mean() if len(df) else 0.0
    delta_color = "normal"
    if puan >= 4.5:
        delta_color = "inverse"
    elif puan < 3.5:
        delta_color = "off"

    c2.metric("Ortalama Puan", f"{puan:.2f}", delta_color=delta_color)
    c3.metric("Kötü Yorumlar", int((df["review_rating"] <= 3).sum()))
    st.divider()

    # Hedef simülasyonu
    st.markdown("### 🎯 Gelecek Performans Hedefi")

    col_sim_1, col_sim_2 = st.columns([2, 1])
    mevcut_puan = float(df["review_rating"].mean()) if len(df) else 0.0
    mevcut_adet = int(len(df))
    mevcut_toplam_puan = float(df["review_rating"].sum()) if len(df) else 0.0

    with col_sim_1:
        gelecek_yorum_sayisi = st.slider("Önümüzdeki kaç yorum için plan yapıyorsunuz?", 10, 1000, 100, 10)
        yeni_hedef_puan = st.slider(
            "Ulaşmak istediğiniz GENEL ortalama kaçtır?",
            float(mevcut_puan),
            5.0,
            min(5.0, float(mevcut_puan) + 0.1),
            0.01
        )

    with col_sim_2:
        toplam_adet_sonra = mevcut_adet + gelecek_yorum_sayisi
        gereken_toplam_puan_sonra = yeni_hedef_puan * toplam_adet_sonra
        yeni_yorumlardan_beklenen_puan = gereken_toplam_puan_sonra - mevcut_toplam_puan
        gereken_ortalama = yeni_yorumlardan_beklenen_puan / gelecek_yorum_sayisi

        if gereken_ortalama > 5.0:
            st.error("❌ İmkansız Hedef!")
            st.metric("Gereken Ort.", f"{gereken_ortalama:.2f}")
        elif gereken_ortalama < 0:
            st.success("✅ Hedefe Zaten Ulaşılmış")
        else:
            if gereken_ortalama > 4.8:
                durum = "⚠️ Zor"
            elif gereken_ortalama > 4.0:
                durum = "⚡ Çalışılmalı"
            else:
                durum = "✅ Başarılabilir"
            st.metric(
                label=f"Gereken Performans ({durum})",
                value=f"{gereken_ortalama:.2f} / 5.0",
                delta="Sonraki yorumların ortalaması"
            )

    st.markdown("---")

    # Zaman seçimi
    col_secim, _ = st.columns([1, 4])
    with col_secim:
        zaman_secimi = st.selectbox("⏳ Zaman Aralığı:", ["Haftalık", "Günlük", "Aylık", "Özel Aralık"])

    date_range = None
    zaman_modu = zaman_secimi

    if zaman_secimi == "Özel Aralık":
        _dt = pd.to_datetime(df["review_datetime_utc"], errors="coerce", utc=True).dt.tz_convert(None)
        _dt = _dt.dropna()
        if not _dt.empty:
            min_d = _dt.min().date()
            max_d = _dt.max().date()
        else:
            min_d = pd.Timestamp.today().date()
            max_d = pd.Timestamp.today().date()

        cdr1, cdr2 = st.columns(2)
        with cdr1:
            start_d = st.date_input("Başlangıç Tarihi", value=min_d, min_value=min_d, max_value=max_d)
        with cdr2:
            end_d = st.date_input("Bitiş Tarihi", value=max_d, min_value=min_d, max_value=max_d)

        if start_d > end_d:
            start_d, end_d = end_d, start_d

        date_range = (start_d, end_d)
        zaman_modu = st.selectbox("Gruplama", ["Günlük", "Haftalık", "Aylık"], index=0)
        st.caption(f"Seçilen aralık: {start_d.strftime('%d.%m.%Y')} – {end_d.strftime('%d.%m.%Y')}")

    puan_tablosu, ham_veri = analiz_et(df, zaman_modu, date_range=date_range)

    tab1, tab2, tab3, tab4 = st.tabs(["📄 Puan Tablosu", "🚨 Şikayet Detayları", "🔁 Tekrar Eden Şikayetler", "📋 Veri Listesi"])

    with tab1:
        st.write(f"### 📄 {zaman_modu} Puan Dağılım Tablosu")
        st.dataframe(puan_tablosu, use_container_width=True)

    with tab2:
        st.subheader("Dönemsel Şikayet Listesi (≤ 3 Yıldız)")
        sikayet_df = ham_veri[ham_veri["review_rating"] <= 3].copy()
        if not sikayet_df.empty:
            cols = [c for c in ["Zaman", "author_title", "review_rating", "review_text"] if c in sikayet_df.columns]
            st.dataframe(sikayet_df[cols], use_container_width=True, hide_index=True)
        else:
            st.success("Bu dönemde şikayet bulunamadı.")

    with tab3:
        st.subheader("Tekrar Eden Şikayet İfadeleri (Bigrams)")
        rep = tekrar_eden_sikayetler(ham_veri, top_n=12, min_count=3)
        if rep is None or rep.empty:
            st.info("Yeterli tekrar eden ifade bulunamadı (veya kötü yorum yok).")
        else:
            st.dataframe(rep, use_container_width=True, hide_index=True)

    with tab4:
        st.dataframe(ham_veri, use_container_width=True)

    st.divider()
    excel_data = excel_indir(ham_veri, puan_tablosu)
    st.download_button(
        label="📥 Raporu Excel Olarak İndir",
        data=excel_data,
        file_name="Rapor.xlsx",
        mime="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    )

else:
    st.info("Henüz veri yok. Soldan Demo yükleyin veya OAuth ile API'den çekin.")
