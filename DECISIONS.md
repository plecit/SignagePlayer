# Architecture Decisions (DECISIONS.md)

Dokumen ini mencatat keputusan arsitektural dan teknis dalam pengembangan TV Signage Player untuk Android TV / Google TV (kompatibel Xibo CMS).

---

## ADR 001: Pilihan Arsitektur Aplikasi dan Modul
- **Status**: Diterima
- **Konteks**: Aplikasi ditujukan untuk perangkat Android TV / Google TV dengan remote D-pad, harus tahan berjalan 24/7, hemat memori, dan tahan crash.
- **Keputusan**:
  - Menggunakan Clean Architecture / MVVM dengan Repository Pattern.
  - Async & concurrency menggunakan Kotlin Coroutines (`kotlinx.coroutines`) dan reactive stream `Flow`.
  - Dependensi diatur secara bersih tanpa overhead runtime berat (modular manual DI atau Hilt sederhana).

## ADR 002: Implementasi Klien XMDS (SOAP)
- **Status**: Diterima
- **Konteks**: XMDS Xibo menggunakan protokol SOAP 1.1/1.2 melalui endpoint `xmds.php?v=7`. Library SOAP klasik seperti `ksoap2` sudah usang dan berukuran besar serta kurang fleksibel menangani chunking file binary base64 secara efisien.
- **Keputusan**:
  - Membangun payload SOAP XML secara mandiri menggunakan XML Builder / Template.
  - Parsing respons SOAP menggunakan Android built-in `XmlPullParser` dan DOM parser yang sangat cepat dan hemat alokasi memori.
  - Komunikasi HTTP menggunakan `OkHttpClient` dengan pooling koneksi, configurable timeout, exponential backoff, serta dukungan HTTPS dan HTTP.
  - Setiap request XMDS menyertakan query parameter `?v=7&method=<MethodName>` sesuai rekomendasi dokumentasi resmi Xibo.

## ADR 003: Mesin Render Layout dan Pemutaran Media (XLF Parser & Player)
- **Status**: Diterima
- **Konteks**: Layout Xibo didefinisikan dalam format XLF (XML Layout Format) yang terdiri atas layout dimensions, regions, dan media items di tiap region.
- **Keputusan**:
  - **Video**: Menggunakan AndroidX Media3 ExoPlayer. Untuk transisi antar video tanpa jeda hitam (gapless), dibuat mekanisme pre-buffering / double-player instance (active & standby).
  - **Gambar**: Menggunakan Coil image loader dengan scale type yang menghormati properti XLF (`fit`, `stretch`, `center`).
  - **Widget HTML & Web**: Menggunakan `WebView` terisolasi dengan dukungan offline asset (`GetResource`), hardware acceleration diaktifkan, dan pembersihan memori agresif saat layout berganti.
  - Parser XLF memetakan dimensi koordinat region secara proporsional ke resolusi layar fisik (1080p, 4K, 720p).

## ADR 004: Manajemen Unduhan, Chunking, dan Integritas File
- **Status**: Diterima
- **Konteks**: File media signage bisa berukuran besar (ratusan MB hingga GB) dan koneksi TV bisa tidak stabil.
- **Keputusan**:
  - Unduhan dilakukan per-chunk via `GetFile` (atau direct download jika CMS menyediakan URL HTTP langsung).
  - File sementara ditulis dengan ekstensi `.part`.
  - Setelah semua chunk lengkap, dihitung checksum MD5. Jika cocok dengan `RequiredFiles`, file di-rename atomik ke nama file final. Jika gagal, file dihapus dan diunduh ulang.
  - Mekanisme resume download melanjutkan dari byte terakhir pada file `.part`.
  - Cache purge menghapus file lokal yang tidak lagi terdaftar pada respons `RequiredFiles` terbaru untuk mencegah storage penuh.

## ADR 005: Proof of Play (Statistik) dan Keamanan Data
- **Status**: Diterima
- **Konteks**: CMS Xibo membutuhkan laporan statistik pemutaran layout dan media secara akurat. CMS 4.5.x sensitif terhadap nilai ID non-numerik (misal string "undefined" menyebabkan error 500 pada SubmitStats).
- **Keputusan**:
  - Proof of Play disimpan di database lokal SQLite / Room (`StatRecord`).
  - Validasi ketat diterapkan sebelum enqueue: ID harus berupa integer valid, durasi harus non-negatif, timestamp dalam format UTC ISO-8601 (`yyyy-MM-dd HH:mm:ss`).
  - Stat dikirim secara batch via `SubmitStats`. Data lokal hanya dihapus setelah server mengembalikan status sukses.

## ADR 006: Identitas Perangkat dan Penyimpanan Kredensial
- **Status**: Diterima
- **Konteks**: Player membutuhkan Hardware Key yang stabil dan kredensial CMS (Server Key) yang aman.
- **Keputusan**:
  - `hardwareKey` di-generate sekali saat instalasi menggunakan UUID v4 acak dan disimpan secara persisten.
  - Server Key dan URL CMS disimpan terenkripsi menggunakan `EncryptedSharedPreferences` (didukung Android Keystore).
  - Layar Setup menampilkan Hardware Key secara jelas agar administrator mudah mengotorisasi di dashboard Xibo CMS.

## ADR 007: Kanal Real-Time XMR
- **Status**: Diterima
- **Konteks**: XMR di Xibo CMS tradisional menggunakan ZeroMQ + RSA/RC4 encryption (`openssl_seal`). CMS modern juga mengeksplorasi WebSocket.
- **Keputusan**:
  - Implementasi fallback otomatis: jika koneksi real-time XMR belum terkonfigurasi atau terganggu, player tetap berfungsi optimal menggunakan siklus polling `collectInterval` berkala.
  - Dukungan penanganan perintah esensial: `collectNow`, `screenShot`, `purgeAll`.
