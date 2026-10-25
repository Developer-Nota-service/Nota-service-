package com.sesmartcell.notaservice

import com.sesmartcell.notaservice.data.entity.ServiceNoteEntity
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit test untuk logika biaya nota — total biaya dipakai di dashboard,
 * laporan, pesan WhatsApp, dan penentuan lunas/belum.
 */
class ServiceNoteEntityTest {

    private fun note(serviceFee: Long, sparepartFee: Long, discount: Long) =
        ServiceNoteEntity(
            noteNumber = "SC-TEST-001",
            customerName = "Budi",
            customerPhone = "081234567890",
            deviceType = "Handphone",
            brand = "Samsung",
            model = "A54",
            issueDescription = "Layar retak",
            serviceFee = serviceFee,
            sparepartFee = sparepartFee,
            discount = discount
        )

    @Test
    fun `totalCost menjumlah jasa dan sparepart lalu mengurangi diskon`() {
        assertEquals(150_000L, note(serviceFee = 100_000, sparepartFee = 50_000, discount = 0).totalCost)
        assertEquals(90_000L, note(serviceFee = 100_000, sparepartFee = 50_000, discount = 60_000).totalCost)
    }

    @Test
    fun `totalCost tidak pernah negatif walau diskon melebihi biaya`() {
        assertEquals(0L, note(serviceFee = 50_000, sparepartFee = 0, discount = 100_000).totalCost)
    }
}
