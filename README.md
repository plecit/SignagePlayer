# TV Signage Player (Xibo CMS Compatible) untuk Android TV / Google TV

Aplikasi Android native independen yang dirancang khusus untuk Android TV dan Google TV OS (Chromecast with Google TV, Xiaomi TV Box / Stick, TCL, Sony, Hisense, dll.) untuk berfungsi sebagai display client yang kompatibel penuh dengan **Xibo CMS** melalui protokol publik **XMDS v7** dan **XMR**.

---

## Fitur Utama

- **Navigasi Khusus TV (D-pad Leanback)**: Seluruh antarmuka dirancang untuk input remote TV (Atas, Bawah, Kiri, Kanan, OK/Select, Back) tanpa asumsi touchscreen.
- **Registrasi Otomatis & Identitas Unik**: Menghasilkan Hardware Key berbasis UUID v4 yang persisten. Mendukung status `WAITING` (menunggu otorisasi admin di CMS) dan `READY`.
- **Siklus Sinkronisasi XMDS v7 Penuh**:
  - `RegisterDisplay`: Mendeteksi otorisasi dan konfigurasi CMS.
  - `RequiredFiles`: Manajemen berkas (layout XLF, video, gambar, font, HTML template) dan auto-purge berkas usang.
  - `Schedule`: Parsing jadwal kompleks (dayparting, rentang waktu, prioritas, fallback default layout).
  - `GetFile`: Unduhan chunked dengan verifikasi MD5 hash dan resume otomatis.
  - `MediaInventory`: Pelaporan status kelengkapan media lokal ke server.
  - `SubmitStats`: Proof of play buffer lokal dengan validasi ID numerik bebas error.
  - `SubmitLog` & `NotifyStatus`: Pelaporan kesehatan perangkat (CPU/RAM, disk storage, IP, log error).
  - `SubmitScreenShot`: Pengambilan screenshot real-time untuk pemantauan dari CMS.
- **Playback Gapless & Multi-Region (XLF Renderer)**:
  - Video engine bertenaga AndroidX Media3 (ExoPlayer) dengan pre-buffering untuk mencegah layar hitam antar rotasi.
  - Image engine dengan Coil (scale mode fit, stretch, aspect).
  - Widget HTML / Ticker / Rich Text melalui WebView yang dioptimalkan offline.
- **Offline Resilient**: Pemutaran terus berlangsung tanpa henti meskipun koneksi internet terputus berhari-hari berkat local cache Room & storage.
- **Kiosk & Auto-Recovery**:
  - `BootReceiver` otomatis menyalakan player saat TV dinyalakan.
  - Foreground Service untuk mencegah OS mematikan player di latar belakang.
  - Internal Watchdog mendeteksi dan memulihkan playback jika terjadi anomali/stuck.
  - Menu rahasia Settings dilindungi PIN atau kombinasi D-pad.

---

## Persyaratan Sistem

- **Target Device**: Android TV / Google TV dengan Android 8.0 (API level 26) ke atas (diuji optimal pada Android 11 s.d. 14).
- **Target CMS**: Xibo CMS 4.x (skema XMDS v7) self-hosted via Docker atau cloud.
- **Build Environment**: JDK 17, Android SDK Platform 34, Build Tools 34.0.0.

---

## Panduan Membangun (Build) Proyek

1. **Prasyarat**:
   Pastikan Java JDK 17 dan Android SDK sudah terpasang, atau jalankan skrip pembantu:
   ```powershell
   .\scripts\setup-env.ps1
   ```

2. **Kompilasi Debug APK**:
   ```powershell
   .\gradlew assembleDebug
   ```
   File APK akan dihasilkan di `app/build/outputs/apk/debug/app-debug.apk`.

3. **Menjalankan Unit Test**:
   ```powershell
   .\gradlew testDebugUnitTest
   ```

4. **Kompilasi Release APK**:
   ```powershell
   .\gradlew assembleRelease
   ```

---

## Panduan Instalasi ke TV (Sideloading)

Untuk petunjuk lengkap instalasi melalui USB Drive atau Wi-Fi ADB, silakan baca dokumentasi di [docs/SIDELOAD.md](docs/SIDELOAD.md).

Quick start via ADB jaringan:
```powershell
.\scripts\install.ps1 -DeviceIp 192.168.1.50
```

---

## Menjalankan Xibo CMS Uji Lokal (Docker)

Jika Anda ingin menguji secara mandiri di komputer lokal, Anda dapat menjalankan Xibo CMS menggunakan Docker Compose resmi dari Xibo Signage:

1. Unduh file konfigurasi Docker Xibo:
   ```bash
   mkdir xibo-docker && cd xibo-docker
   curl -O https://github.com/xibosignage/xibo-cms/releases/download/4.1.2/xibo-docker.tar.gz
   tar -zxvf xibo-docker.tar.gz
   ```
2. Buat file `config.env` dan tentukan `MYSQL_PASSWORD` serta `CMS_SERVER_KEY`.
3. Jalankan container:
   ```bash
   docker compose up -d
   ```
4. Buka CMS di browser: `http://localhost:8080`, login dengan user default `xibo_admin` dan password `password`.
5. Buat Display Profile untuk Android, pastikan interval collect dan setting proof of play aktif.
