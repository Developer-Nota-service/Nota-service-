package com.sesmartcell.notaservice

import android.app.Application
import com.google.firebase.FirebaseApp
import com.sesmartcell.notaservice.data.database.AppDatabase
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

        // Bersihkan database lama yang dipakai sebelum isolasi per akun
        // (database bersama "se_smart_cell.db" & "se_smart_cell_guest.db")
        // supaya data campuran versi lama tidak pernah muncul di akun baru.
        // Database per-akun (se_smart_cell_<uid>.db) tidak dihapus.
        CoroutineScope(Dispatchers.IO).launch {
            try {
                AppDatabase.purgeLegacySharedDatabases(this@App)
            } catch (_: Exception) {
            }
        }
    }
}
