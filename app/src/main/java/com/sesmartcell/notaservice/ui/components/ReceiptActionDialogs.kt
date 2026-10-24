package com.sesmartcell.notaservice.ui.components

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sesmartcell.notaservice.data.entity.ServiceNoteEntity
import com.sesmartcell.notaservice.data.entity.StoreSettingsEntity
import com.sesmartcell.notaservice.ui.theme.PrimaryBlue
import com.sesmartcell.notaservice.ui.theme.StatusSuccess
import com.sesmartcell.notaservice.util.FormatUtils
import com.sesmartcell.notaservice.util.PrintReceiptHelper
import com.sesmartcell.notaservice.util.ReceiptBitmapHelper
import com.sesmartcell.notaservice.util.ReceiptType

@Composable
fun PrintReceiptDialog(
    note: ServiceNoteEntity,
    store: StoreSettingsEntity,
    totalPaid: Long,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedFormat by remember { mutableStateOf("80mm") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Print, contentDescription = null, tint = PrimaryBlue)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cetak Nota Service", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Pilih Format Cetak / Simpan Nota:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFormat == "80mm",
                        onClick = { selectedFormat = "80mm" },
                        label = { Text("Thermal 80mm") }
                    )
                    FilterChip(
                        selected = selectedFormat == "58mm",
                        onClick = { selectedFormat = "58mm" },
                        label = { Text("Thermal 58mm") }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = store.storeName,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = Color.Black
                        )
                        Text(
                            text = "NO: ${note.noteNumber}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color.DarkGray
                        )
                        Text(
                            text = "PLG: ${note.customerName} (${note.customerPhone})",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color.DarkGray
                        )
                        Text(
                            text = "UNIT: ${note.brand} ${note.model}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color.DarkGray
                        )
                        Text(
                            text = "KERUSAKAN: ${note.issueDescription.take(35)}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color.DarkGray
                        )
                        Divider(modifier = Modifier.padding(vertical = 4.dp))
                        Text(
                            text = "TOTAL: ${FormatUtils.formatRupiah(note.totalCost)}",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color.Black
                        )
                        Text(
                            text = "TERBAYAR: ${FormatUtils.formatRupiah(totalPaid)}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color.DarkGray
                        )
                        val sisa = maxOf(0L, note.totalCost - totalPaid)
                        Text(
                            text = "SISA: ${if (sisa <= 0) "LUNAS" else FormatUtils.formatRupiah(sisa)}",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (sisa <= 0) Color(0xFF047857) else Color(0xFFB91C1C)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "💡 Kompatibel dengan semua printer thermal Bluetooth/USB serta opsi 'Simpan sebagai PDF' bawaan Android.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    PrintReceiptHelper.printThermalReceipt(
                        context = context,
                        note = note,
                        store = store,
                        totalPaid = totalPaid,
                        is80mm = selectedFormat == "80mm"
                    )
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                modifier = Modifier.testTag("confirm_print_button")
            ) {
                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Cetak / Simpan PDF")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    )
}

@Composable
fun ImageReceiptDialog(
    note: ServiceNoteEntity,
    store: StoreSettingsEntity,
    totalPaid: Long,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedType by remember {
        mutableStateOf(
            if (totalPaid >= note.totalCost && note.totalCost > 0) ReceiptType.PELUNASAN else ReceiptType.TAGIHAN
        )
    }

    val receiptBitmap: Bitmap = remember(selectedType) {
        ReceiptBitmapHelper.generateReceiptBitmap(context, note, store, totalPaid, selectedType)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Gambar Nota SE SMART CELL", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Pilih Tipe Nota:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ReceiptType.entries.forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = receiptBitmap.asImageBitmap(),
                        contentDescription = "Nota Gambar",
                        modifier = Modifier.padding(4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val uri = ReceiptBitmapHelper.saveBitmapToDevice(
                                context,
                                receiptBitmap,
                                "Nota_${note.noteNumber}_${selectedType.name}"
                            )
                            if (uri != null) {
                                Toast.makeText(context, "Gambar nota berhasil disimpan di Galeri / Pictures!", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Gagal menyimpan gambar", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("download_receipt_button")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Simpan Galeri", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            ReceiptBitmapHelper.shareReceiptImage(
                                context = context,
                                note = note,
                                store = store,
                                totalPaid = totalPaid,
                                type = selectedType
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_whatsapp_image_button")
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Kirim ke WA", color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    )
}

@Composable
fun AddPaymentDialog(
    note: ServiceNoteEntity,
    currentTotalPaid: Long,
    onDismiss: () -> Unit,
    onConfirm: (amount: Long, method: String, ref: String, type: String) -> Unit
) {
    val sisa = maxOf(0L, note.totalCost - currentTotalPaid)
    var amountText by remember { mutableStateOf(sisa.toString()) }
    var selectedMethod by remember { mutableStateOf("Tunai") }
    var refNumber by remember { mutableStateOf("") }
    var paymentType by remember {
        mutableStateOf(if (sisa > 0 && currentTotalPaid == 0L) "DP" else "Pelunasan")
    }

    val methods = listOf("Tunai", "QRIS", "Transfer Bank", "E-Wallet")
    val types = listOf("DP", "Cicilan", "Pelunasan")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Payment, contentDescription = null, tint = PrimaryBlue)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Catat Pembayaran", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Nota: ${note.noteNumber} - ${note.customerName}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text(
                    "Sisa Tagihan: ${FormatUtils.formatRupiah(sisa)}",
                    color = if (sisa <= 0) StatusSuccess else Color.Red,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { char -> char.isDigit() } },
                    label = { Text("Jumlah Pembayaran (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("payment_amount_input")
                )

                Spacer(modifier = Modifier.height(10.dp))
                Text("Jenis Pembayaran:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    types.forEach { t ->
                        FilterChip(
                            selected = paymentType == t,
                            onClick = { paymentType = t },
                            label = { Text(t, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text("Metode Pembayaran:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    methods.take(2).forEach { m ->
                        FilterChip(
                            selected = selectedMethod == m,
                            onClick = { selectedMethod = m },
                            label = { Text(m, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    methods.drop(2).forEach { m ->
                        FilterChip(
                            selected = selectedMethod == m,
                            onClick = { selectedMethod = m },
                            label = { Text(m, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = refNumber,
                    onValueChange = { refNumber = it },
                    label = { Text("No. Referensi / Bukti Transfer (Opsional)") },
                    placeholder = { Text("cth: TRX-98213 atau nama pengirim") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("payment_ref_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toLongOrNull() ?: 0L
                    if (amount > 0) {
                        onConfirm(amount, selectedMethod, refNumber, paymentType)
                    }
                },
                enabled = (amountText.toLongOrNull() ?: 0L) > 0,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                modifier = Modifier.testTag("save_payment_button")
            ) {
                Text("Simpan Pembayaran")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
