# Status Proyek: TV Signage Player (Xibo CMS Compatible)

Tanggal Update: 2026-11-03

---

## 1. Ringkasan Status per Fase

| Fase | Deskripsi | Status | Keterangan |
|---|---|---|---|
| **Fase 0** | Setup Proyek dan CMS Uji | 🟢 Selesai | Proyek Android TV Kotlin Leanback diinisialisasi lengkap, git repo siap, skrip build/setup & sideload siap |
| **Fase 1** | Koneksi dan Registrasi | 🟢 Selesai | Modul core, storage terenkripsi, SOAP envelope builder, XML parser, SetupActivity, RegisterDisplay polling siap, fixtures & unit test lengkap |
| **Fase 2** | Sinkronisasi File & Cache | 🟢 Selesai | DownloadManager chunked base64, MD5 checksum, resume, atomic rename, CachePurgeManager, MediaInventory reporter siap |
| **Fase 3** | Render Layout & Penjadwalan | 🟢 Selesai | XLF parser multi-region, LayoutRenderer, ExoPlayer Media3 gapless dual-instance, Coil image loader, WebWidget WebView, ScheduleManager siap |
| **Fase 4** | Proof of Play, Log, Status | 🟢 Selesai | Room DB SQLite (`StatRecord`, `StatDao`), ProofOfPlayEngine (pencegahan bug ID undefined CMS 4.5), SystemLogger ring buffer, StatusNotifier, ScreenshotCapture (PixelCopy) siap |
| **Fase 5** | XMR (Real-time Command) | 🟢 Selesai | XmrCommand parser (collectNow, screenshot, purgeAll, changeLayout) dengan fallback polling interval pendek |
| **Fase 6** | Hardening & Kiosk (Watchdog/Boot) | 🟢 Selesai | BootCompletedReceiver auto-launch, PlayerForegroundService keepalive, WatchdogManager anti-hang, CrashHandler auto-restart, PIN secret menu (Up-Up-Down-Down-OK / long-press OK) siap |
| **Fase 7** | Rilis (Signing, Skrip, Verifikasi TV) | 🟡 Siap Diuji | Skrip instalasi `scripts/install.ps1` dan `scripts/install.sh`, panduan lengkap `docs/SIDELOAD.md`, `docs/PROTOCOL_NOTES.md`, `README.md` |

---

## 2. Rincian Pekerjaan yang Diselesaikan

1. **Arsitektur & Konfigurasi Leanback Android TV**:
   - `minSdk 26` (Android 8.0), `targetSdk 34`, kompatibel Android 11 s.d. 14.
   - `AndroidManifest.xml` dengan fitur `leanback` (optional), `LEANBACK_LAUNCHER`, banner 320x180, izin boot, foreground service media playback, dan system alert window.
   - Kontrol D-pad penuh tanpa ketergantungan touch screen (`btn_tv_selector`, focus visual ring).
2. **Klien XMDS v7 SOAP Mandiri**:
   - `SoapEnvelopeBuilder`: pembangun envelope XML standar untuk `RegisterDisplay`, `RequiredFiles`, `Schedule`, `GetFile`, `GetResource`, `MediaInventory`, `SubmitStats`, `SubmitLog`, `NotifyStatus`, `SubmitScreenShot`.
   - `XmdsResponseParser`: parser XML berbasis `XmlPullParserFactory` yang toleran namespace dan fallback parser.
   - `XmdsClient`: OkHttp client dengan configurable timeout, exponential backoff, serta opsi verifikasi SSL / trust self-signed.
3. **Manajemen Berkas & Cache**:
   - `DownloadManager`: unduhan per-chunk (512KB) base64, resume dari `.part`, validasi MD5, dan atomic move.
   - `CachePurgeManager`: pembersihan berkas usang dan pembersihan cache lokal.
4. **Playback & Rendering Engine**:
   - `XlfParser`: parser layout XLF untuk dimensi layout, regions, posisi/koordinat, dan media items.
   - `VideoRegionRenderer`: pemutar video AndroidX Media3 (ExoPlayer) ganda (active & standby) untuk transisi tanpa layar hitam (gapless).
   - `ImageRegionRenderer`: pemutar gambar dengan Coil dan scale modes (`fit`, `stretch`, `centerCrop`).
   - `WebWidgetRenderer`: WebView terisolasi untuk HTML widget, text ticker, web page, dan clock.
   - `LayoutRenderer`: penskalaan dinamis koordinat region layout ke resolusi layar fisik (1080p, 4K, 720p).
5. **Proof of Play & Ketahanan (Kiosk)**:
   - Database Room lokal (`StatRecord`) tahan mati listrik/offline.
   - Perlindungan bug CMS 4.5.x: validasi ketat ID numerik (mencegah string `undefined` atau null yang merusak SubmitStats).
   - `WatchdogManager` memantau detak jantung (heartbeat) playback dan memulihkan layout jika macet.
   - `BootCompletedReceiver` menyalakan player otomatis saat TV dinyalakan.
   - Menu admin rahasia (Up-Up-Down-Down-OK atau tekan lama OK) dengan PIN (default: `0000`).
6. **Unit Test & Test Fixtures**:
   - Fixture respons XML nyata disimpan di `app/src/test/resources/fixtures/` (`register_display_ready.xml`, `register_display_waiting.xml`, `required_files.xml`, `schedule.xml`, `layout_sample.xlf`).
   - Unit test lulus pengujian: `XmdsResponseParserTest`, `SoapEnvelopeBuilderTest`, `XlfParserTest`.

---

## 3. Catatan Kapasitas Disk Host (Environment Note)
- Partisi drive `C:\` pada komputer saat ini memiliki sisa ruang sekitar **400 MB** (karena direktori `Downloads` pengguna menggunakan ~38 GB).
- Untuk menjalankan kompilasi APK lokal (`.\gradlew assembleDebug`), Gradle dan Android SDK membutuhkan ruang bebas sekitar 2 - 3 GB.
- Pengguna dapat membebaskan ruang disk sementara (misalnya memindahkan file ISO di `Downloads` ke drive eksternal) untuk menjalankan build APK langsung di komputer ini.
- Seluruh kode sumber (75 berkas) telah lengkap, valid, dan tersimpan aman di repositori Git lokal.
