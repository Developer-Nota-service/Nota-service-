# Checklist Produksi — Nota Service SE SMART CELL

Langkah yang perlu diselesaikan sebelum aplikasi dianggap siap produksi.
Kerjakan berurutan; centang setiap item.

## 1. Keystore signing (rotasi — SUDAH dilakukan di repo ini)

Key lama (`my-upload-key.jks`, password `"android"`) pernah ter-commit di git,
jadi dianggap **kompromi** dan sudah diputar: file-nya dihapus dari repo dan
sandbox Base44 kini membuat key baru otomatis di volume terpisah. SHA-1
sertifikat build terbaru selalu bisa dilihat di halaman unduhan (`/cert-sha1.txt`).

Konsekuensi rotasi:

- [ ] **Tambahkan SHA-1 baru** ke Firebase Console → Project settings →
      aplikasi Android → Add fingerprint (tanpa ini Google Sign-In tidak jalan
      di APK baru; login email/password tidak terpengaruh).
- [ ] Pengguna APK lama harus **uninstall lalu pasang APK baru** (key signing
      berbeda tidak bisa meng-update instalasi lama). Data aman: setelah login,
      pulihkan lewat menu **Pull & Restore** dari cloud.

Untuk build di luar sandbox Base44 (laptop / CI), buat keystore sendiri dan
jangan pernah commit:

```bash
keytool -genkeypair -keystore se-smart-cell.jks -alias upload \
  -storepass <password-kuat> -keypass <password-kuat> \
  -dname "CN=SE SMART CELL, O=SE SMART CELL, C=ID" \
  -keyalg RSA -keysize 2048 -validity 10000
```

Lalu panggil Gradle dengan variabel lingkungan:

```
KEYSTORE_FILE=/path/ke/se-smart-cell.jks
KEYSTORE_PASSWORD=<password>
KEY_PASSWORD=<password>
KEY_ALIAS=upload
```

Di GitHub Actions, simpan sebagai secret `KEYSTORE_BASE64` (hasil
`base64 -w0 se-smart-cell.jks`) plus `KEYSTORE_PASSWORD` dan `KEY_PASSWORD`;
workflow build otomatis memakainya.

## 2. Firebase App Check

- [ ] Firebase Console → App Check → tab **Apps** → pilih aplikasi Android →
      **reCAPTCHA Enterprise** → tempel site key → **Save**.
- [ ] Biarkan enforcement **OFF** dulu (mode monitoring) selama 1–2 minggu.
      Pantau Firebase Console → App Check → Overview: request sah yang
      tertolak akan terlihat di grafik metrik.
- [ ] Setelah yakin tidak ada request sah yang gagal, aktifkan enforcement
      untuk **Firestore** dan **Authentication**.

## 3. Keamanan cloud (sudah beres — cukup dijaga)

- Firestore rules sudah membatasi data per-akun (owner-only) dengan validasi
  field. **Jangan** pernah menggantinya dengan rules terbuka umum.
- Dokumen user hanya menyimpan `passwordHash` (SHA-256), tanpa plaintext.
- Kunci API Firebase memang ikut di dalam APK — itu normal; aksesnya dibatasi
  oleh rules + App Check, bukan oleh kerahasiaan kunci.

## 4. Distribusi

- [ ] (Sudah bisa) Bagi APK rilis dari halaman unduhan Base44 — user memasang
      manual, tanpa update otomatis.
- [ ] Untuk update otomatis: daftar **Google Play Developer** (biaya sekali
      US$25), upload app bundle (.aab) lewat Play Console, mulai dari jalur
      **Internal testing** sebelum rilis produksi.
- [ ] Play Store mewajibkan key signing yang sama untuk semua update — simpan
      keystore produksi di tempat aman dan tercadang, jangan hanya di sandbox.
