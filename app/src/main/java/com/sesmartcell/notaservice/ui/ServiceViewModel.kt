package com.sesmartcell.notaservice.ui

import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sesmartcell.notaservice.auth.AppUser
import com.sesmartcell.notaservice.auth.AuthManager
import com.sesmartcell.notaservice.data.database.AppDatabase
import com.sesmartcell.notaservice.data.entity.PaymentRecordEntity
import com.sesmartcell.notaservice.data.entity.ServiceNoteEntity
import com.sesmartcell.notaservice.data.entity.StoreSettingsEntity
import com.sesmartcell.notaservice.data.repository.ServiceRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * ViewModel utama.
 *
 * - Sesi login dipantau lewat [sessionUser]; [refreshSession] dipanggil di
 *   setiap transisi auth (login, logout, hapus akun).
 * - Data lokal tersimpan PER AKUN: tiap uid punya file database Room sendiri
 *   (lihat AppDatabase) dan seluruh aliran data otomatis berpindah ke
 *   database akun yang aktif.
 * - Semua proses auth & simpan berjalan di viewModelScope — tidak tergantung
 *   pada composition, sehingga bebas dari error
 *   "The coroutine scope left the composition".
 * - Setiap aksi simpan/ubah/hapus memberi notifikasi Toast ke pengguna.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ServiceViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application
    val firestoreSync = com.sesmartcell.notaservice.data.firebase.FirestoreSyncManager(application)

    // ==== Sesi & akun aktif ====

    private val _sessionUser = MutableStateFlow(AuthManager.getAppUser(application))
    val sessionUser: StateFlow<AppUser?> = _sessionUser.asStateFlow()

    /** uid akun yang sedang aktif — menentukan file database lokal mana yang dipakai. */
    val activeUserUid = MutableStateFlow(_sessionUser.value?.uid ?: "guest")

    fun refreshSession() {
        val user = AuthManager.getAppUser(appContext)
        _sessionUser.value = user
        activeUserUid.value = user?.uid ?: "guest"
    }

    // ==== Database lokal PER AKUN ====

    private val repositories = ConcurrentHashMap<String, ServiceRepository>()

    private fun repositoryFor(uid: String): ServiceRepository = repositories.getOrPut(uid) {
        val database = AppDatabase.getInstanceForUser(appContext, uid)
        ServiceRepository(
            serviceNoteDao = database.serviceNoteDao(),
            paymentRecordDao = database.paymentRecordDao(),
            storeSettingsDao = database.storeSettingsDao()
        )
    }

    private val repository: ServiceRepository
        get() = repositoryFor(activeUserUid.value)

    init {
        // Umpan balik kegagalan sinkronisasi cloud (Firebase) ke pengguna.
        viewModelScope.launch {
            firestoreSync.syncErrors.collect { message ->
                if (message.isNotBlank()) notifyUser("$message — data aman di perangkat.")
            }
        }
    }

    private fun notifyUser(message: String) {
        Toast.makeText(appContext, message, Toast.LENGTH_LONG).show()
    }

    // ==== Autentikasi (wajib: Google -> buat ID & password -> login) ====

    /** Langkah 1: daftar & verifikasi via akun Google. */
    fun registerWithGoogle(context: Context, onResult: (Result<AppUser>) -> Unit) {
        viewModelScope.launch {
            onResult(AuthManager.registerWithGoogle(context))
        }
    }

    /** Langkah 2: membuat ID Login & Password setelah verifikasi Google. */
    fun completeRegistration(
        context: Context,
        googleUser: AppUser,
        customUserId: String,
        customPassword: String,
        whatsapp: String,
        onResult: (Result<AppUser>) -> Unit
    ) {
        viewModelScope.launch {
            val result = AuthManager.completeRegistration(context, googleUser, customUserId, customPassword, whatsapp)
            if (result.isSuccess) {
                // Akun baru mulai dari nol: buang repository cache uid ini agar
                // aliran data mengambil database baru yang sudah bersih.
                repositories.remove(googleUser.uid)
            }
            onResult(result)
        }
    }

    /** Kirim sandi baru ke nomor WhatsApp terdaftar (alur lupa sandi). */
    fun resetPasswordViaWhatsApp(whatsapp: String, onResult: (Result<String>) -> Unit) {
        viewModelScope.launch {
            onResult(AuthManager.resetPasswordViaWhatsApp(appContext, whatsapp))
        }
    }

    /** Langkah 3: login dengan ID Login & Password. */
    fun loginWithCredentials(
        context: Context,
        customUserId: String,
        password: String,
        onResult: (Result<AppUser>) -> Unit
    ) {
        viewModelScope.launch {
            onResult(AuthManager.loginWithCredentials(context, customUserId, password))
        }
    }

    // ==== Aliran data akun aktif ====

    val allNotes: StateFlow<List<ServiceNoteEntity>> = activeUserUid
        .flatMapLatest { uid -> repositoryFor(uid).allNotes }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allPayments: StateFlow<List<PaymentRecordEntity>> = activeUserUid
        .flatMapLatest { uid -> repositoryFor(uid).allPayments }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val storeSettings: StateFlow<StoreSettingsEntity> = activeUserUid
        .flatMapLatest { uid -> repositoryFor(uid).storeSettings.map { it ?: StoreSettingsEntity() } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = StoreSettingsEntity()
        )

    val searchQuery = MutableStateFlow("")
    val selectedStatusFilter = MutableStateFlow("Semua")
    val selectedDeviceFilter = MutableStateFlow("Semua")

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

    fun getDashboardStats(notes: List<ServiceNoteEntity>, payments: List<PaymentRecordEntity>): DashboardStats {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        val startOfToday = cal.timeInMillis

        cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
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
            notifyUser("Nota ${note.noteNumber} berhasil disimpan")
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
            } catch (_: Exception) {
            }
            onSuccess(id)
        }
    }

    fun updateNote(note: ServiceNoteEntity, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.updateNote(note)
            notifyUser("Perubahan nota ${note.noteNumber} berhasil disimpan")
            try {
                firestoreSync.syncNoteToCloud(note)
            } catch (_: Exception) {
            }
            onSuccess()
        }
    }

    fun updateStatus(noteNumber: String, newStatus: String) {
        viewModelScope.launch {
            repository.updateStatus(noteNumber, newStatus)
            notifyUser("Status servis diperbarui: $newStatus")
            try {
                val note = allNotes.value.find { it.noteNumber == noteNumber }
                if (note != null) {
                    firestoreSync.syncNoteToCloud(note.copy(serviceStatus = newStatus))
                }
            } catch (_: Exception) {
            }
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
            notifyUser("Pembayaran berhasil dicatat")
            try {
                firestoreSync.syncPaymentToCloud(payment.copy(id = id))
            } catch (_: Exception) {
            }
            onSuccess()
        }
    }

    fun deleteNote(note: ServiceNoteEntity, onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deleteNote(note)
            notifyUser("Nota ${note.noteNumber} berhasil dihapus")
            try {
                firestoreSync.deleteNoteFromCloud(note.noteNumber)
            } catch (_: Exception) {
            }
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
                } catch (_: Exception) {
                }
            }
            notifyUser("Nota ${note.noteNumber} ditandai lunas")
            onSuccess()
        }
    }

    fun updateStoreSettings(settings: StoreSettingsEntity, onSaved: () -> Unit) {
        viewModelScope.launch {
            repository.updateSettings(settings)
            notifyUser("Pengaturan berhasil disimpan")
            try {
                firestoreSync.syncSettingsToCloud(settings)
            } catch (_: Exception) {
            }
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
