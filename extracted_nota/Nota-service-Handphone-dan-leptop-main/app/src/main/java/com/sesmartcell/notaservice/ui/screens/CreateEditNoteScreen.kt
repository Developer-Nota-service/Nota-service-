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
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sesmartcell.notaservice.data.entity.ServiceNoteEntity
import com.sesmartcell.notaservice.ui.ServiceViewModel
import com.sesmartcell.notaservice.ui.theme.PrimaryBlue
import com.sesmartcell.notaservice.ui.theme.StatusSuccess
import com.sesmartcell.notaservice.util.FormatUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateEditNoteScreen(
    viewModel: ServiceViewModel,
    existingNoteId: Long? = null,
    onNavigateBack: () -> Unit,
    onNoteSaved: (Long) -> Unit
) {
    val context = LocalContext.current

    var noteNumber by remember { mutableStateOf("") }
    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    var deviceType by remember { mutableStateOf("HP") }
    var brand by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var imeiOrSerial by remember { mutableStateOf("") }
    val selectedAccessories = remember { mutableStateListOf<String>() }

    var issueDescription by remember { mutableStateOf("") }
    var checkResult by remember { mutableStateOf("") }
    var repairAction by remember { mutableStateOf("") }

    var serviceFeeText by remember { mutableStateOf("") }
    var sparepartFeeText by remember { mutableStateOf("") }
    var discountText by remember { mutableStateOf("") }
    var downPaymentText by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("Tunai") }
    var paymentRef by remember { mutableStateOf("") }

    var estimatedDate by remember { mutableStateOf("") }
    var technicianName by remember { mutableStateOf("Rian (SE SMART)") }
    var serviceStatus by remember { mutableStateOf("Menunggu") }
    var warrantyPeriod by remember { mutableStateOf("30 Hari") }
    var warrantyEndDate by remember { mutableStateOf("") }
    var notesAdditional by remember { mutableStateOf("") }

    var isEditMode by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var successDialogMessage by remember { mutableStateOf("") }
    var savedNoteIdToNavigate by remember { mutableStateOf(0L) }

    LaunchedEffect(existingNoteId) {
        if (existingNoteId != null && existingNoteId > 0) {
            val note = viewModel.allNotes.value.find { it.id == existingNoteId }
            if (note != null) {
                isEditMode = true
                noteNumber = note.noteNumber
                customerName = note.customerName
                customerPhone = note.customerPhone
                deviceType = note.deviceType
                brand = note.brand
                model = note.model
                imeiOrSerial = note.imeiOrSerial
                if (note.accessories.isNotBlank()) {
                    selectedAccessories.clear()
                    selectedAccessories.addAll(note.accessories.split(", ").filter { it.isNotBlank() })
                }
                issueDescription = note.issueDescription
                checkResult = note.technicianCheckResult
                repairAction = note.repairAction
                serviceFeeText = if (note.serviceFee > 0) note.serviceFee.toString() else ""
                sparepartFeeText = if (note.sparepartFee > 0) note.sparepartFee.toString() else ""
                discountText = if (note.discount > 0) note.discount.toString() else ""
                downPaymentText = if (note.downPayment > 0) note.downPayment.toString() else ""
                paymentMethod = note.initialPaymentMethod
                estimatedDate = note.estimatedCompletionDate
                technicianName = note.technicianName
                serviceStatus = note.serviceStatus
                warrantyPeriod = note.warrantyPeriod
                warrantyEndDate = note.warrantyEndDate
                notesAdditional = note.notes
            }
        } else {
            isEditMode = false
            noteNumber = viewModel.getNextNoteNumber()
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, 2)
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
            estimatedDate = sdf.format(cal.time)
        }
    }

    val serviceFee = serviceFeeText.toLongOrNull() ?: 0L
    val sparepartFee = sparepartFeeText.toLongOrNull() ?: 0L
    val discount = discountText.toLongOrNull() ?: 0L
    val totalCost = maxOf(0L, serviceFee + sparepartFee - discount)
    val downPayment = downPaymentText.toLongOrNull() ?: 0L
    val sisa = maxOf(0L, totalCost - downPayment)

    val commonBrands = if (deviceType == "HP") {
        listOf("iPhone", "Samsung", "Oppo", "Vivo", "Xiaomi", "Realme", "Infinix", "Google Pixel", "Asus ROG")
    } else {
        listOf("Asus", "Lenovo", "Acer", "HP", "Apple MacBook", "Dell", "MSI", "Axioo", "Toshiba")
    }

    val accessoryOptions = listOf("Unit Saja", "Charger Original", "Kabel Data", "Dus / Box", "SIM Tray", "Casing / Softcase", "Tas Laptop")

    val issuePresets = if (deviceType == "HP") {
        listOf("Layar LCD Pecah / Blank", "Baterai Kembung / Drop", "Mati Total / Short", "Konslet Masuk Air", "Gagal Bootloop / Logo", "Tidak Bisa Di-charge", "Touchscreen Loncat")
    } else {
        listOf("Mati Total / No Display", "Keyboard Eror / Ngetik Sendiri", "Cepat Panas / Overheating", "Install Ulang Windows", "Baterai Not Charging", "Engsel Patah / Rusak", "Layar Bergaris")
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditMode) "Edit Nota Service" else "Form Buat Nota Service",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "1. IDENTITAS NOTA",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = noteNumber,
                            onValueChange = { noteNumber = it },
                            label = { Text("Nomor Nota") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("note_number_input")
                        )
                        OutlinedTextField(
                            value = "Otomatis Hari Ini",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Tanggal Masuk") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("2. DATA PELANGGAN", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = customerName,
                        onValueChange = { customerName = it },
                        label = { Text("Nama Pelanggan *") },
                        placeholder = { Text("Cth: Budi Pratama") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("customer_name_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = customerPhone,
                        onValueChange = { customerPhone = it },
                        label = { Text("Nomor WhatsApp Pelanggan *") },
                        placeholder = { Text("08123456789 atau 628...") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("customer_phone_input")
                    )
                }
            }

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("3. INFORMASI PERANGKAT", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Jenis Perangkat:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("HP" to Icons.Default.PhoneAndroid, "Laptop" to Icons.Default.Laptop).forEach { (type, icon) ->
                            FilterChip(
                                selected = deviceType == type,
                                onClick = { deviceType = type },
                                leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                label = { Text(type, fontWeight = FontWeight.SemiBold) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Pilih / Ketik Merek:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        commonBrands.forEach { b ->
                            FilterChip(
                                selected = brand.equals(b, ignoreCase = true),
                                onClick = { brand = b },
                                label = { Text(b, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = brand,
                            onValueChange = { brand = it },
                            label = { Text("Merek Perangkat *") },
                            placeholder = { Text("cth: Samsung") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("brand_input")
                        )

                        OutlinedTextField(
                            value = model,
                            onValueChange = { model = it },
                            label = { Text("Model / Tipe *") },
                            placeholder = { Text("cth: Galaxy A54") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1.3f)
                                .testTag("model_input")
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = imeiOrSerial,
                        onValueChange = { imeiOrSerial = it },
                        label = { Text("No. IMEI / Serial Number (Opsional)") },
                        placeholder = { Text("15 digit IMEI atau SN di belakang unit") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("imei_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Kelengkapan Diterima:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        accessoryOptions.forEach { acc ->
                            val isSelected = selectedAccessories.contains(acc)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (isSelected) selectedAccessories.remove(acc)
                                    else selectedAccessories.add(acc)
                                },
                                label = { Text(acc, fontSize = 11.sp) }
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Build, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("4. KERUSAKAN & TINDAKAN", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Preset Keluhan Populer:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        issuePresets.forEach { p ->
                            FilterChip(
                                selected = issueDescription.contains(p),
                                onClick = {
                                    issueDescription = if (issueDescription.isBlank()) p else "$issueDescription, $p"
                                },
                                label = { Text(p, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = issueDescription,
                        onValueChange = { issueDescription = it },
                        label = { Text("Keluhan Pelanggan / Kerusakan *") },
                        placeholder = { Text("Deskripsikan kondisi kerusakan perangkat...") },
                        minLines = 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("issue_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = checkResult,
                        onValueChange = { checkResult = it },
                        label = { Text("Hasil Pengecekan Teknisi") },
                        placeholder = { Text("Hasil tes fisik, arus tegangan, LCD, atau mesin...") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = repairAction,
                        onValueChange = { repairAction = it },
                        label = { Text("Tindakan Perbaikan / Solusi") },
                        placeholder = { Text("Penggantian sparepart, reball, jumper, flash, dll...") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("5. PERHITUNGAN BIAYA & PEMBAYARAN", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = serviceFeeText,
                            onValueChange = { serviceFeeText = it.filter { c -> c.isDigit() } },
                            label = { Text("Biaya Jasa (Rp)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("service_fee_input")
                        )

                        OutlinedTextField(
                            value = sparepartFeeText,
                            onValueChange = { sparepartFeeText = it.filter { c -> c.isDigit() } },
                            label = { Text("Biaya Sparepart (Rp)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("sparepart_fee_input")
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = discountText,
                            onValueChange = { discountText = it.filter { c -> c.isDigit() } },
                            label = { Text("Diskon (Rp)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = downPaymentText,
                            onValueChange = { downPaymentText = it.filter { c -> c.isDigit() } },
                            label = { Text("Uang Muka / DP (Rp)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("down_payment_input")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total Biaya Service:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color.Black)
                                Text(FormatUtils.formatRupiah(totalCost), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PrimaryBlue)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Sisa Pembayaran:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color.Black)
                                Text(
                                    if (sisa <= 0) "LUNAS (Rp 0)" else FormatUtils.formatRupiah(sisa),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (sisa <= 0) StatusSuccess else Color(0xFFDC2626)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Metode Pembayaran Awal:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Tunai", "QRIS", "Transfer Bank", "E-Wallet").forEach { method ->
                            FilterChip(
                                selected = paymentMethod == method,
                                onClick = { paymentMethod = method },
                                label = { Text(method, fontSize = 11.sp) }
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
                    Text("6. STATUS & JAMINAN GARANSI", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Status Pengerjaan:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(4.dp))
                    val statusOptions = listOf("Menunggu", "Sedang Dikerjakan", "Menunggu Sparepart", "Selesai", "Diambil Pelanggan")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        statusOptions.forEach { st ->
                            FilterChip(
                                selected = serviceStatus == st,
                                onClick = { serviceStatus = st },
                                label = { Text(st, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = estimatedDate,
                            onValueChange = { estimatedDate = it },
                            label = { Text("Estimasi Selesai") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = technicianName,
                            onValueChange = { technicianName = it },
                            label = { Text("Nama Teknisi") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Masa Garansi Servis:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Tidak Ada", "7 Hari", "14 Hari", "30 Hari", "90 Hari").forEach { g ->
                            FilterChip(
                                selected = warrantyPeriod == g,
                                onClick = { warrantyPeriod = g },
                                label = { Text(g, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = notesAdditional,
                        onValueChange = { notesAdditional = it },
                        label = { Text("Catatan Tambahan") },
                        placeholder = { Text("Cth: Sandi/pola layar kunci, kondisi lecet awal...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Button(
                onClick = {
                    if (customerName.isBlank()) {
                        Toast.makeText(context, "Mohon isi Nama Pelanggan", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (customerPhone.isBlank()) {
                        Toast.makeText(context, "Mohon isi Nomor WhatsApp Pelanggan", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (brand.isBlank() || model.isBlank()) {
                        Toast.makeText(context, "Mohon lengkapi Merek dan Model perangkat", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (issueDescription.isBlank()) {
                        Toast.makeText(context, "Mohon isi keluhan / kerusakan perangkat", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val finalAccessories = selectedAccessories.joinToString(", ")
                    val noteEntity = ServiceNoteEntity(
                        id = if (isEditMode) existingNoteId ?: 0L else 0L,
                        noteNumber = noteNumber.ifBlank { "SC-${System.currentTimeMillis() % 100000}" },
                        customerName = customerName.trim(),
                        customerPhone = customerPhone.trim(),
                        deviceType = deviceType,
                        brand = brand.trim(),
                        model = model.trim(),
                        imeiOrSerial = imeiOrSerial.trim(),
                        accessories = finalAccessories,
                        issueDescription = issueDescription.trim(),
                        technicianCheckResult = checkResult.trim(),
                        repairAction = repairAction.trim(),
                        serviceFee = serviceFee,
                        sparepartFee = sparepartFee,
                        discount = discount,
                        downPayment = downPayment,
                        estimatedCompletionDate = estimatedDate.trim(),
                        technicianName = technicianName.trim(),
                        serviceStatus = serviceStatus,
                        warrantyPeriod = warrantyPeriod,
                        warrantyEndDate = warrantyEndDate,
                        initialPaymentMethod = paymentMethod,
                        notes = notesAdditional.trim()
                    )

                    if (isEditMode) {
                        viewModel.updateNote(noteEntity) {
                            successDialogMessage = "Nota berhasil diperbarui!"
                            savedNoteIdToNavigate = noteEntity.id
                            showSuccessDialog = true
                        }
                    } else {
                        viewModel.saveNewNote(
                            note = noteEntity,
                            initialPayment = downPayment,
                            paymentMethod = paymentMethod,
                            refNumber = paymentRef
                        ) { newId ->
                            successDialogMessage = "Nota ${noteEntity.noteNumber} berhasil dibuat & disimpan!"
                            savedNoteIdToNavigate = newId
                            showSuccessDialog = true
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_note_submit_button"),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isEditMode) "Perbarui Nota Service" else "Simpan Nota Service",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (showSuccessDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { },
                title = { Text("Berhasil Disimpan!", fontWeight = FontWeight.Bold) },
                text = { Text(successDialogMessage) },
                confirmButton = {
                    Button(
                        onClick = {
                            showSuccessDialog = false
                            onNoteSaved(savedNoteIdToNavigate)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Text("OK")
                    }
                }
            )
        }
    }
}
