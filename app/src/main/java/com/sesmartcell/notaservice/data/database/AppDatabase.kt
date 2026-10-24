package com.sesmartcell.notaservice.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sesmartcell.notaservice.auth.AuthManager
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

        private fun resolveDatabaseName(context: Context): String {
            val uid = context
                .getSharedPreferences(AuthManager.PREFS_NAME, Context.MODE_PRIVATE)
                .getString(AuthManager.KEY_UID, null)
                ?: AuthManager.currentUser?.uid
                ?: "guest"

            val safeUid = uid.replace(Regex("[^A-Za-z0-9_.-]"), "_")
            return if (safeUid.isBlank()) {
                "se_smart_cell_guest.db"
            } else {
                "se_smart_cell_${safeUid}.db"
            }
        }

        fun getInstance(context: Context): AppDatabase {
            val databaseName = resolveDatabaseName(context)

            return DATABASES[databaseName] ?: synchronized(this) {
                DATABASES.getOrPut(databaseName) {
                    Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        databaseName
                    )
                        .addCallback(object : Callback() {
                            override fun onCreate(db: SupportSQLiteDatabase) {
                                super.onCreate(db)
                                CoroutineScope(Dispatchers.IO).launch {
                                    val dbInstance = getInstance(context)
                                    dbInstance.storeSettingsDao().insertOrUpdate(
                                        StoreSettingsEntity()
                                    )
                                }
                            }
                        })
                        .build()
                }
            }
        }
    }
}
