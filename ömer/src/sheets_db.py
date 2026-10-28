"""Yorumları Google E-Tablo'da saklayan yardımcı.

Her yorum türü tabloda kendi sayfasında durur. İlk satır sütun adlarıdır;
uygulama sütunları sırasına göre değil adına göre bulur. Tekrar eden yorumlar
"anahtar" sütunuyla ayıklanır.

Gerekli secrets:
    GSHEETS_URL                   -> tablonun tarayıcıdaki adresi (veya kimliği)
    GSHEETS_SERVICE_ACCOUNT_JSON  -> servis hesabı JSON dosyasının tamamı
"""
from __future__ import annotations

import json
import numbers
from typing import Any, Dict, Iterable, List, Mapping, Optional, Sequence, Tuple

import gspread
import pandas as pd
import streamlit as st
from gspread.utils import InsertDataOption, ValueInputOption, ValueRenderOption, extract_id_from_url

from local_secrets import get_secret

SCOPES = ["https://www.googleapis.com/auth/spreadsheets"]
OKUMA_TTL_SANIYE = 600   # tabloda elle yapılan değişiklikler en geç 10 dakikada görünür
HUCRE_SINIRI = 50_000    # Google E-Tablo'da bir hücreye sığan en fazla karakter


class SheetsHatasi(RuntimeError):
    """Kullanıcıya olduğu gibi gösterilebilecek Türkçe hata."""


# ==========================================
# AYARLAR
# ==========================================
HESAP_ANAHTARI = "GSHEETS_SERVICE_ACCOUNT_JSON"
ADRES_ANAHTARI = "GSHEETS_URL"


def _secrets_kokleri() -> Mapping[str, Any]:
    try:
        return st.secrets.to_dict()
    except Exception:
        return {}


def _secrets_yolu(kok: Mapping[str, Any], *yol: str) -> Any:
    deger: Any = kok
    for parca in yol:
        if not isinstance(deger, Mapping) or parca not in deger:
            return None
        deger = deger[parca]
    return deger


def _hesap_bilgisi(kok: Mapping[str, Any]) -> Optional[Dict[str, Any]]:
    ham = _secrets_yolu(kok, HESAP_ANAHTARI)
    if isinstance(ham, Mapping):
        return dict(ham)
    ham = ham or get_secret(HESAP_ANAHTARI, __file__)
    if ham:
        try:
            return json.loads(ham)
        except json.JSONDecodeError as e:
            raise SheetsHatasi(
                f"{HESAP_ANAHTARI} okunamadı. İndirilen JSON dosyasının tamamı "
                "''' işaretlerinin arasına, hiç değiştirilmeden yapıştırılmalı."
            ) from e
    # Streamlit belgelerindeki diğer yazım şekilleri de kabul edilir.
    for yol in (("gcp_service_account",), ("connections", "gsheets")):
        tablo = _secrets_yolu(kok, *yol)
        if isinstance(tablo, Mapping) and "private_key" in tablo:
            return dict(tablo)
    return None


def _tablo_adresi(kok: Mapping[str, Any]) -> str:
    for deger in (
        get_secret(ADRES_ANAHTARI, __file__),
        _secrets_yolu(kok, "private_gsheets_url"),
        _secrets_yolu(kok, "connections", "gsheets", "spreadsheet"),
    ):
        if isinstance(deger, str) and deger.strip():
            return deger.strip()
    return ""


def _eksik_ayar_mesaji(kok: Mapping[str, Any], eksikler: List[str]) -> str:
    """Değerleri göstermeden, ayarın neden bulunamadığını tahmin eder."""
    ipuclari = []

    def gez(dugum: Mapping[str, Any], yol: Tuple[str, ...]) -> None:
        for anahtar, deger in dugum.items():
            for eksik in eksikler:
                if str(anahtar).lower() != eksik.lower():
                    continue
                if yol:
                    ipuclari.append(
                        f"{eksik} satırı [{'.'.join(yol)}] başlığının altında kalmış; "
                        "bu satırı köşeli parantezli başlıkların ÜSTÜNE taşıyın."
                    )
                elif anahtar != eksik:
                    ipuclari.append(f"'{anahtar}' yazılmış, tam olarak '{eksik}' olmalı.")
            if isinstance(deger, Mapping):
                gez(deger, yol + (str(anahtar),))

    gez(kok, ())
    mesaj = f"Google E-Tablo ayarı bulunamadı: {', '.join(eksikler)}."
    if ipuclari:
        return mesaj + " " + " ".join(ipuclari)
    return mesaj + (
        " Streamlit Cloud > Settings > Secrets'ta bu adla bir satır olduğundan ve "
        "'Save changes' ile kaydedildiğinden emin olun."
    )


# ==========================================
# BAĞLANTI
# ==========================================
@st.cache_resource(show_spinner=False)
def _tablo() -> gspread.Spreadsheet:
    kok = _secrets_kokleri()
    hesap = _hesap_bilgisi(kok)
    tablo_adresi = _tablo_adresi(kok)
    eksikler = [ad for ad, deger in ((ADRES_ANAHTARI, tablo_adresi), (HESAP_ANAHTARI, hesap)) if not deger]
    if eksikler:
        raise SheetsHatasi(_eksik_ayar_mesaji(kok, eksikler))

    tablo_id = extract_id_from_url(tablo_adresi) if tablo_adresi.startswith("http") else tablo_adresi
    robot = hesap.get("client_email", "servis hesabı")
    try:
        istemci = gspread.service_account_from_dict(hesap, scopes=SCOPES)
    except (ValueError, KeyError) as e:
        raise SheetsHatasi(
            f"{HESAP_ANAHTARI} eksik ya da bozuk. İndirilen JSON dosyasının tamamını "
            "kopyaladığınızdan emin olun."
        ) from e
    try:
        return istemci.open_by_key(tablo_id)
    except (gspread.exceptions.SpreadsheetNotFound, PermissionError) as e:
        raise SheetsHatasi(
            f"Tablo açılamadı. Adresin doğru olduğundan ve tablonun {robot} "
            "adresiyle 'Düzenleyen' olarak paylaşıldığından emin olun."
        ) from e
    except gspread.exceptions.APIError as e:
        raise SheetsHatasi(
            "Google E-Tablo'ya bağlanılamadı. Google Cloud projesinde "
            f"'Google Sheets API' açık olmalı. ({e})"
        ) from e


def _sayfa(ad: str, sutunlar: Sequence[str]) -> gspread.Worksheet:
    tablo = _tablo()
    try:
        return tablo.worksheet(ad)
    except gspread.exceptions.WorksheetNotFound:
        sayfa = tablo.add_worksheet(title=ad, rows=1000, cols=len(sutunlar))
        sayfa.update([list(sutunlar)], "A1")
        sayfa.freeze(rows=1)
        return sayfa


def _sutun_yerleri(
    ad: str, baslik: List[Any], sutunlar: Sequence[str], istege_bagli: Sequence[str] = ()
) -> Dict[str, int]:
    """Sütunların yerini adına göre bulur; isteğe bağlı olup tabloda olmayanlar -1 döner."""
    baslik = [str(b).strip() for b in baslik]
    eksik = [s for s in sutunlar if s not in baslik and s not in istege_bagli]
    if eksik:
        raise SheetsHatasi(
            f"'{ad}' sayfasında şu sütun başlıkları bulunamadı: {', '.join(eksik)}. "
            "İlk satırdaki başlıkları değiştirmeyin."
        )
    return {s: baslik.index(s) if s in baslik else -1 for s in sutunlar}


def _hucre(deger: Any) -> Any:
    if deger is None or (pd.api.types.is_scalar(deger) and pd.isna(deger)):
        return ""
    if isinstance(deger, numbers.Integral):
        return int(deger)
    if isinstance(deger, numbers.Real):
        return float(deger)
    return str(deger)[:HUCRE_SINIRI]


# ==========================================
# OKU / EKLE / TEMİZLE
# ==========================================
@st.cache_data(ttl=OKUMA_TTL_SANIYE, show_spinner=False)
def oku(ad: str, sutunlar: Tuple[str, ...], istege_bagli: Tuple[str, ...] = ()) -> pd.DataFrame:
    """Sayfadaki tüm satırları, istenen sütunlarla DataFrame olarak döner.

    `istege_bagli` sütunlar eski sayfalarda henüz yoksa boş gelir.
    """
    degerler = _sayfa(ad, sutunlar).get_all_values(value_render_option=ValueRenderOption.unformatted)
    if not degerler:
        return pd.DataFrame(columns=list(sutunlar))

    yer = _sutun_yerleri(ad, degerler[0], sutunlar, istege_bagli)
    satirlar = [r for r in degerler[1:] if any(str(v).strip() for v in r)]
    return pd.DataFrame({
        s: [r[i] if 0 <= i < len(r) else "" for r in satirlar]
        for s, i in yer.items()
    })


def yaz_tumu(ad: str, sutunlar: Tuple[str, ...], kayitlar: Iterable[Dict[str, Any]]) -> None:
    """Sayfayı başlık ve verilen kayıtlarla baştan yazar (eksik sütunlar da böylece eklenir)."""
    sayfa = _sayfa(ad, sutunlar)
    degerler = [list(sutunlar)] + [[_hucre(k.get(s, "")) for s in sutunlar] for k in kayitlar]
    eski_satir = sayfa.row_count
    if sayfa.row_count < len(degerler) or sayfa.col_count < len(sutunlar):
        sayfa.resize(rows=max(sayfa.row_count, len(degerler)), cols=max(sayfa.col_count, len(sutunlar)))
    # Önce üzerine yazılır, sonra artan eski satırlar temizlenir: yarıda kalırsa veri kaybolmaz.
    sayfa.update(degerler, "A1")
    if eski_satir > len(degerler):
        sayfa.batch_clear([f"{len(degerler) + 1}:{eski_satir}"])
    oku.clear()


def ekle(ad: str, sutunlar: Tuple[str, ...], kayitlar: Iterable[Dict[str, Any]]) -> int:
    """Anahtarı tabloda olmayan kayıtları tek seferde ekler; eklenen sayısını döner."""
    sayfa = _sayfa(ad, sutunlar)
    degerler = sayfa.get_all_values()
    if not degerler:
        sayfa.update([list(sutunlar)], "A1")
        degerler = [list(sutunlar)]

    baslik = [str(b).strip() for b in degerler[0]]
    k = _sutun_yerleri(ad, baslik, sutunlar)["anahtar"]
    mevcut = {r[k] for r in degerler[1:] if k < len(r) and r[k]}

    yeni = []
    for kayit in kayitlar:
        anahtar = kayit["anahtar"]
        if anahtar in mevcut:
            continue
        mevcut.add(anahtar)
        yeni.append([_hucre(kayit.get(b, "")) for b in baslik])

    if yeni:
        # RAW: "=..." ile başlayan yorumlar formül değil düz yazı olarak kalır.
        # INSERT_ROWS: tabloda boş satır olsa bile alttaki veri ezilmez.
        sayfa.append_rows(
            yeni,
            value_input_option=ValueInputOption.raw,
            insert_data_option=InsertDataOption.insert_rows,
            table_range="A1",
        )
        oku.clear()
    return len(yeni)


def temizle(ad: str, sutunlar: Tuple[str, ...]) -> None:
    """Başlık satırı dışındaki her şeyi siler."""
    sayfa = _sayfa(ad, sutunlar)
    # Dondurulmuş başlığın altında en az bir satır kalmalı, yoksa Google izin vermez.
    sayfa.resize(rows=2)
    sayfa.batch_clear(["2:2"])
    oku.clear()
