package com.cnctech.process.data.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

class TechProcessRulesTest {
    @Test
    fun setupOrderLabel_romanAndFallback() {
        assertEquals("I", TechProcessRules.setupOrderLabel(0))
        assertEquals("II", TechProcessRules.setupOrderLabel(1))
        assertEquals("X", TechProcessRules.setupOrderLabel(9))
        assertEquals("11", TechProcessRules.setupOrderLabel(10))
    }

    @Test
    fun nextOrder_fromEmptyAndExisting() {
        assertEquals(0, TechProcessRules.nextOrder(emptyList()))
        assertEquals(3, TechProcessRules.nextOrder(listOf(0, 2)))
        assertEquals(2, TechProcessRules.nextOrder(listOf(1)))
    }

    @Test
    fun validateReorderIds_acceptsPermutation() {
        val result = TechProcessRules.validateReorderIds(listOf(3L, 1L, 2L), listOf(1L, 2L, 3L))
        assertNotNull(result)
        assertEquals(mapOf(3L to 0, 1L to 1, 2L to 2), result)
    }

    @Test
    fun validateReorderIds_rejectsBad() {
        assertNull(TechProcessRules.validateReorderIds(listOf(1L, 2L), listOf(1L, 2L, 3L)))
        assertNull(TechProcessRules.validateReorderIds(listOf(1L, 1L, 2L), listOf(1L, 2L, 3L)))
        assertNull(TechProcessRules.validateReorderIds(listOf(9L, 1L, 2L), listOf(1L, 2L, 3L)))
    }
}
