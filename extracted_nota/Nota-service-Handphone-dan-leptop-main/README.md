# SE SMART CELL - Aplikasi Nota Service HP & Laptop 📱💻

Aplikasi manajemen dan nota service HP & Laptop modern, elegan, dan profesional untuk usaha **SE SMART CELL**. Dibangun secara native menggunakan **Kotlin** dan **Jetpack Compose (Material Design 3)** serta terintegrasi dengan **Firebase Firestore**, **Google Sign-In via Credential Manager**, dan basis data lokal **Room Database** (offline-first).

---

## 🚀 Fitur Unggulan

### 1. 📊 Dashboard Manajemen
- Ringkasan statistik harian: **Nota Masuk Hari Ini**, **Sedang Dikerjakan**, **Service Selesai**, dan **Total Pendapatan**.
- Pencarian cepat realtime (No Nota, Nama, Nomor WhatsApp, Tipe/Model, IMEI/SN).
- Filter status pengerjaan: *Semua, Menunggu, Sedang Dikerjakan, Menunggu Sparepart, Selesai, Diambil Pelanggan*.

### 2. 📝 Form Pembuatan & Edit Nota
- **Nomor Nota Otomatis & Unik** (Format `SC-YYYYMMDD-00X`).
- Pilihan jenis perangkat: **HP** & **Laptop**.
- Chip merek populer (*iPhone, Samsung, Oppo, Vivo, Xiaomi, Realme, Infinix, Asus, Lenovo, HP, Acer, dll.*).
- Input kelengkapan unit, IMEI/SN, dan preset kerusakan populer (*LCD Pecah, Mati Total, Baterai Drop, Konslet, dll.*).
- Rincian biaya: Biaya Jasa + Biaya Sparepart - Diskon = **Total Biaya**.
- Perhitungan otomatis Uang Muka (DP) & Sisa Pembayaran.
- Estimasi pengerjaan, nama teknisi, status service, dan masa garansi.

### 3. 🖨️ Cetak Nota & Simpan PDF
- Kompatibel dengan printer **Thermal POS 58 mm** dan **80 mm** (Bluetooth/USB).
- Opsi langsung cetak fisik atau **"Simpan sebagai PDF"** melalui Android `PrintManager`.
- Desain nota profesional dengan kop toko lengkap, rincian biaya, ketentuan garansi, dan kolom tanda tangan pelanggan & teknisi.

### 4. 🖼️ Buat Gambar Nota (PNG/JPG)
- Pembuatan gambar nota resolusi tinggi (1080px) yang pas di layar HP.
- 3 Mode nota: **Tanda Terima Masuk**, **Invoice Biaya**, dan **Nota Pelunasan & Garansi**.
- Tombol **Simpan Gambar ke Galeri** perangkat.
- Tombol **Kirim Gambar ke WhatsApp** dengan *caption* otomatis.

### 5. 💬 Integrasi WhatsApp Otomatis
- **Update Status Servis:** Kirim progres perbaikan, estimasi selesai, dan biaya terkini.
- **Instruksi Pembayaran:** Kirim detail rekening bank (BCA, Mandiri, BRI) dan informasi QRIS/E-Wallet.
- **Bukti Pelunasan & Garansi:** Ucapan terima kasih dan konfirmasi masa garansi aktif.

### 6. 💳 Manajemen Pembayaran & Rekening
- Metode pembayaran: **Tunai**, **QRIS**, **Transfer Bank**, dan **E-Wallet** (DANA, OVO, GoPay, ShopeePay).
- Mendukung pembayaran bertahap (DP dan cicilan/pelunasan) dengan riwayat tercatat rapi.

### 7. 📈 Laporan & Ekspor Data
- Laporan pendapatan per periode: *Hari Ini, 7 Hari Terakhir, Bulan Ini, dan Semua*.
- Rincian total jasa, sparepart, diskon, dan total piutang.
- **Ekspor Laporan ke CSV / Excel** yang siap dibuka di spreadsheet.

### 8. ☁️ Sinkronisasi Cloud Firebase
- Data tersimpan secara aman di **Google Cloud Firestore**.
- Autentikasi aman menggunakan **Google Sign-In** melalui Android Jetpack `CredentialManager`.

---

## 🛠️ Cara Deploy & Jadikan Aplikasi di GitHub

Repository ini sudah dilengkapi dengan **GitHub Actions CI/CD workflow** (`.github/workflows/build-apk.yml`). Anda dapat membuat file `.apk` secara otomatis langsung di GitHub tanpa perlu menginstal Android Studio di komputer Anda!

### Langkah 1: Push ke Repository GitHub Anda

1. Di terminal komputer Anda (atau via GitHub Desktop), inisialisasi dan push:
   ```bash
   git add .
   git commit -m "Initial commit: SE SMART CELL Note App"
   git branch -M main
   git remote add origin https://github.com/USERNAME/NAMA-REPO-ANDA.git
   git push -u origin main
   ```

### Langkah 2: Build APK Otomatis di GitHub

1. Buka halaman repository GitHub Anda di browser.
2. Klik tab **Actions** di bagian atas repository.
3. Workflow **Build Android APK (SE SMART CELL)** berjalan otomatis setelah push ke branch `main`/`master`. Anda juga dapat menjalankannya dari **Actions → Build Android APK (SE SMART CELL) → Run workflow**.
4. Setelah build selesai (sekitar 2–3 menit), klik workflow run tersebut.
5. Di bagian **Artifacts**, klik **SE-SMART-CELL-Debug-APK** untuk mengunduh ZIP berisi file APK. Ekstrak ZIP tersebut untuk mendapatkan `app-debug.apk`.

---

## 💻 Cara Menjalankan Secara Lokal (Android Studio)

1. Pastikan Anda telah menginstal **Android Studio** versi terbaru dan menggunakan JDK yang kompatibel (workflow GitHub Actions memakai JDK 21).
2. Buka Android Studio -> Pilih **Open** -> Arahkan ke folder proyek ini.
3. Tunggu proses **Gradle Sync** selesai.
4. Jalankan aplikasi pada Emulator Android atau perangkat fisik dengan mode USB Debugging aktif.
5. Untuk build APK secara manual via terminal:
   ```bash
   ./gradlew assembleDebug
   ```
   File APK hasil build akan berada di folder:
   `app/build/outputs/apk/debug/app-debug.apk`

---

## 📂 Struktur Proyek

```
SE-SMART-CELL/
├── .github/workflows/
│   └── build-apk.yml          # Otomasi build APK via GitHub Actions
├── app/
│   ├── src/main/
│   │   ├── java/com/example/
│   │   │   ├── auth/          # Google Sign-In & Credential Manager
│   │   │   ├── data/
│   │   │   │   ├── dao/       # Room Database DAOs
│   │   │   │   ├── database/  # Room Database Instance & Seeder
│   │   │   │   ├── entity/    # Data Entities
│   │   │   │   ├── firebase/  # Firestore Cloud Sync Layer
│   │   │   │   └── repository/# Service Repository
│   │   │   ├── ui/
│   │   │   │   ├── components/# Badges & Action Dialogs (Print, Image, Payment)
│   │   │   │   ├── screens/   # Dashboard, Form, List, Detail, Reports, Settings
│   │   │   │   └── theme/     # Skema Warna Modern (Hitam, Putih, Biru)
│   │   │   ├── util/          # Formatting, WhatsApp Helper, Bitmap & Print Engine
│   │   │   └── MainActivity.kt
│   │   ├── res/               # Resource gambar, logo SE SMART CELL, icon, XML
│   │   └── AndroidManifest.xml
│   ├── google-services.json   # Konfigurasi Google Cloud & Firebase
│   └── build.gradle.kts       # Konfigurasi Gradle & Dependensi
├── firebase-blueprint.json    # Skema representasi database Firestore
├── firestore.rules            # Keamanan aturan akses Firestore
└── README.md
```

---

## 📄 Lisensi & Hak Cipta
Hak Cipta © 2026 **SE SMART CELL**. Dibuat untuk solusi digital usaha service HP & Laptop.
