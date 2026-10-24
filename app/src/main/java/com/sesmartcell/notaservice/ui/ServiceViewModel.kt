package com.sesmartcell.notaservice.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sesmartcell.notaservice.data.database.AppDatabase
import com.sesmartcell.notaservice.data.entity.PaymentRecordEntity
import com.sesmartcell.notaservice.data.entity.ServiceNoteEntity
import com.sesmartcell.notaservice.data.entity.StoreSettingsEntity
import com.sesmartcell.notaservice.data.repository.ServiceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ServiceViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    val firestoreSync = com.sesmartcell.notaservice.data.firebase.FirestoreSyncManager(application)
    val repository = ServiceRepository(
        serviceNoteDao = database.serviceNoteDao(),
        paymentRecordDao = database.paymentRecordDao(),
        storeSettingsDao = database.storeSettingsDao()
    )

    val allNotes: StateFlow<List<ServiceNoteEntity>> = repository.allNotes.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allPayments: StateFlow<List<PaymentRecordEntity>> = repository.allPayments.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val storeSettings: StateFlow<StoreSettingsEntity> = repository.storeSettings
        .combine(MutableStateFlow(StoreSettingsEntity())) { settings, default ->
            settings ?: default
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = StoreSettingsEntity()
        )

    val searchQuery = MutableStateFlow("")
    val selectedStatusFilter = MutableStateFlow("Semua")
    val selectedDeviceFilter = MutableStateFlow("Semua")

    fun signInWithGoogle(
        context: android.content.Context,
        onSuccess: (com.sesmartcell.notaservice.auth.AppUser) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val result = com.sesmartcell.notaservice.auth.AuthManager.signInWithGoogle(context)
                result.fold(
                    onSuccess = { user -> onSuccess(user) },
                    onFailure = { e -> onError(e.localizedMessage ?: "Terjadi kesalahan saat masuk") }
                )
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Terjadi kesalahan")
            }
        }
    }

    // Filtered notes reactive flow
    val filteredNotes: StateFlow<List<ServiceNoteEntity>> = combine(
        allNotes,
        searchQuery,
        selectedStatusFilter,
        selectedDeviceFilter
    ) { notes, query, status, device ->
        notes.filter { note ->
            val matchQuery = query.isBlank() ||
                note.noteNumber.contains(query, ignoreCase = true) ||
                note.customerName.contains(query, ignoreCase = true) ||
                note.customerPhone.contains(query, ignoreCase = true) ||
                note.brand.contains(query, ignoreCase = true) ||
                note.model.contains(query, ignoreCase = true) ||
                note.imeiOrSerial.contains(query, ignoreCase = true)

            val matchStatus = status == "Semua" || note.serviceStatus == status
            val matchDevice = device == "Semua" || note.deviceType.equals(device, ignoreCase = true)

            matchQuery && matchStatus && matchDevice
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun calculateTotalPaidForNote(noteNumber: String, payments: List<PaymentRecordEntity>): Long {
        return payments.filter { it.noteNumber == noteNumber && it.isVerified }.sumOf { it.amount }
    }

    // Dashboard Statistics
    fun getDashboardStats(notes: List<ServiceNoteEntity>, payments: List<PaymentRecordEntity>): DashboardStats {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfToday = cal.timeInMillis

        cal.set(Calendar.DAY_OF_MONTH, 1)
        val startOfMonth = cal.timeInMillis

        val notesToday = notes.count { it.entryDate >= startOfToday }
        val inProgress = notes.count { it.serviceStatus == "Sedang Dikerjakan" || it.serviceStatus == "Menunggu Sparepart" }
        val completed = notes.count { it.serviceStatus == "Selesai" || it.serviceStatus == "Diambil Pelanggan" }
        val waiting = notes.count { it.serviceStatus == "Menunggu" }

        val revenueToday = payments.filter { it.isVerified && it.paymentDate >= startOfToday }.sumOf { it.amount }
        val revenueMonth = payments.filter { it.isVerified && it.paymentDate >= startOfMonth }.sumOf { it.amount }
        val totalRevenueAll = payments.filter { it.isVerified }.sumOf { it.amount }

        val paymentMap = payments.filter { it.isVerified }.groupBy { it.noteNumber }
        val totalPiutang = notes.sumOf { note ->
            val paid = paymentMap[note.noteNumber]?.sumOf { it.amount } ?: 0L
            maxOf(0L, note.totalCost - paid)
        }

        return DashboardStats(
            notesToday = notesToday,
            inProgressCount = inProgress,
            completedCount = completed,
            waitingCount = waiting,
            revenueToday = revenueToday,
            revenueMonth = revenueMonth,
            totalRevenueAll = totalRevenueAll,
            totalPiutang = totalPiutang,
            totalNotes = notes.size
        )
    }

    fun saveNewNote(
        note: ServiceNoteEntity,
        initialPayment: Long,
        paymentMethod: String,
        refNumber: String,
        onSuccess: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val id = repository.saveNoteWithPayment(note, initialPayment, paymentMethod, refNumber)
            try {
                firestoreSync.syncNoteToCloud(note)
                if (initialPayment > 0) {
                    firestoreSync.syncPaymentToCloud(
                        PaymentRecordEntity(
                            noteNumber = note.noteNumber,
                            amount = initialPayment,
                            paymentMethod = paymentMethod,
                            referenceNumber = refNumber,
                            paymentDate = System.currentTimeMillis(),
                            paymentType = if (initialPayment >= note.totalCost) "Pelunasan" else "DP"
                        )
                    )
                }
            } catch (e: Exception) {}
            onSuccess(id)
        }
    }

    fun updateNote(note: ServiceNoteEntity, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.updateNote(note)
            try {
                firestoreSync.syncNoteToCloud(note)
            } catch (e: Exception) {}
            onSuccess()
        }
    }

    fun updateStatus(noteNumber: String, newStatus: String) {
        viewModelScope.launch {
            repository.updateStatus(noteNumber, newStatus)
            try {
                val note = allNotes.value.find { it.noteNumber == noteNumber }
                if (note != null) {
                    firestoreSync.syncNoteToCloud(note.copy(serviceStatus = newStatus))
                }
            } catch (e: Exception) {}
        }
    }

    fun addPaymentRecord(
        noteNumber: String,
        amount: Long,
        method: String,
        refNumber: String,
        type: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val payment = PaymentRecordEntity(
                noteNumber = noteNumber,
                amount = amount,
                paymentMethod = method,
                referenceNumber = refNumber,
                paymentDate = System.currentTimeMillis(),
                paymentType = type,
                isVerified = true
            )
            val id = repository.addPayment(payment)
            try {
                firestoreSync.syncPaymentToCloud(payment.copy(id = id))
            } catch (e: Exception) {}
            onSuccess()
        }
    }

    fun deleteNote(note: ServiceNoteEntity, onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deleteNote(note)
            try {
                firestoreSync.deleteNoteFromCloud(note.noteNumber)
            } catch (e: Exception) {}
            onDeleted()
        }
    }

    fun markNoteAsPaid(note: ServiceNoteEntity, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val payments = allPayments.value
            val totalPaid = payments.filter { it.noteNumber == note.noteNumber && it.isVerified }.sumOf { it.amount }
            val sisa = maxOf(0L, note.totalCost - totalPaid)
            if (sisa > 0) {
                val payment = PaymentRecordEntity(
                    noteNumber = note.noteNumber,
                    amount = sisa,
                    paymentMethod = "Tunai",
                    referenceNumber = "Pelunasan Cepat",
                    paymentDate = System.currentTimeMillis(),
                    paymentType = "Pelunasan",
                    isVerified = true
                )
                val id = repository.addPayment(payment)
                try {
                    firestoreSync.syncPaymentToCloud(payment.copy(id = id))
                } catch (e: Exception) {}
            }
            onSuccess()
        }
    }

    fun updateStoreSettings(settings: StoreSettingsEntity, onSaved: () -> Unit) {
        viewModelScope.launch {
            repository.updateSettings(settings)
            try {
                firestoreSync.syncSettingsToCloud(settings)
            } catch (e: Exception) {}
            onSaved()
        }
    }

    suspend fun getNextNoteNumber(): String {
        return repository.generateNextNoteNumber()
    }

    fun getCsvReport(): String {
        return repository.generateCsvReport(allNotes.value, allPayments.value)
    }
}

data class DashboardStats(
    val notesToday: Int,
    val inProgressCount: Int,
    val completedCount: Int,
    val waitingCount: Int,
    val revenueToday: Long,
    val revenueMonth: Long,
    val totalRevenueAll: Long,
    val totalPiutang: Long,
    val totalNotes: Int
)
