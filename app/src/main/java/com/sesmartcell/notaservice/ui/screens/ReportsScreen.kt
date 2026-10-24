package com.sesmartcell.notaservice.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sesmartcell.notaservice.ui.ServiceViewModel
import com.sesmartcell.notaservice.ui.theme.PrimaryBlue
import com.sesmartcell.notaservice.ui.theme.StatusSuccess
import com.sesmartcell.notaservice.ui.theme.StatusWarning
import com.sesmartcell.notaservice.util.FormatUtils
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: ServiceViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val notes by viewModel.allNotes.collectAsStateWithLifecycle()
    val payments by viewModel.allPayments.collectAsStateWithLifecycle()

    var selectedPeriod by remember { mutableStateOf("Bulan Ini") }

    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    val startOfToday = cal.timeInMillis

    cal.add(Calendar.DAY_OF_YEAR, -7)
    val startOf7Days = cal.timeInMillis

    val calMonth = Calendar.getInstance()
    calMonth.set(Calendar.DAY_OF_MONTH, 1)
    calMonth.set(Calendar.HOUR_OF_DAY, 0)
    val startOfMonth = calMonth.timeInMillis

    val filteredPayments = remember(payments, selectedPeriod) {
        payments.filter { p ->
            p.isVerified && when (selectedPeriod) {
                "Hari Ini" -> p.paymentDate >= startOfToday
                "7 Hari" -> p.paymentDate >= startOf7Days
                "Bulan Ini" -> p.paymentDate >= startOfMonth
                else -> true
            }
        }
    }

    val filteredNotes = remember(notes, selectedPeriod) {
        notes.filter { n ->
            when (selectedPeriod) {
                "Hari Ini" -> n.entryDate >= startOfToday
                "7 Hari" -> n.entryDate >= startOf7Days
                "Bulan Ini" -> n.entryDate >= startOfMonth
                else -> true
            }
        }
    }

    val totalRevenue = filteredPayments.sumOf { it.amount }
    val totalJasa = filteredNotes.sumOf { it.serviceFee }
    val totalSparepart = filteredNotes.sumOf { it.sparepartFee }
    val totalDiskon = filteredNotes.sumOf { it.discount }
    val totalTagihan = filteredNotes.sumOf { it.totalCost }

    val paymentMap = payments.filter { it.isVerified }.groupBy { it.noteNumber }
    val totalPiutang = filteredNotes.sumOf { n ->
        val paid = paymentMap[n.noteNumber]?.sumOf { it.amount } ?: 0L
        maxOf(0L, n.totalCost - paid)
    }

    val completedCount = filteredNotes.count { it.serviceStatus == "Selesai" || it.serviceStatus == "Diambil Pelanggan" }
    val inProgressCount = filteredNotes.count { it.serviceStatus == "Sedang Dikerjakan" || it.serviceStatus == "Menunggu Sparepart" }
    val waitingCount = filteredNotes.count { it.serviceStatus == "Menunggu" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Laporan Keuangan & Service", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Hari Ini", "7 Hari", "Bulan Ini", "Semua").forEach { period ->
                    FilterChip(
                        selected = selectedPeriod == period,
                        onClick = { selectedPeriod = period },
                        label = { Text(period, fontSize = 12.sp) }
                    )
                }
            }

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = PrimaryBlue)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Total Uang Masuk ($selectedPeriod)", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = FormatUtils.formatRupiah(totalRevenue),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Total ${filteredPayments.size} transaksi pembayaran terverifikasi",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Rincian Pendapatan & Biaya", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Pendapatan Jasa", fontSize = 13.sp)
                        Text(FormatUtils.formatRupiah(totalJasa), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Biaya Sparepart", fontSize = 13.sp)
                        Text(FormatUtils.formatRupiah(totalSparepart), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                    if (totalDiskon > 0) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Diskon Diberikan", fontSize = 13.sp)
                            Text("- ${FormatUtils.formatRupiah(totalDiskon)}", color = StatusSuccess, fontSize = 13.sp)
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Nilai Total Semua Servis", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(FormatUtils.formatRupiah(totalTagihan), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PrimaryBlue)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Sisa Piutang Belum Dibayar", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(
                            text = FormatUtils.formatRupiah(totalPiutang),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (totalPiutang > 0) Color.Red else StatusSuccess
                        )
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Statistik Pengerjaan Servis", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    val totalCount = filteredNotes.size
                    val completionRate = if (totalCount > 0) completedCount.toFloat() / totalCount.toFloat() else 0f

                    Text("Tingkat Penyelesaian: ${(completionRate * 100).toInt()}% ($completedCount dari $totalCount unit)", fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { completionRate },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = StatusSuccess,
                        trackColor = Color(0xFFE2E8F0)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("✅ Selesai / Diambil", fontSize = 12.sp, color = StatusSuccess, fontWeight = FontWeight.Bold)
                            Text("$completedCount unit", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("⚙️ Sedang Dikerjakan", fontSize = 12.sp, color = StatusWarning, fontWeight = FontWeight.Bold)
                            Text("$inProgressCount unit", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("⏳ Menunggu Antrean", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            Text("$waitingCount unit", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Button(
                onClick = {
                    try {
                        val csvContent = viewModel.getCsvReport()
                        val fileName = "Laporan_SE_Smart_Cell_${selectedPeriod.replace(" ", "_")}.csv"

                        val cachePath = File(context.cacheDir, "exports")
                        cachePath.mkdirs()
                        val file = File(cachePath, fileName)
                        FileOutputStream(file).use { out ->
                            out.write(csvContent.toByteArray(Charsets.UTF_8))
                        }

                        val contentUri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )

                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/csv"
                            putExtra(Intent.EXTRA_STREAM, contentUri)
                            putExtra(Intent.EXTRA_SUBJECT, "Laporan Service SE SMART CELL")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Ekspor Laporan ke Excel / CSV"))
                    } catch (e: Exception) {
                        Toast.makeText(context, "Gagal mengekspor: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("export_csv_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF047857)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Download, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ekspor Laporan ke Excel / CSV", fontWeight = FontWeight.Bold)
            }
        }
    }
}
