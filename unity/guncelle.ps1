# BAĞIŞIK — script güncelleyici
#
# Dosya listesini GitHub'dan CANLI okur. Yeni bir script eklendiğinde bu
# dosyanın güncellenmesi gerekmez — kendiliğinden yakalar.
#
# Kullanım: Unity proje kökune koy, guncelle.bat'a çift tıkla.

$ErrorActionPreference = "Stop"
$owner  = "meroddi"
$repo   = "GoogleYorumAnalizi"
$branch = "claude/walking-dead-style-3d-game-ee1m68"

Set-Location -Path $PSScriptRoot

Write-Host ""
Write-Host "  BAGISIK - script guncelleme" -ForegroundColor Cyan
Write-Host "  $(Get-Location)" -ForegroundColor DarkGray
Write-Host ""

if (-not (Test-Path ".\Assets")) {
    Write-Host "  HATA: Burada Assets klasoru yok." -ForegroundColor Red
    Write-Host "  Bu dosyayi Unity projesinin ana klasorune koy" -ForegroundColor Yellow
    Write-Host "  (icinde Assets, Library, Packages olan yer) ve tekrar calistir." -ForegroundColor Yellow
    Write-Host ""
    Read-Host "  Kapatmak icin Enter"
    exit 1
}

# TLS 1.2 — eski PowerShell surumleri GitHub'a baglanamiyor
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

$ok = 0
$fail = 0
$skip = 0

foreach ($dir in @("Scripts", "Editor")) {
    $api = "https://api.github.com/repos/$owner/$repo/contents/unity/$dir" + "?ref=$branch"

    try {
        $items = Invoke-RestMethod -Uri $api -Headers @{ "User-Agent" = "bagisik-updater" } -ErrorAction Stop
    }
    catch {
        Write-Host "  HATA: $dir listesi alinamadi - $($_.Exception.Message)" -ForegroundColor Red
        $fail++
        continue
    }

    $target = ".\Assets\$dir"
    New-Item -ItemType Directory -Force -Path $target | Out-Null

    Write-Host "  $dir/" -ForegroundColor White
    foreach ($item in $items) {
        if ($item.type -ne "file" -or $item.name -notlike "*.cs") { continue }

        $dest = Join-Path $target $item.name
        try {
            # Ayni icerik zaten varsa dokunma - Unity gereksiz yere derlemesin
            $before = if (Test-Path $dest) { (Get-FileHash $dest -Algorithm SHA1).Hash } else { $null }

            Invoke-WebRequest $item.download_url -OutFile $dest -UseBasicParsing
            $after = (Get-FileHash $dest -Algorithm SHA1).Hash
            $kb = [math]::Round((Get-Item $dest).Length / 1KB, 1)

            if ($before -eq $after) {
                Write-Host ("    ayni {0,-24} {1,6} KB" -f $item.name, $kb) -ForegroundColor DarkGray
                $skip++
            }
            else {
                Write-Host ("    YENI {0,-24} {1,6} KB" -f $item.name, $kb) -ForegroundColor Green
                $ok++
            }
        }
        catch {
            Write-Host ("    HATA {0,-24} {1}" -f $item.name, $_.Exception.Message) -ForegroundColor Red
            $fail++
        }
    }
}

Write-Host ""
if ($fail -eq 0) {
    Write-Host "  $ok guncellendi, $skip zaten gunceldi." -ForegroundColor Green
    Write-Host "  Simdi Unity'ye gec - otomatik derleyecek." -ForegroundColor Cyan
}
else {
    Write-Host "  $ok guncellendi, $skip ayni, $fail HATA." -ForegroundColor Yellow
    Write-Host "  Hata mesajini Claude'a gonder." -ForegroundColor Yellow
}
Write-Host ""
Read-Host "  Kapatmak icin Enter"
