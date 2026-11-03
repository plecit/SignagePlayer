# Project: TV Signage Player (kompatibel Xibo CMS) untuk Android TV / Google TV

> File ini adalah brief utama untuk agent. Baca seluruhnya sebelum menulis kode.
> Bahasa dokumen: Indonesia. Bahasa kode, komentar, nama class, dan commit: Inggris.

---

## 0. Instruksi untuk Agent

1. Kerjakan fase secara berurutan (Bagian 7). Jangan lompat ke fase berikutnya sebelum acceptance criteria fase saat ini lulus.
2. Jangan menebak format protokol. Sumber kebenaran adalah:
   - dokumentasi resmi XMDS: https://xibo.org.uk/docs/developer/creating-a-player/xmds
   - WSDL dari CMS target yang sedang berjalan: `https://<CMS>/xmds.php?v=7&wsdl`
   - implementasi referensi `xibosignage/xibo-dotnetclient` (GitHub, folder Web References/xmds/service_v7.wsdl) untuk melihat urutan panggilan nyata.
3. Simpan respons XML asli dari CMS ke `app/src/test/resources/fixtures/` dan jadikan dasar unit test parser.
4. Setelah setiap fase: update `STATUS.md` (apa selesai, apa belum, masalah yang diketahui), commit kecil dengan pesan jelas.
5. Jika ada keputusan yang mengubah arsitektur atau ada ambiguitas besar, tulis di `DECISIONS.md` beserta alasannya, lalu lanjut dengan opsi yang paling aman. Jangan berhenti hanya karena detail kecil.
6. Jangan pernah hardcode server key, URL CMS, atau kredensial. Semua dari layar Settings dan disimpan terenkripsi.
7. Selalu verifikasi sendiri: build harus hijau (`./gradlew assembleDebug testDebugUnitTest lint`) sebelum menandai fase selesai.

---

## 1. Tujuan

Membangun aplikasi Android native (Kotlin) yang berjalan di Android TV / Google TV dan berperan sebagai **display player** untuk **Xibo CMS**:

- Muncul sebagai display baru di Xibo CMS (register, menunggu authorise, lalu aktif).
- Menerima jadwal, layout, dan media dari CMS, lalu memutarnya fullscreen tanpa henti.
- Tetap memutar secara offline dari cache lokal jika jaringan putus.
- Mengirim **proof of play** (statistik layout dan media), log, media inventory, status, dan screenshot ke CMS.
- Menerima perintah real-time dari CMS (collect now, screenshot, dll.) lewat XMR.
- Auto-start saat TV menyala dan pulih sendiri jika crash.

### Target platform
- Android TV OS / Google TV (Chromecast with Google TV, Google TV Streamer, Sony/TCL/Hisense/Xiaomi Android TV, set-top box Android).
- `minSdk 26` (Android 8.0), `targetSdk` terbaru yang stabil, uji utama di Android 11 sampai 14.
- Input: D-pad remote saja. Tidak boleh mengasumsikan layar sentuh.

### CMS target
- Xibo CMS 4.x (rilis saat ini 4.5.x). Gunakan **XMDS schema v7** (terbaru). Pastikan juga berfungsi di CMS self-hosted via Docker (`xibosignage/xibo-docker`).

---

## 2. Catatan Lisensi dan Penamaan

- Xibo CMS berlisensi AGPLv3. Membuat player sendiri yang berbicara dengan CMS lewat protokol publik (XMDS/XMR) diperbolehkan.
- **Xibo for Android resmi adalah produk komersial.** Proyek ini adalah player independen, bukan fork-nya. Jangan salin kode, aset, atau APK Xibo for Android.
- Jangan memakai nama atau logo "Xibo" sebagai nama/ikon aplikasi. Nama sementara: **TV Signage Player**, package `com.example.tvsignage` (ganti nanti sesuai milik pengguna). Di UI boleh menulis "Compatible with Xibo CMS".
- Jika menyalin potongan kode dari proyek AGPL (mis. dotnetclient), proyek ini harus ikut AGPLv3. Lebih baik tulis ulang sendiri dari spesifikasi dan jadikan dotnetclient hanya referensi perilaku.
- Tambahkan `LICENSE` dan `NOTICE.md` yang menyebut library pihak ketiga.

---

## 3. Stack dan Keputusan Teknis

| Area | Pilihan | Catatan |
|---|---|---|
| Bahasa | Kotlin | Coroutines + Flow |
| UI | Jetpack Compose for TV atau View klasik | Layar player utama pakai View/SurfaceView agar stabil; Settings boleh Compose |
| Video | AndroidX Media3 ExoPlayer | Preload item berikutnya untuk gapless |
| Gambar | Coil atau Glide | Scale mode sesuai XLF |
| Widget HTML | `WebView` | Banyak widget Xibo berbasis HTML dari `GetResource` |
| HTTP | OkHttp | Timeout, retry exponential backoff, dukung HTTPS dan HTTP |
| SOAP | Bangun envelope XML manual + `XmlPullParser` | Hindari library SOAP berat (ksoap2) |
| Penyimpanan | Room (state/cache index) + file di `filesDir`/external app dir | |
| Secret | EncryptedSharedPreferences / Keystore | Server key, RSA private key |
| XMR | Lihat Bagian 6.4 | Wajib verifikasi mekanisme yang dipakai CMS target |
| DI | Hilt atau manual (pilih yang sederhana) | |
| Background | Foreground Service + WorkManager | Collect cycle, upload stats |
| Test | JUnit5, MockWebServer, Robolectric | Plus uji manual di device |

**Strategi render layout**
- Parse file layout (XLF) sendiri: region, posisi/ukuran, urutan media, durasi, transisi dasar.
- Image dan video dirender native. Widget HTML dirender di WebView memakai HTML hasil `GetResource` (dan dependency seperti font/bundle yang diminta CMS).
- Fase awal cukup: image, video, text/rich text (HTML), web page, clock, ticker/RSS (via GetResource). Widget lain menyusul.

---

## 4. Struktur Repo yang Diharapkan

```
tv-signage-player/
├─ app/                         # modul aplikasi Android TV
│  └─ src/main/
│     ├─ java/.../
│     │  ├─ core/               # config, hardware key, logger, clock, storage
│     │  ├─ xmds/               # SOAP client, request builder, response parser
│     │  ├─ xmr/                # real-time channel + enkripsi/dekripsi pesan
│     │  ├─ files/              # download manager, chunking, MD5 verify, cache, purge
│     │  ├─ schedule/           # parser jadwal, prioritas, dayparting, default layout
│     │  ├─ layout/             # parser XLF, region, renderer
│     │  ├─ player/             # state machine pemutaran, preload, transisi
│     │  ├─ stats/              # proof of play, buffer, upload
│     │  ├─ report/             # log, media inventory, notify status, screenshot
│     │  ├─ service/            # foreground service, boot receiver, watchdog
│     │  └─ ui/                 # setup screen, status overlay, settings
│     └─ AndroidManifest.xml
├─ docs/
│  ├─ SIDELOAD.md               # panduan instal ke TV (sudah disediakan, jangan hapus)
│  └─ PROTOCOL_NOTES.md         # catatan temuan protokol dari fixture nyata
├─ scripts/
│  ├─ install.sh                # adb connect + install + grant izin (lihat SIDELOAD.md)
│  └─ install.ps1               # versi Windows
├─ STATUS.md
├─ DECISIONS.md
├─ project.md
└─ README.md
```

---

## 5. Persyaratan Android TV

- `AndroidManifest.xml`:
  - `<uses-feature android:name="android.software.leanback" android:required="false" />`
  - `<uses-feature android:name="android.hardware.touchscreen" android:required="false" />`
  - Activity utama memiliki intent-filter `LEANBACK_LAUNCHER` (agar muncul di Google TV) dan `LAUNCHER`.
  - Banner 320x180 (`android:banner`) dan ikon.
  - Izin: `INTERNET`, `ACCESS_NETWORK_STATE`, `RECEIVE_BOOT_COMPLETED`, `FOREGROUND_SERVICE`, `WAKE_LOCK`, `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, `SYSTEM_ALERT_WINDOW` (untuk start dari background di Android 10+), `ACCESS_WIFI_STATE`.
  - `android:usesCleartextTraffic="true"` hanya untuk mode debug atau jika pengguna memilih HTTP di Settings. Default HTTPS.
- Layar tetap menyala (`FLAG_KEEP_SCREEN_ON`), fullscreen immersive, tombol Back tidak keluar dari player tanpa konfirmasi/PIN.
- Menu tersembunyi untuk Settings: tekan lama OK atau kombinasi D-pad (mis. atas-atas-bawah-bawah-OK) lalu PIN opsional.
- Layar pertama kali: form CMS URL, server key, display name, tombol Connect. Navigasi seluruhnya D-pad friendly (fokus jelas).

---

## 6. Spesifikasi Fungsional

### 6.1 Identitas Display
- Hardware key: UUID acak yang dibuat sekali, disimpan persisten. Jangan memakai MAC/serial yang bisa berubah atau diblokir OS. Tampilkan di layar Setup.
- Kirim metadata: `clientType` (android), `clientVersion`, `clientCode`, `operatingSystem`, `macAddress` jika tersedia, `displayName`. Cek tipe parameter persis di WSDL.

### 6.2 Siklus Collect (interval dari setting display di CMS)
Urutan (per dokumentasi XMDS):
1. `RegisterDisplay` : ambil settings (collectInterval, stat enabled, ukuran download, dll.), status `READY`/`ADDED`/waiting authorise. Jika belum di-authorise, tampilkan layar "Menunggu authorise di CMS" dengan hardware key dan kode, lalu polling berkala.
2. `RequiredFiles` : daftar media, layout, resource, dependency yang dibutuhkan, plus file yang harus dihapus (purge).
3. `Schedule` : jadwal. **Validasi** bahwa semua file sebuah layout sudah lengkap sebelum layout itu boleh diputar (layout muncul di Schedule dan RequiredFiles bersamaan).
4. Download: `GetFile` (chunked, dengan chunkOffset/chunkSize) atau HTTP langsung jika CMS memberi path download. Dependency lewat `GetDependency` bila diminta. Widget HTML lewat `GetResource`.
5. `MediaInventory` : laporkan file yang sudah lengkap/belum.
6. `SubmitStats` : kirim proof of play.
7. `SubmitLog` : kirim log.
8. `NotifyStatus` : kirim status device (storage, versi, timezone, dll.).
9. `SubmitScreenShot` : saat diminta (atau sesuai interval).

Aturan penting:
- Tambahkan query parameter `method` di semua request XMDS, mis. `/xmds.php?v=7&method=RegisterDisplay` (membantu rate limit di proxy/cloud).
- Sertakan `serverKey` dan `hardwareKey` di setiap method.
- RegisterDisplay juga harus dipanggil berkala, bukan hanya sekali, supaya perubahan settings diterapkan.
- Verifikasi MD5 setiap file selesai diunduh; hapus dan unduh ulang jika mismatch.
- Unduhan: resume jika terputus, batasi paralelisme, hormati batas bandwidth dari settings display, tulis ke file `.part` lalu rename atomik.
- Cache cukup untuk memutar offline selama window lookahead CMS (default sekitar 4 hari). Purge file yang tidak lagi diminta; jaga sisa storage minimum.

### 6.3 Penjadwalan dan Pemutaran
- Dukung: default layout, jadwal berdasarkan waktu (from/to), prioritas, dayparting/recurrence yang diberikan CMS, campaign (urutan layout dalam satu campaign), overlay layout (tahap lanjut), interrupt/share-of-voice (tahap lanjut).
- Gunakan zona waktu sesuai setting display.
- Jika tidak ada jadwal valid atau file belum lengkap: putar **default layout**; jika itu pun tidak ada, tampilkan splash/logo + status.
- Transisi layout tanpa layar hitam (preload layout/video berikutnya selagi yang sekarang tampil).
- Jika layout gagal dirender: catat error, kirim log, lompat ke berikutnya, jangan crash.
- Layout yang sama tidak boleh diputar ulang terus-menerus jika ada layout lain yang valid (ikuti rotasi sesuai jadwal).

### 6.4 XMR (Perintah Real-time)
- Saat `RegisterDisplay`, kirim channel dan public key (RSA) bila CMS mengharapkannya. Generate pasangan kunci sekali, simpan private key di Keystore/encrypted storage.
- CMS mengirim pesan terenkripsi (skema `openssl_seal` pada implementasi klasik: RSA untuk kunci sesi + RC4 untuk payload). **Verifikasi di dokumentasi/CMS 4.5 apakah XMR kini juga tersedia via WebSocket**, dan implementasikan transport yang benar-benar didukung CMS target. Catat temuan di `DECISIONS.md`. Bila XMR sulit, fallback: polling collect interval pendek (fitur tetap berjalan, hanya kurang real-time).
- Perintah minimal: `collectNow`, `screenShot`, `rekeyAction`, `licenceCheck`, `purgeAll`, `changeLayout`/`overlayLayout`, `dataUpdate`, `commandAction` (yang aman saja). Perintah yang butuh root (reboot, dll.) hanya dijalankan jika tersedia izinnya; jika tidak, laporkan "unsupported".
- Auto-reconnect dengan backoff. Jangan crash saat pesan tidak valid.

### 6.5 Proof of Play (Statistik)
- Catat setiap tayangan:
  - **layout**: layoutId, scheduleId, fromDt, toDt.
  - **media/widget**: mediaId/widgetId, layoutId, scheduleId, fromDt, toDt, count/duration.
- Buffer di database lokal (tahan restart/offline). Kirim batch via `SubmitStats` sesuai interval collect; hapus dari buffer hanya setelah CMS menjawab sukses.
- Ikuti pengaturan CMS: stat dikirim hanya jika "stats enabled" di Display Profile, dan per-layout/per-media sesuai flag dari XLF/schedule.
- Pastikan semua ID numerik valid sebelum dikirim (ada bug CMS 4.5.0 saat `scheduleid="undefined"` membuat seluruh SubmitStats gagal). Jangan pernah kirim nilai kosong atau string "undefined".
- Waktu pakai format dan zona waktu sesuai spesifikasi XMDS (cek di dokumentasi).
- Tutup stat dengan benar saat layout berganti, app di-pause, atau crash (flush saat `onStop` dan saat start berikutnya).
- Verifikasi hasil di CMS: Reports > Proof of Play / Display Reports.

### 6.6 Log, Inventory, Status, Screenshot
- Log: level (error/audit/info/debug) sesuai setting `logLevel` dari CMS. Buffer lokal, kirim via `SubmitLog`, rotasi file log.
- `MediaInventory` dan `NotifyStatus`: sertakan storage tersedia, IP, waktu, versi app, layout aktif.
- Screenshot: ambil frame yang sedang tampil (PixelCopy/`drawToBitmap` untuk View; untuk video SurfaceView gunakan `PixelCopy`), kirim JPEG/PNG via `SubmitScreenShot` sesuai ukuran yang diminta.

### 6.7 Ketahanan
- Foreground service menjaga proses; `BootReceiver` (`BOOT_COMPLETED`, `LOCKED_BOOT_COMPLETED`) meluncurkan Activity (butuh izin overlay di Android 10+; lihat SIDELOAD.md).
- Watchdog internal: jika thread player macet > N detik, restart komponen render.
- Global uncaught exception handler: catat, lalu restart Activity.
- Tidak boleh ada memory leak WebView/ExoPlayer; release resource saat berpindah layout.
- Soak test: 24 jam tanpa crash, memori stabil.

---

## 7. Fase Pengerjaan dan Acceptance Criteria

### Fase 0: Setup Proyek dan CMS Uji
- Buat proyek Gradle Android TV, CI lokal (`./gradlew assembleDebug testDebugUnitTest lint`).
- Siapkan CMS uji via Docker (`xibosignage/xibo-docker`), buat 1 layout sederhana dengan 1 gambar, 1 video, 1 teks.
- **AC**: app kosong terpasang dan terbuka di emulator Android TV / TV fisik; CMS uji dapat diakses; dokumentasi cara menjalankan di README.

### Fase 1: Koneksi dan Registrasi
- Layar Setup, hardware key, klien SOAP, `RegisterDisplay`, polling status authorise.
- **AC**: display muncul di CMS (Displays) sebagai "waiting authorise"; setelah di-authorise, app menampilkan status "Registered" dan membaca settings (collect interval). Fixture XML tersimpan, unit test parser lulus.

### Fase 2: Sinkronisasi File
- `RequiredFiles`, `Schedule`, download manager (chunk, MD5, resume, purge), `MediaInventory`.
- **AC**: semua file layout uji ter-download dan lolos MD5; file yang dihapus dari CMS ikut terhapus di device; cabut jaringan lalu nyalakan lagi: unduhan resume.

### Fase 3: Render dan Penjadwalan
- Parser XLF, renderer image/video/teks/web, scheduler (default layout, prioritas, jadwal waktu), preload.
- **AC**: layout uji tampil benar fullscreen sesuai posisi region dan durasi; mengganti jadwal di CMS menghasilkan perubahan pada siklus collect berikutnya; mode offline tetap memutar dari cache; tidak ada layar hitam antar layout.

### Fase 4: Proof of Play, Log, Status
- Stat engine + `SubmitStats`, `SubmitLog`, `NotifyStatus`, screenshot.
- **AC**: setelah beberapa menit pemutaran, data muncul di CMS (Proof of Play report) dengan layout/media dan durasi yang masuk akal; screenshot muncul di halaman display; log error terlihat di CMS; stat yang terkumpul saat offline terkirim setelah online tanpa duplikat.

### Fase 5: XMR dan Kontrol Jarak Jauh
- Registrasi key, koneksi XMR, dekripsi pesan, command handler.
- **AC**: "Collect Now" dan "Screenshot" dari CMS dieksekusi dalam beberapa detik (bukan menunggu interval); reconnect otomatis setelah koneksi diputus.

### Fase 6: Hardening dan Kiosk
- Boot receiver, foreground service, watchdog, handler crash, PIN settings, tampilan error ramah, optimasi memori, widget tambahan (clock, ticker/RSS, dataset via GetData bila feasible).
- **AC**: TV dinyalakan ulang: app otomatis tampil dan memutar; soak test 24 jam lolos; tidak ada ANR.

### Fase 7: Rilis
- Signing release (keystore dibuat pengguna, jangan commit), ProGuard/R8 aman untuk parser dan WebView, `scripts/install.sh`/`.ps1`, versi, changelog, README lengkap.
- **AC**: APK release terpasang lewat adb dan lewat USB mengikuti `docs/SIDELOAD.md` pada minimal 1 perangkat Google TV nyata.

---

## 8. Non-Functional

- Startup sampai konten tampil (cache sudah ada) < 10 detik.
- Penggunaan RAM stabil, tidak tumbuh seiring waktu.
- Semua operasi jaringan dengan timeout dan retry backoff; tidak ada operasi jaringan di main thread.
- HTTPS direkomendasikan; izinkan HTTP hanya jika pengguna memilih. Validasi sertifikat secara default (opsi "trust self-signed" harus eksplisit dan diberi peringatan).
- Log tidak boleh memuat server key atau private key.
- Semua string UI dapat diterjemahkan (`strings.xml`), minimal Inggris dan Indonesia.

---

## 9. Strategi Pengujian

- **Unit**: parser SOAP (RegisterDisplay, RequiredFiles, Schedule), parser XLF, scheduler (prioritas, dayparting, zona waktu), stat builder, MD5/chunk assembler. Gunakan fixture dari CMS nyata.
- **Integrasi**: MockWebServer yang meniru XMDS untuk skenario error (500, timeout, XML rusak, file terpotong).
- **Manual (checklist di STATUS.md)**: register/authorise, ganti schedule, hapus media, offline/online, reboot TV, cabut listrik, video besar, layout multi-region, stat muncul di CMS, XMR command.
- **Perangkat**: emulator Android TV (API 30+) untuk pengembangan harian, minimal satu TV/box nyata untuk acceptance akhir.

---

## 10. Deliverables Akhir

1. Kode sumber lengkap di repo, build hijau.
2. `app-release.apk` (signed) dan `app-debug.apk`.
3. `README.md` (build, konfigurasi CMS, penggunaan).
4. `docs/SIDELOAD.md` (cara pasang ke TV lewat USB dan ADB jaringan).
5. `docs/PROTOCOL_NOTES.md` (temuan protokol, perbedaan versi CMS).
6. `STATUS.md` dan `DECISIONS.md` terbaru.
7. `scripts/install.sh` dan `scripts/install.ps1`.

---

## 11. Di Luar Lingkup (Tahap Lanjut)

- Sync multi-display (synchronised events), geo-fencing penuh, interactive actions/touch, rekayasa lisensi komersial, white-label, MDM, OTA self-update (boleh dipertimbangkan setelah Fase 7).

---

## 12. Prompt Pembuka yang Disarankan untuk Agent

> Baca `project.md` dan `docs/SIDELOAD.md`. Mulai dari Fase 0. Ringkas rencana kerja Fase 0 dan 1 dalam `STATUS.md`, jalankan, lalu lanjutkan fase berikutnya secara berurutan sesuai acceptance criteria. Jika butuh data dari saya (URL CMS, server key, akses perangkat), tulis di `STATUS.md` bagian "Butuh dari pengguna" dan lanjutkan pekerjaan yang tidak bergantung padanya.
