package com.sesmartcell.notaservice

import com.sesmartcell.notaservice.data.entity.ServiceNoteEntity
import com.sesmartcell.notaservice.util.FormatUtils
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testFinancialCalculation() {
        val note = ServiceNoteEntity(
            noteNumber = "SC-20261020-001",
            customerName = "Budi Pratama",
            customerPhone = "08123456789",
            deviceType = "HP",
            brand = "Samsung",
            model = "A54",
            issueDescription = "LCD Pecah",
            serviceFee = 150000,
            sparepartFee = 650000,
            discount = 50000,
            downPayment = 300000
        )
        // Total cost = 150000 + 650000 - 50000 = 750000
        assertEquals(750000L, note.totalCost)
        val sisa = maxOf(0L, note.totalCost - note.downPayment)
        assertEquals(450000L, sisa)
    }

    @Test
    fun testPhoneSanitizationForWhatsApp() {
        assertEquals("628123456789", FormatUtils.sanitizePhone("0812-3456-789"))
        assertEquals("628123456789", FormatUtils.sanitizePhone("628123456789"))
        assertEquals("628123456789", FormatUtils.sanitizePhone("+62 812-3456-789"))
    }
}
