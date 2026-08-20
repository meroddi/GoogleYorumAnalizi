# BAĞIŞIK — script güncelleyici
# Depodaki tüm oyun ve editör script'lerini Unity projesine indirir.
# Kullanım: bu dosyayı Unity proje kökune koy, guncelle.bat'a çift tıkla.

$ErrorActionPreference = "Stop"
$base = "https://raw.githubusercontent.com/meroddi/GoogleYorumAnalizi/claude/walking-dead-style-3d-game-ee1m68/unity"

# Script'in bulunduğu klasörde çalış — nereden başlatıldığı fark etmesin.
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

$sets = [ordered]@{
    "Scripts" = @(
        "PlayerMover.cs",
        "CameraRig.cs",
        "HotspotTarget.cs",
        "HotspotDetector.cs",
        "FrameRateController.cs"
    )
    "Editor" = @(
        "DemoSceneBuilder.cs",
        "ProjectSetup.cs",
        "LookSetup.cs",
        "CharacterSetup.cs"
    )
}

$ok = 0
$fail = 0

foreach ($dir in $sets.Keys) {
    $target = ".\Assets\$dir"
    New-Item -ItemType Directory -Force -Path $target | Out-Null

    Write-Host "  $dir/" -ForegroundColor White
    foreach ($file in $sets[$dir]) {
        try {
            Invoke-WebRequest "$base/$dir/$file" -OutFile "$target\$file" -UseBasicParsing
            $kb = [math]::Round((Get-Item "$target\$file").Length / 1KB, 1)
            Write-Host ("    OK   {0,-26} {1,6} KB" -f $file, $kb) -ForegroundColor Green
            $ok++
        }
        catch {
            Write-Host ("    HATA {0,-26} {1}" -f $file, $_.Exception.Message) -ForegroundColor Red
            $fail++
        }
    }
}

Write-Host ""
if ($fail -eq 0) {
    Write-Host "  $ok dosya guncellendi." -ForegroundColor Green
    Write-Host "  Simdi Unity'ye gec - otomatik derleyecek." -ForegroundColor Cyan
} else {
    Write-Host "  $ok basarili, $fail hatali." -ForegroundColor Yellow
    Write-Host "  Hata mesajini Claude'a gonder." -ForegroundColor Yellow
}
Write-Host ""
Read-Host "  Kapatmak icin Enter"
