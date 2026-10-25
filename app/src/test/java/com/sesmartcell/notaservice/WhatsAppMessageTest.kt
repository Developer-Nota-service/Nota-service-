package com.sesmartcell.notaservice

import com.sesmartcell.notaservice.data.entity.ServiceNoteEntity
import com.sesmartcell.notaservice.data.entity.StoreSettingsEntity
import com.sesmartcell.notaservice.util.WhatsAppHelper
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test untuk template pesan WhatsApp yang dikirim ke pelanggan:
 * status pengerjaan, instruksi pembayaran, dan bukti pengambilan.
 */
class WhatsAppMessageTest {

    private val store = StoreSettingsEntity(
        storeName = "SE SMART CELL",
        storeTagline = "Service Profesional",
        storeAddress = "Jl. Merdeka 1",
        storePhone = "6281346043318",
        qrisMerchantName = "SE SMART CELL",
        bankAccounts = "BCA 1234567890 a.n. Toko",
        ewalletAccounts = "DANA 081234567890",
        warrantyTerms = "Garansi 30 hari sejak pengambilan."
    )

    private fun note(status: String, repairAction: String = "Ganti layar") =
        ServiceNoteEntity(
            noteNumber = "SC-20261025-001",
            customerName = "Siti",
            customerPhone = "081234567890",
            deviceType = "Handphone",
            brand = "Xiaomi",
            model = "Redmi 10",
            issueDescription = "Layar pecah",
            repairAction = repairAction,
            serviceFee = 100_000,
            sparepartFee = 50_000,
            serviceStatus = status,
            warrantyPeriod = "30 Hari"
        )

    @Test
    fun `pesan status memakai emoji sesuai status pengerjaan`() {
        assertTrue(WhatsAppHelper.createStatusMessage(note("Diambil Pelanggan"), store, 150_000, 0).contains("🎉"))
        assertTrue(WhatsAppHelper.createStatusMessage(note("Selesai"), store, 150_000, 0).contains("✅"))
        assertTrue(WhatsAppHelper.createStatusMessage(note("Menunggu"), store, 0, 150_000).contains("📋"))
    }

    @Test
    fun `pesan status menandai LUNAS hanya saat sisa nol`() {
        val lunas = WhatsAppHelper.createStatusMessage(note("Selesai"), store, 150_000, 0)
        val belum = WhatsAppHelper.createStatusMessage(note("Selesai"), store, 50_000, 100_000)

        assertTrue(lunas.contains("(LUNAS)"))
        assertFalse(belum.contains("(LUNAS)"))
        assertTrue("Sisa pembayaran harus tertera: $belum", belum.contains("Rp 100.000"))
    }

    @Test
    fun `baris tindakan hanya muncul saat repairAction terisi`() {
        val withAction = WhatsAppHelper.createStatusMessage(note("Selesai", repairAction = "Ganti layar"), store, 150_000, 0)
        val withoutAction = WhatsAppHelper.createStatusMessage(note("Selesai", repairAction = ""), store, 150_000, 0)

        assertTrue(withAction.contains("🛠"))
        assertFalse(withoutAction.contains("🛠"))
    }

    @Test
    fun `instruksi pembayaran memakai sisa tagihan, atau total saat sudah lunas`() {
        val masihSisa = WhatsAppHelper.createPaymentInstruction(note("Selesai"), store, 75_000)
        assertTrue(masihSisa.contains("Rp 75.000"))

        val lunas = WhatsAppHelper.createPaymentInstruction(note("Selesai"), store, 0)
        assertTrue(lunas.contains("Rp 150.000"))
    }

    @Test
    fun `bukti pengambilan menyertakan status LUNAS dan ketentuan garansi`() {
        val message = WhatsAppHelper.createCompletionMessage(note("Diambil Pelanggan"), store, 150_000)
        assertTrue(message.contains("LUNAS"))
        assertTrue(message.contains(store.warrantyTerms))
        assertTrue(message.contains("30 Hari"))
    }
}
