# Panduan Sideload TV Signage Player ke Android TV / Google TV

Dokumen ini menjelaskan langkah-langkah instalasi file APK (`app-debug.apk` atau `app-release.apk`) ke perangkat Android TV / Google TV (Chromecast with Google TV, Sony, TCL, Xiaomi Mi Box / Mi TV Stick, Hisense, dsb.).

---

## Metode 1: Instalasi Melalui ADB Jaringan (Wi-Fi) - Direkomendasikan

Metode ini paling praktis untuk instalasi dan pembaruan berkala.

### 1. Aktifkan Opsi Pengembang (Developer Options) di TV
1. Nyalakan TV / Google TV.
2. Buka **Settings (Pengaturan)** > **System (Sistem)** > **About (Tentang)**.
3. Gulir ke bawah ke baris **Android TV OS build (Nomor Build)**.
4. Tekan tombol **OK/Select** pada remote sebanyak **7 kali** berturut-turut hingga muncul pesan: *"You are now a developer!"* (*Anda sekarang seorang pengembang!*).

### 2. Aktifkan Debugging Jaringan (Network Debugging)
1. Kembali ke **Settings** > **System** > **Developer options (Opsi pengembang)**.
2. Aktifkan **USB Debugging**.
3. Jika tersedia opsi **Wireless Debugging** atau **Network Debugging**, aktifkan juga.
4. Periksa alamat IP perangkat TV Anda di **Settings** > **Network & Internet** > pilih Wi-Fi yang terhubung (misalnya `192.168.1.50`).

### 3. Hubungkan Komputer ke TV dan Pasang APK
Dari komputer/laptop yang berada dalam satu jaringan Wi-Fi yang sama dengan TV:

**Di Windows (PowerShell):**
```powershell
# Hubungkan ke TV
adb connect 192.168.1.50:5555

# Cek apakah perangkat sudah authorized (di layar TV akan muncul dialog "Allow USB Debugging?", centang "Always allow" lalu pilih OK)
adb devices

# Pasang APK (opsi -g otomatis mengizinkan runtime permissions)
adb install -r -g app-debug.apk

# Berikan izin khusus latar belakang (Android 10+ untuk auto-start):
adb shell appops set com.example.tvsignage SYSTEM_ALERT_WINDOW allow
adb shell dumpsys deviceidle whitelist +com.example.tvsignage
```

**Atau gunakan skrip otomatis:**
```powershell
.\scripts\install.ps1 -DeviceIp 192.168.1.50
```

---

## Metode 2: Instalasi Melalui USB Flashdisk

Metode ini cocok jika TV tidak dapat dihubungkan ke ADB jaringan atau diletakkan di lokasi terpencil.

### 1. Persiapan File
1. Salin file `app-release.apk` (atau `app-debug.apk`) ke dalam flashdisk USB (format FAT32 atau exFAT).
2. Tancapkan flashdisk ke port USB di TV / TV Box.

### 2. Pasang File Manager di TV
1. Buka Google Play Store di Android TV / Google TV.
2. Cari dan pasang salah satu aplikasi pengelola berkas:
   - **AnExplorer File Manager** (Leanback friendly)
   - **X-plore File Manager**
   - **File Commander**

### 3. Izinkan Pemasangan Aplikasi Tak Dikenal (Unknown Sources)
1. Buka **Settings** > **Apps** > **Security & Restrictions** > **Unknown sources** (atau **Special app access** > **Install unknown apps**).
2. Aktifkan izin untuk aplikasi File Manager yang baru dipasang.

### 4. Eksekusi Instalasi
1. Buka File Manager di TV, akses drive USB.
2. Pilih file `app-release.apk`.
3. Pilih **Install**.
4. Setelah selesai, pilih **Open** untuk menjalankan aplikasi Signage Player.

---

## Pasca Instalasi: Konfigurasi Pertama Kali
1. Saat pertama kali dibuka, aplikasi akan menampilkan layar **Setup**:
   - **CMS URL**: URL instalasi Xibo CMS Anda (misal `http://192.168.1.100:8080` atau `https://signage.perusahaan.com`).
   - **Server Key**: Kunci server yang diatur di Xibo CMS.
   - **Display Name**: Nama display yang diinginkan (misal `Lobby-TV-01`).
   - Layar juga menampilkan **Hardware Key** unik dari perangkat ini.
2. Klik tombol **Connect / Register**.
3. Buka dashboard Xibo CMS di browser:
   - Masuk ke menu **Displays**.
   - Cari display baru Anda yang berstatus *"Waiting Authorise"*.
   - Klik aksi pada display tersebut, pilih **Authorise**.
4. Dalam hitungan detik (atau sesuai interval polling), player di TV akan otomatis mendeteksi otorisasi, mulai mengunduh jadwal serta file media, dan memutarnya fullscreen.

---

## Troubleshooting
- **Layar TV meminta otorisasi RSA saat `adb connect`**: Pastikan remote TV Anda siap untuk menekan "Always allow from this computer" dan klik "Allow".
- **Aplikasi tertutup setelah TV restart**: Pastikan izin `RECEIVE_BOOT_COMPLETED` aktif dan izin `SYSTEM_ALERT_WINDOW` telah diberikan melalui perintah adb di atas agar Android mengizinkan membuka Activity dari background service.
- **D-pad Remote tidak bisa fokus pada tombol**: Aplikasi ini dirancang khusus untuk D-pad Leanback. Jika tombol tidak merespons, tekan tombol Directional (Atas/Bawah/Kiri/Kanan) sekali untuk memunculkan visual highlight fokus.
