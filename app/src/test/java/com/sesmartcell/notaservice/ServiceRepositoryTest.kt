package com.sesmartcell.notaservice

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sesmartcell.notaservice.data.database.AppDatabase
import com.sesmartcell.notaservice.data.entity.PaymentRecordEntity
import com.sesmartcell.notaservice.data.entity.ServiceNoteEntity
import com.sesmartcell.notaservice.data.repository.ServiceRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Integration test ServiceRepository + Room in-memory (Robolectric):
 * alur DP/pelunasan saat nota dibuat, perubahan status, perhitungan piutang
 * di laporan CSV, dan idempotensi Pull & Restore dari cloud.
 */
// SDK 35 (bukan 36): sandbox Robolectric SDK 36 menuntut Java 21, sedangkan
// builder dan CI memakai JDK 17.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ServiceRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: ServiceRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = ServiceRepository(
            db.serviceNoteDao(),
            db.paymentRecordDao(),
            db.storeSettingsDao()
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun note(number: String, fee: Long = 150_000L) = ServiceNoteEntity(
        noteNumber = number,
        customerName = "Budi",
        customerPhone = "081234567890",
        deviceType = "Handphone",
        brand = "Samsung",
        model = "A54",
        issueDescription = "Layar retak",
        serviceFee = fee
    )

    @Test
    fun `DP di bawah total biaya tercatat sebagai tipe DP`() = runBlocking {
        val (noteId, paymentId) = repository.saveNoteWithPayment(
            note = note("SC-001"),
            initialPayment = 50_000L,
            paymentMethod = "Tunai",
            refNumber = ""
        )
        assertTrue(noteId > 0)
        assertTrue(paymentId > 0)

        val payments = db.paymentRecordDao().getAllPaymentsSync()
        assertEquals(1, payments.size)
        assertEquals("DP", payments[0].paymentType)
        assertTrue("Ref otomatis diawali DP-: ${payments[0].referenceNumber}",
            payments[0].referenceNumber.startsWith("DP-"))
    }

    @Test
    fun `DP sebesar total biaya tercatat sebagai Pelunasan`() = runBlocking {
        repository.saveNoteWithPayment(note("SC-002"), 150_000L, "Tunai", "")
        val payments = db.paymentRecordDao().getAllPaymentsSync()
        assertEquals("Pelunasan", payments[0].paymentType)
    }

    @Test
    fun `nota tanpa DP tidak membuat baris pembayaran`() = runBlocking {
        val (noteId, paymentId) = repository.saveNoteWithPayment(note("SC-003"), 0L, "Tunai", "")
        assertTrue(noteId > 0)
        assertEquals(-1L, paymentId)
        assertTrue(db.paymentRecordDao().getAllPaymentsSync().isEmpty())
    }

    @Test
    fun `updateStatus mengubah status nota tersimpan`() = runBlocking {
        repository.saveNoteWithPayment(note("SC-004"), 0L, "Tunai", "")
        repository.updateStatus("SC-004", "Selesai")
        assertEquals("Selesai", db.serviceNoteDao().getNoteByNumberSync("SC-004")?.serviceStatus)
    }

    @Test
    fun `laporan CSV menghitung piutang hanya dari pembayaran terverifikasi`() = runBlocking {
        repository.saveNoteWithPayment(note("SC-005", fee = 100_000L), 0L, "Tunai", "")
        repository.addPayment(
            PaymentRecordEntity(noteNumber = "SC-005", amount = 40_000L, paymentMethod = "Transfer", isVerified = true)
        )
        repository.addPayment(
            PaymentRecordEntity(noteNumber = "SC-005", amount = 60_000L, paymentMethod = "Transfer", isVerified = false)
        )

        val csv = repository.generateCsvReport(
            notes = db.serviceNoteDao().getAllNotesSync(),
            payments = db.paymentRecordDao().getAllPaymentsSync()
        )
        assertTrue(csv.startsWith("No Nota"))
        // Piutang = 100.000 - 40.000 (pembayaran belum diverifikasi diabaikan)
        assertTrue("Piutang salah di CSV: $csv", csv.contains("\"60000\""))
    }

    @Test
    fun `restoreFromCloud idempoten - restore ulang tidak mendobol data`() = runBlocking {
        val cloudNote = note("SC-006")
        val cloudPayment = PaymentRecordEntity(
            id = 42L, noteNumber = "SC-006", amount = 50_000L, paymentMethod = "Tunai"
        )

        val first = repository.restoreFromCloud(listOf(cloudNote), listOf(cloudPayment), null)
        assertEquals(1 to 1, first)

        val second = repository.restoreFromCloud(listOf(cloudNote), listOf(cloudPayment), null)
        assertEquals(1 to 1, second)

        assertEquals(1, db.serviceNoteDao().getAllNotesSync().size)
        assertEquals(1, db.paymentRecordDao().getAllPaymentsSync().size)
    }
}
