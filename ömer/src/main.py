import streamlit as st
import time
import hmac
import hashlib
import sqlite3
import os
import sys
import logging
import faulthandler
from local_secrets import get_secret

# ==========================================
# 0.0 ÇÖKME TEŞHİSİ (faulthandler) + LOGLAMA
# ==========================================
# Segmentation fault / native çökme anında TÜM thread'lerin C-seviyesi
# traceback'ini stderr'e (Streamlit Cloud loglarına) yazar.
# Secret veya kullanıcı/yorum verisi ASLA loglanmaz.
try:
    faulthandler.enable(all_threads=True)
except Exception:
    pass

logger = logging.getLogger("yorumanaliz")
logger.setLevel(logging.INFO)
if not logger.handlers:
    _log_handler = logging.StreamHandler(sys.stderr)
    _log_handler.setFormatter(
        logging.Formatter("%(asctime)s %(levelname)s [%(threadName)s] %(message)s")
    )
    logger.addHandler(_log_handler)
logger.propagate = False


def _process_rss_mb():
    """Süreç RSS bellek kullanımı (MB). Yalnızca stdlib; secret/kullanıcı verisi içermez."""
    try:
        with open("/proc/self/status", "r") as fh:  # Linux (Streamlit Cloud)
            for line in fh:
                if line.startswith("VmRSS:"):
                    return float(line.split()[1]) / 1024.0  # kB -> MB
    except Exception:
        pass
    try:
        import resource  # Unix yedek
        return resource.getrusage(resource.RUSAGE_SELF).ru_maxrss / 1024.0
    except Exception:
        return None


def _log_mem(stage: str, df=None):
    """Bellek kullanımını güvenli şekilde loglar: aşama adı + süreç RSS + (varsa) df satır/bellek.
    Secret, parola, token veya yorum metni ASLA loglanmaz."""
    try:
        parts = ["[MEM] " + str(stage)]
        rss = _process_rss_mb()
        if rss is not None:
            parts.append("process_rss=%.1fMB" % rss)
        if df is not None:
            try:
                parts.append("df_rows=%d" % len(df))
                parts.append("df_mem=%.1fMB" % (df.memory_usage(deep=True).sum() / 1e6))
            except Exception:
                pass
        logger.info(" ".join(parts))
    except Exception:
        pass


# ==========================================
# 0. GİRİŞ SİSTEMİ (her şeyden önce çalışır)
# ==========================================

def sha256(s: str) -> str:
    return hashlib.sha256((s or "").encode("utf-8")).hexdigest()

APP_PASSWORD_HASH = get_secret("APP_PASSWORD_HASH", __file__)
SESSION_TTL_SECONDS = 60 * 60  # 1 saat

if "logged_in" not in st.session_state:
    st.session_state.logged_in = False
if "login_time" not in st.session_state:
    st.session_state.login_time = 0.0

if st.session_state.logged_in and (time.time() - st.session_state.login_time > SESSION_TTL_SECONDS):
    st.session_state.logged_in = False

if not st.session_state.logged_in:
    st.set_page_config(page_title="Giriş", page_icon="🔒", layout="centered")
    st.title("🔒 Yönetici Girişi")
    password = st.text_input("Şifre", type="password")
    if st.button("Giriş Yap", width="stretch"):
        if not APP_PASSWORD_HASH:
            st.error("APP_PASSWORD_HASH bulunamadi. .streamlit/secrets.toml dosyasini kontrol edin.")
        elif hmac.compare_digest(sha256(password), APP_PASSWORD_HASH):
            st.session_state.logged_in = True
            st.session_state.login_time = time.time()
            st.rerun()
        else:
            st.error("Şifre yanlış.")
    st.stop()

# ==========================================
# 1. AYARLAR
# ==========================================
import pandas as pd
import numpy as np
import requests
import random
import concurrent.futures
import re
from datetime import datetime, timedelta
from io import BytesIO
from typing import Dict, List, Optional, Tuple
import os

st.set_page_config(page_title="Google Yorum Analizi", page_icon="📊", layout="wide")
_log_mem("app_start")

# ==========================================
# PLACES API
# ==========================================
PLACES_API_KEY = get_secret("PLACES_API_KEY", __file__)

@st.cache_data(ttl=3600, show_spinner=False)
def places_get_rating_and_total(place_id: str, api_key: str) -> Tuple[Optional[float], Optional[int], Optional[str], Optional[dict]]:
    if not place_id or not api_key:
        return None, None, None, {"status": "MISSING_PLACE_ID_OR_KEY"}
    url = "https://maps.googleapis.com/maps/api/place/details/json"
    params = {"place_id": place_id, "fields": "rating,user_ratings_total,name", "key": api_key, "language": "tr"}
    try:
        js = requests.get(url, params=params, timeout=30).json()
    except Exception as e:
        return None, None, None, {"status": "REQUEST_FAILED", "error": str(e)}
    status = js.get("status")
    if status != "OK":
        return None, None, None, js
    r = js.get("result", {}) or {}
    rating = r.get("rating")
    total  = r.get("user_ratings_total")
    name   = r.get("name")
    try:
        rating = float(rating) if rating is not None else None
    except Exception:
        rating = None
    try:
        total = int(total) if total is not None else None
    except Exception:
        total = None
    return rating, total, name, None


def google_kpi_from_place_ids(place_ids: List[str]) -> Tuple[Optional[float], Optional[int], pd.DataFrame]:
    rows = []
    for pid in sorted(set([p for p in place_ids if p and str(p).strip()])):
        rating, total, name, err = places_get_rating_and_total(pid, PLACES_API_KEY)
        rows.append({
            "place_id": pid,
            "google_name": name,
            "google_rating": rating,
            "google_total": total,
            "google_status": "OK" if err is None else (err.get("status") if isinstance(err, dict) else "ERR"),
        })
    dfk = pd.DataFrame(rows)
    if dfk.empty:
        return None, None, dfk
    valid = dfk.dropna(subset=["google_total", "google_rating"]).copy()
    valid = valid[valid["google_total"] > 0]
    if valid.empty:
        total_sum = int(dfk["google_total"].dropna().sum()) if "google_total" in dfk.columns else None
        return None, total_sum if total_sum else None, dfk
    total_sum = int(valid["google_total"].sum())
    weighted  = float((valid["google_rating"] * valid["google_total"]).sum() / total_sum) if total_sum > 0 else None
    return weighted, total_sum, dfk


# ==========================================
# VERİTABANI
# ==========================================
DB_PATH = os.path.join(os.path.dirname(os.path.abspath(__file__)), "reviews.db")
ASSETS_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "assets")
DATA_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "data")
TRIPADVISOR_SEED_FILES = ("tripadvisor_seed.xlsx",)


def find_logo_path(*logo_names: str) -> str:
    for logo_name in logo_names:
        logo_path = os.path.join(ASSETS_DIR, logo_name)
        if os.path.exists(logo_path):
            return logo_path
    return ""


def render_title_with_logo(title: str, *logo_names: str) -> None:
    left_col, right_col = st.columns([12, 2])
    logo_path = find_logo_path(*logo_names)

    with left_col:
        st.write(f"### {title}")

    with right_col:
        if logo_path:
            st.image(logo_path, width=60)


def render_df_paginated(df: pd.DataFrame, key: str, page_size: int = 250, **kwargs):
    """Büyük tabloları sayfalayarak render eder. Varsayılan olarak yalnızca
    ilk 250 satır render edilir; kullanıcı sayfa seçerek gezinir.
    Bu, her rerun'da tüm DataFrame'in Arrow'a serialize edilmesini önleyerek
    bellek baskısını ve çökme riskini azaltır."""
    if df is None:
        return
    n = len(df)
    if n <= page_size:
        st.dataframe(df, **kwargs)
        return
    _log_mem("table_render:" + str(key), df)
    total_pages = (n + page_size - 1) // page_size
    c1, c2 = st.columns([1, 3])
    with c1:
        page = st.number_input(
            "Sayfa", min_value=1, max_value=int(total_pages), value=1, step=1, key=f"{key}_page"
        )
    start = (int(page) - 1) * page_size
    end = min(start + page_size, n)
    with c2:
        st.caption(
            f"{n:,} satırın {start + 1:,}–{end:,} arası gösteriliyor "
            f"(sayfa {int(page)}/{total_pages}, sayfa başına {page_size})"
        )
    st.dataframe(df.iloc[start:end], **kwargs)

def db_init():
    con = sqlite3.connect(DB_PATH)

    # Google / Outscraper
    con.execute("""
        CREATE TABLE IF NOT EXISTS outscraper_reviews (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            author_title TEXT,
            review_rating INTEGER,
            review_text TEXT,
            review_datetime_utc TEXT,
            location_title TEXT,
            anahtar TEXT UNIQUE
        )
    """)

    # TripAdvisor
    con.execute("""
        CREATE TABLE IF NOT EXISTS tripadvisor_reviews (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            author_title TEXT,
            review_rating INTEGER,
            review_text TEXT,
            review_datetime_utc TEXT,
            location_title TEXT,
            title TEXT,
            tripType TEXT,
            lang TEXT,
            platform_total_reviews INTEGER,
            platform_rating REAL,
            anahtar TEXT UNIQUE
        )
    """)

    con.commit()
    con.close()


def db_outscraper_kaydet(df: pd.DataFrame) -> int:
    if df is None or df.empty:
        return 0
    con = sqlite3.connect(DB_PATH)
    eklenen = 0
    for _, row in df.iterrows():
        tarih_str = pd.to_datetime(row.get("review_datetime_utc"), errors="coerce")
        tarih_str = tarih_str.strftime("%Y-%m-%d") if not pd.isna(tarih_str) else "?"
        metin   = str(row.get("review_text", ""))[:50].lower().strip()
        yazar   = str(row.get("author_title", "")).lower().strip()
        puan    = str(row.get("review_rating", ""))
        anahtar = f"{yazar}|{puan}|{metin}|{tarih_str}"
        try:
            con.execute("""
                INSERT OR IGNORE INTO outscraper_reviews
                (author_title, review_rating, review_text, review_datetime_utc, location_title, anahtar)
                VALUES (?, ?, ?, ?, ?, ?)
            """, (
                row.get("author_title", ""),
                int(row.get("review_rating", 0)),
                row.get("review_text", ""),
                str(row.get("review_datetime_utc", "")),
                row.get("location_title", ""),
                anahtar,
            ))
            if con.execute("SELECT changes()").fetchone()[0] > 0:
                eklenen += 1
        except Exception:
            pass
    con.commit()
    con.close()
    return eklenen


def db_outscraper_yukle() -> pd.DataFrame:
    try:
        con = sqlite3.connect(DB_PATH)
        df = pd.read_sql("SELECT author_title, review_rating, review_text, review_datetime_utc, location_title FROM outscraper_reviews", con)
        con.close()
        df["kaynak"] = "outscraper"
        df["review_rating"] = pd.to_numeric(df["review_rating"], errors="coerce").fillna(0).astype(int)
        df["review_datetime_utc"] = pd.to_datetime(df["review_datetime_utc"], errors="coerce", utc=True).dt.tz_convert(None)
        return df
    except Exception:
        return pd.DataFrame()


def db_outscraper_sil():
    con = sqlite3.connect(DB_PATH)
    con.execute("DELETE FROM outscraper_reviews")
    con.commit()
    con.close()


def db_outscraper_adet() -> int:
    try:
        con = sqlite3.connect(DB_PATH)
        adet = con.execute("SELECT COUNT(*) FROM outscraper_reviews").fetchone()[0]
        con.close()
        return adet
    except Exception:
        return 0


def db_tripadvisor_kaydet(df: pd.DataFrame) -> int:
    if df is None or df.empty:
        return 0

    con = sqlite3.connect(DB_PATH)
    eklenen = 0

    for _, row in df.iterrows():
        tarih_str = pd.to_datetime(row.get("review_datetime_utc"), errors="coerce")
        tarih_str = tarih_str.strftime("%Y-%m-%d") if not pd.isna(tarih_str) else "?"
        metin = str(row.get("review_text", ""))[:80].lower().strip()
        yazar = str(row.get("author_title", "")).lower().strip()
        puan = str(row.get("review_rating", ""))
        tesis = str(row.get("location_title", "")).lower().strip()
        anahtar = f"{tesis}|{yazar}|{puan}|{metin}|{tarih_str}"

        try:
            con.execute("""
                INSERT OR IGNORE INTO tripadvisor_reviews
                (
                    author_title, review_rating, review_text, review_datetime_utc,
                    location_title, title, tripType, lang,
                    platform_total_reviews, platform_rating, anahtar
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, (
                row.get("author_title", ""),
                int(row.get("review_rating", 0)),
                row.get("review_text", ""),
                str(row.get("review_datetime_utc", "")),
                row.get("location_title", ""),
                row.get("title", ""),
                row.get("tripType", ""),
                row.get("lang", ""),
                int(row.get("platform_total_reviews")) if pd.notna(row.get("platform_total_reviews")) else None,
                float(row.get("platform_rating")) if pd.notna(row.get("platform_rating")) else None,
                anahtar
            ))
            if con.execute("SELECT changes()").fetchone()[0] > 0:
                eklenen += 1
        except Exception:
            pass

    con.commit()
    con.close()
    return eklenen


def db_tripadvisor_yukle() -> pd.DataFrame:
    try:
        con = sqlite3.connect(DB_PATH)
        df = pd.read_sql("""
            SELECT
                author_title,
                review_rating,
                review_text,
                review_datetime_utc,
                location_title,
                title,
                tripType,
                lang,
                platform_total_reviews,
                platform_rating
            FROM tripadvisor_reviews
        """, con)
        con.close()

        if df.empty:
            return df

        df["kaynak"] = "tripadvisor_db"
        df["review_rating"] = pd.to_numeric(df["review_rating"], errors="coerce")
        df["review_datetime_utc"] = pd.to_datetime(df["review_datetime_utc"], errors="coerce", utc=True).dt.tz_convert(None)
        df["platform_total_reviews"] = pd.to_numeric(df["platform_total_reviews"], errors="coerce")
        df["platform_rating"] = pd.to_numeric(df["platform_rating"], errors="coerce")
        df = df.dropna(subset=["review_rating", "review_datetime_utc"])
        df = df[(df["review_rating"] >= 1) & (df["review_rating"] <= 5)].copy()
        df["review_rating"] = df["review_rating"].round().astype(int)
        df = df.sort_values("review_datetime_utc", ascending=False, na_position="last").reset_index(drop=True)
        return df

    except Exception:
        return pd.DataFrame()


def db_tripadvisor_sil():
    con = sqlite3.connect(DB_PATH)
    con.execute("DELETE FROM tripadvisor_reviews")
    con.commit()
    con.close()


def db_tripadvisor_adet() -> int:
    try:
        con = sqlite3.connect(DB_PATH)
        adet = con.execute("SELECT COUNT(*) FROM tripadvisor_reviews").fetchone()[0]
        con.close()
        return adet
    except Exception:
        return 0


db_init()


def dosya_isle(uploaded_file) -> pd.DataFrame:
    fname = uploaded_file.name.lower()
    try:
        if fname.endswith(".csv"):
            try:
                df = pd.read_csv(uploaded_file, encoding="utf-8")
            except Exception:
                df = pd.read_csv(uploaded_file, encoding="latin-1")
        else:
            df = pd.read_excel(uploaded_file)
    except Exception as e:
        st.error(f"Dosya okunamadı: {e}")
        return pd.DataFrame()

    cols = {c.lower().strip(): c for c in df.columns}

    def pick(*names):
        for n in names:
            if n in cols:
                return cols[n]
        return None

    c_author   = pick("author_title", "reviewer", "reviewer_name", "name", "author")
    c_rating   = pick("review_rating", "rating", "stars", "puan")
    c_text     = pick("review_text", "comment", "text", "review")
    c_date     = pick("review_datetime_utc", "createtime", "date", "datetime", "published_at_date", "tarih")
    c_location = pick("location_title", "location", "place_name", "tesis", "source_file")

    star_map = {"ONE": 1, "TWO": 2, "THREE": 3, "FOUR": 4, "FIVE": 5, "STAR_RATING_UNSPECIFIED": 0}

    out = pd.DataFrame()
    out["author_title"]        = df[c_author].fillna("Gizli Kullanıcı") if c_author else "Bilinmiyor"
    out["review_text"]         = df[c_text].fillna("") if c_text else ""
    out["review_datetime_utc"] = pd.to_datetime(df[c_date], errors="coerce", utc=True).dt.tz_convert(None) if c_date else pd.NaT
    out["location_title"]      = df[c_location].fillna("Barnathan İstanbul") if c_location else "Barnathan İstanbul"
    out["kaynak"]              = "outscraper"

    if c_rating:
        raw = df[c_rating]
        if raw.dtype == object:
            out["review_rating"] = raw.map(star_map).fillna(pd.to_numeric(raw, errors="coerce")).fillna(0).astype(int)
        else:
            out["review_rating"] = pd.to_numeric(raw, errors="coerce").fillna(0).astype(int)
    else:
        out["review_rating"] = 0

    out = out[(out["review_rating"] >= 1) & (out["review_rating"] <= 5)]
    return out.reset_index(drop=True)


def api_ile_birlestir(api_df: pd.DataFrame) -> pd.DataFrame:
    os_df = db_outscraper_yukle()
    if os_df.empty:
        if api_df is not None and not api_df.empty:
            api_df = api_df.copy()
            api_df["kaynak"] = "api"
        return api_df if api_df is not None else pd.DataFrame()
    if api_df is None or api_df.empty:
        return os_df
    api_df = api_df.copy()
    api_df["kaynak"] = "api"

    def anahtar(df_):
        tarih_str = pd.to_datetime(df_["review_datetime_utc"], errors="coerce").dt.strftime("%Y-%m-%d").fillna("?")
        metin = df_["review_text"].fillna("").astype(str).str[:50].str.lower().str.strip()
        yazar = df_["author_title"].fillna("").astype(str).str.lower().str.strip()
        puan  = df_["review_rating"].astype(str)
        return yazar + "|" + puan + "|" + metin + "|" + tarih_str

    api_df["_k"] = anahtar(api_df)
    os_df["_k"]  = anahtar(os_df)
    api_anahtarlar = set(api_df["_k"].tolist())
    yeni = os_df[~os_df["_k"].isin(api_anahtarlar)].copy()
    birlesik = pd.concat([api_df, yeni], ignore_index=True)
    birlesik = birlesik.drop(columns=["_k"])
    birlesik["review_datetime_utc"] = pd.to_datetime(birlesik["review_datetime_utc"], errors="coerce", utc=True).dt.tz_convert(None)
    birlesik = birlesik.sort_values("review_datetime_utc", ascending=False, na_position="last").reset_index(drop=True)
    return birlesik


# ==========================================
# AĞIRLIKLI PUAN HESAPLAMA
# ==========================================

def _month_start(dt: datetime) -> datetime:
    return datetime(dt.year, dt.month, 1)

def aylik_yorum_serisi(df: pd.DataFrame, lookback_months: int = 24) -> pd.Series:
    if df is None or df.empty or "review_datetime_utc" not in df.columns:
        return pd.Series(dtype=float)
    d = df.copy()
    dt = pd.to_datetime(d["review_datetime_utc"], utc=True, errors="coerce")
    dt = dt.dropna()
    if dt.empty:
        return pd.Series(dtype=float)
    ms = dt.dt.to_period("M").dt.to_timestamp()
    s = ms.value_counts().sort_index()
    now = datetime.utcnow()
    start = _month_start(now) - pd.DateOffset(months=lookback_months-1)
    s = s[s.index >= pd.Timestamp(start)]
    if not s.empty:
        all_months = pd.date_range(s.index.min(), _month_start(now), freq="MS")
        s = s.reindex(all_months, fill_value=0)
    return s.astype(float)

def yorum_hizi_tahmini(df: pd.DataFrame, months_ahead: int = 12, base_window: int = 3, season_lookback_months: int = 24) -> pd.DataFrame:
    s = aylik_yorum_serisi(df, lookback_months=season_lookback_months)
    now = datetime.utcnow()
    this_ms = _month_start(now)

    if s.empty:
        base = 0.0
        season = {m: 1.0 for m in range(1, 13)}
    else:
        base = float(s.tail(max(base_window, 1)).mean()) if len(s) else float(s.mean())
        tmp = pd.DataFrame({"cnt": s.values}, index=pd.to_datetime(s.index))
        tmp["moy"] = tmp.index.month
        mean_by_moy = tmp.groupby("moy")["cnt"].mean()
        overall = float(tmp["cnt"].mean()) if float(tmp["cnt"].mean()) > 0 else 1.0
        season = {m: float(mean_by_moy.get(m, overall)) / overall for m in range(1, 13)}
        for m in season:
            season[m] = float(np.clip(season[m], 0.6, 1.8))

    future_months = [(_month_start(this_ms) + pd.DateOffset(months=i+1)).to_pydatetime() for i in range(months_ahead)]
    preds = []
    for ms in future_months:
        preds.append(max(0.0, base * season.get(ms.month, 1.0)))
    out = pd.DataFrame({"month_start": future_months, "pred_count": preds})
    return out

def kac_ayda_yorum_hedefi(pred_df: pd.DataFrame, hedef_yorum_sayisi: int) -> Tuple[int, pd.DataFrame]:
    if pred_df is None or pred_df.empty or hedef_yorum_sayisi <= 0:
        return 0, pd.DataFrame(columns=["month_start", "pred_count"])
    cum = 0.0
    rows = []
    for _, r in pred_df.iterrows():
        c = float(r["pred_count"])
        rows.append({"month_start": r["month_start"], "pred_count": c})
        cum += c
        if cum >= float(hedef_yorum_sayisi):
            break
    used = pd.DataFrame(rows)
    return int(len(used)), used

def google_agirlik_hesapla(gun_farki: float) -> float:
    ay = gun_farki / 30.0
    if ay <= 3:
        return 1.00
    elif ay <= 6:
        return 1.00 - (0.20 * (ay - 3) / 3)
    elif ay <= 12:
        return 0.80 - (0.30 * (ay - 6) / 6)
    elif ay <= 24:
        return 0.50 - (0.30 * (ay - 12) / 12)
    elif ay <= 36:
        return 0.20
    elif ay <= 60:
        return 0.20 - (0.15 * (ay - 36) / 24)
    else:
        return 0.01


def google_etkin_puan(df: pd.DataFrame) -> Optional[float]:
    if df is None or df.empty or "review_datetime_utc" not in df.columns or "review_rating" not in df.columns:
        return None
    try:
        df2 = df.copy()
        simdi = pd.Timestamp.now()
        df2["gun_farki"] = (simdi - pd.to_datetime(df2["review_datetime_utc"], errors="coerce")).dt.days.clip(lower=0)
        df2 = df2.dropna(subset=["gun_farki", "review_rating"])
        if df2.empty:
            return None
        df2["agirlik"] = df2["gun_farki"].apply(google_agirlik_hesapla)
        puanlar = pd.to_numeric(df2["review_rating"], errors="coerce")
        mask = puanlar.notna()
        df2 = df2[mask].copy()
        puanlar = puanlar[mask]
        toplam_agirlik = float(df2["agirlik"].sum())
        if toplam_agirlik <= 0:
            return None
        return float((df2["agirlik"] * puanlar).sum() / toplam_agirlik)
    except Exception:
        return None


def gereken_yeni_ortalama_google(
    df: pd.DataFrame,
    hedef_puan: float,
    yeni_yorum_sayisi: int,
    bayesian_prior: float = 0.0,
    bayesian_weight: int = 0
) -> Optional[float]:
    if df is None or df.empty or "review_datetime_utc" not in df.columns or "review_rating" not in df.columns:
        return None
    try:
        df2 = df.copy()
        simdi = pd.Timestamp.now()
        df2["gun_farki"] = (simdi - pd.to_datetime(df2["review_datetime_utc"], errors="coerce")).dt.days.clip(lower=0)
        df2 = df2.dropna(subset=["gun_farki", "review_rating"])
        df2["agirlik"] = df2["gun_farki"].apply(google_agirlik_hesapla)

        puanlar = pd.to_numeric(df2["review_rating"], errors="coerce")
        mask = puanlar.notna() & (puanlar >= 1) & (puanlar <= 5)
        df2 = df2[mask].copy()
        puanlar = puanlar[mask]
        if df2.empty:
            return None

        W_mevcut  = float(df2["agirlik"].sum())
        WR_mevcut = float((df2["agirlik"] * puanlar).sum())
        if bayesian_weight > 0:
            W_mevcut += float(bayesian_weight)
            WR_mevcut += float(bayesian_weight) * float(bayesian_prior)

        W_yeni = float(yeni_yorum_sayisi) * 1.0

        if W_yeni <= 0:
            return None

        gereken = (hedef_puan * (W_mevcut + W_yeni) - WR_mevcut) / W_yeni
        return round(float(gereken), 2)
    except Exception:
        return None


def gereken_yeni_ortalama_basit(
    mevcut_ortalama: float,
    mevcut_yorum_sayisi: int,
    hedef_puan: float,
    yeni_yorum_sayisi: int
) -> Optional[float]:
    try:
        n0 = int(mevcut_yorum_sayisi)
        n1 = int(yeni_yorum_sayisi)
        r0 = float(mevcut_ortalama)
        rt = float(hedef_puan)
        if n0 < 0 or n1 <= 0:
            return None
        if not (0.0 <= r0 <= 5.0 and 0.0 <= rt <= 5.0):
            return None
        gereken = (rt * (n0 + n1) - r0 * n0) / n1
        return round(float(gereken), 2)
    except Exception:
        return None


def agirlikli_puan_hesapla(df: pd.DataFrame, yari_omur_gun: int = 180) -> Tuple[float, float]:
    df2 = df.copy()
    df2["review_datetime_utc"] = pd.to_datetime(df2["review_datetime_utc"], errors="coerce")
    df2 = df2.dropna(subset=["review_datetime_utc", "review_rating"])

    if df2.empty:
        return round(float(df["review_rating"].mean()), 4), float(len(df))

    bugun = pd.Timestamp.now()
    df2["gun_farki"] = (bugun - df2["review_datetime_utc"]).dt.days.clip(lower=0)
    df2["agirlik"]   = np.power(0.5, df2["gun_farki"] / float(yari_omur_gun))

    toplam_agirlik = df2["agirlik"].sum()
    if toplam_agirlik == 0:
        return round(float(df["review_rating"].mean()), 4), float(len(df))

    agirlikli_ort = float((df2["review_rating"] * df2["agirlik"]).sum() / toplam_agirlik)
    return round(agirlikli_ort, 4), round(float(toplam_agirlik), 1)


# ==========================================
# OAUTH
# ==========================================
CLIENT_ID     = get_secret("OAUTH_CLIENT_ID", __file__)
CLIENT_SECRET = get_secret("OAUTH_CLIENT_SECRET", __file__)
REFRESH_TOKEN = get_secret("OAUTH_REFRESH_TOKEN", __file__)

with st.sidebar:
    if st.button("🚪 Çıkış Yap", width="stretch"):
        st.session_state.clear()
        st.rerun()


def load_css(path: str):
    base_dir = os.path.dirname(os.path.abspath(__file__))
    css_path = os.path.join(base_dir, path)
    if os.path.exists(css_path):
        with open(css_path, "r", encoding="utf-8") as f:
            st.markdown(f"<style>{f.read()}</style>", unsafe_allow_html=True)

load_css("styles.css")

# ==========================================
# 2. MOTOR
# ==========================================

def get_headers(access_token: str) -> Dict[str, str]:
    return {"Authorization": f"Bearer {access_token}"}


def access_token_from_refresh_token(client_id, client_secret, refresh_token):
    try:
        r = requests.post("https://oauth2.googleapis.com/token", data={
            "client_id": client_id, "client_secret": client_secret,
            "refresh_token": refresh_token, "grant_type": "refresh_token",
        }, timeout=30)
        js = r.json()
        if r.status_code != 200:
            return None, js.get("error_description") or js.get("error") or str(js)
        return js.get("access_token"), None
    except Exception as e:
        return None, str(e)


def list_accounts(access_token: str) -> List[Dict]:
    resp = requests.get("https://mybusinessaccountmanagement.googleapis.com/v1/accounts",
                        headers=get_headers(access_token), timeout=30).json()
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
    all_locs: List[Dict] = []
    page_token = None
    base_url = f"https://mybusinessbusinessinformation.googleapis.com/v1/{account_name}/locations"
    while True:
        params = {"pageSize": page_size, "readMask": "name,title,storeCode,metadata"}
        if page_token:
            params["pageToken"] = page_token
        js = requests.get(base_url, headers=get_headers(access_token), params=params, timeout=60).json()
        all_locs.extend(js.get("locations", []))
        page_token = js.get("nextPageToken")
        if not page_token:
            break
    return all_locs


def yorumlari_getir_business_api(account_name: str, location_name: str, access_token: str):
    """WORKER-SAFE: Bu fonksiyon ThreadPoolExecutor worker thread'lerinde de çalışır;
    bu yüzden hiçbir st.* / st.sidebar.* / st.session_state çağrısı YAPMAZ.
    Yalnızca HTTP isteği + veri işleme yapar. UI güncellemesi ana thread'e bırakılır.
    Dönüş: (pd.DataFrame, List[str] uyarı mesajları)."""
    all_reviews: List[Dict] = []
    page_token = None
    base_url = f"https://mybusiness.googleapis.com/v4/{account_name}/{location_name}/reviews"
    sayfa = 0
    max_retries = 5
    warnings_out: List[str] = []

    while True:
        params = {"pageSize": 50}
        if page_token:
            params["pageToken"] = page_token
        success = False
        last_err = None

        for attempt in range(max_retries):
            try:
                r = requests.get(base_url, headers=get_headers(access_token), params=params, timeout=60)
                if r.status_code != 200:
                    try:
                        err_json = r.json()
                    except Exception:
                        err_json = {"text": r.text[:500]}
                    last_err = f"HTTP {r.status_code}: {err_json}"
                    if r.status_code in (429, 500, 503):
                        wait = 2.0 * (attempt + 1)
                        time.sleep(wait)
                        continue
                    else:
                        break
                resp = r.json()
                if "error" in resp:
                    last_err = str(resp["error"])
                    time.sleep(1.5 * (attempt + 1))
                    continue
                new_reviews = resp.get("reviews", [])
                all_reviews.extend(new_reviews)
                page_token = resp.get("nextPageToken")
                sayfa += 1
                success = True
                break
            except requests.exceptions.Timeout:
                last_err = "Zaman aşımı (timeout)"
                time.sleep(2.0 * (attempt + 1))
            except Exception as e:
                last_err = str(e)
                time.sleep(1.5 * (attempt + 1))

        if not success:
            warnings_out.append(
                f"⚠️ {location_name}: sayfa {sayfa+1} alınamadı ({max_retries} deneme). "
                f"Toplam {len(all_reviews)} yorum. Hata: {last_err}"
            )
            break
        if not page_token:
            break
        time.sleep(0.8)

    star_map = {"STAR_RATING_UNSPECIFIED": 0, "ONE": 1, "TWO": 2, "THREE": 3, "FOUR": 4, "FIVE": 5}
    processed = []
    for r in all_reviews:
        processed.append({
            "author_title": r.get("reviewer", {}).get("displayName", "Gizli Kullanıcı"),
            "review_rating": star_map.get(r.get("starRating", "ONE"), 0),
            "review_text": r.get("comment", ""),
            "review_datetime_utc": r.get("createTime", ""),
        })
    return pd.DataFrame(processed), warnings_out


def fetch_reviews_for_locations(access_token, account_name, locations, parallel=False):
    loc_items = []
    for loc in locations:
        loc_name = loc.get("name")
        title    = loc.get("title") or loc_name
        store_code = loc.get("storeCode")
        place_id   = (loc.get("metadata") or {}).get("placeId")
        if loc_name:
            loc_items.append((loc_name, title, store_code, place_id))
    if not loc_items:
        return pd.DataFrame()

    # UI öğeleri yalnızca ANA thread'de oluşturulur ve güncellenir.
    sidebar_status = st.sidebar.empty()
    sidebar_bar    = st.sidebar.progress(0)
    results = []
    all_warnings: List[str] = []

    def _one(loc_name, title, store_code, place_id):
        # WORKER-SAFE: burada hiçbir st.* çağrısı yok; yalnızca veri döner.
        df_loc, warns = yorumlari_getir_business_api(account_name, loc_name, access_token)
        if not df_loc.empty:
            df_loc.insert(0, "location_title", title)
            df_loc.insert(1, "location_name", loc_name)
            df_loc.insert(2, "store_code", store_code)
            df_loc.insert(3, "place_id", place_id)
        return df_loc, warns

    if parallel and len(loc_items) > 1:
        max_workers = min(5, len(loc_items))
        with concurrent.futures.ThreadPoolExecutor(max_workers=max_workers) as ex:
            futs = {ex.submit(_one, a, b, c, d): (a, b) for a, b, c, d in loc_items}
            done = 0
            for fut in concurrent.futures.as_completed(futs):  # ana thread
                done += 1
                sidebar_status.text(f"Yorumlar çekiliyor: {done}/{len(loc_items)}")
                sidebar_bar.progress(int(done / len(loc_items) * 100))
                try:
                    df_loc, warns = fut.result()
                    results.append(df_loc)
                    all_warnings.extend(warns)
                except Exception:
                    pass
    else:
        for i, (loc_name, title, store_code, place_id) in enumerate(loc_items, start=1):
            sidebar_status.text(f"Yorumlar çekiliyor: {i}/{len(loc_items)}")
            sidebar_bar.progress(int(i / len(loc_items) * 100))
            try:
                df_loc, warns = _one(loc_name, title, store_code, place_id)
                results.append(df_loc)
                all_warnings.extend(warns)
                time.sleep(0.05)
            except Exception:
                pass

    sidebar_bar.empty()
    sidebar_status.empty()
    # Uyarılar da ana thread'de gösterilir (worker'larda değil).
    for w in all_warnings:
        st.sidebar.warning(w)
    if not results:
        return pd.DataFrame()
    return pd.concat(results, ignore_index=True)


def tarih_etiketi_olustur(tarih_obj, mod: str) -> str:
    if isinstance(tarih_obj, str):
        try:
            tarih_obj = pd.to_datetime(tarih_obj)
        except Exception:
            return "-"
    aylar = {1:"Ocak",2:"Şubat",3:"Mart",4:"Nisan",5:"Mayıs",6:"Haziran",
              7:"Temmuz",8:"Ağustos",9:"Eylül",10:"Ekim",11:"Kasım",12:"Aralık"}
    if mod == "Günlük":
        return f"{tarih_obj.day} {aylar[tarih_obj.month]} {tarih_obj.year}"
    elif mod == "Haftalık":
        baslangic = tarih_obj - timedelta(days=tarih_obj.weekday())
        bitis = baslangic + timedelta(days=6)
        return f"{baslangic.day:02d} {aylar[baslangic.month][:3]} - {bitis.day:02d} {aylar[bitis.month][:3]} {bitis.year}"
    elif mod == "Aylık":
        return f"{aylar[tarih_obj.month]} {tarih_obj.year}"
    return str(tarih_obj)


def demo_veri_uret() -> pd.DataFrame:
    isimler = ["Jean (Fr)", "Ivan (Ru)", "Ahmed (Ar)", "Ayşe (Tr)", "John (En)", "Zeynep S."]
    yorumlar_iyi  = ["Yemekler harikaydı", "The service was fast", "Mükemmel", "Tavsiye ederim"]
    yorumlar_kotu = ["Yemekler soğuk geldi", "Personel kabaydı", "Hijyen sıfır", "Çok pahalı"]
    data = []
    bugun = datetime.now()
    for _ in range(150):
        if random.random() > 0.3:
            puan  = random.choice([4, 5])
            yorum = random.choice(yorumlar_iyi)
        else:
            puan  = random.choice([1, 2, 3])
            yorum = random.choice(yorumlar_kotu)
        tarih = bugun - timedelta(days=random.randint(0, 365))
        data.append({"author_title": random.choice(isimler), "review_rating": puan,
                     "review_text": yorum, "review_datetime_utc": tarih})
    return pd.DataFrame(data)


def analiz_et(df: pd.DataFrame, zaman_modu: str, date_range=None) -> Tuple[pd.DataFrame, pd.DataFrame]:
    df_copy = df.copy()
    df_copy["review_datetime_utc"] = pd.to_datetime(df_copy["review_datetime_utc"], utc=True, errors="coerce").dt.tz_convert(None)
    df_copy = df_copy.dropna(subset=["review_datetime_utc"])
    if date_range and len(date_range) == 2 and date_range[0] and date_range[1]:
        start_dt = pd.to_datetime(date_range[0])
        end_dt   = pd.to_datetime(date_range[1]) + pd.Timedelta(days=1) - pd.Timedelta(seconds=1)
        df_copy  = df_copy[(df_copy["review_datetime_utc"] >= start_dt) & (df_copy["review_datetime_utc"] <= end_dt)].copy()
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
    for i in [1, 2, 3, 4, 5]:
        if i not in analiz_tablosu.columns:
            analiz_tablosu[i] = 0
    analiz_tablosu = analiz_tablosu[[1, 2, 3, 4, 5]]
    analiz_tablosu.columns = [f"{c} Yıldız" for c in analiz_tablosu.columns]
    analiz_tablosu["TOPLAM YORUM"] = analiz_tablosu.sum(axis=1)
    ort_puan = df_copy.groupby("Zaman")["review_rating"].mean()
    analiz_tablosu["ORT. PUAN"] = ort_puan.reindex(analiz_tablosu.index).round(2)
    cols_yildiz = [c for c in analiz_tablosu.columns if c.endswith("Yıldız")]
    genel_toplam = analiz_tablosu[cols_yildiz + ["TOPLAM YORUM"]].sum(axis=0)
    analiz_tablosu.loc["GENEL TOPLAM", cols_yildiz] = genel_toplam[cols_yildiz].values
    analiz_tablosu.loc["GENEL TOPLAM", "TOPLAM YORUM"] = float(genel_toplam["TOPLAM YORUM"])
    analiz_tablosu.loc["GENEL TOPLAM", "ORT. PUAN"] = round(float(df_copy["review_rating"].mean()), 2)
    return analiz_tablosu, df_copy


def _normalize_text(s: str) -> str:
    s = (s or "").lower()
    s = re.sub(r"https?://\S+|www\.\S+", " ", s)
    s = re.sub(r"[^\w\sçğıöşüÇĞİÖŞÜ]", " ", s)
    s = re.sub(r"\s+", " ", s).strip()
    return s


def tekrar_eden_sikayetler(df: pd.DataFrame, top_n: int = 12, min_count: int = 3) -> pd.DataFrame:
    if df is None or df.empty:
        return pd.DataFrame()
    bad = df[df["review_rating"] <= 3].copy()
    if bad.empty:
        return pd.DataFrame()
    stop = set([
        "ve","veya","ama","çok","daha","bir","bu","şu","o","de","da","ile","için","gibi","kadar","her",
        "the","and","or","but","very","more","a","an","to","of","in","on","for","with","is","was","were",
        "it","we","they","you","i","my","our","your","at","as"
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
    out = pd.DataFrame([{"ifade": k, "adet": v} for k, v in counts.items()]).sort_values(["adet", "ifade"], ascending=[False, True])
    out = out[out["adet"] >= min_count].head(top_n).reset_index(drop=True)
    return out




def _first_existing_col(df: pd.DataFrame, candidates: List[str]) -> Optional[str]:
    for c in candidates:
        if c in df.columns:
            return c
    return None


def _yorum_metin_sec(row: pd.Series) -> str:
    adaylar = []
    for col in ["review_text_tr", "review_text"]:
        val = row.get(col, "")
        if pd.notna(val):
            val = str(val).strip()
            if val:
                adaylar.append(val)
    if not adaylar:
        return ""
    adaylar = sorted(adaylar, key=lambda x: (len(x.split()), len(x)), reverse=True)
    return adaylar[0]


def en_iyi_yorumlari_sec(
    df: pd.DataFrame,
    top_n: int = 20,
    recent_weight: float = 0.30,
    generic_penalty: float = 0.35
) -> pd.DataFrame:
    if df is None or df.empty:
        return pd.DataFrame()

    work = df.copy()
    rating_col = _first_existing_col(work, ["review_rating", "rating", "puan"])
    date_col = _first_existing_col(work, ["review_datetime_utc", "date", "datetime", "tarih"])
    author_col = _first_existing_col(work, ["author_title", "reviewer", "author", "name"])
    location_col = _first_existing_col(work, ["location_title", "location", "tesis"])
    if rating_col is None:
        return pd.DataFrame()

    work[rating_col] = pd.to_numeric(work[rating_col], errors="coerce")
    work = work[work[rating_col] >= 4].copy()
    if work.empty:
        return pd.DataFrame()

    work["secilen_yorum"] = work.apply(_yorum_metin_sec, axis=1)
    work["secilen_yorum"] = work["secilen_yorum"].fillna("").astype(str).str.strip()
    work = work[work["secilen_yorum"] != ""].copy()
    if work.empty:
        return pd.DataFrame()

    work["yorum_norm"] = work["secilen_yorum"].apply(_normalize_text)
    work["kelime_sayisi"] = work["secilen_yorum"].str.split().str.len().fillna(0)
    work["karakter_sayisi"] = work["secilen_yorum"].str.len().fillna(0)

    negatif_anahtarlar = {
        "kaba", "kabaydı", "rude", "berbat", "soğuk", "tuzlu", "kirli", "hijyen", "gürültü",
        "gürültülü", "pahalı", "cold", "salty", "dirty", "bad", "terrible", "awful", "şikayet"
    }
    work = work[~work["yorum_norm"].apply(lambda x: any(k in x for k in negatif_anahtarlar))].copy()
    if work.empty:
        return pd.DataFrame()

    generic_ifadeler = {
        "harika", "süper", "super", "mukemmel", "mükemmel", "good food", "nice place",
        "tavsiye ederim", "lezzetli", "good", "perfect", "great", "excellent"
    }
    work["jenerik_mi"] = work["yorum_norm"].isin(generic_ifadeler)

    if date_col:
        work[date_col] = pd.to_datetime(work[date_col], errors="coerce", utc=True).dt.tz_convert(None)
        valid_dates = work[date_col].dropna()
        if not valid_dates.empty:
            min_date = valid_dates.min()
            max_date = valid_dates.max()
            span_days = max((max_date - min_date).days, 1)
            work["yenilik_skoru"] = work[date_col].apply(
                lambda x: 0.0 if pd.isna(x) else round(((x - min_date).days / span_days), 6)
            )
        else:
            work["yenilik_skoru"] = 0.0
    else:
        work["yenilik_skoru"] = 0.0

    work["puan_skoru"] = work[rating_col].map({5: 1.0, 4: 0.82}).fillna(0.0)
    work["uzunluk_skoru"] = (work["kelime_sayisi"].clip(lower=0, upper=35) / 35.0).round(6)

    tekrar_sayisi = work["yorum_norm"].value_counts()
    work["tekrar_cezasi"] = work["yorum_norm"].map(lambda x: 0.22 if tekrar_sayisi.get(x, 0) > 1 else 0.0)
    work["jenerik_cezasi"] = work["jenerik_mi"].map(lambda x: generic_penalty if x else 0.0)

    work["secim_skoru"] = (
        work["puan_skoru"] * 0.52
        + work["uzunluk_skoru"] * 0.18
        + work["yenilik_skoru"] * recent_weight
        - work["tekrar_cezasi"]
        - work["jenerik_cezasi"]
    )

    work = work.sort_values(
        ["secim_skoru", "yenilik_skoru", rating_col, "kelime_sayisi"],
        ascending=[False, False, False, False]
    ).drop_duplicates(subset=["yorum_norm"], keep="first")

    bes = work[work[rating_col] == 5].head(top_n)
    if len(bes) < top_n:
        dort = work[work[rating_col] == 4].head(top_n - len(bes))
        secilen = pd.concat([bes, dort], ignore_index=True)
    else:
        secilen = bes.head(top_n).copy()

    secilen = secilen.sort_values(
        ["secim_skoru", "yenilik_skoru", rating_col, "kelime_sayisi"],
        ascending=[False, False, False, False]
    ).head(top_n).copy()

    secilen["yorum_tarihi"] = ""
    if date_col:
        secilen["yorum_tarihi"] = pd.to_datetime(secilen[date_col], errors="coerce").dt.strftime("%d.%m.%Y").fillna("")

    secilen["neden_seçildi"] = secilen.apply(
        lambda r: "Yeni + detaylı"
        if (r["yenilik_skoru"] >= 0.65 and r["kelime_sayisi"] >= 4)
        else ("Yeni yorum" if r["yenilik_skoru"] >= 0.65 else ("Detaylı yorum" if r["kelime_sayisi"] >= 4 else "Yüksek puanlı")),
        axis=1
    )

    out = pd.DataFrame({
        "Sıra": range(1, len(secilen) + 1),
        "Yazar": secilen[author_col].fillna("Misafir").astype(str) if author_col else "Misafir",
        "Puan": secilen[rating_col].astype(int),
        "Tarih": secilen["yorum_tarihi"],
        "Şube": secilen[location_col].fillna("").astype(str) if location_col else "",
        "Yorum": secilen["secilen_yorum"].astype(str),
        "Seçilme Nedeni": secilen["neden_seçildi"].astype(str),
        "Skor": secilen["secim_skoru"].round(3),
    })

    return out.reset_index(drop=True)


# ==========================================
# TRIPADVISOR ANALİZ FONKSİYONLARI
# ==========================================

def ta_isle(uploaded_file) -> pd.DataFrame:
    try:
        if str(uploaded_file.name).lower().endswith('.csv'):
            df = pd.read_csv(uploaded_file)
        else:
            df = pd.read_excel(uploaded_file)
    except Exception as e:
        st.error(f"Dosya okunamadı: {e}")
        return pd.DataFrame()

    rating_col = _first_existing_col(df, ["rating", "Rating", "puan", "stars"])
    date_col = _first_existing_col(df, ["publishedDate", "date", "createdAt", "review_date"])
    text_col = _first_existing_col(df, ["text", "review", "comment", "title"])
    author_col = _first_existing_col(df, ["user/name", "author", "reviewer", "name"])
    title_col = _first_existing_col(df, ["placeInfo/name", "location_title", "hotel", "place_name"])

    if rating_col is None or date_col is None:
        st.error("TripAdvisor dosyasında en az 'rating' ve 'publishedDate' benzeri sütunlar olmalı.")
        return pd.DataFrame()

    out = pd.DataFrame()
    out["author_title"] = df[author_col].fillna("Misafir").astype(str) if author_col else "Misafir"
    out["review_rating"] = pd.to_numeric(df[rating_col], errors="coerce")
    out["review_text"] = df[text_col].fillna("").astype(str) if text_col else ""
    out["review_datetime_utc"] = pd.to_datetime(df[date_col], errors="coerce", utc=True).dt.tz_convert(None)
    out["location_title"] = df[title_col].fillna("TripAdvisor").astype(str) if title_col else "TripAdvisor"
    out["kaynak"] = "tripadvisor"

    if "title" in df.columns:
        out["title"] = df["title"]
    if "tripType" in df.columns:
        out["tripType"] = df["tripType"]
    if "lang" in df.columns:
        out["lang"] = df["lang"]
    if "placeInfo/numberOfReviews" in df.columns:
        out["platform_total_reviews"] = pd.to_numeric(df["placeInfo/numberOfReviews"], errors="coerce")
    if "placeInfo/rating" in df.columns:
        out["platform_rating"] = pd.to_numeric(df["placeInfo/rating"], errors="coerce")

    out = out.dropna(subset=["review_rating", "review_datetime_utc"])
    out = out[(out["review_rating"] >= 1) & (out["review_rating"] <= 5)].copy()
    out["review_rating"] = out["review_rating"].round().astype(int)
    return out.reset_index(drop=True)


def db_tripadvisor_seed_yukle() -> int:
    eklenen_toplam = 0
    for seed_file_name in TRIPADVISOR_SEED_FILES:
        seed_path = os.path.join(DATA_DIR, seed_file_name)
        if not os.path.exists(seed_path):
            continue

        try:
            with open(seed_path, "rb") as seed_file:
                seed_df = ta_isle(seed_file)
            if not seed_df.empty:
                eklenen_toplam += db_tripadvisor_kaydet(seed_df)
        except Exception:
            pass

    return eklenen_toplam


db_tripadvisor_seed_yukle()


def ta_performans_alani(df: pd.DataFrame):
    st.markdown("### 🎯 Gelecek Performans Hedefi")

    platform_total = None
    platform_rating = None
    if "platform_total_reviews" in df.columns:
        s = pd.to_numeric(df["platform_total_reviews"], errors="coerce").dropna()
        if not s.empty:
            platform_total = int(s.iloc[0])
    if "platform_rating" in df.columns:
        s = pd.to_numeric(df["platform_rating"], errors="coerce").dropna()
        if not s.empty:
            platform_rating = float(s.iloc[0])

    col_sim_1, col_sim_2 = st.columns([2, 1])
    with col_sim_1:
        gelecek_yorum_sayisi = st.slider(
            "Hedeflenen yeni yorum sayısı:",
            min_value=10, max_value=3000, value=250, step=10, key="ta_future_reviews"
        )
        anlik_puan = platform_rating if platform_rating is not None else float(pd.to_numeric(df["review_rating"], errors="coerce").mean())
        if np.isnan(anlik_puan):
            anlik_puan = 0.0
        mevcut_puan = float(round(min(max(anlik_puan, 0.0), 5.0), 2))
        hedef_puan = st.slider(
            "Ulaşmak istediğiniz hedef puan:",
            min_value=mevcut_puan,
            max_value=5.0,
            value=min(5.0, round(max(mevcut_puan, 4.8), 2)),
            step=0.01,
            key="ta_target_rating"
        )
        st.caption(f"Mevcut puan: {mevcut_puan:.2f} | Hedef alt sınır bu değere göre ayarlanır")

    with col_sim_2:
        pred_df = yorum_hizi_tahmini(df.rename(columns={"review_datetime_utc": "review_datetime_utc"}), months_ahead=18, base_window=3, season_lookback_months=24)
        ay_sayisi, used_months = kac_ayda_yorum_hedefi(pred_df, int(gelecek_yorum_sayisi))

        if ay_sayisi <= 0 or used_months.empty:
            st.warning("Yorum hızı tahmini için yeterli geçmiş veri yok.")
        else:
            tahmini_aylik = float(used_months["pred_count"].mean()) if len(used_months) else 0.0
            st.metric("Tahmini Süre", f"{ay_sayisi} ay", delta=f"~{tahmini_aylik:.0f} yorum/ay")

            baz_ortalama = float(platform_rating) if platform_rating is not None else float(pd.to_numeric(df["review_rating"], errors="coerce").mean())
            baz_toplam = int(platform_total) if platform_total is not None else int(len(df))
            model_etiket = "TripAdvisor platform verisi" if platform_total is not None and platform_rating is not None else "Yüklenen dosya"

            gereken_ortalama = gereken_yeni_ortalama_basit(
                mevcut_ortalama=baz_ortalama,
                mevcut_yorum_sayisi=baz_toplam,
                hedef_puan=float(hedef_puan),
                yeni_yorum_sayisi=int(gelecek_yorum_sayisi)
            )

            if gereken_ortalama is None:
                st.warning("Gereken ortalama hesaplanamadı.")
            elif gereken_ortalama > 5.0:
                st.error("❌ İmkansız Hedef!")
                st.metric("Gereken Ort.", f"{gereken_ortalama:.2f}")
            elif gereken_ortalama < 1.0:
                st.success("✅ Hedefe Zaten Ulaşılmış")
            else:
                if gereken_ortalama > 4.8:
                    durum, renk = "⚠️ Zor", "🔴"
                elif gereken_ortalama > 4.0:
                    durum, renk = "⚡ Çalışılmalı", "🟡"
                else:
                    durum, renk = "✅ Başarılabilir", "🟢"
                st.metric(
                    label=f"{renk} Gereken Performans ({durum})",
                    value=f"{gereken_ortalama:.2f} / 5.0",
                    delta=f"{gelecek_yorum_sayisi} yeni yorumun ortalaması"
                )
                st.caption(
                    f"Hesap tabanı: {model_etiket} ({baz_ortalama:.2f} / {baz_toplam} yorum)\n\n"
                    f"Geçmiş hıza göre **{gelecek_yorum_sayisi}** yeni yoruma yaklaşık **{ay_sayisi} ayda** ulaşılır. "
                    f"Bu sürede hedef **{hedef_puan:.2f}** için yeni yorumların ortalaması **{gereken_ortalama:.2f}** ⭐ olmalı."
                )


def ta_analiz_goster(df: pd.DataFrame):
    st.markdown("### TripAdvisor Yorum Analizi")

    place_name = df["location_title"].dropna().iloc[0] if "location_title" in df.columns and not df["location_title"].dropna().empty else "TripAdvisor"
    platform_total = None
    platform_rating = None
    if "platform_total_reviews" in df.columns:
        s = pd.to_numeric(df["platform_total_reviews"], errors="coerce").dropna()
        if not s.empty:
            platform_total = int(s.iloc[0])
    if "platform_rating" in df.columns:
        s = pd.to_numeric(df["platform_rating"], errors="coerce").dropna()
        if not s.empty:
            platform_rating = float(s.iloc[0])

    st.caption(
        f"Tesis: **{place_name}**"
        + (f"  |  Platform toplam yorum: **{platform_total}**" if platform_total is not None else "")
        + (f"  |  Platform puanı: **{platform_rating:.2f}**" if platform_rating is not None else "")
    )

    toplam_yorum = platform_total if platform_total is not None else len(df)
    analiz_yorum = len(df)
    ort_puan = platform_rating if platform_rating is not None else float(df["review_rating"].mean())
    kotu = int((df["review_rating"] <= 3).sum())

    c1, c2, c3, c4 = st.columns(4)
    c1.metric("Toplam Yorum (TripAdvisor)" if platform_total is not None else "Toplam Yorum", int(toplam_yorum))
    c2.metric("Analize Dahil Yorum", int(analiz_yorum))
    c3.metric("Ortalama Puan (TripAdvisor)" if platform_rating is not None else "Ortalama Puan", f"{ort_puan:.2f}")
    c4.metric("Kötü Yorumlar", kotu)

    st.divider()
    ta_performans_alani(df)
    st.markdown("---")

    col_secim, _ = st.columns([1, 4])
    with col_secim:
        zaman_secimi = st.selectbox("⏳ Zaman Aralığı:", ["Haftalık", "Günlük", "Aylık", "Özel Aralık"], key="ta_time_mode")

    date_range = None
    zaman_modu = zaman_secimi

    if zaman_secimi == "Özel Aralık":
        _dt = pd.to_datetime(df["review_datetime_utc"], errors="coerce").dropna()
        if not _dt.empty:
            min_d = _dt.min().date()
            max_d = _dt.max().date()
        else:
            min_d = max_d = pd.Timestamp.today().date()
        cdr1, cdr2 = st.columns(2)
        with cdr1:
            start_d = st.date_input("Başlangıç Tarihi", value=min_d, min_value=min_d, max_value=max_d, key="ta_start")
        with cdr2:
            end_d = st.date_input("Bitiş Tarihi", value=max_d, min_value=min_d, max_value=max_d, key="ta_end")
        if start_d > end_d:
            start_d, end_d = end_d, start_d
        date_range = (start_d, end_d)
        zaman_modu = st.selectbox("Gruplama", ["Günlük", "Haftalık", "Aylık"], index=0, key="ta_group_mode")
        st.caption(f"Seçilen aralık: {start_d.strftime('%d.%m.%Y')} – {end_d.strftime('%d.%m.%Y')}")

    puan_tablosu, ham_veri = analiz_et(df, zaman_modu, date_range=date_range)

    _log_mem("ta_analiz", ham_veri)

    view = st.segmented_control(
        "Görünüm",
        ["📄 Puan Tablosu", "🚨 Şikayet Detayları", "🔁 Tekrar Eden Şikayetler", "⭐ En İyi Yorumlar", "📋 Veri Listesi"],
        default="📄 Puan Tablosu",
        key="ta_view_nav",
        label_visibility="collapsed",
    )
    if view is None:
        view = "📄 Puan Tablosu"

    # Yalnızca seçilen görünüm hesaplanır/render edilir (tüm sekmeler her rerun'da değil).
    if view == "📄 Puan Tablosu":
        render_title_with_logo(
            f"📄 {zaman_modu} Puan Dağılım Tablosu",
            "tripadvisor-logo-cropped.png",
            "tripadvisor-logo.png",
        )
        st.dataframe(puan_tablosu, width="stretch")

    elif view == "🚨 Şikayet Detayları":
        st.subheader("Dönemsel Şikayet Listesi (≤ 3 Yıldız)")
        sikayet_df = ham_veri[ham_veri["review_rating"] <= 3].copy()
        if not sikayet_df.empty:
            cols = [c for c in ["Zaman", "author_title", "review_rating", "review_text", "title", "tripType"] if c in sikayet_df.columns]
            render_df_paginated(sikayet_df[cols], key="ta_sikayet", width="stretch", hide_index=True)
        else:
            st.success("Bu dönemde şikayet bulunamadı.")

    elif view == "🔁 Tekrar Eden Şikayetler":
        st.subheader("Tekrar Eden Şikayet İfadeleri (Bigrams)")
        rep = tekrar_eden_sikayetler(ham_veri, top_n=12, min_count=3)
        if rep is None or rep.empty:
            st.info("Yeterli tekrar eden ifade bulunamadı (veya kötü yorum yok).")
        else:
            st.dataframe(rep, width="stretch", hide_index=True)

    elif view == "⭐ En İyi Yorumlar":
        st.subheader("Web Sitesi İçin Öne Çıkan 20 Yorum")
        en_iyi_df = en_iyi_yorumlari_sec(ham_veri, top_n=20)
        if en_iyi_df is None or en_iyi_df.empty:
            st.info("Yeterli nitelikli olumlu yorum bulunamadı.")
        else:
            st.dataframe(en_iyi_df, width="stretch", hide_index=True)

    else:  # "📋 Veri Listesi"
        render_df_paginated(ham_veri, key="ta_veri", width="stretch", hide_index=True)

    st.divider()
    # Excel raporu her rerun'da değil, yalnızca butona basınca üretilir.
    if st.button("📊 Raporu Hazırla (TripAdvisor)", key="ta_prep_report"):
        with st.spinner("TripAdvisor raporu hazırlanıyor..."):
            st.session_state["ta_excel_bytes"] = excel_indir(ham_veri, puan_tablosu)
            _log_mem("excel_generation_ta", ham_veri)
    if st.session_state.get("ta_excel_bytes"):
        st.download_button(
            label="📥 TripAdvisor Raporunu İndir",
            data=st.session_state["ta_excel_bytes"],
            file_name="TripAdvisor_Analiz_Raporu.xlsx",
            mime="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        )


def excel_indir(df: pd.DataFrame, puan_tablosu: pd.DataFrame) -> bytes:
    output = BytesIO()
    toplam   = len(df)
    ort_puan = float(df["review_rating"].mean()) if toplam else 0.0
    kotu     = int((df["review_rating"] <= 3).sum()) if toplam else 0
    kotu_oran = (kotu / toplam) if toplam else 0.0
    kpi = pd.DataFrame([{
        "Toplam Yorum": toplam, "Ortalama Puan": round(ort_puan, 2),
        "Kötü Yorum": kotu, "Kötü Oran (%)": round(kotu_oran * 100, 1),
    }])
    en_iyi = en_iyi_yorumlari_sec(df, top_n=20)
    with pd.ExcelWriter(output, engine="xlsxwriter") as writer:
        df_copy = df.copy()
        if "review_datetime_utc" in df_copy.columns:
            df_copy["review_datetime_utc"] = df_copy["review_datetime_utc"].astype(str)
        df_copy.to_excel(writer, sheet_name="Tum_Veriler", index=False)
        puan_tablosu.to_excel(writer, sheet_name="Puan_Analizi", index=True)
        kpi.to_excel(writer, sheet_name="KPI_Ozet", index=False)
        if en_iyi is not None and not en_iyi.empty:
            en_iyi.to_excel(writer, sheet_name="En_Iyi_Yorumlar", index=False)
    return output.getvalue()


# ==========================================
# 3. ARAYÜZ
# ==========================================
st.title("📊 Yorum Analiz Paneli")

# st.tabs yerine koşullu navigasyon: st.tabs HER İKİ platformun içeriğini de
# her rerun'da hesaplar; bu bellek baskısını artırır. segmented_control ile
# YALNIZCA seçilen platform ve analiz ekranı hesaplanır/render edilir.
platform = st.segmented_control(
    "Platform",
    ["🔵 Google Yorumları", "🟡 TripAdvisor Yorumları"],
    default="🔵 Google Yorumları",
    key="platform_nav",
    label_visibility="collapsed",
)
if platform is None:
    platform = "🔵 Google Yorumları"

if platform == "🟡 TripAdvisor Yorumları":
    st.markdown("### TripAdvisor Yorum Analizi")
    st.caption("TripAdvisor verileri veritabanından okunur. Yeni bir Excel/CSV dosyası eklemek isterseniz önce seçip sonra DB'ye kaydedin.")

    ta_db_adet = db_tripadvisor_adet()
    if ta_db_adet > 0:
        st.success(f"✅ Veritabanında kayıtlı TripAdvisor yorumu: {ta_db_adet}")
    else:
        st.info("Henüz TripAdvisor verisi yok. Dosyayı seçip veritabanına kaydedin.")

    col_ta1, col_ta2, col_ta3 = st.columns([3, 1, 1])

    with col_ta1:
        ta_file = st.file_uploader("Excel dosyası yükle (.xlsx / .xls / .csv)", type=["xlsx", "xls", "csv"], key="ta_uploader")

    with col_ta2:
        st.write("")
        st.write("")
        ta_kaydet = st.button("💾 Trip DB'ye Kaydet", width="stretch", key="ta_save_db")

    with col_ta3:
        st.write("")
        st.write("")
        ta_temizle = st.button("🗑️ Trip DB Temizle", width="stretch", key="ta_clear_db")

    if ta_temizle:
        db_tripadvisor_sil()
        st.success("TripAdvisor veritabanı temizlendi.")
        st.rerun()

    if ta_kaydet:
        if ta_file is None:
            st.warning("Önce bir TripAdvisor dosyası seçin.")
        else:
            with st.spinner("TripAdvisor dosyası veritabanına kaydediliyor..."):
                ta_df_raw = ta_isle(ta_file)
                if not ta_df_raw.empty:
                    eklenen = db_tripadvisor_kaydet(ta_df_raw)
                    toplam = db_tripadvisor_adet()
                    st.success(f"✅ {eklenen} yeni TripAdvisor yorumu veritabanına kaydedildi. DB toplam: {toplam}")
                    st.rerun()

    ta_df = db_tripadvisor_yukle()
    if ta_df is not None and not ta_df.empty:
        ta_analiz_goster(ta_df)
    else:
        st.info("📂 Veritabanında TripAdvisor verisi yok. Dosya seçip 'Trip DB'ye Kaydet' butonunu kullanın.")

elif platform == "🔵 Google Yorumları":
    if "data_loaded_at" in st.session_state and st.session_state.data_loaded_at:
        loaded_at = st.session_state.data_loaded_at
        gecen_dk  = int((datetime.now() - loaded_at).total_seconds() / 60)
        if gecen_dk < 60:
            st.caption(f"🕐 Son güncelleme: {gecen_dk} dakika önce ({loaded_at.strftime('%H:%M')})")
        else:
            st.warning(f"⚠️ Veri {gecen_dk // 60} saat önce çekildi. Sol panelden VERİLERİ ÇEK butonuna basarak güncelleyin.")

    if "data_frame" not in st.session_state:
        st.session_state.data_frame = None

    df_loaded = st.session_state.get("data_frame") is not None
    access_token = None
    err = None

    # =========================
    # A) VERİ YOKKEN
    # =========================
    if not df_loaded:
        st.sidebar.markdown("## 🧭 Kontrol Paneli")
        st.sidebar.caption("Adım adım ilerleyin")
        st.sidebar.divider()

        st.sidebar.markdown('<div class="sb-card">', unsafe_allow_html=True)
        st.sidebar.markdown('<div class="sb-title">① Veri Modu</div>', unsafe_allow_html=True)
        mod = st.sidebar.radio("Çalışma tipi", ["Gerçek Veri (API)", "Demo Modu"], label_visibility="collapsed")
        st.sidebar.markdown('<div class="sb-muted">Demo ile test, API ile canlı çekim.</div>', unsafe_allow_html=True)
        st.sidebar.markdown("</div>", unsafe_allow_html=True)

        st.sidebar.markdown('<div class="sb-card">', unsafe_allow_html=True)
        st.sidebar.markdown('<div class="sb-title">② Veri Kaynağı</div>', unsafe_allow_html=True)

        if mod == "Demo Modu":
            if st.sidebar.button("🚀 Demo Verileri Yükle", width="stretch"):
                raw_df = demo_veri_uret()
                st.session_state.data_frame = raw_df
                st.session_state.data_loaded_at = datetime.now()
                _log_mem("data_load_demo", raw_df)
                st.sidebar.success("✅ Demo verileri yüklendi!")
                st.rerun()
        else:
            if CLIENT_ID and CLIENT_SECRET and REFRESH_TOKEN:
                st.sidebar.info("OAuth bilgileri Secrets'tan yüklendi.")
                token, err = access_token_from_refresh_token(CLIENT_ID, CLIENT_SECRET, REFRESH_TOKEN)
                if not err:
                    access_token = token
            else:
                st.sidebar.error("OAuth bilgileri eksik! Secrets'a ekleyin.")

        st.sidebar.markdown("</div>", unsafe_allow_html=True)

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

        if not PLACES_API_KEY:
            st.sidebar.warning("⚠️ PLACES_API_KEY secrets'ta yok.")
        else:
            st.sidebar.markdown("✅ Places API Key Hazır")

        st.sidebar.markdown("🟡 Veri Yüklenmedi")
        st.sidebar.markdown("</div>", unsafe_allow_html=True)

        selected_account_name  = None
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
                                loc_options    = {f"{l.get('title','(isimsiz)')} — {l.get('name')}": l for l in locs}
                                default_labels = list(loc_options.keys())
                                chosen = st.sidebar.multiselect("Tesis(ler) (Location)", options=list(loc_options.keys()), default=[])
                                selected_locations = [loc_options[c] for c in chosen]
                                parallel_fetch = st.sidebar.checkbox("Hızlı çek (paralel)", value=False)
                            else:
                                st.sidebar.warning("Bu account altında tesis bulunamadı.")
                        else:
                            st.sidebar.warning("Meroddi Hotels işletme hesabı bulunamadı.")
                    else:
                        st.sidebar.warning("Accounts listesi boş.")
                except Exception:
                    st.sidebar.warning("Accounts/locations okunamadı.")
            else:
                st.sidebar.warning("OAuth token yok veya alınamadı.")

            st.sidebar.markdown("</div>", unsafe_allow_html=True)

            st.sidebar.markdown('<div class="sb-card">', unsafe_allow_html=True)
            st.sidebar.markdown('<div class="sb-title">🚀 İşlem</div>', unsafe_allow_html=True)

            veri_cek = st.sidebar.button("🌍 VERİLERİ ÇEK", width="stretch")

            if veri_cek:
                if not access_token:
                    st.sidebar.warning("OAuth token yok.")
                elif not selected_account_name:
                    st.sidebar.warning("Account seçilemedi.")
                elif not selected_locations:
                    st.sidebar.warning("En az 1 tesis seçiniz.")
                else:
                    raw_df = fetch_reviews_for_locations(access_token, selected_account_name, selected_locations, parallel=parallel_fetch)
                    if not raw_df.empty:
                        raw_df["kaynak"] = "api"
                        birlesik = api_ile_birlestir(raw_df)
                        st.session_state.data_frame = birlesik
                        st.session_state.data_loaded_at = datetime.now()
                        _log_mem("data_load_api", birlesik)
                        os_adet = db_outscraper_adet()
                        if os_adet > 0:
                            st.sidebar.success(f"✅ {len(raw_df)} API + {os_adet} DB = {len(birlesik)} toplam")
                        else:
                            st.sidebar.success(f"✅ {len(birlesik)} yorum yüklendi")
                        st.rerun()
                    else:
                        st.sidebar.error("Hiç yorum bulunamadı (veya yetki yok).")

            st.sidebar.markdown("</div>", unsafe_allow_html=True)

    # =========================
    # B) VERİ VARKEN
    # =========================
    else:
        st.sidebar.markdown("## 🧭 Kontrol Paneli")
        st.sidebar.caption("Filtreleyip analiz edin")
        st.sidebar.divider()

        df = st.session_state.data_frame

        st.sidebar.markdown('<div class="sb-card">', unsafe_allow_html=True)
        st.sidebar.markdown('<div class="sb-title">🛰️ Sistem Durumu</div>', unsafe_allow_html=True)
        st.sidebar.markdown("🟢 Veri Hazır")
        if not PLACES_API_KEY:
            st.sidebar.warning("⚠️ PLACES_API_KEY yok → Google KPI kapalı")
        st.sidebar.markdown("</div>", unsafe_allow_html=True)

        st.sidebar.markdown('<div class="sb-card">', unsafe_allow_html=True)
        st.sidebar.markdown('<div class="sb-title">🎛️ Filtreler</div>', unsafe_allow_html=True)
        min_star, max_star = st.sidebar.slider("Yıldız aralığı", 1, 5, (1, 5))
        keyword = st.sidebar.text_input("Yorum içinde ara (keyword)", value="")
        loc_choice = None
        if "location_title" in df.columns:
            locs = sorted(df["location_title"].dropna().unique().tolist())
            loc_choice = st.sidebar.multiselect("Tesis", options=locs, default=locs)
        st.sidebar.markdown("</div>", unsafe_allow_html=True)

        c1, c2 = st.sidebar.columns(2)
        with c1:
            if st.sidebar.button("🔄 Yenile", width="stretch"):
                st.rerun()
        with c2:
            if st.sidebar.button("🧹 Veriyi Sıfırla", width="stretch"):
                st.session_state.data_frame = None
                st.rerun()

        st.sidebar.divider()
        os_adet = db_outscraper_adet()
        st.sidebar.markdown("### 📥 Yorum Veritabanı")
        if os_adet > 0:
            st.sidebar.success(f"✅ {os_adet} yorum kayıtlı (DB)")
        else:
            st.sidebar.info("DB boş. Excel/CSV yükle.")

        os_file = st.sidebar.file_uploader("Excel veya CSV yükle", type=["csv", "xlsx", "xls"], key="os_uploader")
        if os_file:
            if st.sidebar.button("💾 DB'ye Kaydet", width="stretch"):
                with st.spinner("İşleniyor..."):
                    os_df = dosya_isle(os_file)
                    if not os_df.empty:
                        eklenen = db_outscraper_kaydet(os_df)
                        st.sidebar.success(f"✅ {eklenen} yeni yorum eklendi. DB toplam: {db_outscraper_adet()}")
                        mevcut = st.session_state.get("data_frame")
                        if mevcut is not None and "kaynak" in mevcut.columns:
                            st.session_state.data_frame = api_ile_birlestir(mevcut[mevcut["kaynak"] == "api"].copy())
                        st.rerun()
                    else:
                        st.sidebar.error("Dosya işlenemedi.")

        if os_adet > 0:
            if st.sidebar.button("🗑️ DB'yi Temizle", width="stretch", type="secondary"):
                db_outscraper_sil()
                st.sidebar.success("DB temizlendi.")
                st.rerun()

        st.session_state["flt_min_star"] = min_star
        st.session_state["flt_max_star"] = max_star
        st.session_state["flt_keyword"]  = keyword
        st.session_state["flt_locs"]     = loc_choice

    # ---- ANA EKRAN ----
    df = st.session_state.data_frame

    if df is not None and not df.empty:
        min_star   = st.session_state.get("flt_min_star", 1)
        max_star   = st.session_state.get("flt_max_star", 5)
        keyword    = st.session_state.get("flt_keyword", "")
        loc_choice = st.session_state.get("flt_locs", None)

        df = df.copy()
        df = df[(df["review_rating"] >= min_star) & (df["review_rating"] <= max_star)]
        if "location_title" in df.columns and loc_choice:
            df = df[df["location_title"].isin(loc_choice)].copy()
        if keyword and keyword.strip():
            k = keyword.strip().lower()
            df = df[df["review_text"].fillna("").astype(str).str.lower().str.contains(k)].copy()

        g_rating, g_total, g_detail = None, None, pd.DataFrame()
        if PLACES_API_KEY and "place_id" in df.columns:
            place_ids = df["place_id"].dropna().astype(str).tolist()
            if place_ids:
                g_rating, g_total, g_detail = google_kpi_from_place_ids(place_ids)

        c1, c2, c3 = st.columns(3)
        c1.metric("Toplam Yorum (Google)" if g_total else "Toplam Yorum", int(g_total) if g_total else len(df))
        puan = df["review_rating"].mean() if len(df) else 0.0
        if g_rating:
            c2.metric("Ortalama Puan (Google)", f"{g_rating:.2f}")
        else:
            delta_color = "inverse" if puan >= 4.5 else ("off" if puan < 3.5 else "normal")
            c2.metric("Ortalama Puan", f"{puan:.2f}", delta_color=delta_color)
        c3.metric("Kötü Yorumlar", int((df["review_rating"] <= 3).sum()))

        if "kaynak" in df.columns:
            parts = []
            for k_src, v in df["kaynak"].value_counts().items():
                etiket = "🌐 Google API" if k_src == "api" else "💾 Veritabanı"
                parts.append(f"{etiket}: **{v}**")
            st.caption("Kaynak — " + "  |  ".join(parts))

        st.divider()

        st.markdown("### 🎯 Gelecek Performans Hedefi")

        col_sim_1, col_sim_2 = st.columns([2, 1])

        with col_sim_1:
            gelecek_yorum_sayisi = st.slider(
                "Hedeflenen yeni yorum sayısı:",
                min_value=10, max_value=3000, value=1000, step=10
            )

            if g_rating is not None:
                anlik_puan = float(g_rating)
            else:
                anlik_puan = float(pd.to_numeric(df["review_rating"], errors="coerce").mean())
            if np.isnan(anlik_puan):
                anlik_puan = 0.0
            mevcut_google_puan = float(round(min(max(anlik_puan, 0.0), 5.0), 2))
            yeni_hedef_puan = st.slider(
                "Ulaşmak istediğiniz hedef puan:",
                min_value=mevcut_google_puan,
                max_value=5.0,
                value=mevcut_google_puan,
                step=0.01
            )
            st.caption(
                f"Mevcut puan: {mevcut_google_puan:.2f} | Hedef alt sınır bu değere göre ayarlanır"
            )

        with col_sim_2:
            pred_df = yorum_hizi_tahmini(df, months_ahead=18, base_window=3, season_lookback_months=24)
            ay_sayisi, used_months = kac_ayda_yorum_hedefi(pred_df, int(gelecek_yorum_sayisi))

            if ay_sayisi <= 0 or used_months.empty:
                st.warning("Yorum hızı tahmini için yeterli geçmiş veri yok.")
            else:
                tahmini_aylik = float(used_months["pred_count"].mean()) if len(used_months) else 0.0
                st.metric("Tahmini Süre", f"{ay_sayisi} ay", delta=f"~{tahmini_aylik:.0f} yorum/ay")

                if g_rating is not None and g_total is not None and int(g_total) > 0:
                    baz_ortalama = float(g_rating)
                    baz_toplam = int(g_total)
                    model_etiket = "Google KPI tabanı"
                else:
                    baz_ortalama = float(pd.to_numeric(df["review_rating"], errors="coerce").mean())
                    baz_toplam = int(len(df))
                    model_etiket = "Filtrelenmiş veri tabanı"

                gereken_ortalama = gereken_yeni_ortalama_basit(
                    mevcut_ortalama=baz_ortalama,
                    mevcut_yorum_sayisi=baz_toplam,
                    hedef_puan=float(yeni_hedef_puan),
                    yeni_yorum_sayisi=int(gelecek_yorum_sayisi)
                )

                if gereken_ortalama is None:
                    st.warning("Gereken ortalama hesaplanamadı (veri/format kontrolü gerekli).")
                else:
                    if gereken_ortalama > 5.0:
                        st.error("❌ İmkansız Hedef!")
                        st.metric("Gereken Ort.", f"{gereken_ortalama:.2f}")
                    elif gereken_ortalama < 1.0:
                        st.success("✅ Hedefe Zaten Ulaşılmış")
                    else:
                        if gereken_ortalama > 4.8:
                            durum, renk = "⚠️ Zor", "🔴"
                        elif gereken_ortalama > 4.0:
                            durum, renk = "⚡ Çalışılmalı", "🟡"
                        else:
                            durum, renk = "✅ Başarılabilir", "🟢"

                        st.metric(
                            label=f"{renk} Gereken Performans ({durum})",
                            value=f"{gereken_ortalama:.2f} / 5.0",
                            delta=f"{gelecek_yorum_sayisi} yeni yorumun ortalaması"
                        )

                        st.caption(
                            f"Hesap tabanı: {model_etiket} ({baz_ortalama:.2f} / {baz_toplam} yorum)\n\n"
                            f"Geçmiş aylara göre tahminle **{gelecek_yorum_sayisi}** yeni yoruma "
                            f"yaklaşık **{ay_sayisi} ayda** ulaşılır. "
                            f"Bu sürede hedef **{yeni_hedef_puan:.2f}** için "
                            f"yeni yorumların ortalaması **{gereken_ortalama:.2f}** ⭐ olmalı."
                        )

        st.markdown("---")

        col_secim, _ = st.columns([1, 4])
        with col_secim:
            zaman_secimi = st.selectbox("⏳ Zaman Aralığı:", ["Haftalık", "Günlük", "Aylık", "Özel Aralık"])

        date_range = None
        zaman_modu = zaman_secimi

        if zaman_secimi == "Özel Aralık":
            _dt = pd.to_datetime(df["review_datetime_utc"], errors="coerce", utc=True).dt.tz_convert(None).dropna()
            if not _dt.empty:
                min_d = _dt.min().date()
                max_d = _dt.max().date()
            else:
                min_d = max_d = pd.Timestamp.today().date()
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

        _log_mem("google_analiz", ham_veri)

        view = st.segmented_control(
            "Görünüm",
            ["📄 Puan Tablosu", "🚨 Şikayet Detayları", "🔁 Tekrar Eden Şikayetler", "⭐ En İyi Yorumlar", "📋 Veri Listesi"],
            default="📄 Puan Tablosu",
            key="google_view_nav",
            label_visibility="collapsed",
        )
        if view is None:
            view = "📄 Puan Tablosu"

        # Yalnızca seçilen görünüm hesaplanır/render edilir (tüm sekmeler her rerun'da değil).
        if view == "📄 Puan Tablosu":
            render_title_with_logo(
                f"📄 {zaman_modu} Puan Dağılım Tablosu",
                "google-logo-cropped.png",
                "google-logo.png",
            )
            st.dataframe(puan_tablosu, width="stretch")

        elif view == "🚨 Şikayet Detayları":
            st.subheader("Dönemsel Şikayet Listesi (≤ 3 Yıldız)")
            sikayet_df = ham_veri[ham_veri["review_rating"] <= 3].copy()
            if not sikayet_df.empty:
                cols = [c for c in ["Zaman", "author_title", "review_rating", "review_text"] if c in sikayet_df.columns]
                render_df_paginated(sikayet_df[cols], key="google_sikayet", width="stretch", hide_index=True)
            else:
                st.success("Bu dönemde şikayet bulunamadı.")

        elif view == "🔁 Tekrar Eden Şikayetler":
            st.subheader("Tekrar Eden Şikayet İfadeleri (Bigrams)")
            rep = tekrar_eden_sikayetler(ham_veri, top_n=12, min_count=3)
            if rep is None or rep.empty:
                st.info("Yeterli tekrar eden ifade bulunamadı (veya kötü yorum yok).")
            else:
                st.dataframe(rep, width="stretch", hide_index=True)

        elif view == "⭐ En İyi Yorumlar":
            st.subheader("Web Sitesi İçin Öne Çıkan 20 Yorum")
            st.caption("Seçimde 5 yıldız önceliği, yorumun güncelliği, metnin açıklayıcı olması ve tekrar etmeme kriterleri kullanılır.")
            en_iyi_df = en_iyi_yorumlari_sec(ham_veri, top_n=20)
            if en_iyi_df is None or en_iyi_df.empty:
                st.info("Yeterli nitelikli olumlu yorum bulunamadı.")
            else:
                st.dataframe(
                    en_iyi_df.drop(columns=["Skor"]),
                    width="stretch",
                    hide_index=True,
                    column_config={
                        "Yorum": st.column_config.TextColumn("Yorum", width="large"),
                        "Seçilme Nedeni": st.column_config.TextColumn("Seçilme Nedeni", width="medium"),
                    }
                )

        else:  # "📋 Veri Listesi"
            render_df_paginated(ham_veri, key="google_veri", width="stretch")

        st.divider()
        # Excel raporu her rerun'da değil, yalnızca butona basınca üretilir.
        if st.button("📊 Raporu Hazırla (Google)", key="google_prep_report"):
            with st.spinner("Rapor hazırlanıyor..."):
                st.session_state["google_excel_bytes"] = excel_indir(ham_veri, puan_tablosu)
                _log_mem("excel_generation_google", ham_veri)
        if st.session_state.get("google_excel_bytes"):
            st.download_button(
                label="📥 Raporu Excel Olarak İndir",
                data=st.session_state["google_excel_bytes"],
                file_name="Rapor.xlsx",
                mime="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            )
