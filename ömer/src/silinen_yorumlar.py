"""Silinen TripAdvisor yorumları: doğrulanmış JSON dosyasını okur, kontrol eder,
arşiv satırlarına ve Excel raporuna çevirir.

Streamlit'e bağlı değildir. Yorumlar işletme + TripAdvisor yorum kimliğiyle
eşleşir; yazar, metin, çeviri veya tarih değişse de aynı yorum sayılır.
"""
from __future__ import annotations

import json
import re
from html import escape
from io import BytesIO
from typing import Any, Dict, List, Optional, Tuple

import pandas as pd

BILINMEYEN_YAZAR = "Bilinmiyor"
ARSIV_SUTUNLARI = (
    "anahtar", "review_id", "location_id", "location_title", "author_title", "review_rating",
    "review_datetime_utc", "title", "review_text", "review_url", "lang", "original_language",
    "is_machine_translated", "tripType", "son_gorulen_cekim", "ilk_eksik_cekim",
    "referans_cekim", "referans_tarihi",
)
_ZORUNLU = ("review_id", "review_rating", "review_datetime_utc", "review_text")
_TIPLI = ("review_rating", "review_datetime_utc", "is_machine_translated")
_AYLAR = ("Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran",
          "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık")
_CEKIM_ZAMANI = re.compile(r"_(\d{4})-(\d{2})-(\d{2})_(\d{2})-(\d{2})-")
_KIMLIK_ANAHTARI = r"\d*\|\d+"  # "işletme|yorum" kimliği; eski yüklemelerden gelen anahtarlar başka biçimde


def cekim_etiketi(dosya_adi: Any, yedek: Optional[str] = None) -> str:
    """'dataset_..._2026-10-02_06-28-29-596.xlsx' -> '02.10.2026 06:28'; tanınmayan adda `yedek`, o da yoksa ad."""
    metin = _metin(dosya_adi)
    m = _CEKIM_ZAMANI.search(metin)
    if m:
        return f"{m[3]}.{m[2]}.{m[1]} {m[4]}:{m[5]}"
    return metin if yedek is None else yedek


def cekim_tarihi(dosya_adi: Any) -> str:
    """Dosya adındaki çekim günü ('2026-10-02'); yoksa boş."""
    m = _CEKIM_ZAMANI.search(_metin(dosya_adi))
    return f"{m[1]}-{m[2]}-{m[3]}" if m else ""


def ay_adi(tarih) -> str:
    return f"{_AYLAR[tarih.month - 1]} {tarih.year}"


def _metin(deger: Any) -> str:
    if deger is None or (pd.api.types.is_scalar(deger) and pd.isna(deger)):
        return ""
    return str(deger).strip()


def _evet_mi(deger: Any) -> bool:
    if isinstance(deger, bool):
        return deger
    return _metin(deger).lower() in ("true", "1", "evet", "yes")


def hazirla(df: pd.DataFrame) -> pd.DataFrame:
    """JSON'dan ya da tablodan gelen satırları analiz için tiplere çevirir."""
    df = df.copy()
    for s in ARSIV_SUTUNLARI:
        if s not in df.columns:
            df[s] = ""
    for s in ARSIV_SUTUNLARI:
        if s not in _TIPLI:
            df[s] = df[s].map(_metin)
    df["review_rating"] = pd.to_numeric(df["review_rating"], errors="coerce")
    # Yayın tarihi kaynakta yalnız gün olarak var; saat uydurulmaz.
    df["review_datetime_utc"] = pd.to_datetime(
        df["review_datetime_utc"].map(_metin).str[:10], errors="coerce", format="%Y-%m-%d"
    )
    df["is_machine_translated"] = df["is_machine_translated"].map(_evet_mi)
    df = df.dropna(subset=["review_rating", "review_datetime_utc"])
    df = df[df["anahtar"] != ""].copy()
    df["review_rating"] = df["review_rating"].round().astype(int)
    return df.drop_duplicates("anahtar", keep="last").reset_index(drop=True)


def veri_oku(icerik: bytes) -> Tuple[pd.DataFrame, Dict[str, Any]]:
    """Doğrulanmış silinen yorum JSON'unu okur; bozuk dosyada ValueError verir."""
    try:
        veri = json.loads(icerik.decode("utf-8-sig"))
    except (UnicodeDecodeError, json.JSONDecodeError) as e:
        raise ValueError("Dosya okunamadı: geçerli bir JSON dosyası değil.") from e
    yorumlar = veri.get("reviews") if isinstance(veri, dict) else None
    if not isinstance(yorumlar, list) or not yorumlar:
        raise ValueError("Dosyada yorum listesi ('reviews') bulunamadı.")

    referans_cekim = cekim_etiketi(veri.get("reference_snapshot"))
    satirlar = []
    for i, r in enumerate(yorumlar, 1):
        if not isinstance(r, dict):
            raise ValueError(f"{i}. kayıt okunamadı.")
        konum = _metin(r.get("location_id")) or _metin(veri.get("location_id"))
        eksik = [a for a in _ZORUNLU if _metin(r.get(a)) == ""] + ([] if konum else ["location_id"])
        if eksik:
            raise ValueError(f"{i}. yorumda zorunlu alan eksik: {', '.join(eksik)}.")
        yorum_id = _metin(r["review_id"])
        satirlar.append({
            "anahtar": f"{konum}|{yorum_id}",
            "review_id": yorum_id,
            "location_id": konum,
            "location_title": r.get("location_title") or veri.get("location_title"),
            "author_title": r.get("author_title"),
            "review_rating": r.get("review_rating"),
            "review_datetime_utc": r.get("review_datetime_utc"),
            "title": r.get("title"),
            "review_text": r.get("review_text"),
            "review_url": r.get("review_url"),
            "lang": r.get("lang"),
            "original_language": r.get("original_language"),
            "is_machine_translated": r.get("is_machine_translated"),
            "tripType": r.get("tripType"),
            "son_gorulen_cekim": cekim_etiketi(r.get("last_seen_snapshot")),
            "ilk_eksik_cekim": cekim_etiketi(r.get("first_absent_snapshot_after_last_seen")),
            "referans_cekim": referans_cekim,
            "referans_tarihi": veri.get("reference_date"),
        })
    bilgi = {k: v for k, v in veri.items() if k != "reviews"}
    return hazirla(pd.DataFrame(satirlar)), bilgi


def _sayim(sozluk: Any) -> Dict[str, int]:
    """{ay ya da yıldız: adet} sözlüğü ya da value_counts sonucu; sıfırlar atılır."""
    if not hasattr(sozluk, "items"):
        return {}
    return {str(k): int(v) for k, v in sozluk.items() if int(v)}


def kontroller(df: pd.DataFrame, bilgi: Dict[str, Any]) -> Tuple[pd.DataFrame, bool]:
    """Dosyadaki beklenen değerleri yeniden hesaplananla karşılaştırır; (tablo, hepsi tuttu mu) döner."""
    def aylar(d):
        return " · ".join(f"{ay_adi(pd.Period(k).to_timestamp())}: {v}" for k, v in sorted(d.items()))

    def yildizlar(d):
        return " · ".join(f"{k} yıldız: {v}" for k, v in sorted(d.items(), reverse=True))

    sayilar = bilgi.get("counts") if isinstance(bilgi.get("counts"), dict) else {}
    ortalama = round(float(df["review_rating"].mean()), 2) if len(df) else 0.0
    satirlar = [
        ("Farklı yorum (yorum kimliği)", sayilar.get("absent_reviews"), int(df["anahtar"].nunique()), str, None),
        ("Yayın ayları", bilgi.get("expected_publication_month_counts"),
         _sayim(df["review_datetime_utc"].dt.to_period("M").astype(str).value_counts()), aylar, _sayim),
        ("Yıldız dağılımı", bilgi.get("expected_rating_counts"),
         _sayim(df["review_rating"].value_counts()), yildizlar, _sayim),
        ("Ortalama puan", bilgi.get("expected_mean_rating"), ortalama, lambda x: f"{float(x):.2f}", None),
        ("Yazarı bilinmeyen", bilgi.get("expected_unknown_author_count"), int((df["author_title"] == "").sum()), str, None),
        ("Makine çevirisi", bilgi.get("expected_machine_translated_count"), int(df["is_machine_translated"].sum()), str, None),
    ]
    tablo, hepsi_tuttu = [], True
    for ad, beklenen, bulunan, goster, cevir in satirlar:
        if beklenen is None:
            durum = "Dosyada beklenen değer yok"
        else:
            beklenen = cevir(beklenen) if cevir else beklenen
            if ad == "Ortalama puan":
                tuttu = abs(float(beklenen) - bulunan) < 0.005
            else:
                tuttu = (beklenen == bulunan) if cevir else int(beklenen) == bulunan
            durum = "Tamam" if tuttu else "Tutmuyor"
            hepsi_tuttu = hepsi_tuttu and tuttu
        tablo.append({
            "Kontrol": ad,
            "Dosyada beklenen": "—" if beklenen is None else goster(beklenen),
            "Hesaplanan": goster(bulunan),
            "Durum": durum,
        })
    return pd.DataFrame(tablo), hepsi_tuttu


def referans_metni(df: pd.DataFrame, son_yukleme: Optional[Tuple[str, str]] = None) -> str:
    """'2 Kasım 2026 son çekimi (02.11.2026 05:18)'; bilgi yoksa boş.

    Arşivdeki en yeni referans kullanılır; `son_yukleme` (gün, çekim adı) daha yeniyse o, çünkü
    her yükleme arşivi günceller: listede o yüklemede bulunmayan yorumlar kalır.
    """
    adaylar = []
    if not df.empty:
        son = df.sort_values("referans_tarihi").iloc[-1]
        adaylar.append((son["referans_tarihi"], son["referans_cekim"]))
    if son_yukleme and son_yukleme[0]:
        adaylar.append(son_yukleme)
    if not adaylar:
        return ""
    gun, cekim = max(adaylar, key=lambda a: str(a[0]))
    tarih = pd.to_datetime(gun, errors="coerce")
    metin = "son çekim" if pd.isna(tarih) else f"{tarih.day} {ay_adi(tarih)} son çekimi"
    return f"{metin} ({cekim})" if cekim else metin


def liste_tablosu(df: pd.DataFrame) -> pd.DataFrame:
    """Ekranda ve Excel'de gösterilen yorum listesi, yeniden eskiye."""
    df = df.sort_values(["review_datetime_utc", "review_id"], ascending=[False, True])
    return pd.DataFrame({
        "Yayın tarihi": df["review_datetime_utc"].dt.strftime("%d.%m.%Y"),
        "Başlık": df["title"],
        "Yorum": df["review_text"],
        "Yazar": df["author_title"].replace("", BILINMEYEN_YAZAR),
        "Puan": df["review_rating"],
        "Makine çevirisi": df["is_machine_translated"].map({True: "Evet", False: "Hayır"}),
        "Son görüldüğü çekim": df["son_gorulen_cekim"],
        "İlk eksik çekim": df["ilk_eksik_cekim"],
        "Yorum ID": df["review_id"],
        "Bağlantı": df["review_url"],
    }).reset_index(drop=True)


def kart_html(satir: pd.Series) -> str:
    """'Tam metinleri oku' kartı; yorum kaçışlanır ve olduğu gibi gösterilir (çevrilmez, kısaltılmaz)."""
    dil = satir["original_language"] or satir["lang"]
    ceviri = f"Makine çevirisi (orijinal dil: {dil})" if satir["is_machine_translated"] else f"Orijinal metin ({dil})"
    detay = " · ".join([
        satir["review_datetime_utc"].strftime("%d.%m.%Y"),
        satir["author_title"] or BILINMEYEN_YAZAR,
        f"{satir['review_rating']} yıldız",
        ceviri,
    ])
    cekim = escape(f"Son görüldüğü çekim {satir['son_gorulen_cekim']} · İlk eksik çekim {satir['ilk_eksik_cekim']}")
    if satir["review_url"].startswith("https://"):
        cekim += f' · <a href="{escape(satir["review_url"])}" target="_blank">TripAdvisor\'da aç</a>'
    # Boş satır kalmasın: Streamlit'te HTML bloğu boş satırda biter, kalan metin markdown sayılır.
    metin = escape(satir["review_text"].replace("\r\n", "\n")).replace("\n", "<br>")
    return (
        '<div class="ta-silinen-kart">'
        f'<div class="ta-silinen-kart__baslik">{escape(satir["title"])}</div>'
        f'<div class="ta-silinen-kart__detay">{escape(detay)}</div>'
        f'<div class="ta-silinen-kart__metin">{metin}</div>'
        f'<div class="ta-silinen-kart__detay">{cekim}</div>'
        "</div>"
    )


def excel_raporu(puan_tablosu: pd.DataFrame, liste: pd.DataFrame, bilgi_satirlari: List[Tuple[str, Any]]) -> bytes:
    """Seçilen kapsamın raporu: Bilgi, Puan_Analizi ve Silinen_Yorumlar sayfaları."""
    cikti = BytesIO()
    # "=" ile başlayan yorumlar formül sayılmasın.
    with pd.ExcelWriter(cikti, engine="xlsxwriter", engine_kwargs={"options": {"strings_to_formulas": False}}) as writer:
        pd.DataFrame(bilgi_satirlari, columns=["Bilgi", "Değer"]).to_excel(writer, sheet_name="Bilgi", index=False)
        puan_tablosu.to_excel(writer, sheet_name="Puan_Analizi", index=True)
        liste.to_excel(writer, sheet_name="Silinen_Yorumlar", index=False)
    return cikti.getvalue()


def yuklemeden_kayit(kayit: Dict[str, Any], son_gorulen: str, ilk_eksik: str, referans_tarihi: str) -> Dict[str, Any]:
    """Normal yükleme karşılaştırmasında silinen bulunan satırı arşiv satırına çevirir."""
    anahtar = _metin(kayit.get("anahtar"))
    kimlik = re.fullmatch(r"(\d*)\|(\d+)", anahtar)
    yazar = _metin(kayit.get("author_title"))
    return {
        "anahtar": anahtar,
        "review_id": kimlik[2] if kimlik else "",
        "location_id": kimlik[1] if kimlik else "",
        "location_title": kayit.get("location_title"),
        "author_title": "" if yazar.lower() == "misafir" else yazar,  # yükleme, yazarsız yoruma "Misafir" yazar
        "review_rating": kayit.get("review_rating"),
        "review_datetime_utc": kayit.get("review_datetime_utc"),
        "title": kayit.get("title"),
        "review_text": kayit.get("review_text"),
        "review_url": kayit.get("review_url"),
        "lang": kayit.get("lang"),
        "original_language": kayit.get("original_language"),
        "is_machine_translated": kayit.get("is_machine_translated"),
        "tripType": kayit.get("tripType"),
        "son_gorulen_cekim": son_gorulen,
        "ilk_eksik_cekim": ilk_eksik,
        "referans_cekim": ilk_eksik,
        "referans_tarihi": referans_tarihi,
    }


def _gevsek_anahtarlar(r: pd.Series) -> List[str]:
    """Kimliksiz bir kaydı aynı yorumun kimlikli kaydıyla eşleştirmek için: metin+puan, yazar+puan+gün."""
    anahtarlar = []
    metin = r["review_text"][:80].lower()
    if metin:
        anahtarlar.append(f"metin|{r['review_rating']}|{metin}")
    yazar = r["author_title"].lower()
    if yazar:
        anahtarlar.append(f"yazar|{yazar}|{r['review_rating']}|{r['review_datetime_utc']:%Y-%m-%d}")
    return anahtarlar


def arsiv_kayitlari(mevcut: pd.DataFrame, yeni: pd.DataFrame) -> List[Dict[str, Any]]:
    """Arşivdekilerle yenileri anahtara göre birleştirir; aynı yorum güncellenir, çoğalmaz."""
    parcalar = [hazirla(d) for d in (mevcut, yeni) if d is not None and not d.empty]
    if not parcalar:
        return []
    birlesik = pd.concat(parcalar, ignore_index=True).drop_duplicates("anahtar", keep="last")
    # Kimlik eşleştirmesinden önceki yüklemeden gelen kimliksiz kayıt, aynı yorumun kimlikli kaydı varsa atılır;
    # böylece doğrulanmış dosya ile yükleme hangi sırada gelirse gelsin yorum iki kez görünmez.
    kimlikli = birlesik["anahtar"].str.fullmatch(_KIMLIK_ANAHTARI)
    if (~kimlikli).any():
        bilinen = {g for _, r in birlesik[kimlikli].iterrows() for g in _gevsek_anahtarlar(r)}
        yinelenen = birlesik[~kimlikli].apply(lambda r: any(g in bilinen for g in _gevsek_anahtarlar(r)), axis=1)
        birlesik = birlesik.drop(yinelenen[yinelenen].index)
    birlesik = birlesik.sort_values(["review_datetime_utc", "review_id"], ascending=[False, True])
    kayitlar = []
    for r in birlesik.to_dict("records"):
        r["review_datetime_utc"] = r["review_datetime_utc"].strftime("%Y-%m-%d")
        r["is_machine_translated"] = "TRUE" if r["is_machine_translated"] else "FALSE"
        kayitlar.append({s: r[s] for s in ARSIV_SUTUNLARI})
    return kayitlar
