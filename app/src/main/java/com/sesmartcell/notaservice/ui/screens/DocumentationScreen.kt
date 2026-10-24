package com.sesmartcell.notaservice.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentationScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    val sqlSchema = """
-- ========================================================
-- DATABASE SCHEMA: SE SMART CELL - NOTA SERVICE HP & LAPTOP
-- Engine: MySQL 8.0+ / MariaDB 10.4+
-- ========================================================

CREATE DATABASE IF NOT EXISTS `se_smart_cell` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `se_smart_cell`;

-- 1. Tabel Pengguna / Admin
CREATE TABLE IF NOT EXISTS `users` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `username` VARCHAR(50) NOT NULL UNIQUE,
  `password` VARCHAR(255) NOT NULL,
  `nama_lengkap` VARCHAR(100) NOT NULL,
  `role` ENUM('admin', 'teknisi') DEFAULT 'admin',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- 2. Tabel Nota Service
CREATE TABLE IF NOT EXISTS `service_notes` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `nomor_nota` VARCHAR(30) NOT NULL UNIQUE,
  `tanggal_masuk` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `nama_pelanggan` VARCHAR(100) NOT NULL,
  `no_whatsapp` VARCHAR(20) NOT NULL,
  `jenis_perangkat` ENUM('HP', 'Laptop', 'Lainnya') NOT NULL,
  `merek` VARCHAR(50) NOT NULL,
  `model` VARCHAR(100) NOT NULL,
  `imei_sn` VARCHAR(50) NULL,
  `kelengkapan` VARCHAR(255) NULL,
  `keluhan` TEXT NOT NULL,
  `hasil_pengecekan` TEXT NULL,
  `tindakan_perbaikan` TEXT NULL,
  `biaya_jasa` DECIMAL(12,2) DEFAULT 0.00,
  `biaya_sparepart` DECIMAL(12,2) DEFAULT 0.00,
  `diskon` DECIMAL(12,2) DEFAULT 0.00,
  `total_biaya` DECIMAL(12,2) GENERATED ALWAYS AS (biaya_jasa + biaya_sparepart - diskon) STORED,
  `uang_muka` DECIMAL(12,2) DEFAULT 0.00,
  `estimasi_selesai` VARCHAR(50) NULL,
  `nama_teknisi` VARCHAR(100) DEFAULT 'Teknisi SE SMART',
  `status_service` ENUM('Menunggu', 'Sedang Dikerjakan', 'Menunggu Sparepart', 'Selesai', 'Diambil Pelanggan', 'Dibatalkan') DEFAULT 'Menunggu',
  `garansi` VARCHAR(50) DEFAULT '30 Hari',
  `garansi_berakhir` DATE NULL,
  `metode_pembayaran_awal` VARCHAR(50) DEFAULT 'Tunai',
  `catatan` TEXT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX `idx_pelanggan` (`nama_pelanggan`),
  INDEX `idx_status` (`status_service`),
  INDEX `idx_whatsapp` (`no_whatsapp`)
) ENGINE=InnoDB;

-- 3. Tabel Riwayat Pembayaran (Mendukung Pembayaran Bertahap)
CREATE TABLE IF NOT EXISTS `payment_records` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `nomor_nota` VARCHAR(30) NOT NULL,
  `jumlah` DECIMAL(12,2) NOT NULL,
  `metode` ENUM('Tunai', 'QRIS', 'Transfer Bank', 'E-Wallet') NOT NULL,
  `no_referensi` VARCHAR(100) NULL,
  `tanggal_bayar` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `jenis_bayar` ENUM('DP', 'Cicilan', 'Pelunasan') DEFAULT 'DP',
  `is_verified` TINYINT(1) DEFAULT 1,
  `keterangan` VARCHAR(255) NULL,
  FOREIGN KEY (`nomor_nota`) REFERENCES `service_notes`(`nomor_nota`) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 4. Tabel Pengaturan Toko & Rekening
CREATE TABLE IF NOT EXISTS `store_settings` (
  `id` INT PRIMARY KEY DEFAULT 1,
  `nama_toko` VARCHAR(100) DEFAULT 'SE SMART CELL',
  `slogan` VARCHAR(255) DEFAULT 'Service Smartphone & Laptop Profesional',
  `alamat` TEXT,
  `no_whatsapp` VARCHAR(20) DEFAULT '081234567890',
  `qris_merchant` VARCHAR(100) DEFAULT 'SE SMART CELL INDONESIA',
  `qris_nmid` VARCHAR(50) DEFAULT 'ID102030405060',
  `rekening_bank` TEXT,
  `ewallet` TEXT,
  `syarat_garansi` TEXT
) ENGINE=InnoDB;
    """.trimIndent()

    val apiDocs = """
REST API ENDPOINTS REFERENSI:
------------------------------------------
1. GET /api/notes.php
   Params: ?search=...&status=...&device=...
   Returns: List[ServiceNote]

2. POST /api/notes.php (Create Note)
   Body: JSON { customerName, phone, deviceType, brand, model, issue, serviceFee, sparepartFee, downPayment, method }
   Returns: { status: "success", noteNumber: "SC-20261020-001" }

3. PUT /api/notes.php?action=status
   Body: JSON { noteNumber, status }
   Returns: { status: "updated" }

4. POST /api/payments.php
   Body: JSON { noteNumber, amount, method, refNumber, type }
   Returns: { status: "payment_recorded", balanceRemaining }

5. GET /api/reports.php
   Params: ?period=month
   Returns: { totalRevenue, totalJasa, totalSparepart, totalPiutang, completedCount }
    """.trimIndent()

    fun copyToClipboard(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "$label berhasil disalin ke clipboard!", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dokumentasi & Skema Database", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
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
            Text(
                text = "Panduan Arsitektur & Database SE SMART CELL",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                text = "Aplikasi ini dirancang modular menggunakan SQLite/Room di Android serta kompatibel penuh dengan skema MySQL dan REST API untuk kebutuhan integrasi backend PHP.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

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
                        Text("Skema MySQL (database.sql)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        OutlinedButton(
                            onClick = { copyToClipboard("database.sql", sqlSchema) },
                            modifier = Modifier.testTag("copy_sql_button")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Salin SQL", fontSize = 11.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = sqlSchema,
                            color = Color(0xFF38BDF8),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        )
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
                        Text("Dokumentasi REST API", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        OutlinedButton(onClick = { copyToClipboard("REST API", apiDocs) }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Salin API", fontSize = 11.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = apiDocs,
                            color = Color(0xFFA7F3D0),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }
    }
}
