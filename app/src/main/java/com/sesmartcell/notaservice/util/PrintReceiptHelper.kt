package com.sesmartcell.notaservice.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.widget.Toast
import com.sesmartcell.notaservice.data.entity.ServiceNoteEntity
import com.sesmartcell.notaservice.data.entity.StoreSettingsEntity
import java.io.FileOutputStream
import java.io.IOException

object PrintReceiptHelper {

    fun printThermalReceipt(
        context: Context,
        note: ServiceNoteEntity,
        store: StoreSettingsEntity,
        totalPaid: Long,
        is80mm: Boolean = true
    ) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
        if (printManager == null) {
            Toast.makeText(context, "Layanan cetak tidak tersedia di perangkat ini", Toast.LENGTH_SHORT).show()
            return
        }

        val jobName = "Nota_${note.noteNumber}_Thermal"
        printManager.print(
            jobName,
            ThermalPrintAdapter(context, note, store, totalPaid, is80mm),
            PrintAttributes.Builder()
                .setColorMode(PrintAttributes.COLOR_MODE_MONOCHROME)
                .build()
        )
    }

    private class ThermalPrintAdapter(
        private val context: Context,
        private val note: ServiceNoteEntity,
        private val store: StoreSettingsEntity,
        private val totalPaid: Long,
        private val is80mm: Boolean
    ) : PrintDocumentAdapter() {

        private var pdfDocument: PdfDocument? = null
        private val pageWidth = if (is80mm) 230 else 165
        private val pageHeight = 580

        override fun onLayout(
            oldAttributes: PrintAttributes?,
            newAttributes: PrintAttributes?,
            cancellationSignal: CancellationSignal?,
            callback: LayoutResultCallback?,
            extras: Bundle?
        ) {
            if (cancellationSignal?.isCanceled == true) {
                callback?.onLayoutCancelled()
                return
            }

            val pdi = PrintDocumentInfo.Builder("Nota_${note.noteNumber}.pdf")
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(1)
                .build()

            callback?.onLayoutFinished(pdi, true)
        }

        override fun onWrite(
            pages: Array<out PageRange>?,
            destination: ParcelFileDescriptor?,
            cancellationSignal: CancellationSignal?,
            callback: WriteResultCallback?
        ) {
            pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = pdfDocument!!.startPage(pageInfo)

            if (cancellationSignal?.isCanceled == true) {
                callback?.onWriteCancelled()
                pdfDocument?.close()
                pdfDocument = null
                return
            }

            drawThermalContent(page.canvas)
            pdfDocument!!.finishPage(page)

            try {
                destination?.fileDescriptor?.let { fd ->
                    FileOutputStream(fd).use { out ->
                        pdfDocument?.writeTo(out)
                    }
                }
                callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
            } catch (e: IOException) {
                callback?.onWriteFailed(e.toString())
            } finally {
                pdfDocument?.close()
                pdfDocument = null
            }
        }

        private fun drawThermalContent(canvas: Canvas) {
            canvas.drawColor(Color.WHITE)
            val paint = Paint().apply {
                isAntiAlias = true
                color = Color.BLACK
            }

            val margin = 8f
            val right = pageWidth - 8f
            var y = 20f

            paint.textAlign = Paint.Align.CENTER
            paint.textSize = if (is80mm) 12f else 10f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(store.storeName, pageWidth / 2f, y, paint)

            y += 12f
            paint.textSize = 7f
            paint.typeface = Typeface.DEFAULT
            canvas.drawText(store.storeTagline, pageWidth / 2f, y, paint)

            y += 10f
            canvas.drawText(store.storeAddress, pageWidth / 2f, y, paint)
            y += 10f
            canvas.drawText("WA: ${store.storePhone}", pageWidth / 2f, y, paint)

            y += 8f
            paint.strokeWidth = 0.8f
            canvas.drawLine(margin, y, right, y, paint)
            y += 12f

            paint.textAlign = Paint.Align.LEFT
            paint.textSize = 7.5f
            paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            canvas.drawText("NO: ${note.noteNumber}", margin, y, paint)
            y += 10f
            paint.typeface = Typeface.MONOSPACE
            paint.textSize = 6.8f
            canvas.drawText("TGL: ${FormatUtils.formatDate(note.entryDate, "dd/MM/yyyy HH:mm")}", margin, y, paint)
            y += 10f
            canvas.drawText("PLG: ${note.customerName} (${note.customerPhone})", margin, y, paint)
            y += 10f
            canvas.drawText("TEK: ${note.technicianName}", margin, y, paint)

            y += 8f
            canvas.drawLine(margin, y, right, y, paint)
            y += 12f

            paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            canvas.drawText("UNIT: ${note.brand} ${note.model}", margin, y, paint)
            y += 10f
            paint.typeface = Typeface.MONOSPACE
            val imei = if (note.imeiOrSerial.isNotBlank()) note.imeiOrSerial else "-"
            canvas.drawText("IMEI: $imei", margin, y, paint)
            y += 10f
            canvas.drawText("KLH : ${note.issueDescription.take(28)}", margin, y, paint)
            if (note.repairAction.isNotBlank()) {
                y += 10f
                canvas.drawText("TDK : ${note.repairAction.take(28)}", margin, y, paint)
            }

            y += 8f
            canvas.drawLine(margin, y, right, y, paint)
            y += 12f

            fun printLine(label: String, value: String, bold: Boolean = false) {
                paint.typeface = if (bold) Typeface.create(Typeface.MONOSPACE, Typeface.BOLD) else Typeface.MONOSPACE
                paint.textAlign = Paint.Align.LEFT
                canvas.drawText(label, margin, y, paint)
                paint.textAlign = Paint.Align.RIGHT
                canvas.drawText(value, right, y, paint)
                y += 11f
            }

            printLine("Jasa Service", FormatUtils.formatRupiah(note.serviceFee))
            printLine("Sparepart", FormatUtils.formatRupiah(note.sparepartFee))
            if (note.discount > 0) {
                printLine("Diskon", "-${FormatUtils.formatRupiah(note.discount)}")
            }
            y += 2f
            canvas.drawLine(margin, y, right, y, paint)
            y += 10f

            printLine("TOTAL", FormatUtils.formatRupiah(note.totalCost), bold = true)
            printLine("Terbayar", FormatUtils.formatRupiah(totalPaid))
            val sisa = maxOf(0L, note.totalCost - totalPaid)
            printLine("SISA", if (sisa <= 0) "LUNAS" else FormatUtils.formatRupiah(sisa), bold = true)

            y += 6f
            canvas.drawLine(margin, y, right, y, paint)
            y += 12f

            paint.textAlign = Paint.Align.LEFT
            paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            canvas.drawText("STATUS : ${note.serviceStatus.uppercase()}", margin, y, paint)
            y += 10f
            canvas.drawText("GARANSI: ${note.warrantyPeriod}", margin, y, paint)

            y += 15f
            paint.textAlign = Paint.Align.CENTER
            paint.textSize = 6f
            paint.typeface = Typeface.MONOSPACE
            canvas.drawText("Segel rusak = Garansi batal", pageWidth / 2f, y, paint)
            y += 8f
            canvas.drawText("Harap bawa nota saat pengambilan", pageWidth / 2f, y, paint)
            y += 12f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 7f
            canvas.drawText("--- TERIMA KASIH ---", pageWidth / 2f, y, paint)
        }
    }
}
