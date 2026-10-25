package com.sesmartcell.notaservice

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.recaptcha.RecaptchaAppCheckProviderFactory
import com.sesmartcell.notaservice.R
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

        // Aktivasi Firebase App Check: melindungi Auth & Firestore dari
        // permintaan di luar aplikasi ini.
        //  - build debug  -> debug provider (token dicetak ke logcat;
        //    daftarkan tokennya di Firebase console -> App Check -> Apps ->
        //    Manage debug tokens),
        //  - build rilis  -> reCAPTCHA Enterprise. APK ini dibagikan langsung
        //    tanpa Play Store, jadi Play Integrity tidak bisa dipakai. Site key
        //    diambil dari string appcheck_recaptcha_site_key (strings.xml);
        //    selama kosong, App Check nonaktif di build rilis.
        // Enforcement diaktifkan terpisah di Firebase console -> App Check.
        try {
            if (BuildConfig.DEBUG) {
                FirebaseAppCheck.getInstance()
                    .installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
            } else {
                val siteKey = getString(R.string.appcheck_recaptcha_site_key).trim()
                if (siteKey.isNotBlank()) {
                    FirebaseAppCheck.getInstance()
                        .installAppCheckProviderFactory(
                            RecaptchaAppCheckProviderFactory.getInstance(siteKey)
                        )
                } else {
                    Log.w("NotaService", "Site key reCAPTCHA kosong - App Check nonaktif di build rilis")
                }
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
