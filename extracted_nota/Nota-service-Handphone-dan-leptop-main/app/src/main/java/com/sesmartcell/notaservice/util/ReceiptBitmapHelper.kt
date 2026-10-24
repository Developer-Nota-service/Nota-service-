package com.sesmartcell.notaservice.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import com.sesmartcell.notaservice.data.entity.ServiceNoteEntity
import com.sesmartcell.notaservice.data.entity.StoreSettingsEntity
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

enum class ReceiptType(val title: String) {
    PENERIMAAN("TANDA TERIMA SERVICE"),
    TAGIHAN("INVOICE BIAYA SERVICE"),
    PELUNASAN("NOTA PELUNASAN & GARANSI")
}

object ReceiptBitmapHelper {

    fun generateReceiptBitmap(
        context: Context,
        note: ServiceNoteEntity,
        store: StoreSettingsEntity,
        totalPaid: Long,
        type: ReceiptType = ReceiptType.TAGIHAN
    ): Bitmap {
        val width = 1080
        val height = 2400
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        canvas.drawColor(Color.WHITE)

        val paint = Paint().apply {
            isAntiAlias = true
        }

        paint.color = Color.parseColor("#0F172A")
        canvas.drawRect(0f, 0f, width.toFloat(), 260f, paint)

        paint.color = Color.parseColor("#2563EB")
        canvas.drawRect(0f, 252f, width.toFloat(), 260f, paint)

        paint.color = Color.WHITE
        paint.textSize = 52f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(store.storeName, width / 2f, 90f, paint)

        paint.textSize = 28f
        paint.typeface = Typeface.DEFAULT
        paint.color = Color.parseColor("#93C5FD")
        canvas.drawText(store.storeTagline, width / 2f, 135f, paint)

        paint.textSize = 24f
        paint.color = Color.parseColor("#CBD5E1")
        canvas.drawText(store.storeAddress, width / 2f, 175f, paint)
        canvas.drawText("WhatsApp: ${store.storePhone}", width / 2f, 215f, paint)

        val typeBadgeRect = RectF(60f, 290f, width - 60f, 360f)
        paint.color = when (type) {
            ReceiptType.PELUNASAN -> Color.parseColor("#065F46")
            ReceiptType.PENERIMAAN -> Color.parseColor("#1E3A8A")
            ReceiptType.TAGIHAN -> Color.parseColor("#312E81")
        }
        canvas.drawRoundRect(typeBadgeRect, 16f, 16f, paint)

        paint.color = Color.WHITE
        paint.textSize = 32f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(type.title, width / 2f, 336f, paint)

        paint.textAlign = Paint.Align.LEFT
        var yPos = 410f
        val margin = 60f
        val rightEdge = width - 60f

        paint.color = Color.parseColor("#F1F5F9")
        canvas.drawRoundRect(RectF(margin, yPos, rightEdge, yPos + 180f), 16f, 16f, paint)

        paint.color = Color.parseColor("#1E293B")
        paint.textSize = 28f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("No. Nota: ${note.noteNumber}", margin + 24f, yPos + 48f, paint)
        canvas.drawText("Pelanggan: ${note.customerName}", margin + 24f, yPos + 95f, paint)

        paint.typeface = Typeface.DEFAULT
        paint.textSize = 24f
        paint.color = Color.parseColor("#475569")
        canvas.drawText("Tanggal: ${FormatUtils.formatDate(note.entryDate, "dd MMM yyyy, HH:mm")}", margin + 24f, yPos + 140f, paint)

        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("WA: ${note.customerPhone}", rightEdge - 24f, yPos + 95f, paint)
        canvas.drawText("Teknisi: ${note.technicianName}", rightEdge - 24f, yPos + 140f, paint)
        paint.textAlign = Paint.Align.LEFT

        yPos += 210f

        paint.color = Color.parseColor("#F8FAFC")
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(RectF(margin, yPos, rightEdge, yPos + 270f), 16f, 16f, paint)
        paint.color = Color.parseColor("#CBD5E1")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawRoundRect(RectF(margin, yPos, rightEdge, yPos + 270f), 16f, 16f, paint)
        paint.style = Paint.Style.FILL

        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 28f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("DATA PERANGKAT & KERUSAKAN", margin + 24f, yPos + 45f, paint)

        paint.textSize = 25f
        paint.color = Color.parseColor("#334155")
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("Perangkat : ${note.deviceType} - ${note.brand} ${note.model}", margin + 24f, yPos + 88f, paint)
        val imeiText = if (note.imeiOrSerial.isNotBlank()) note.imeiOrSerial else "-"
        canvas.drawText("IMEI/SN   : $imeiText", margin + 24f, yPos + 128f, paint)
        val accText = if (note.accessories.isNotBlank()) note.accessories else "Unit Saja"
        canvas.drawText("Kelengkapan: $accText", margin + 24f, yPos + 168f, paint)

        paint.color = Color.parseColor("#B91C1C")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Keluhan   : ${note.issueDescription.take(50)}", margin + 24f, yPos + 210f, paint)

        val actionText = if (note.repairAction.isNotBlank()) note.repairAction.take(50) else "Dalam proses diagnostik teknisi"
        paint.color = Color.parseColor("#047857")
        canvas.drawText("Tindakan  : $actionText", margin + 24f, yPos + 248f, paint)

        yPos += 300f

        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 28f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("RINCIAN BIAYA & PEMBAYARAN", margin + 10f, yPos + 35f, paint)
        yPos += 60f

        fun drawCostRow(label: String, value: String, isBold: Boolean = false, isAccent: Boolean = false) {
            paint.typeface = if (isBold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
            paint.textSize = if (isBold) 28f else 25f
            paint.color = if (isAccent) Color.parseColor("#1D4ED8") else Color.parseColor("#1E293B")
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText(label, margin + 20f, yPos, paint)

            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText(value, rightEdge - 20f, yPos, paint)
            paint.textAlign = Paint.Align.LEFT
            yPos += 42f
        }

        drawCostRow("Biaya Jasa Perbaikan", FormatUtils.formatRupiah(note.serviceFee))
        drawCostRow("Biaya Penggantian Sparepart", FormatUtils.formatRupiah(note.sparepartFee))
        if (note.discount > 0) {
            drawCostRow("Potongan Diskon", "- " + FormatUtils.formatRupiah(note.discount))
        }

        paint.color = Color.parseColor("#94A3B8")
        paint.strokeWidth = 2f
        canvas.drawLine(margin + 20f, yPos - 10f, rightEdge - 20f, yPos - 10f, paint)
        yPos += 20f

        drawCostRow("TOTAL BIAYA", FormatUtils.formatRupiah(note.totalCost), isBold = true, isAccent = true)
        drawCostRow("Total Terbayar", FormatUtils.formatRupiah(totalPaid), isBold = false)
        val sisa = maxOf(0L, note.totalCost - totalPaid)
        val sisaColor = if (sisa <= 0) "#059669" else "#DC2626"
        paint.color = Color.parseColor(sisaColor)
        drawCostRow("SISA PEMBAYARAN", if (sisa <= 0) "LUNAS (Rp 0)" else FormatUtils.formatRupiah(sisa), isBold = true)

        yPos += 30f

        val statusRect = RectF(margin, yPos, rightEdge, yPos + 110f)
        paint.color = Color.parseColor("#EFF6FF")
        canvas.drawRoundRect(statusRect, 16f, 16f, paint)

        paint.color = Color.parseColor("#1E3A8A")
        paint.textSize = 26f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Status Servis: ${note.serviceStatus.uppercase()}", margin + 24f, yPos + 45f, paint)

        val warText = "Garansi: ${note.warrantyPeriod} ${if (note.warrantyEndDate.isNotBlank()) "(s/d ${note.warrantyEndDate})" else ""}"
        paint.color = Color.parseColor("#065F46")
        canvas.drawText(warText, margin + 24f, yPos + 85f, paint)

        yPos += 140f

        // QRIS & Payment Methods section on receipt
        val qrisBmp = if (store.qrisImagePath.isNotBlank()) {
            try { BitmapFactory.decodeFile(store.qrisImagePath) } catch (e: Exception) { null }
        } else null

        if (qrisBmp != null || store.bankAccounts.isNotBlank() || store.ewalletAccounts.isNotBlank()) {
            paint.color = Color.parseColor("#F8FAFC")
            paint.style = Paint.Style.FILL
            val paymentBoxHeight = if (qrisBmp != null) 360f else 220f
            canvas.drawRoundRect(RectF(margin, yPos, rightEdge, yPos + paymentBoxHeight), 16f, 16f, paint)
            paint.color = Color.parseColor("#CBD5E1")
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2f
            canvas.drawRoundRect(RectF(margin, yPos, rightEdge, yPos + paymentBoxHeight), 16f, 16f, paint)
            paint.style = Paint.Style.FILL

            paint.color = Color.parseColor("#0F172A")
            paint.textSize = 26f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("PEMBAYARAN VIA TRANSFER & QRIS", margin + 24f, yPos + 38f, paint)

            var textY = yPos + 75f
            if (store.bankAccounts.isNotBlank()) {
                paint.textSize = 22f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("Transfer Bank:", margin + 24f, textY, paint)
                textY += 28f
                paint.typeface = Typeface.DEFAULT
                paint.textSize = 21f
                for (line in store.bankAccounts.lines().take(4)) {
                    canvas.drawText(line, margin + 24f, textY, paint)
                    textY += 26f
                }
            }

            if (store.ewalletAccounts.isNotBlank()) {
                textY += 4f
                paint.textSize = 22f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("E-Wallet (DANA/OVO/GoPay):", margin + 24f, textY, paint)
                textY += 28f
                paint.typeface = Typeface.DEFAULT
                paint.textSize = 21f
                for (line in store.ewalletAccounts.lines().take(3)) {
                    canvas.drawText(line, margin + 24f, textY, paint)
                    textY += 26f
                }
            }

            if (qrisBmp != null) {
                val qrisDest = RectF(rightEdge - 240f, yPos + 60f, rightEdge - 30f, yPos + 300f)
                canvas.drawBitmap(qrisBmp, null, qrisDest, paint)
                paint.textSize = 18f
                paint.typeface = Typeface.DEFAULT
                paint.textAlign = Paint.Align.CENTER
                canvas.drawText("Scan QRIS (${store.qrisMerchantName.ifBlank { "Merchant" }})", rightEdge - 135f, yPos + 322f, paint)
                paint.textAlign = Paint.Align.LEFT
            }

            yPos += paymentBoxHeight + 20f
        }

        paint.color = Color.parseColor("#64748B")
        paint.textSize = 19f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("* Segel utuh adalah syarat sah klaim garansi.", margin + 10f, yPos + 20f, paint)
        canvas.drawText("* Unit tidak diambil melebihi 60 hari di luar tanggung jawab toko.", margin + 10f, yPos + 48f, paint)

        return bitmap
    }

    fun saveBitmapToDevice(context: Context, bitmap: Bitmap, fileName: String): Uri? {
        var outputStream: OutputStream? = null
        var imageUri: Uri? = null

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, "$fileName.png")
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/SE_Smart_Cell")
                }
                imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (imageUri != null) {
                    outputStream = resolver.openOutputStream(imageUri)
                }
            } else {
                val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() + "/SE_Smart_Cell"
                val fileDir = File(imagesDir)
                if (!fileDir.exists()) fileDir.mkdirs()
                val file = File(fileDir, "$fileName.png")
                outputStream = FileOutputStream(file)
                imageUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            }

            outputStream?.let {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                it.flush()
                it.close()
            }
            return imageUri
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun shareReceiptImage(
        context: Context,
        note: ServiceNoteEntity,
        store: StoreSettingsEntity,
        totalPaid: Long,
        type: ReceiptType
    ) {
        val bitmap = generateReceiptBitmap(context, note, store, totalPaid, type)
        try {
            val cachePath = File(context.cacheDir, "receipts")
            cachePath.mkdirs()
            val file = File(cachePath, "Nota_${note.noteNumber}.png")
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                this.type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                val sisa = maxOf(0L, note.totalCost - totalPaid)
                val caption = WhatsAppHelper.createStatusMessage(note, store, totalPaid, sisa)
                putExtra(Intent.EXTRA_TEXT, caption)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Kirim Gambar Nota SE SMART CELL"))
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal membagikan gambar: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
