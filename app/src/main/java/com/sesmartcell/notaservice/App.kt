package com.sesmartcell.notaservice

import android.app.Application
import com.google.firebase.FirebaseApp
import com.sesmartcell.notaservice.data.database.AppDatabase
import com.sesmartcell.notaservice.util.StorageCleanup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Pemeliharaan penyimpanan sekali per startup:
        //  - database bersama lama ("se_smart_cell.db" & "se_smart_cell_guest.db")
        //    dihapus supaya data campuran versi lama tidak pernah muncul lagi;
        //  - folder warisan & semua cache sementara di penyimpanan internal
        //    dibuang (StorageCleanup).
        // Database per-akun (se_smart_cell_<uid>.db) tidak dihapus.
        CoroutineScope(Dispatchers.IO).launch {
            try {
                AppDatabase.purgeLegacySharedDatabases(this@App)
                StorageCleanup.purgeLegacyStorage(this@App)
            } catch (_: Exception) {
            }
        }
    }
}
