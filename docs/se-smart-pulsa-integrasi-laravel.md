# Integrasi SE SMART PULSA (frontend) dengan backend Laravel

Dokumen ini menjelaskan **apa yang sudah ada**, **apa yang sudah diuji**, **apa yang masih
perlu disiapkan**, dan **bagaimana keamanan pembayaran/saldo harus dijaga** saat menghubungkan
frontend Base44 (`web/`) dengan backend Laravel + PostgreSQL yang sudah berjalan di Termux.

> Ringkas: **Tahap 2 sudah tersambung dan diuji** lewat Cloudflare Tunnel (26 Okt 2026) —
> masuk, saldo, riwayat deposit, detail deposit, dan keluar memakai data backend Laravel
> yang sebenarnya. Base44 tetap **tidak** mengimpor atau menjalankan proyek Laravel: hanya
> frontend yang berjalan di preview. Backend lama tidak diubah atau dihapus.

---

## 1. Arsitektur yang dituju

```
Browser (Base44 preview / hosting web SE SMART PULSA)
        │  HTTPS + JSON (REST)
        ▼
Backend Laravel (yang sudah ada) ──► PostgreSQL (se_smart_pulsa)
        │                                   ▲
        ├──► Midtrans Sandbox (deposit)     │  ledger & saldo dicatat di sini
        └──► Penyedia PPOB (Tahap 5)
```

Aturan yang **wajib** dipertahankan:

- Frontend **tidak pernah** berbicara langsung ke Midtrans atau penyedia PPOB.
- Notifikasi Midtrans hanya diterima backend (`POST /api/v1/payments/midtrans/notification`).
- Server Key Midtrans hanya ada di backend (`.env` Laravel), tidak pernah di frontend.
- Saldo, harga, dan status pembayaran hanya boleh berubah di backend.

## 2. Endpoint yang sudah ada dan bentuk responsnya (sudah dipastikan)

Hasil pemeriksaan langsung terhadap backend (26 Okt 2026):

| Metode | Endpoint | Bentuk respons |
| --- | --- | --- |
| POST | `/api/v1/login` | `{ "message": "Login berhasil.", "user": {...}, "token": "..." }` |
| GET | `/api/v1/me` | `{ "user": {...}, "wallet": { "balance_idr": 11000 } }` |
| GET | `/api/v1/deposits` | `{ "deposits": [{ "order_id", "amount_idr", "status", "created_at", "paid_at" }] }` |
| POST | `/api/v1/deposits` | menerima **`{ "amount_idr": <integer> }`** |
| POST | `/api/v1/logout` | mencabut **token yang dipakai pada permintaan itu** saja |
| POST | `/api/v1/register` | dipakai halaman Daftar (belum diuji end-to-end — lihat bagian 8) |
| POST | `/api/v1/payments/midtrans/notification` | **Hanya server Midtrans** — tidak pernah dipanggil frontend |

Catatan penting yang sudah dikonfirmasi:

- **Tidak ada pembungkus `{"data": ...}`.** Koleksi datang dengan nama resource, mis.
  `{ "deposits": [...] }`.
- **Saldo ada di `wallet.balance_idr`**, bukan di objek `user`.
- **Nominal deposit** dikirim sebagai `amount_idr` (bukan `amount`). Batas backend:
  minimal **10.000**, maksimal **2.000.000** (pesan validasi 422 memastikan keduanya).
- **401** = sesi berakhir (dipakai frontend untuk menghapus sesi dan kembali ke halaman masuk).
- **422** = validasi, mis. `{"message": "Email atau password salah.", "errors": {"email": [...]}}`.

Adapter frontend (`web/src/lib/api/live.ts`) sudah disesuaikan dengan bentuk di atas
(ditambah toleransi untuk bentuk alternatif seperti `access_token` atau `balance` datar).

## 3. Endpoint yang masih perlu ditambahkan di backend

Halaman yang membutuhkannya menampilkan pesan jelas ("Backend Laravel belum menyediakan
…") — bukan data karangan dan bukan pesan debug Laravel yang mentah.

| Metode | Usulan endpoint | Dipakai halaman | Bentuk data minimal |
| --- | --- | --- | --- |
| GET | `/api/v1/wallet/ledger` | Saldo (riwayat saldo) | `[{id, type: credit\|debit, amount, balance_after, description, reference, created_at}]` |
| GET | `/api/v1/deposits/{order_number}` | Detail deposit | objek deposit (+ `payment_url`/`va_number` bila ada). **Sementara ini frontend mengambil detail dari daftar deposit** karena endpoint-nya belum ada. |
| GET | `/api/v1/categories` | Beranda, Beli | `[{slug, name, description, icon, destination:{label, placeholder, hint, pattern, pattern_message}}]` |
| GET | `/api/v1/products?category={slug}&popular=1` | Beranda, Beli | `[{id, category_slug, name, provider, sku_code, price, admin_fee, demo}]` |
| GET | `/api/v1/products/{id}` | Konfirmasi pembelian | satu objek produk |
| POST | `/api/v1/transactions` | Konfirmasi pembelian | `{order_number, status, amount, admin_fee, total, serial?, created_at}` |
| GET | `/api/v1/transactions?status=&category=&from=&to=` | Riwayat transaksi | daftar transaksi |
| GET | `/api/v1/transactions/{order_number}` | Detail transaksi | satu objek transaksi |
| GET | `/api/v1/login-history` (opsional) | Riwayat login | `[{id, ip_address, user_agent, logged_in_at}]` |
| GET | `/api/v1/settings/support` (opsional) | Pusat bantuan | `{whatsapp, whatsapp_label, email, hours, address}` |

Selama endpoint katalog dan transaksi belum ada, Beranda/Transaksi/Beli menampilkan
kartu "Gagal memuat data" berisi penjelasan endpoint mana yang belum tersedia. Ini
disengaja: lebih baik pesan jelas daripada produk/harga karangan.

## 4. URL HTTPS backend

Backend di Termux (`127.0.0.1:5432`) **tidak dapat diakses** Base44: alamat itu hanya
berlaku di dalam perangkat. Yang dipakai sekarang:

```
VITE_API_BASE_URL=https://assembled-modems-pilot-artist.trycloudflare.com
```

URL tersebut adalah **quick tunnel Cloudflare** dari Termux. Sifatnya sementara: URL
berubah setiap kali tunnel dijalankan ulang, dan selama tunnel mati frontend akan
menampilkan "Tidak dapat terhubung ke server".

Untuk pengujian serius, pakai **named tunnel** dengan domain sendiri
(`https://api.sesmartpulsa.id` → `localhost:8000`): URL tetap, TLS ditangani Cloudflare,
tidak perlu membuka port router. Alternatif permanen: VPS/hosting dengan domain +
sertifikat Let's Encrypt (Laravel Forge, Ploi, atau Nginx + Certbot).

**Jangan** mengekspos PostgreSQL ke internet. Bila perlu akses DB dari luar, gunakan SSH
tunneling atau VPN, bukan port publik.

Cara mengganti alamat backend di preview: nilai `VITE_API_BASE_URL` pada servis `web` di
`docker-compose.base44.yml` (environment Base44). Mengosongkan nilai itu membuat aplikasi
kembali ke **mode DEMO** (data contoh di perangkat, tanpa backend).

## 5. Autentikasi, CORS, dan sesi

- Frontend mengirim `Authorization: Bearer <token>` dari `localStorage`.
- **Kredensial cookie tidak dikirim ke backend lintas origin.** Backend saat ini menjawab
  `Access-Control-Allow-Origin: *` tanpa `Access-Control-Allow-Credentials`, dan browser
  menolak respons CORS semacam itu bila permintaan membawa kredensial. Karena itu klien
  mengirim `credentials: 'omit'` untuk backend lintas origin (dan `'include'` hanya bila
  backend berada di origin yang sama).
- Bila nanti ingin memakai cookie sesi lintas origin (Sanctum stateful), backend harus:
  `supports_credentials => true` dengan `allowed_origins` **eksplisit** (bukan `*`),
  plus `SameSite=None; Secure`. Untuk sekarang jalur token Bearer sudah cukup.
- Preflight `OPTIONS` backend sudah mengizinkan header `authorization`, `content-type`,
  dan `idempotency-key` (diuji).
- **Rate limit login** aktif di backend (header `X-RateLimit-Limit: 10`). Pembatasan di
  browser (`web/src/lib/constants.ts`) hanya untuk UX dan mudah dilewati.
- Respons `401` menandakan sesi berakhir: frontend menghapus sesi dan mengarahkan ke `/masuk`.
- Respons `422` dibaca sebagai error validasi per field.
- Respons `409` dipakai untuk **transaksi ganda / idempotency** (bagian 7).

## 6. Deposit & Midtrans Sandbox (Tahap 3)

Alur yang benar (semua keputusan di backend):

1. Frontend `POST /api/v1/deposits` dengan **hanya** `{amount_idr}`.
   Backend memvalidasi `10000 ≤ amount_idr ≤ 2000000`, membuat `order_id` unik, memanggil
   Midtrans Snap, lalu menyimpan deposit berstatus `pending`.
2. Respons backend ke frontend: `{order_id, amount_idr, status: 'pending', payment_url?, va_number?, expires_at?}`.
   Untuk sementara daftar deposit backend hanya mengirim `order_id, amount_idr, status,
   created_at, paid_at`, sehingga halaman detail menampilkan "Instruksi pembayaran belum
   tersedia" — tanpa mengarang nomor VA.
3. Midtrans memanggil `POST /api/v1/payments/midtrans/notification`. Backend **wajib**:
   - memverifikasi `signature_key` = `sha512(order_id + status_code + gross_amount + ServerKey)`;
   - memastikan `order_id` ada, `gross_amount` sama dengan nominal pesanan, dan
     `transaction_status` dipetakan dengan benar
     (`capture/settlement` → `paid`; `deny/cancel` → `failed`; `expire` → `expired`;
     `pending` → tetap `pending`);
   - memproses dalam `DB::transaction()` + `lockForUpdate()` pada baris deposit;
   - **hanya** transisi `pending → paid` yang menambah saldo (Midtrans mengirim notifikasi
     yang sama lebih dari sekali — jangan sampai saldo bertambah dua kali). Cara paling
     aman: unique index pada `order_id` di tabel ledger + cek status deposit di dalam transaksi;
   - mencatat baris ledger (`credit`) berisi `balance_after`;
   - mengembalikan HTTP 200 setelah berhasil agar Midtrans tidak mengulang.
4. Frontend hanya **menampilkan** status; tidak ada tombol untuk mengubah status pembayaran.

## 7. Saldo, ledger, dan pencegahan transaksi ganda (Tahap 4)

- Semua operasi saldo: `DB::transaction()` + `lockForUpdate()` pada baris pengguna, lalu
  `UPDATE saldo` dan `INSERT` ledger dalam transaksi yang sama. Tidak ada endpoint yang
  menulis saldo langsung dari nilai yang dikirim klien.
- Tabel ledger minimal: `id, user_id, type (credit|debit), amount, balance_after,
  description, reference, created_at` dengan **unique index** pada `(reference, type, user_id)`
  untuk mencegah pemrosesan ganda.
- `POST /api/v1/transactions` harus:
  1. validasi `product_id`, format nomor tujuan, dan ketersediaan produk;
  2. memakai **idempotency key** (header `Idempotency-Key` + body `idempotency_key`):
     simpan kunci unik, bila kunci yang sama datang lagi **kembalikan transaksi yang sama**
     (HTTP 200), jangan membuat transaksi baru;
  3. memotong saldo sebesar `harga + biaya layanan` di dalam transaksi database;
  4. membuat `order_number` unik dan transaksi berstatus `pending`/`processing`;
  5. meneruskan permintaan ke penyedia PPOB lewat **queue/job** (bukan di dalam request HTTP);
  6. bila penyedia menolak → ubah status ke `failed` dan kembalikan saldo lewat baris ledger
     `credit` (juga idempoten).
- Validasi ulang harga dari database produk **selalu** dilakukan; harga dari frontend tidak
  pernah dipercaya.

## 8. Status pengujian Tahap 2 dan yang perlu disiapkan untuk Tahap 3

Sudah diuji di preview (26 Okt 2026, akun uji backend):

| Alur | Status |
| --- | --- |
| Masuk dengan akun uji → diarahkan ke Beranda | Berhasil |
| Saldo dari `GET /api/v1/me` (`wallet.balance_idr`) tampil di kartu saldo & header | Berhasil |
| Riwayat deposit `GET /api/v1/deposits` tampil (status Berhasil / Menunggu pembayaran) | Berhasil |
| Detail deposit (dari daftar, endpoint detail belum ada) | Berhasil |
| Keluar lewat UI → kembali ke `/masuk` | Berhasil |
| Endpoint katalog/transaksi yang belum ada → pesan penjelasan, bukan data karangan | Berhasil |
| **Pendaftaran (register)** | **Belum diuji** end-to-end — alasan: akun uji tidak dapat dihapus dari backend, jadi pendaftaran akan meninggalkan akun permanen. Uji manual dengan email sekali pakai lalu hapus dari DB. |
| Deposit baru (`POST /api/v1/deposits`) & Midtrans | Belum diuji — bagian Tahap 3 (akan membuat pesanan `pending` nyata di backend). |

Yang perlu disiapkan untuk melanjutkan ke Tahap 3:

1. **URL HTTPS stabil** (named tunnel) supaya pengujian tidak putus.
2. Konfirmasi Midtrans masih dalam mode **Sandbox**.
3. Endpoint detail deposit (`GET /api/v1/deposits/{order_id}`) bila frontend perlu menampilkan
   `payment_url`/`va_number`.
4. Endpoint perubahan akun (`PUT /api/v1/profile`) bila halaman Profil ingin bisa mengedit —
   saat ini halaman Profil hanya menampilkan data.
5. Akun uji khusus yang boleh dipakai bersama (jangan kirim password lewat chat).

## 9. Batas Base44 yang perlu diketahui

- Base44 **tidak dapat mengimpor/menjalankan** proyek Laravel Anda; hanya frontend yang
  berjalan di sini (`web/`, dev server Vite pada port 3000 dengan live reload).
- Aplikasi Android di repositori yang sama tetap dibangun oleh environment ini: APK rilis
  otomatis dibuat ulang saat sumber berubah dan bisa diunduh dari halaman pada port 8000.
- Preview Base44 berjalan pada URL publik sementara; untuk produksi, frontend perlu
  di-deploy terpisah (mis. hosting statis/VPS) dan diarahkan ke URL backend yang sama.
- Backend tetap sumber kebenaran. Tidak ada perubahan pada backend lama sampai Anda
  menyetujuinya.
