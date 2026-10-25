# SE SMART CELL — Aplikasi Nota Service HP & Laptop 📱💻

Aplikasi manajemen service dan nota untuk usaha **SE SMART CELL**. Dibangun **native Android** dengan **Kotlin** dan **Jetpack Compose (Material Design 3)**, berbasis **offline-first** (Room Database) dengan sinkronisasi cloud **Firebase Firestore**, serta autentikasi **Google Sign-In** dan **email/password**.

- **Package / applicationId:** `com.sesmartcell.notaservice`
- **minSdk 24 · targetSdk 36 · compileSdk 36**
- **versionName 1.0 (versionCode 1)**

---

## 🚀 Fitur

### 1. 📊 Dashboard Manajemen
- Ringkasan statistik harian: **Nota Masuk Hari Ini**, **Sedang Dikerjakan**, **Service Selesai**, **Total Pendapatan**.
- Pencarian cepat realtime (No Nota, Nama, No. WhatsApp, Tipe/Model, IMEI/SN).
- Filter status pengerjaan: *Semua, Menunggu, Sedang Dikerjakan, Menunggu Sparepart, Selesai, Diambil Pelanggan*.

### 2. 📝 Form Pembuatan & Edit Nota
- **Nomor nota otomatis & unik** (format `SC-YYYYMMDD-00X`).
- Jenis perangkat: **HP** & **Laptop**, dengan chip merek populer (*iPhone, Samsung, Oppo, Vivo, Xiaomi, Realme, Infinix, Asus, Lenovo, Acer, dll.*).
- Input kelengkapan unit, IMEI/SN, dan preset kerusakan umum (*LCD Pecah, Mati Total, Baterai Drop, Konslet, dll.*).
- Rincian biaya: Biaya Jasa + Biaya Sparepart − Diskon = **Total Biaya**, dengan perhitungan otomatis Uang Muka (DP) & Sisa Pembayaran.
- Estimasi pengerjaan, nama teknisi, status service, dan masa garansi.

### 3. 🖨️ Cetak Nota & Simpan PDF
- Kompatibel printer **thermal POS 58 mm / 80 mm** (Bluetooth/USB).
- Cetak fisik langsung atau **"Simpan sebagai PDF"** melalui Android `PrintManager`.
- Desain nota profesional: kop toko, rincian biaya, ketentuan garansi, kolom tanda tangan pelanggan & teknisi.

### 4. 🖼️ Buat Gambar Nota (PNG)
- Gambar nota resolusi tinggi (1080px) siap kirim ke pelanggan.
- 3 mode: **Tanda Terima Masuk**, **Invoice Biaya**, **Nota Pelunasan & Garansi**.
- Simpan ke galeri atau kirim ke **WhatsApp** dengan caption otomatis.

### 5. 💬 Integrasi WhatsApp Otomatis
- **Update status servis** progres perbaikan, estimasi selesai, dan biaya terkini.
- **Instruksi pembayaran** dengan detail rekening bank (BCA, Mandiri, BRI) dan QRIS/E-Wallet.
- **Bukti pelunasan & garansi** berisi ucapan terima kasih dan konfirmasi masa garansi.

### 6. 💳 Manajemen Pembayaran
- Metode: **Tunai, QRIS, Transfer Bank, E-Wallet** (DANA, OVO, GoPay, ShopeePay).
- Pembayaran bertahap (DP dan pelunasan) dengan riwayat tercatat.

### 7. 📈 Laporan & Ekspor Data
- Laporan pendapatan per periode: *Hari Ini, 7 Hari Terakhir, Bulan Ini, Semua*.
- Rincian total jasa, sparepart, diskon, dan piutang.
- **Ekspor laporan ke CSV/Excel** siap dibuka di spreadsheet.

### 8. 🔐 Autentikasi & Akun
- **Google Sign-In** via Android Credential Manager.
- **Login/registrasi email-password** kustom (password disimpan sebagai SHA-256 hash, tidak pernah plaintext — divalidasi oleh Firestore rules).
- **Logout tetap menyimpan data lokal** — data di perangkat tidak dihapus saat keluar akun (lihat `docs/point-4-data-lokal-saat-logout.md`).

### 9. ☁️ Cloud Sync & Backup
- Sinkronisasi dua arah dengan **Firestore** (per-akun / owner-only).
- **Backup database** otomatis di perangkat.
- **Pull & Restore dari Cloud** — pulihkan seluruh data ke perangkat baru setelah login.
- **Firebase App Check** (reCAPTCHA Enterprise) melindungi Firestore & Authentication.

---

## 🛠️ Build APK Otomatis di GitHub

Repository sudah dilengkapi workflow **GitHub Actions** (`.github/workflows/build-apk.yml`) yang otomatis menjalankan **unit test** lalu mem-build APK pada push/PR ke `main`/`master` — tanpa perlu Android Studio.

1. Push repository ini ke GitHub Anda.
2. Buka tab **Actions** → jalankan **Build Android APK (SE SMART CELL)**.
3. Setelah selesai, unduh artifact **`SE-SMART-CELL-Debug-APK`** (berisi `app-debug.apk`), plus artifact hasil unit test bila diperlukan.

### Secrets GitHub Actions (opsional tapi disarankan)

| Secret | Fungsi |
|---|---|
| `KEYSTORE_BASE64` | Keystore signing (hasil `base64 -w0 se-smart-cell.jks`). Tanpa ini CI membuat keystore sementara — APK-nya **tidak bisa meng-update** instalasi sebelumnya. |
| `KEYSTORE_PASSWORD` / `KEY_PASSWORD` / `KEY_ALIAS` | Password & alias keystore. |
| `FIREBASE_GOOGLE_SERVICES_JSON_B64` | Timpa `app/google-services.json` dengan konfigurasi Firebase Anda sendiri. |

> **Catatan keystore:** keystore lama yang pernah ter-commit dianggap kompromi dan sudah dirotasi — file `.jks` tidak lagi ada di repo. Buat keystore sendiri (panduan lengkap di `docs/production-checklist.md`) dan jangan pernah commit.

---

## 💻 Menjalankan Secara Lokal (Android Studio)

1. Instal **Android Studio** terbaru dengan **JDK 17**.
2. **Open** → pilih folder proyek ini, tunggu **Gradle Sync** selesai.
3. Pastikan `app/google-services.json` tersedia (repo menyimpan salinan `app/google-services (1).json`; workflow CI menyalinnya otomatis bila perlu).
4. Jalankan di emulator atau perangkat fisik dengan USB Debugging.
5. Build APK manual:
   ```bash
   ./gradlew assembleDebug
   ```
   Hasil: `app/build/outputs/apk/debug/app-debug.apk`
6. Jalankan unit test:
   ```bash
   ./gradlew testDebugUnitTest
   ```

### Variabel lingkungan build (signing)

```
KEYSTORE_FILE=/path/ke/se-smart-cell.jks
KEYSTORE_PASSWORD=<password>
KEY_PASSWORD=<password>
KEY_ALIAS=upload
```

Plugin Gradle *secrets* membaca `.env` (fallback: `.env.example` yang di-commit).

---

## 🧪 Testing

- **Unit test** (JUnit + Robolectric + coroutines-test): `app/src/test/` — mencakup format utils, entity nota, repository, dan pesan WhatsApp.
- **Instrumented test**: `app/src/androidTest/`.
- **Firestore rules test**: `firestore.test.js` menguji aturan keamanan owner-only di `firestore.rules` (lihat juga `firebase-blueprint.json` untuk skema database).

---

## 📂 Struktur Proyek

```
SE-SMART-CELL/
├── .github/workflows/
│   └── build-apk.yml                  # CI: unit test + build APK via GitHub Actions
├── app/
│   ├── src/main/java/com/sesmartcell/notaservice/
│   │   ├── App.kt                     # Application class (DI / inisialisasi)
│   │   ├── MainActivity.kt           # Single-activity host Compose
│   │   ├── auth/AuthManager.kt        # Google Sign-In + email/password, hash SHA-256
│   │   ├── data/
│   │   │   ├── dao/AppDao.kt          # Room DAO
│   │   │   ├── database/AppDatabase.kt# Room instance + seeder
│   │   │   ├── entity/Entities.kt     # Entity ServiceNote, pembayaran, dll.
│   │   │   ├── firebase/FirestoreSyncManager.kt  # Sinkronisasi cloud
│   │   │   └── repository/ServiceRepository.kt   # Sumber tunggal data
│   │   ├── ui/
│   │   │   ├── ServiceViewModel.kt    # ViewModel utama
│   │   │   ├── components/           # CommonUi, PressFeedback, dialog cetak/gambar/pembayaran
│   │   │   ├── screens/              # Auth, Dashboard, List, Detail, CreateEdit, Reports, Settings, Documentation
│   │   │   └── theme/                 # Material 3 color/type/theme
│   │   └── util/                      # FormatUtils, PrintReceiptHelper, ReceiptBitmapHelper, StorageCleanup
│   ├── src/test/ & src/androidTest/   # Unit & instrumented tests
│   ├── google-services.json           # Konfigurasi Firebase (CI bisa menimpa via secret)
│   └── build.gradle.kts               # Konfig module & dependensi
├── docs/
│   ├── production-checklist.md        # Checklist siap produksi (keystore, App Check, distribusi)
│   └── point-4-data-lokal-saat-logout.md
├── firebase.json / firebase-blueprint.json   # Skema Firestore
├── firestore.rules / firestore.test.js       # Security rules owner-only + test
└── README.md
```

---

## 🔒 Keamanan

- **Firestore rules** membatasi setiap dokumen ke pemiliknya (owner-only) dengan validasi field — jangan pernah diganti rules terbuka umum.
- Password hanya disimpan sebagai **hash SHA-256**; rules menolak dokumen yang menyimpan plaintext.
- Kunci API Firebase memang ikut di dalam APK (normal); akses dibatasi oleh **rules + App Check**, bukan kerahasiaan kunci.

---

## 📄 Lisensi & Hak Cipta

Hak Cipta © 2026 **SE SMART CELL**. Dibuat untuk solusi digital usaha service HP & Laptop.
