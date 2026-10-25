package com.sesmartcell.notaservice.data.database

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sesmartcell.notaservice.data.dao.PaymentRecordDao
import com.sesmartcell.notaservice.data.dao.ServiceNoteDao
import com.sesmartcell.notaservice.data.dao.StoreSettingsDao
import com.sesmartcell.notaservice.data.entity.PaymentRecordEntity
import com.sesmartcell.notaservice.data.entity.ServiceNoteEntity
import com.sesmartcell.notaservice.data.entity.StoreSettingsEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ServiceNoteEntity::class,
        PaymentRecordEntity::class,
        StoreSettingsEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun serviceNoteDao(): ServiceNoteDao
    abstract fun paymentRecordDao(): PaymentRecordDao
    abstract fun storeSettingsDao(): StoreSettingsDao

    companion object {
        @Volatile
        private var DATABASES: MutableMap<String, AppDatabase> = mutableMapOf()

        /** Nama file database PER AKUN: se_smart_cell_<uid>.db. */
        private fun databaseNameForUid(userUid: String): String {
            val safeUid = userUid.replace(Regex("[^A-Za-z0-9_.-]"), "_")
            return if (safeUid.isBlank()) {
                "se_smart_cell_guest.db"
            } else {
                "se_smart_cell_${safeUid}.db"
            }
        }

        /**
         * Database LOKAL PER AKUN: setiap uid mendapat file Room tersendiri
         * (se_smart_cell_<uid>.db) sehingga data antar akun tidak pernah
         * bercampur, dan penghapusan akun hanya menghapus file akun itu.
         */
        fun getInstanceForUser(context: Context, userUid: String): AppDatabase {
            val databaseName = databaseNameForUid(userUid)
            return DATABASES[databaseName] ?: synchronized(DATABASES) {
                DATABASES.getOrPut(databaseName) { buildDatabase(context, userUid) }
            }
        }

        private fun buildDatabase(context: Context, userUid: String): AppDatabase {
            val appContext = context.applicationContext
            return Room.databaseBuilder(
                appContext,
                AppDatabase::class.java,
                databaseNameForUid(userUid)
            )
                .addCallback(object : Callback() {
                    // Seed pengaturan default HANYA saat file database akun
                    // itu pertama kali dibuat (callback onCreate Room).
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                getInstanceForUser(appContext, userUid)
                                    .storeSettingsDao()
                                    .insertOrUpdate(StoreSettingsEntity())
                            } catch (_: Exception) {
                            }
                        }
                    }
                })
                .build()
        }

        /** Tutup instance aktif lalu hapus file database milik satu akun. */
        fun deleteDatabaseForUser(context: Context, userUid: String): Boolean {
            val databaseName = databaseNameForUid(userUid)
            val existing = DATABASES.remove(databaseName)
            existing?.close()
            return context.deleteDatabase(databaseName)
        }

        /**
         * Database warisan dari versi SEBELUM isolasi per akun. Versi lama memakai
         * satu database bersama ("se_smart_cell.db") dan fallback "guest"
         * ("se_smart_cell_guest.db") sehingga data antar akun tercampur di sana.
         * Dihapus sekali di setiap startup agar data lama campuran TIDAK PERNAH
         * muncul lagi di akun mana pun — database per-akun (se_smart_cell_<uid>.db)
         * tidak disentuh, jadi data milik akun sebelumnya tetap utuh.
         */
        private val LEGACY_SHARED_DATABASES = listOf(
            "se_smart_cell.db",
            "se_smart_cell_guest.db"
        )

        fun purgeLegacySharedDatabases(context: Context) {
            for (name in LEGACY_SHARED_DATABASES) {
                try {
                    val open = DATABASES.remove(name)
                    open?.close()
                    context.deleteDatabase(name)
                } catch (e: Exception) {
                    Log.w("AppDatabase", "Gagal membersihkan database lama $name: ${e.message}")
                }
            }
        }
    }
}
