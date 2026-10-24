package com.sesmartcell.notaservice.data.firebase

import android.content.Context
import android.util.Log
import com.sesmartcell.notaservice.R
import com.sesmartcell.notaservice.auth.AuthManager
import com.sesmartcell.notaservice.data.entity.PaymentRecordEntity
import com.sesmartcell.notaservice.data.entity.ServiceNoteEntity
import com.sesmartcell.notaservice.data.entity.StoreSettingsEntity
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

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

    private fun getUserId(): String? = AuthManager.currentUser?.uid ?: AuthManager.getAppUser(context)?.uid

    suspend fun syncNoteToCloud(note: ServiceNoteEntity) = withContext(Dispatchers.IO) {
        val uid = getUserId() ?: return@withContext
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
            docRef.set(data).await()
            Log.d("FirestoreSync", "Note ${note.noteNumber} synced to Firestore")
        } catch (e: Exception) {
            Log.e("FirestoreSync", "Failed to sync note: ${e.message}", e)
        }
    }

    suspend fun syncPaymentToCloud(payment: PaymentRecordEntity) = withContext(Dispatchers.IO) {
        val uid = getUserId() ?: return@withContext
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
            docRef.set(data).await()
            Log.d("FirestoreSync", "Payment ${payment.id} synced to Firestore")
        } catch (e: Exception) {
            Log.e("FirestoreSync", "Failed to sync payment: ${e.message}", e)
        }
    }

    suspend fun syncSettingsToCloud(settings: StoreSettingsEntity) = withContext(Dispatchers.IO) {
        val uid = getUserId() ?: return@withContext
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
            docRef.set(data).await()
            Log.d("FirestoreSync", "Store settings synced to Firestore")
        } catch (e: Exception) {
            Log.e("FirestoreSync", "Failed to sync settings: ${e.message}", e)
        }
    }

    suspend fun deleteNoteFromCloud(noteNumber: String) = withContext(Dispatchers.IO) {
        val uid = getUserId() ?: return@withContext
        try {
            db.collection("users")
                .document(uid)
                .collection("service_notes")
                .document(noteNumber)
                .delete()
                .await()
        } catch (e: Exception) {
            Log.e("FirestoreSync", "Failed to delete note from cloud: ${e.message}", e)
        }
    }
}
