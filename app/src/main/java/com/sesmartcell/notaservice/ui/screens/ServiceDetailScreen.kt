package com.sesmartcell.notaservice.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sesmartcell.notaservice.ui.ServiceViewModel
import com.sesmartcell.notaservice.ui.components.AddPaymentDialog
import com.sesmartcell.notaservice.ui.components.DeviceTypeBadge
import com.sesmartcell.notaservice.ui.components.ImageReceiptDialog
import com.sesmartcell.notaservice.ui.components.PaymentStatusBadge
import com.sesmartcell.notaservice.ui.components.PrintReceiptDialog
import com.sesmartcell.notaservice.ui.components.StatusBadge
import com.sesmartcell.notaservice.ui.components.pressClickScale
import com.sesmartcell.notaservice.ui.theme.PrimaryBlue
import com.sesmartcell.notaservice.ui.theme.StatusError
import com.sesmartcell.notaservice.ui.theme.StatusSuccess
import com.sesmartcell.notaservice.util.FormatUtils
import com.sesmartcell.notaservice.util.WhatsAppHelper
import com.sesmartcell.notaservice.util.PrintReceiptHelper
import com.sesmartcell.notaservice.util.ReceiptBitmapHelper

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ServiceDetailScreen(
    noteId: Long,
    viewModel: ServiceViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit
) {
    val context = LocalContext.current
    val notes by viewModel.allNotes.collectAsStateWithLifecycle()
    val payments by viewModel.allPayments.collectAsStateWithLifecycle()
    val storeSettings by viewModel.storeSettings.collectAsStateWithLifecycle()

    val note = notes.find { it.id == noteId }

    var showPrintDialog by remember { mutableStateOf(false) }
    var showImageDialog by remember { mutableStateOf(false) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    if (note == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Data nota tidak ditemukan atau telah dihapus")
        }
        return
    }

    val notePayments = remember(payments, note.noteNumber) {
        payments.filter { it.noteNumber == note.noteNumber && it.isVerified }
    }
    val totalPaid = remember(notePayments) {
        notePayments.sumOf { it.amount }
    }
    val sisa = maxOf(0L, note.totalCost - totalPaid)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(note.noteNumber, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigateToEdit(note.id) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { showDeleteConfirmDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color.Red)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Status Pengerjaan Saat Ini:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        StatusBadge(status = note.serviceStatus)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Ubah Status Cepat:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    val statusList = listOf("Menunggu", "Sedang Dikerjakan", "Menunggu Sparepart", "Selesai", "Diambil Pelanggan")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        statusList.forEach { st ->
                            FilterChip(
                                selected = note.serviceStatus == st,
                                onClick = {
                                    if (note.serviceStatus != st) {
                                        viewModel.updateStatus(note.noteNumber, st)
                                        Toast.makeText(context, "Status diubah menjadi: $st", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                label = { Text(st, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(note.customerName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(
                                "WhatsApp: ${note.customerPhone}",
                                fontSize = 13.sp,
                                color = PrimaryBlue,
                                modifier = Modifier.clickable {
                                    WhatsAppHelper.openWhatsApp(context, note.customerPhone, "Halo Kak ${note.customerName}, kami dari ${storeSettings.storeName}...")
                                }
                            )
                        }
                        DeviceTypeBadge(deviceType = note.deviceType)
                    }

                    Divider(modifier = Modifier.padding(vertical = 10.dp))

                    Text("Perangkat : ${note.brand} ${note.model}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    if (note.imeiOrSerial.isNotBlank()) {
                        Text("IMEI / SN : ${note.imeiOrSerial}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("Kelengkapan: ${note.accessories.ifBlank { "Unit Saja" }}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Masuk Pada : ${FormatUtils.formatDate(note.entryDate)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (note.estimatedCompletionDate.isNotBlank()) {
                        Text("Estimasi   : ${note.estimatedCompletionDate}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("Teknisi    : ${note.technicianName}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Keluhan / Kerusakan:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                    Text(note.issueDescription, fontSize = 13.sp)

                    if (note.technicianCheckResult.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Hasil Pengecekan Teknisi:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                        Text(note.technicianCheckResult, fontSize = 13.sp)
                    }

                    if (note.repairAction.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Tindakan Perbaikan:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StatusSuccess)
                        Text(note.repairAction, fontSize = 13.sp)
                    }

                    if (note.notes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Catatan Internal:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(note.notes, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Masa Garansi: ${note.warrantyPeriod} ${if (note.warrantyEndDate.isNotBlank()) "(s/d ${note.warrantyEndDate})" else ""}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF047857)
                    )
                }
            }

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Rincian Biaya & Pembayaran", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        PaymentStatusBadge(totalCost = note.totalCost, totalPaid = totalPaid)
                    }

                    // Indikator status pembayaran yang jelas + progres pembayaran.
                    val isLunasNow = sisa <= 0
                    val isPartialPaid = !isLunasNow && totalPaid > 0
                    val (bannerBg, bannerFg) = when {
                        isLunasNow -> Color(0xFFD1FAE5) to Color(0xFF047857)
                        isPartialPaid -> Color(0xFFFEF3C7) to Color(0xFFB45309)
                        else -> Color(0xFFFEE2E2) to Color(0xFFB91C1C)
                    }
                    val (bannerLabel, bannerDesc) = when {
                        isLunasNow -> "LUNAS" to "Pembayaran nota ini sudah lengkap. Terima kasih!"
                        isPartialPaid -> "DP / SEBAGIAN" to
                            "Terbayar ${FormatUtils.formatRupiah(totalPaid)} dari ${FormatUtils.formatRupiah(note.totalCost)}"
                        else -> "BELUM BAYAR" to "Belum ada pembayaran masuk untuk nota ini"
                    }
                    val bannerIcon = when {
                        isLunasNow -> Icons.Default.CheckCircle
                        isPartialPaid -> Icons.Default.Payment
                        else -> Icons.Default.Warning
                    }
                    val paidFraction = if (note.totalCost > 0) {
                        (totalPaid.toFloat() / note.totalCost).coerceIn(0f, 1f)
                    } else {
                        0f
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(bannerBg)
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(bannerIcon, contentDescription = null, tint = bannerFg, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(bannerLabel, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = bannerFg)
                                Text(bannerDesc, fontSize = 11.sp, color = bannerFg)
                            }
                        }
                        if (!isLunasNow) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(bannerFg.copy(alpha = 0.2f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(paidFraction)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(bannerFg)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Terbayar ${(paidFraction * 100).toInt()}% • Sisa tagihan ${FormatUtils.formatRupiah(sisa)}",
                                fontSize = 11.sp,
                                color = bannerFg,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Biaya Jasa", fontSize = 13.sp)
                        Text(FormatUtils.formatRupiah(note.serviceFee), fontSize = 13.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Biaya Sparepart", fontSize = 13.sp)
                        Text(FormatUtils.formatRupiah(note.sparepartFee), fontSize = 13.sp)
                    }
                    if (note.discount > 0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Diskon", fontSize = 13.sp)
                            Text("- ${FormatUtils.formatRupiah(note.discount)}", fontSize = 13.sp, color = StatusSuccess)
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Biaya", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(FormatUtils.formatRupiah(note.totalCost), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PrimaryBlue)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Terbayar", fontSize = 13.sp)
                        Text(FormatUtils.formatRupiah(totalPaid), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Sisa Tagihan", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            text = if (sisa <= 0) "LUNAS (Rp 0)" else FormatUtils.formatRupiah(sisa),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (sisa <= 0) StatusSuccess else Color(0xFFDC2626)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (sisa > 0) {
                        Button(
                            onClick = {
                                viewModel.addPaymentRecord(
                                    noteNumber = note.noteNumber,
                                    amount = sisa,
                                    method = "Tunai",
                                    refNumber = "Lunas Cepat",
                                    type = "Pelunasan"
                                ) {
                                    Toast.makeText(
                                        context,
                                        "Nota Lunas! Status pengerjaan otomatis: Diambil Pelanggan",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                                // Saat nota ditandai lunas, status pengerjaan
                                // langsung diperbarui menjadi "Diambil Pelanggan"
                                // (juga tersinkron ke cloud).
                                if (note.serviceStatus != "Diambil Pelanggan") {
                                    viewModel.updateStatus(note.noteNumber, "Diambil Pelanggan")
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .pressClickScale()
                                .testTag("quick_lunas_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StatusSuccess,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Tandai Nota Ini Lunas", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    "Sisa ${FormatUtils.formatRupiah(sisa)} • Status otomatis: Diambil Pelanggan",
                                    fontSize = 10.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Button(
                        onClick = { showPaymentDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .pressClickScale()
                            .testTag("add_payment_dialog_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Tambah / Lunasi Pembayaran", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    if (notePayments.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Riwayat Pembayaran Masuk:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        notePayments.forEach { p ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .background(Color(0xFFF8FAFC), RoundedCornerShape(6.dp))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("${p.paymentType} • ${p.paymentMethod}", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                    Text(
                                        "${FormatUtils.formatDate(p.paymentDate, "dd/MM/yyyy HH:mm")}${if (p.referenceNumber.isNotBlank()) " (Ref: ${p.referenceNumber})" else ""}",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    FormatUtils.formatRupiah(p.amount),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF047857)
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { showPrintDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .pressClickScale()
                        .testTag("print_receipt_action_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Cetak Nota", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { showImageDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .pressClickScale()
                        .testTag("image_receipt_action_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Gambar Nota", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "INTEGRASI WHATSAPP PELANGGAN",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF25D366)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            val msg = WhatsAppHelper.createStatusMessage(note, storeSettings, totalPaid, sisa)
                            WhatsAppHelper.openWhatsApp(context, note.customerPhone, msg)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF128C7E)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .pressClickScale()
                            .testTag("wa_status_button")
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("1. Kirim Update Status Pengerjaan", fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            val msg = WhatsAppHelper.createPaymentInstruction(note, storeSettings, sisa)
                            WhatsAppHelper.openWhatsApp(context, note.customerPhone, msg)
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .pressClickScale()
                            .testTag("wa_payment_instruction_button")
                    ) {
                        Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("2. Kirim Instruksi Pembayaran (Rekening/QRIS)", fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            val msg = WhatsAppHelper.createCompletionMessage(note, storeSettings, totalPaid)
                            WhatsAppHelper.openWhatsApp(context, note.customerPhone, msg)
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .pressClickScale()
                            .testTag("wa_completion_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("3. Kirim Bukti Pelunasan & Garansi", fontSize = 12.sp)
                    }
                }
            }
        }
    }

    if (showPrintDialog) {
        PrintReceiptDialog(
            note = note,
            store = storeSettings,
            totalPaid = totalPaid,
            onDismiss = { showPrintDialog = false }
        )
    }

    if (showImageDialog) {
        ImageReceiptDialog(
            note = note,
            store = storeSettings,
            totalPaid = totalPaid,
            onDismiss = { showImageDialog = false }
        )
    }

    var showPaymentSuccessDialog by remember { mutableStateOf(false) }
    var savedPaymentAmount by remember { mutableStateOf(0L) }

    if (showPaymentDialog) {
        AddPaymentDialog(
            note = note,
            currentTotalPaid = totalPaid,
            onDismiss = { showPaymentDialog = false },
            onConfirm = { amount, method, ref, type ->
                savedPaymentAmount = amount
                viewModel.addPaymentRecord(
                    noteNumber = note.noteNumber,
                    amount = amount,
                    method = method,
                    refNumber = ref,
                    type = type
                ) {
                    showPaymentDialog = false
                    showPaymentSuccessDialog = true
                }
            }
        )
    }

    if (showPaymentSuccessDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Berhasil Disimpan!", fontWeight = FontWeight.Bold) },
            text = { Text("Pembayaran sebesar ${FormatUtils.formatRupiah(savedPaymentAmount)} berhasil disimpan dan diverifikasi.") },
            confirmButton = {
                Button(
                    onClick = { showPaymentSuccessDialog = false },
                    modifier = Modifier.pressClickScale(),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("OK")
                }
            }
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Konfirmasi Hapus Nota") },
            text = { Text("Apakah Anda yakin ingin menghapus nota ${note.noteNumber} (${note.customerName})? Data yang dihapus tidak dapat dipulihkan.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteNote(note) {
                            Toast.makeText(context, "Nota berhasil dihapus", Toast.LENGTH_SHORT).show()
                            showDeleteConfirmDialog = false
                            onNavigateBack()
                        }
                    },
                    modifier = Modifier.pressClickScale(),
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                ) {
                    Text("Hapus Permanen")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}
