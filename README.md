# AG HIKING Pro - Outdoor GPS & Navigation

Aplikasi Android outdoor, pendakian gunung, trekking, dan navigasi GPS offline profesional yang dirancang khusus untuk penggunaan di lapangan tanpa ketergantungan internet.

---

## 🏔 Fitur Utama

1. **GPS Tracking Foreground Service**
   - Merekam rute pendakian secara presisi (Jarak, Elevasi, Durasi, Kecepatan, Gain/Loss, Akurasi GPS).
   - Tetap aktif merekam saat layar mati, aplikasi diminimize, atau beralih aplikasi dengan Android Foreground Service & Notifikasi status interaktif (Pause, Resume, Stop).
   - Filter kebisingan (GPS noise reduction & vertical jitter hysteresis).

2. **Sistem Peta Outdoor & Offline Tile Engine**
   - Mendukung multi-layer:
     - **OpenStreetMap Standard**
     - **OpenTopoMap (Topografi, Kontur & Relief Bukit)**
     - **Esri World Imagery (Satelit Resolusi Tinggi)**
     - **CartoDB Dark Matter (Mode Malam Hemat Baterai)**
     - **CartoDB Positron (Tampilan Terang Minimalis)**
     - **Outdoor Terrain**
   - Offline Tile Caching 3 tingkat (RAM LruCache -> Penyimpanan Lokal Disk -> Network).
   - Pengunduh Area Peta Offline (Offline Map Manager) dengan estimasi storage, tile count, progress, dan opsi hapus cache.

3. **Navigasi Rute & Off-Route Detection**
   - Panduan rute aktif: Sisa jarak ke tujuan, elevasi tersisa, estimasi waktu tiba (ETA), waypoint berikutnya.
   - Peringatan otomatis **"⚠ ANDA KELUAR DARI JALUR"** ketika melenceng dari rute rencana, lengkap dengan indikator jarak keluar jalur dan panah arah kembali.
   - Toleransi jarak yang dapat disesuaikan (10m, 25m, 50m, 100m) dengan kompensasi akurasi satelit.

4. **Impor & Analisis Rute (GPX / KML / GeoJSON)**
   - Mendukung format geospasial umum menggunakan Android Storage Access Framework (SAF).
   - Termasuk data bawaan rute **Gunung Bawakaraeng via Buluballea** (12 Pos + Puncak 2.830 mdpl).
   - Profil Elevasi Interaktif: Geser scrubber untuk melihat elevasi, kemiringan medan, dan waypoint terdekat di setiap kilometer.

5. **Waypoints & POI Manager**
   - Tandai titik strategis: Puncak (Summit), Basecamp, Camp Site, Sumber Air, Pos Pendakian, Titik Bahaya (Cliff), Shelter, dll.
   - Tambah waypoint melalui koordinat GPS saat ini atau tekan lama pada peta.

6. **Alat Navigasi & Outdoor Toolkit**
   - Kompas Magnetik & Gyroscope dengan visual dial, azimuth derajat, kardinal (U/TL/T/TG/S/BD/B/BL), dan pitch/roll.
   - Altimeter GPS presisi.
   - Konverter Koordinat Decimal Degrees (DD) ⟷ Degrees Minutes Seconds (DMS) ⟷ UTM.
   - Alat Pengukur Jarak (Ruler) & Pengukur Luas Area (Polygon Area) langsung di peta.
   - Senter Cepat & Sinyal Lampu Darurat SOS Strobe (Kode Morse).
   - Stopwatch Pendakian & Pemantau Baterai.

7. **Emergency / SOS**
   - Tampilan koordinat darurat instan beresolusi tinggi (Latitude, Longitude, Elevasi, Akurasi).
   - Salin 1-sentuhan (Copy Coordinates).
   - Bagikan Lokasi Darurat via SMS / WhatsApp / Pesan Seluler.
   - Panggilan Langsung Basarnas (115) & Darurat Nasional (112).

8. **Ekspor Data & Riwayat**
   - Ekspor pendakian ke format `.gpx`, `.kml`, `.geojson`, dan `.csv` dengan penamaan standar `AG_HIKING_{Nama}_{Tanggal}.gpx`.

9. **100% Offline-First & Privasi Terjamin**
   - Seluruh database (Room/SQLite), rute, waypoint, dan riwayat disimpan secara lokal di perangkat Anda.
   - Tidak ada pengiriman data lokasi ke server cloud pihak ketiga.

---

## 🛠 Panduan Build & Deployment

### Prasyarat
- Android SDK 36 (targetSdk 36, minSdk 24 - mendukung Android 10 s/d Android 16+)
- Gradle 8+ & JDK 11/17

### Perintah Build APK Debug
Jalankan perintah berikut di terminal:
```bash
gradle assembleDebug
```
File APK akan dibuat di:
```
app/build/outputs/apk/debug/app-debug.apk
```

### Perintah Build AAB (Android App Bundle) untuk Google Play Store
```bash
gradle bundleRelease
```
File AAB akan dibuat di:
```
app/build/outputs/bundle/release/app-release.aab
```

### Menjalankan Unit Test
```bash
gradle :app:testDebugUnitTest
```

---

## 🔒 Izin & Privasi (Permissions)
Aplikasi hanya meminta izin yang mutlak dibutuhkan untuk fungsi navigasi luar ruangan:
- `ACCESS_FINE_LOCATION` & `ACCESS_COARSE_LOCATION`: Pembacaan posisi satelit GPS.
- `FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_LOCATION`: Merekam trek GPS saat layar mati.
- `POST_NOTIFICATIONS`: Notifikasi status rekaman berjalan di latar belakang (Android 13+).
- `WAKE_LOCK`: Menjaga perekaman GPS tidak terhenti saat layar mati.
- `CAMERA`: Opsional untuk aktivasi lampu kilat senter/SOS.
