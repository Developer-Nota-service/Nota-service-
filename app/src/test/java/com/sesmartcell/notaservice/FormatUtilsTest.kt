package com.sesmartcell.notaservice

import com.sesmartcell.notaservice.util.FormatUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

/**
 * Unit test untuk utilitas format yang dipakai nota, laporan, dan pesan
 * WhatsApp — murni JVM, tanpa perangkat/emulator.
 */
class FormatUtilsTest {

    @Test
    fun `formatRupiah memakai pemisah ribuan Indonesia`() {
        assertEquals("Rp 1.500.000", FormatUtils.formatRupiah(1_500_000L))
        assertEquals("Rp 150.000", FormatUtils.formatRupiah(150_000L))
        assertEquals("Rp 0", FormatUtils.formatRupiah(0L))
    }

    @Test
    fun `formatDate menghasilkan tanggal yang bisa dibaca`() {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"))
        val formatted = FormatUtils.formatDate(0L)
        // Epoch (1 Jan 1970 00:00 GMT) — tanggalnya pasti muncul di hasil.
        assertTrue("Hasil bukan tanggal: $formatted", formatted.contains("1970"))
    }

    @Test
    fun `sanitizePhone menormalkan nomor Indonesia ke format 62`() {
        // 0xx -> 62xx
        assertEquals("6281346043318", FormatUtils.sanitizePhone("081346043318"))
        // Sudah 62 -> tetap
        assertEquals("6281346043318", FormatUtils.sanitizePhone("6281346043318"))
        // Tanpa awalan -> 62
        assertEquals("6281345678901", FormatUtils.sanitizePhone("81345678901"))
        // Simbol seperti + dan spasi dibuang
        assertEquals("6281234567890", FormatUtils.sanitizePhone("+62 812-3456-7890"))
        // Nomor asing/tidak dikenal tidak dipaksa
        assertEquals("123", FormatUtils.sanitizePhone("123"))
    }
}
