package com.sesmartcell.notaservice.data.firebase

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.sesmartcell.notaservice.R
import com.sesmartcell.notaservice.auth.AuthManager
import com.sesmartcell.notaservice.data.entity.PaymentRecordEntity
import com.sesmartcell.notaservice.data.entity.ServiceNoteEntity
import com.sesmartcell.notaservice.data.entity.StoreSettingsEntity
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Sinkronisasi data per-akun ke Firestore (users/{uid}/service_notes, dst).
 *
 * Semua penulisan bersifat fire-and-forget: Firestore menyimpan write di cache
 * lokal dan mengirimkannya ke server saat online — jadi penyimpanan data TIDAK
 * PERNAH gagal/diblokir karena perangkat sedang offline. Kegagalan permanen
 * (mis. aturan rules) dilaporkan lewat [syncErrors] agar pengguna mendapat
 * notifikasi.
 */
class FirestoreSyncManager(private val context: Context) {

    private val db: FirebaseFirestore by lazy {
        try {
            val dbId = context.getString(R.string.firestore_database_id)
            if (dbId.isNotBlank() && dbId != "(default)") {
                FirebaseFirestore.getInstance(dbId)
            } else {
                FirebaseFirestore.getInstance()
            }
        } catch (e: Exception) {
            FirebaseFirestore.getInstance()
        }
    }

    private val _syncErrors = MutableSharedFlow<String>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    /** Pesan error sinkronisasi terakhir — dikumpulkan ViewModel untuk notifikasi. */
    val syncErrors: SharedFlow<String> = _syncErrors.asSharedFlow()

    // Akun sesi aplikasi dipakai PERTAMA agar data cloud selalu tersinkron ke
    // path akun yang datanya sedang aktif di layar — tidak pernah ke akun lain
    // meski sesi Firebase di perangkat sempat berbeda.
    private fun getUserId(): String? =
        AuthManager.getAppUser(context)?.uid ?: AuthManager.currentUser?.uid

    private fun reportError(operation: String, error: Exception?) {
        val message = "Sinkron cloud gagal ($operation)" +
            (error?.message?.let { ": $it" } ?: "")
        Log.e("FirestoreSync", message, error)
        _syncErrors.tryEmit(message)
    }

    fun syncNoteToCloud(note: ServiceNoteEntity) {
        val uid = getUserId() ?: run {
            Log.w("FirestoreSync", "User belum login, sinkronisasi note dibatalkan")
            return
        }

        try {
            val docRef = db.collection("users")
                .document(uid)
                .collection("service_notes")
                .document(note.noteNumber)

            val data = mapOf(
                "userId" to uid,
                "noteNumber" to note.noteNumber,
                "entryDate" to note.entryDate,
                "customerName" to note.customerName,
                "customerPhone" to note.customerPhone,
                "deviceType" to note.deviceType,
                "brand" to note.brand,
                "model" to note.model,
                "imeiOrSerial" to note.imeiOrSerial,
                "accessories" to note.accessories,
                "issueDescription" to note.issueDescription,
                "technicianCheckResult" to note.technicianCheckResult,
                "repairAction" to note.repairAction,
                "serviceFee" to note.serviceFee,
                "sparepartFee" to note.sparepartFee,
                "discount" to note.discount,
                "downPayment" to note.downPayment,
                "estimatedCompletionDate" to note.estimatedCompletionDate,
                "technicianName" to note.technicianName,
                "serviceStatus" to note.serviceStatus,
                "warrantyPeriod" to note.warrantyPeriod,
                "warrantyEndDate" to note.warrantyEndDate,
                "initialPaymentMethod" to note.initialPaymentMethod,
                "notes" to note.notes,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(data)
                .addOnSuccessListener { Log.d("FirestoreSync", "Note ${note.noteNumber} synced ke Firestore") }
                .addOnFailureListener { reportError("nota ${note.noteNumber}", it) }
        } catch (e: Exception) {
            reportError("nota ${note.noteNumber}", e)
        }
    }

    fun syncPaymentToCloud(payment: PaymentRecordEntity) {
        val uid = getUserId() ?: run {
            Log.w("FirestoreSync", "User belum login, sinkronisasi payment dibatalkan")
            return
        }

        try {
            val docRef = db.collection("users")
                .document(uid)
                .collection("payment_records")
                .document(payment.id.toString())

            val data = mapOf(
                "userId" to uid,
                "noteNumber" to payment.noteNumber,
                "amount" to payment.amount,
                "paymentMethod" to payment.paymentMethod,
                "referenceNumber" to payment.referenceNumber,
                "paymentDate" to payment.paymentDate,
                "paymentType" to payment.paymentType,
                "isVerified" to payment.isVerified,
                "notes" to payment.notes,
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(data)
                .addOnSuccessListener { Log.d("FirestoreSync", "Payment ${payment.id} synced ke Firestore") }
                .addOnFailureListener { reportError("pembayaran", it) }
        } catch (e: Exception) {
            reportError("pembayaran", e)
        }
    }

    fun syncSettingsToCloud(settings: StoreSettingsEntity) {
        val uid = getUserId() ?: run {
            Log.w("FirestoreSync", "User belum login, sinkronisasi settings dibatalkan")
            return
        }

        try {
            val docRef = db.collection("users")
                .document(uid)
                .collection("store_settings")
                .document("default")

            val data = mapOf(
                "userId" to uid,
                "storeName" to settings.storeName,
                "storeTagline" to settings.storeTagline,
                "storeAddress" to settings.storeAddress,
                "storePhone" to settings.storePhone,
                "qrisMerchantName" to settings.qrisMerchantName,
                "bankAccounts" to settings.bankAccounts,
                "ewalletAccounts" to settings.ewalletAccounts,
                "warrantyTerms" to settings.warrantyTerms,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(data)
                .addOnSuccessListener { Log.d("FirestoreSync", "Store settings synced ke Firestore") }
                .addOnFailureListener { reportError("pengaturan toko", it) }
        } catch (e: Exception) {
            reportError("pengaturan toko", e)
        }
    }

    fun deleteNoteFromCloud(noteNumber: String) {
        val uid = getUserId() ?: run {
            Log.w("FirestoreSync", "User belum login, delete note dari cloud dibatalkan")
            return
        }

        try {
            db.collection("users")
                .document(uid)
                .collection("service_notes")
                .document(noteNumber)
                .delete()
                .addOnSuccessListener { Log.d("FirestoreSync", "Note $noteNumber dihapus dari Firestore") }
                .addOnFailureListener { reportError("hapus nota $noteNumber", it) }
        } catch (e: Exception) {
            reportError("hapus nota $noteNumber", e)
        }

        // Pembayaran milik nota ikut dihapus dari cloud supaya tidak menyisakan
        // data yatim (orphan) di payment_records.
        try {
            db.collection("users")
                .document(uid)
                .collection("payment_records")
                .whereEqualTo("noteNumber", noteNumber)
                .get()
                .addOnSuccessListener { snapshot ->
                    for (doc in snapshot.documents) {
                        doc.reference.delete()
                            .addOnFailureListener { reportError("hapus pembayaran nota $noteNumber", it) }
                    }
                }
                .addOnFailureListener { reportError("hapus pembayaran nota $noteNumber", it) }
        } catch (e: Exception) {
            reportError("hapus pembayaran nota $noteNumber", e)
        }
    }
}
