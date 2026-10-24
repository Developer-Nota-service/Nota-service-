package com.sesmartcell.notaservice.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.sesmartcell.notaservice.data.entity.ServiceNoteEntity
import com.sesmartcell.notaservice.data.entity.StoreSettingsEntity
import java.net.URLEncoder
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FormatUtils {
    fun formatRupiah(amount: Long): String {
        val formatter = NumberFormat.getNumberInstance(Locale("id", "ID"))
        return "Rp " + formatter.format(amount)
    }

    fun formatDate(timestampMs: Long, pattern: String = "dd MMM yyyy, HH:mm"): String {
        return try {
            val sdf = SimpleDateFormat(pattern, Locale("id", "ID"))
            sdf.format(Date(timestampMs))
        } catch (e: Exception) {
            timestampMs.toString()
        }
    }

    fun sanitizePhone(phone: String): String {
        val digits = phone.filter { it.isDigit() }
        return when {
            digits.startsWith("0") -> "62" + digits.substring(1)
            digits.startsWith("62") -> digits
            digits.startsWith("8") -> "62$digits"
            else -> digits
        }
    }
}

object WhatsAppHelper {

    fun openWhatsApp(context: Context, phone: String, message: String) {
        val formattedPhone = FormatUtils.sanitizePhone(phone)
        try {
            val encodedMsg = URLEncoder.encode(message, "UTF-8")
            val url = "https://api.whatsapp.com/send?phone=$formattedPhone&text=$encodedMsg"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal membuka WhatsApp: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun createStatusMessage(
        note: ServiceNoteEntity,
        store: StoreSettingsEntity,
        totalPaid: Long,
        sisa: Long
    ): String {
        val isLunas = sisa <= 0
        val statusEmoji = when (note.serviceStatus) {
            "Selesai" -> "✅"
            "Sedang Dikerjakan" -> "⚙️"
            "Menunggu Sparepart" -> "⏳"
            "Diambil Pelanggan" -> "🎉"
            else -> "📋"
        }

        return """
            *${store.storeName}*
            _${store.storeTagline}_
            Alamat: ${store.storeAddress}
            No. WhatsApp: ${store.storePhone}
            ─────────────────────────
            *INFORMASI STATUS SERVICE*
            
            Halo Kak *${note.customerName}*, berikut info pengerjaan perangkat Anda:
            
            📄 *No. Nota:* ${note.noteNumber}
            📱 *Perangkat:* ${note.deviceType} - ${note.brand} ${note.model}
            🔍 *Keluhan:* ${note.issueDescription}
            $statusEmoji *Status Pengerjaan:* *${note.serviceStatus.uppercase()}*
            ${if (note.repairAction.isNotBlank()) "🛠 *Tindakan:* ${note.repairAction}\n" else ""}
            💰 *Total Biaya:* ${FormatUtils.formatRupiah(note.totalCost)}
            💵 *Sudah Dibayar:* ${FormatUtils.formatRupiah(totalPaid)}
            📌 *Sisa Pembayaran:* ${FormatUtils.formatRupiah(sisa)} ${if (isLunas) "(LUNAS)" else ""}
            
            ${if (note.estimatedCompletionDate.isNotBlank()) "⏱ *Estimasi Selesai:* ${note.estimatedCompletionDate}\n" else ""}
            🛡 *Garansi Service:* ${note.warrantyPeriod}
            
            Jika ada pertanyaan silakan balas pesan ini. Terima kasih! 🙏
        """.trimIndent()
    }

    fun createPaymentInstruction(
        note: ServiceNoteEntity,
        store: StoreSettingsEntity,
        sisa: Long
    ): String {
        val amountToPay = if (sisa > 0) sisa else note.totalCost
        return """
            *${store.storeName}*
            *INSTRUKSI PEMBAYARAN SERVICE*
            ─────────────────────────
            Halo Kak *${note.customerName}*,
            Detail tagihan untuk Nota *${note.noteNumber}* (${note.brand} ${note.model}):
            
            💵 *Jumlah Yang Perlu Dibayar: ${FormatUtils.formatRupiah(amountToPay)}*
            
            *Pilihan Metode Pembayaran Resmi:*
            
            🏦 *Transfer Bank:*
            ${store.bankAccounts}
            
            📱 *E-Wallet (DANA / OVO / GoPay):*
            ${store.ewalletAccounts}
            
            📲 *QRIS All Payment:*
            Merchant: *${store.qrisMerchantName}*
            (Dapat menggunakan BCA Mobile, Livin, BRImo, Dana, GoPay, OVO, ShopeePay)
            
            _Mohon kirimkan foto/bukti transfer setelah melakukan pembayaran untuk segera diverifikasi oleh admin. Terima kasih._
        """.trimIndent()
    }

    fun createCompletionMessage(
        note: ServiceNoteEntity,
        store: StoreSettingsEntity,
        totalPaid: Long
    ): String {
        return """
            *${store.storeName}*
            *BUKTI PENGAMBILAN & GARANSI AKTIF*
            ─────────────────────────
            Halo Kak *${note.customerName}*,
            Terima kasih telah mempercayakan perbaikan perangkat Anda di ${store.storeName}!
            
            📄 *No. Nota:* ${note.noteNumber}
            📱 *Perangkat:* ${note.brand} ${note.model}
            🛠 *Pekerjaan:* ${note.repairAction.ifBlank { note.issueDescription }}
            💰 *Status Pembayaran:* *LUNAS* (${FormatUtils.formatRupiah(totalPaid)})
            🛡 *Masa Garansi:* ${note.warrantyPeriod} ${if (note.warrantyEndDate.isNotBlank()) "(Hingga ${note.warrantyEndDate})" else ""}
            
            *Ketentuan Garansi:*
            ${store.warrantyTerms}
            
            Simpan nota digital ini sebagai bukti klaim garansi yang sah. Semoga perangkat Anda selalu awet dan lancar digunakan! ✨
        """.trimIndent()
    }
}
