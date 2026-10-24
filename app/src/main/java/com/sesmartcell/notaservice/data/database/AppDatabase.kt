package com.sesmartcell.notaservice.data.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.sesmartcell.notaservice.auth.AuthManager

abstract class AppDatabase : RoomDatabase() {
    companion object {
        @Volatile
        private val DATABASES: MutableMap<String, AppDatabase> = mutableMapOf()

        private fun resolveDatabaseName(context: Context, userUid: String? = null): String {
            val uid = userUid
                ?: context.getSharedPreferences(AuthManager.PREFS_NAME, Context.MODE_PRIVATE)
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

            return DATABASES[databaseName] ?: synchronized(DATABASES) {
                DATABASES.getOrPut(databaseName) {
                    Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        databaseName
                    ).build()
                }
            }
        }

        fun deleteDatabaseForUser(context: Context, userUid: String? = null): Boolean {
            val databaseName = resolveDatabaseName(context, userUid)
            val existing = DATABASES.remove(databaseName)
            existing?.close()
            return context.deleteDatabase(databaseName)
        }
    }
}
