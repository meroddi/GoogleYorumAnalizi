@echo off
REM BAGISIK - script guncelleyici (cift tikla)
REM PowerShell'i kisitlama olmadan calistirir, boylece
REM execution policy ayari gerekmez.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0guncelle.ps1"
