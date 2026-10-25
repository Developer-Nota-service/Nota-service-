package com.sesmartcell.notaservice.util

import android.content.Context
import java.io.File

/**
 * Pembersihan penyimpanan internal aplikasi — dijalankan sekali di setiap
 * startup (App.onCreate) setelah penerapan isolasi file per-akun:
 *  - File ".db" warisan yang terdampar di filesDir dihapus (Room yang benar
 *    menyimpan di databasesDir sebagai se_smart_cell_<uid>.db).
 *  - Folder sementara yang tidak lagi dipakai ("receipts", "exports", "tmp",
 *    "temp") di filesDir dihapus.
 *  - Sisa file ".tmp" / ".bak" di filesDir dihapus.
 *  - Seluruh isi cacheDir (PNG nota yang dibagikan, file export, sisa unduhan)
 *    dibuang — semua cache bersifat sementara dan dibuat ulang saat dibutuhkan.
 *
 * TIDAK disentuh: database per-akun di databasesDir, file QRIS milik toko,
 * dan data pengaturan — hanya file/folder sementara dan warisan versi lama.
 */
object StorageCleanup {

    /** Folder warisan di filesDir yang tidak lagi dipakai. */
    private val LEGACY_FOLDERS = listOf("receipts", "exports", "tmp", "temp")

    fun purgeLegacyStorage(context: Context) {
        val filesDir = context.filesDir ?: return

        // 1) File .db lama yang salah lokasi di filesDir.
        filesDir.listFiles { f -> f.isFile && f.name.endsWith(".db") }
            ?.forEach { deleteQuietly(it) }

        // 2) Folder warisan yang tidak lagi dipakai aplikasi.
        LEGACY_FOLDERS.forEach { name -> deleteQuietly(File(filesDir, name)) }

        // 3) Sisa file sementara di filesDir.
        filesDir.listFiles { f -> f.isFile && (f.name.endsWith(".tmp") || f.name.endsWith(".bak")) }
            ?.forEach { deleteQuietly(it) }

        // 4) Semua cache sementara dibuang; dibuat ulang otomatis saat dibutuhkan.
        context.cacheDir?.let { cache ->
            cache.listFiles()?.forEach { deleteQuietly(it) }
        }
    }

    private fun deleteQuietly(file: File?) {
        try {
            file?.deleteRecursively()
        } catch (_: Exception) {
        }
    }
}
