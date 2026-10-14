package com.greenworldlighting.entrybook.core

import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class FormatTest {
    @Test
    fun normalizesRupees() {
        assertEquals("12000.50", normalizeRupees("12000.50"))
        assertEquals("12000.50", normalizeRupees("12000.5"))
        assertEquals("0.50", normalizeRupees("00.5"))
        assertEquals("0.00", normalizeRupees("0", allowZero = true))
    }

    @Test
    fun rejectsBadAmounts() {
        expectFail { normalizeRupees("0") }
        expectFail { normalizeRupees("12.345") }
        expectFail { normalizeRupees("-1") }
        expectFail { normalizeRupees("1200.") }
    }

    @Test
    fun normalizesServerUrl() {
        assertEquals(
            "https://entrybook.greenworldlighting.com/api/v1/",
            normalizeBaseUrl("https://entrybook.greenworldlighting.com/api/v1"),
        )
        assertEquals("http://10.0.2.2:3000/api/v1/", normalizeBaseUrl("http://10.0.2.2:3000"))
        assertEquals(
            "https://entrybook.greenworldlighting.com/api/v1/",
            normalizeBaseUrl("entrybook.greenworldlighting.com"),
        )
    }

    private fun expectFail(block: () -> Unit) {
        try {
            block()
            fail("expected an error")
        } catch (_: IllegalArgumentException) {
        }
    }
}
