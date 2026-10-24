package com.sesmartcell.notaservice.data.repository

import com.sesmartcell.notaservice.data.dao.PaymentRecordDao
import com.sesmartcell.notaservice.data.dao.ServiceNoteDao
import com.sesmartcell.notaservice.data.dao.StoreSettingsDao
import com.sesmartcell.notaservice.data.entity.PaymentRecordEntity
import com.sesmartcell.notaservice.data.entity.ServiceNoteEntity
import com.sesmartcell.notaservice.data.entity.StoreSettingsEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ServiceRepository(
    private val serviceNoteDao: ServiceNoteDao,
    private val paymentRecordDao: PaymentRecordDao,
    private val storeSettingsDao: StoreSettingsDao
) {
    val allNotes: Flow<List<ServiceNoteEntity>> = serviceNoteDao.getAllNotes()
    val allPayments: Flow<List<PaymentRecordEntity>> = paymentRecordDao.getAllPayments()
    val storeSettings: Flow<StoreSettingsEntity?> = storeSettingsDao.getSettings()

    fun getNoteById(id: Long): Flow<ServiceNoteEntity?> = serviceNoteDao.getNoteById(id)
    fun getPaymentsForNote(noteNumber: String): Flow<List<PaymentRecordEntity>> =
        paymentRecordDao.getPaymentsForNote(noteNumber)

    suspend fun getSettingsSync(): StoreSettingsEntity = withContext(Dispatchers.IO) {
        storeSettingsDao.getSettingsSync() ?: StoreSettingsEntity()
    }

    suspend fun saveNoteWithPayment(
        note: ServiceNoteEntity,
        initialPayment: Long,
        paymentMethod: String,
        refNumber: String
    ): Long = withContext(Dispatchers.IO) {
        val insertedId = serviceNoteDao.insertNote(note)
        if (initialPayment > 0) {
            paymentRecordDao.insertPayment(
                PaymentRecordEntity(
                    noteNumber = note.noteNumber,
                    amount = initialPayment,
                    paymentMethod = paymentMethod,
                    referenceNumber = refNumber.ifBlank { "DP-${System.currentTimeMillis() % 10000}" },
                    paymentDate = System.currentTimeMillis(),
                    paymentType = if (initialPayment >= note.totalCost) "Pelunasan" else "DP"
                )
            )
        }
        insertedId
    }

    suspend fun updateNote(note: ServiceNoteEntity) = withContext(Dispatchers.IO) {
        serviceNoteDao.updateNote(note.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun updateStatus(noteNumber: String, newStatus: String) = withContext(Dispatchers.IO) {
        val note = serviceNoteDao.getNoteByNumberSync(noteNumber)
        if (note != null) {
            serviceNoteDao.updateNote(note.copy(serviceStatus = newStatus, updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun addPayment(payment: PaymentRecordEntity) = withContext(Dispatchers.IO) {
        paymentRecordDao.insertPayment(payment)
    }

    suspend fun deleteNote(note: ServiceNoteEntity) = withContext(Dispatchers.IO) {
        serviceNoteDao.deleteNote(note)
        paymentRecordDao.deletePaymentsForNote(note.noteNumber)
    }

    suspend fun updateSettings(settings: StoreSettingsEntity) = withContext(Dispatchers.IO) {
        storeSettingsDao.insertOrUpdate(settings)
    }

    suspend fun generateNextNoteNumber(): String = withContext(Dispatchers.IO) {
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val datePrefix = "SC-${dateFormat.format(Date())}"
        val latest = serviceNoteDao.getLatestNoteNumber()
        val nextSeq = if (latest != null && latest.startsWith(datePrefix)) {
            val parts = latest.split("-")
            val lastNum = parts.lastOrNull()?.toIntOrNull() ?: 0
            lastNum + 1
        } else {
            1
        }
        String.format(Locale.getDefault(), "%s-%03d", datePrefix, nextSeq)
    }

    fun generateCsvReport(
        notes: List<ServiceNoteEntity>,
        payments: List<PaymentRecordEntity>
    ): String {
        val paymentMap = payments.filter { it.isVerified }
            .groupBy { it.noteNumber }
            .mapValues { (_, list) -> list.sumOf { it.amount } }

        val header = listOf(
            "No Nota",
            "Tanggal Masuk",
            "Nama Pelanggan",
            "Nomor HP",
            "Tipe",
            "Brand",
            "Model",
            "Status",
            "Total Biaya",
            "DP",
            "Terbayar",
            "Piutang",
            "Teknisi"
        ).joinToString(",")

        val rows = notes.joinToString("\n") { note ->
            val paid = paymentMap[note.noteNumber] ?: 0L
            val total = note.totalCost
            val piutang = maxOf(0L, total - paid)
            listOf(
                note.noteNumber,
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(note.entryDate)),
                note.customerName,
                note.customerPhone,
                note.deviceType,
                note.brand,
                note.model,
                note.serviceStatus,
                total.toString(),
                note.downPayment.toString(),
                paid.toString(),
                piutang.toString(),
                note.technicianName
            ).joinToString(",") { value ->
                val normalized = value.replace("\"", "\"\"")
                "\"$normalized\""
            }
        }

        return buildString {
            appendLine(header)
            append(rows)
        }
    }

    suspend fun exportBackupJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        val settings = getSettingsSync()
        val settingsObj = JSONObject().apply {
            put("storeName", settings.storeName)
            put("storeTagline", settings.storeTagline)
            put("storeAddress", settings.storeAddress)
            put("storePhone", settings.storePhone)
            put("qrisMerchantName", settings.qrisMerchantName)
            put("bankAccounts", settings.bankAccounts)
            put("ewalletAccounts", settings.ewalletAccounts)
            put("warrantyTerms", settings.warrantyTerms)
        }
        val notes = serviceNoteDao.getAllNotesSync()
        val payments = paymentRecordDao.getAllPaymentsSync()

        root.put("settings", settingsObj)
        root.put("notes", JSONArray().apply {
            notes.forEach { note ->
                put(JSONObject().apply {
                    put("id", note.id)
                    put("noteNumber", note.noteNumber)
                    put("entryDate", note.entryDate)
                    put("customerName", note.customerName)
                    put("customerPhone", note.customerPhone)
                    put("deviceType", note.deviceType)
                    put("brand", note.brand)
                    put("model", note.model)
                    put("imeiOrSerial", note.imeiOrSerial)
                    put("accessories", note.accessories)
                    put("issueDescription", note.issueDescription)
                    put("technicianCheckResult", note.technicianCheckResult)
                    put("repairAction", note.repairAction)
                    put("serviceFee", note.serviceFee)
                    put("sparepartFee", note.sparepartFee)
                    put("discount", note.discount)
                    put("downPayment", note.downPayment)
                    put("estimatedCompletionDate", note.estimatedCompletionDate)
                    put("technicianName", note.technicianName)
                    put("serviceStatus", note.serviceStatus)
                    put("warrantyPeriod", note.warrantyPeriod)
                    put("warrantyEndDate", note.warrantyEndDate)
                    put("initialPaymentMethod", note.initialPaymentMethod)
                    put("notes", note.notes)
                    put("updatedAt", note.updatedAt)
                })
            }
        })
        root.put("payments", JSONArray().apply {
            payments.forEach { payment ->
                put(JSONObject().apply {
                    put("id", payment.id)
                    put("noteNumber", payment.noteNumber)
                    put("amount", payment.amount)
                    put("paymentMethod", payment.paymentMethod)
                    put("referenceNumber", payment.referenceNumber)
                    put("paymentDate", payment.paymentDate)
                    put("paymentType", payment.paymentType)
                    put("isVerified", payment.isVerified)
                    put("notes", payment.notes)
                })
            }
        })
        root.toString()
    }
}
