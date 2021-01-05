package com.qbutton.qlucene.updater

import com.qbutton.qlucene.dto.DiffCalculationResult
import com.qbutton.qlucene.dto.Operation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class DiffCalculatorTest {
    private val diffCalculator = DiffCalculator()

    @Test
    fun `getDiff should notice added tokens to end`() {
        // given
        val oldTokens = listOf("1", "2", "3")
        val newTokens = listOf("1", "2", "3", "4")

        // when
        val diff = diffCalculator.getDiff(oldTokens, newTokens)

        // then
        assertEquals(1, diff.size)
        val diffCalculationResult = diff[0]
        assertEquals(Operation.CREATE, diffCalculationResult.operation)
        assertEquals(1, diffCalculationResult.count)
        assertEquals("4", diffCalculationResult.token)
    }

    @Test
    fun `getDiff should notice added tokens to middle`() {
        // given
        val oldTokens = listOf("1", "2", "3")
        val newTokens = listOf("1", "2", "4", "3")

        // when
        val diff = diffCalculator.getDiff(oldTokens, newTokens)

        // then
        assertEquals(1, diff.size)
        val diffCalculationResult = diff[0]
        assertEquals(Operation.CREATE, diffCalculationResult.operation)
        assertEquals(1, diffCalculationResult.count)
        assertEquals("4", diffCalculationResult.token)
    }

    @Test
    fun `getDiff should notice added tokens to front`() {
        // given
        val oldTokens = listOf("1", "2", "3")
        val newTokens = listOf("0", "1", "2", "3")

        // when
        val diff = diffCalculator.getDiff(oldTokens, newTokens)

        // then
        assertEquals(1, diff.size)
        val diffCalculationResult = diff[0]
        assertEquals(Operation.CREATE, diffCalculationResult.operation)
        assertEquals(1, diffCalculationResult.count)
        assertEquals("0", diffCalculationResult.token)
    }

    @Test
    fun `getDiff should aggregate added tokens`() {
        // given
        val oldTokens = listOf("1", "2", "3")
        val newTokens = listOf("0", "1", "2", "3", "0", "0")

        // when
        val diff = diffCalculator.getDiff(oldTokens, newTokens)

        // then
        assertEquals(1, diff.size)
        val diffCalculationResult = diff[0]
        assertEquals(Operation.CREATE, diffCalculationResult.operation)
        assertEquals(3, diffCalculationResult.count)
        assertEquals("0", diffCalculationResult.token)
    }

    @Test
    fun `getDiff should notice deleted tokens from end`() {
        // given
        val oldTokens = listOf("1", "2", "3")
        val newTokens = listOf("1", "2")

        // when
        val diff = diffCalculator.getDiff(oldTokens, newTokens)

        // then
        assertEquals(1, diff.size)
        val diffCalculationResult = diff[0]
        assertEquals(Operation.DELETE, diffCalculationResult.operation)
        assertEquals(1, diffCalculationResult.count)
        assertEquals("3", diffCalculationResult.token)
    }

    @Test
    fun `getDiff should notice deleted tokens from middle`() {
        // given
        val oldTokens = listOf("1", "2", "3")
        val newTokens = listOf("1", "3")

        // when
        val diff = diffCalculator.getDiff(oldTokens, newTokens)

        // then
        assertEquals(1, diff.size)
        val diffCalculationResult = diff[0]
        assertEquals(Operation.DELETE, diffCalculationResult.operation)
        assertEquals(1, diffCalculationResult.count)
        assertEquals("2", diffCalculationResult.token)
    }

    @Test
    fun `getDiff should notice deleted tokens from front`() {
        // given
        val oldTokens = listOf("1", "2", "3")
        val newTokens = listOf("2", "3")

        // when
        val diff = diffCalculator.getDiff(oldTokens, newTokens)

        // then
        assertEquals(1, diff.size)
        val diffCalculationResult = diff[0]
        assertEquals(Operation.DELETE, diffCalculationResult.operation)
        assertEquals(1, diffCalculationResult.count)
        assertEquals("1", diffCalculationResult.token)
    }

    @Test
    fun `getDiff should aggregate deleted tokens`() {
        // given
        val oldTokens = listOf("3", "1", "3", "2", "3", "3", "3")
        val newTokens = listOf("1", "2")

        // when
        val diff = diffCalculator.getDiff(oldTokens, newTokens)

        // then
        assertEquals(1, diff.size)
        val diffCalculationResult = diff[0]
        assertEquals(Operation.DELETE, diffCalculationResult.operation)
        assertEquals(5, diffCalculationResult.count)
        assertEquals("3", diffCalculationResult.token)
    }

    @Test
    fun `getDiff should notice changed tokens`() {
        // given
        val oldTokens = listOf("1", "2", "3")
        val newTokens = listOf("1", "4", "3")

        // when
        val diff = diffCalculator.getDiff(oldTokens, newTokens)

        // then
        assertEquals(2, diff.size)
        val deleteDiffCalculationResult = DiffCalculationResult("2", Operation.DELETE, 1)
        val createDiffCalculationResult = DiffCalculationResult("4", Operation.CREATE, 1)
        assertTrue(diff.containsAll(listOf(deleteDiffCalculationResult, createDiffCalculationResult)))
    }

    @Test
    fun `getDiff should work with complex case`() {
        // given
        val oldTokens = listOf("1", "2", "3", "4", "5")
        val newTokens = listOf("0", "1", "4", "3", "4", "5", "0", "4", "2", "1")

        // when
        val diff = diffCalculator.getDiff(oldTokens, newTokens)

        // then
        assertEquals(3, diff.size)
        val diff1 = DiffCalculationResult("0", Operation.CREATE, 2)
        val diff2 = DiffCalculationResult("4", Operation.CREATE, 2)
        val diff3 = DiffCalculationResult("1", Operation.CREATE, 1)
        assertTrue(diff.containsAll(listOf(diff1, diff2, diff3)))
    }
}
