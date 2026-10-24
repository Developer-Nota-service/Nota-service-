package com.sesmartcell.notaservice.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "service_notes",
    indices = [Index(value = ["noteNumber"], unique = true)]
)
data class ServiceNoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val noteNumber: String,
    val entryDate: Long = System.currentTimeMillis(),
    val customerName: String,
    val customerPhone: String,
    val deviceType: String,
    val brand: String,
    val model: String,
    val imeiOrSerial: String = "",
    val accessories: String = "",
    val issueDescription: String,
    val technicianCheckResult: String = "",
    val repairAction: String = "",
    val serviceFee: Long = 0,
    val sparepartFee: Long = 0,
    val discount: Long = 0,
    val downPayment: Long = 0,
    val estimatedCompletionDate: String = "",
    val technicianName: String = "Teknisi SE SMART",
    val serviceStatus: String = "Menunggu",
    val warrantyPeriod: String = "30 Hari",
    val warrantyEndDate: String = "",
    val initialPaymentMethod: String = "Tunai",
    val notes: String = "",
    val updatedAt: Long = System.currentTimeMillis()
) {
    val totalCost: Long
        get() = maxOf(0L, serviceFee + sparepartFee - discount)
}

@Entity(tableName = "payment_records")
data class PaymentRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val noteNumber: String,
    val amount: Long,
    val paymentMethod: String,
    val referenceNumber: String = "",
    val paymentDate: Long = System.currentTimeMillis(),
    val paymentType: String = "Pembayaran",
    val isVerified: Boolean = true,
    val notes: String = ""
)

@Entity(tableName = "store_settings")
data class StoreSettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    val storeName: String = "",
    val storeTagline: String = "Service Smartphone & Laptop Profesional",
    val storeAddress: String = "",
    val storePhone: String = "",
    val qrisMerchantName: String = "",
    val qrisImagePath: String = "",
    val bankAccounts: String = "",
    val ewalletAccounts: String = "",
    val warrantyTerms: String = "1. Garansi servis berlaku sesuai masa garansi sejak tanggal pengambilan unit (dihitung sejak tanggal nota).\n2. Segel garansi, stiker toko, dan komponen original harus dalam keadaan UTUH, tidak rusak, sobek, atau bekas dibongkar pihak lain.\n3. Garansi tidak mencakup kerusakan akibat: cairan/masuk air, jatuh/benturan fisik, lonjakan arus listrik, atau kesalahan pemakaian/human error.\n4. Klaim garansi wajib menyertakan Nota Service fisik/digital asli dari konter kami.",
    val adminPin: String = "1234"
)
