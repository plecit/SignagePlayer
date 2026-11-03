# Protokol XMDS v7 & Catatan Integrasi Xibo CMS

Dokumen ini berisi catatan teknis spesifikasi protokol XMDS (Xibo Message Delivery Service) versi 7 dan format data pertukaran antara player Android TV dan Xibo CMS.

---

## 1. Konvensi Endpoint & Transport

- **Base URL**: `<CMS_URL>/xmds.php?v=7`
- **Method Query Parameter**: Disarankan menyertakan `&method=<MethodName>` pada setiap request URL (misal: `https://cms.example.com/xmds.php?v=7&method=RegisterDisplay`). Ini membantu routing proxy dan logging cloud.
- **SOAP Standard**: SOAP 1.1 / SOAP 1.2 Envelope dengan namespace target `urn:xmds`.
- **Content-Type**: `text/xml; charset=utf-8`
- **SOAPAction**: Header HTTP disetel ke `urn:xmds#<MethodName>` atau `"urn:xmds#<MethodName>"`.

---

## 2. Parameter Kredensial Umum

Semua pemanggilan XMDS menyertakan dua parameter esensial:
- `serverKey` (string): Kunci rahasia instalasi CMS.
- `hardwareKey` (string): UUID unik yang dihasilkan oleh player pada perangkat dan disimpan persisten.

---

## 3. Spesifikasi Metode XMDS v7

### 3.1 RegisterDisplay
Mendaftarkan player ke CMS dan mendapatkan konfigurasi runtime.
- **Request Parameters**:
  - `serverKey`
  - `hardwareKey`
  - `displayName`
  - `clientType`: `android`
  - `clientVersion`: versi semver aplikasi (misal `1.0.0`)
  - `clientCode`: kode build integer
  - `operatingSystem`: `Android TV <OS_VERSION>`
  - `macAddress`: MAC address perangkat atau string identitas jaringan
- **Response**:
  - `Activation`: `READY` (aktif / berlisensi), `WAITING` (menunggu otorisasi admin di CMS), atau `DISABLED`.
  - Display settings XML / JSON di dalam payload:
    - `COLLECT_INTERVAL`: interval sinkronisasi dalam detik (default 60 - 300s).
    - `STATS_ENABLED`: `true` / `false`.
    - `XMR_CHANNEL`: topik / channel push message.
    - `XMR_PUBKEY`: kunci publik RSA CMS jika enkripsi digunakan.
    - `DOWNLOAD_WINDOW_START` / `DOWNLOAD_WINDOW_END`.
    - `TIMEZONE`: zona waktu CMS / display.

### 3.2 RequiredFiles
Meminta daftar semua berkas yang wajib ada di cache lokal player.
- **Request Parameters**: `serverKey`, `hardwareKey`
- **Response XML Structure**:
  - Tag `<files>` berisi daftar `<file>`:
    - `id`: ID unik media / layout.
    - `type`: `media`, `layout`, `resource`, `font`, dll.
    - `size`: ukuran berkas dalam byte.
    - `md5`: MD5 hash berkas untuk validasi integritas.
    - `path`: nama file lokal yang diharapkan disimpan.
  - Tag `<purge>` berisi file yang tidak lagi digunakan dan harus dihapus oleh player.

### 3.3 Schedule
Meminta jadwal penayangan layout.
- **Request Parameters**: `serverKey`, `hardwareKey`
- **Response XML Structure**:
  - `<schedule>`:
    - `<default>`: layout ID untuk pemutaran default ketika tidak ada jadwal aktif.
    - `<event>`:
      - `id`: Event/Schedule ID.
      - `layoutId`: ID layout yang dijadwalkan.
      - `fromDt`: waktu mulai penayangan (`yyyy-MM-dd HH:mm:ss`).
      - `toDt`: waktu selesai penayangan (`yyyy-MM-dd HH:mm:ss`).
      - `priority`: integer prioritas (angka lebih tinggi mengabaikan angka lebih rendah).
      - `dayparting`: aturan jam tertentu.
      - `sync`: sinkronisasi multi-screen jika ada.

### 3.4 GetFile
Mengunduh potongan file (chunked download).
- **Request Parameters**:
  - `serverKey`, `hardwareKey`
  - `fileId`: ID file yang diminta
  - `fileType`: `media` / `layout`
  - `chunkOffset`: posisi byte awal (dimulai dari `0`)
  - `chunkSize`: ukuran chunk (umumnya 512KB - 2MB)
- **Response**:
  - String Base64 yang merepresentasikan data binary chunk.
  - Player menyatukan potongan ke file `.part` dan menghitung hash MD5 setelah byte terakhir diterima.

### 3.5 GetResource
Mengunduh konten resource dinamis berbasis HTML / template widget.
- **Request Parameters**: `serverKey`, `hardwareKey`, `layoutId`, `regionId`, `mediaId`
- **Response**: String HTML / CSS / JS lengkap yang siap dirender di `WebView`.

### 3.6 MediaInventory
Melaporkan status kelengkapan file lokal ke CMS.
- **Request Parameters**:
  - `serverKey`, `hardwareKey`
  - `mediaInventory`: XML ringkas daftar file yang sudah lengkap (`status="complete"`) atau sedang mengunduh (`status="downloading"`).

### 3.7 SubmitStats (Proof of Play)
Mengirimkan riwayat penayangan layout dan media.
- **Request Parameters**:
  - `serverKey`, `hardwareKey`
  - `statList`: XML berformat:
    ```xml
    <stats>
      <stat type="layout" fromDt="2026-11-03 12:00:00" toDt="2026-11-03 12:00:30" layoutId="12" scheduleId="5" duration="30" />
      <stat type="media" fromDt="2026-11-03 12:00:00" toDt="2026-11-03 12:00:15" layoutId="12" scheduleId="5" mediaId="101" duration="15" />
    </stats>
    ```
- **Catatan Penting**:
  - CMS 4.5.x akan gagal (HTTP 500) jika ada atribut `scheduleId` yang berisi `undefined` atau string kosong. Seluruh ID numerik wajib valid.

### 3.8 SubmitLog
Mengirimkan entri log aplikasi ke CMS untuk diagnostik jarak jauh.
- **Request Parameters**:
  - `serverKey`, `hardwareKey`
  - `logList`: XML kumpulan log berisi timestamp, category, level (`ERROR`, `WARN`, `INFO`), dan message.

### 3.9 NotifyStatus
Mengirimkan status kesehatan perangkat.
- **Request Parameters**:
  - `serverKey`, `hardwareKey`
  - `status`: XML ringkasan kondisi memori, sisa penyimpanan (storage), IP address, suhu perangkat, layout aktif, dan uptime.

### 3.10 SubmitScreenShot
Mengunggah tangkapan layar terkini.
- **Request Parameters**:
  - `serverKey`, `hardwareKey`
  - `screenShot`: String Base64 gambar JPEG hasil tangkapan layar.
