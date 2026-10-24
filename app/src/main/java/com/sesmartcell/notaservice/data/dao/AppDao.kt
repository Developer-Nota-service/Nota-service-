package com.sesmartcell.notaservice.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sesmartcell.notaservice.data.entity.PaymentRecordEntity
import com.sesmartcell.notaservice.data.entity.ServiceNoteEntity
import com.sesmartcell.notaservice.data.entity.StoreSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ServiceNoteDao {
    @Query("SELECT * FROM service_notes ORDER BY entryDate DESC")
    fun getAllNotes(): Flow<List<ServiceNoteEntity>>

    @Query("SELECT * FROM service_notes ORDER BY entryDate DESC")
    suspend fun getAllNotesSync(): List<ServiceNoteEntity>

    @Query("SELECT * FROM service_notes WHERE id = :id LIMIT 1")
    fun getNoteById(id: Long): Flow<ServiceNoteEntity?>

    @Query("SELECT * FROM service_notes WHERE noteNumber = :noteNumber LIMIT 1")
    fun getNoteByNumber(noteNumber: String): Flow<ServiceNoteEntity?>

    @Query("SELECT * FROM service_notes WHERE noteNumber = :noteNumber LIMIT 1")
    suspend fun getNoteByNumberSync(noteNumber: String): ServiceNoteEntity?

    @Query("SELECT noteNumber FROM service_notes ORDER BY id DESC LIMIT 1")
    suspend fun getLatestNoteNumber(): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: ServiceNoteEntity): Long

    @Update
    suspend fun updateNote(note: ServiceNoteEntity)

    @Delete
    suspend fun deleteNote(note: ServiceNoteEntity)

    @Query("DELETE FROM service_notes WHERE id = :id")
    suspend fun deleteNoteById(id: Long)

    @Query("DELETE FROM service_notes")
    suspend fun deleteAllNotes()
}

@Dao
interface PaymentRecordDao {
    @Query("SELECT * FROM payment_records WHERE noteNumber = :noteNumber ORDER BY paymentDate ASC")
    fun getPaymentsForNote(noteNumber: String): Flow<List<PaymentRecordEntity>>

    @Query("SELECT * FROM payment_records ORDER BY paymentDate DESC")
    fun getAllPayments(): Flow<List<PaymentRecordEntity>>

    @Query("SELECT * FROM payment_records")
    suspend fun getAllPaymentsSync(): List<PaymentRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentRecordEntity): Long

    @Update
    suspend fun updatePayment(payment: PaymentRecordEntity)

    @Delete
    suspend fun deletePayment(payment: PaymentRecordEntity)

    @Query("DELETE FROM payment_records WHERE noteNumber = :noteNumber")
    suspend fun deletePaymentsForNote(noteNumber: String)

    @Query("DELETE FROM payment_records")
    suspend fun deleteAllPayments()
}

@Dao
interface StoreSettingsDao {
    @Query("SELECT * FROM store_settings WHERE id = 1 LIMIT 1")
    fun getSettings(): Flow<StoreSettingsEntity?>

    @Query("SELECT * FROM store_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsSync(): StoreSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(settings: StoreSettingsEntity)
}
