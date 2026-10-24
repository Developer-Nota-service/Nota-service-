package com.sesmartcell.notaservice.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Pastikan import warna tema menggunakan namespace yang benar:
import com.sesmartcell.notaservice.ui.theme.StatusError
import com.sesmartcell.notaservice.ui.theme.StatusPurple
import com.sesmartcell.notaservice.ui.theme.StatusSuccess
import com.sesmartcell.notaservice.ui.theme.StatusWarning

@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
// ... (sisa kode ke bawah tetap sama)
    val (bgColor, textColor) = when (status) {
        "Menunggu" -> Color(0xFFFEF3C7) to Color(0xFFB45309) // Amber
        "Sedang Dikerjakan" -> Color(0xFFDBEAFE) to Color(0xFF1E40AF) // Blue
        "Menunggu Sparepart" -> Color(0xFFEDE9FE) to Color(0xFF6D28D9) // Purple
        "Selesai" -> Color(0xFFD1FAE5) to Color(0xFF065F46) // Emerald
        "Diambil Pelanggan" -> Color(0xFFE0E7FF) to Color(0xFF3730A3) // Indigo
        "Dibatalkan" -> Color(0xFFFEE2E2) to Color(0xFF991B1B) // Red
        else -> Color(0xFFF1F5F9) to Color(0xFF475569)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(textColor)
            )
            Text(
                text = " $status",
                color = textColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun PaymentStatusBadge(
    totalCost: Long,
    totalPaid: Long,
    modifier: Modifier = Modifier
) {
    val isLunas = totalPaid >= totalCost && totalCost > 0
    val isPartial = totalPaid in 1 until totalCost
    val isUnpaid = totalPaid == 0L

    val (label, bgColor, textColor) = when {
        isLunas -> Triple("LUNAS", Color(0xFFD1FAE5), Color(0xFF047857))
        isPartial -> Triple("DP / SEBAGIAN", Color(0xFFFEF3C7), Color(0xFFB45309))
        else -> Triple("BELUM BAYAR", Color(0xFFFEE2E2), Color(0xFFB91C1C))
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun DeviceTypeBadge(
    deviceType: String,
    modifier: Modifier = Modifier
) {
    val (iconText, bgColor, textColor) = when (deviceType.uppercase()) {
        "HP" -> Triple("📱 HP", Color(0xFFEFF6FF), Color(0xFF1D4ED8))
        "LAPTOP" -> Triple("💻 LAPTOP", Color(0xFFF0FDF4), Color(0xFF15803D))
        else -> Triple("⚙️ LAINNYA", Color(0xFFF8FAFC), Color(0xFF475569))
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .border(0.5.dp, textColor.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = iconText,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
