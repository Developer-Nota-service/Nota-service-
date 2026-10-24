# Point 4: Pembersihan Data Lokal Saat Logout

- Saat pengguna melakukan Sign Out, aplikasi tidak langsung menghapus seluruh data lokal yang tersimpan di perangkat.
- Setiap akun memiliki database lokal yang terpisah berdasarkan `uid` pengguna.
- Jika pengguna logout lalu login dengan akun lain di perangkat yang sama, aplikasi akan otomatis menggunakan database baru untuk akun tersebut.
- Dengan cara ini, nota dan data penting dari akun sebelumnya tidak ikut hilang saat terjadi pergantian akun.
- Data lokal hanya dibersihkan jika memang benar-benar diarahkan untuk reset akun tertentu, bukan saat logout biasa.

Implementasi yang digunakan:
- `AuthManager.signOut()` hanya membersihkan session login dan credential Google.
- `AppDatabase.getInstance(context)` membangun nama database per user, misalnya `se_smart_cell_<uid>.db`.
- Oleh karena itu, 1 perangkat dapat menampung beberapa akun secara aman tanpa saling menimpa data.

