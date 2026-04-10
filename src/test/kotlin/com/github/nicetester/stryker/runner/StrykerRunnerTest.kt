package com.github.nicetester.stryker.runner

import com.github.nicetester.stryker.StrykerBundle
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class StrykerRunnerTest : BasePlatformTestCase() {

    // --- parseMutationSummary: extracts correct counts ---

    fun testSummaryContainsBothSurvivedAndKilledCounts() {
        val output = "3 survived\n7 killed"

        val result = StrykerRunner.parseMutationSummary(output)

        val expected = StrykerBundle.message("notification.stryker.finished.content", "3", "7")
        assertEquals("Summary should match the bundle message format with correct counts", expected, result)
    }

    fun testSummaryExtractsCountsFromVerboseOutput() {
        val output = """
            Mutant killed: src/app.ts:5:10
            Mutant survived: src/app.ts:12:4
            All tests complete
            3 survived
            7 killed
        """.trimIndent()

        val result = StrykerRunner.parseMutationSummary(output)

        val expected = StrykerBundle.message("notification.stryker.finished.content", "3", "7")
        assertEquals(expected, result)
    }

    fun testSummaryHandlesZeroSurvivedCount() {
        val output = "0 survived\n10 killed"

        val result = StrykerRunner.parseMutationSummary(output)

        val expected = StrykerBundle.message("notification.stryker.finished.content", "0", "10")
        assertEquals("Zero survived count should be represented correctly", expected, result)
    }

    fun testSummaryHandlesZeroKilledCount() {
        val output = "5 survived\n0 killed"

        val result = StrykerRunner.parseMutationSummary(output)

        val expected = StrykerBundle.message("notification.stryker.finished.content", "5", "0")
        assertEquals("Zero killed count should be represented correctly", expected, result)
    }

    fun testSummaryIsCaseInsensitive() {
        val output = "3 Survived\n7 Killed"

        val result = StrykerRunner.parseMutationSummary(output)

        val expected = StrykerBundle.message("notification.stryker.finished.content", "3", "7")
        assertEquals("Parsing should be case-insensitive", expected, result)
    }

    fun testSummaryHandlesLargeNumbers() {
        val output = "150 survived\n2340 killed"

        val result = StrykerRunner.parseMutationSummary(output)

        val expected = StrykerBundle.message("notification.stryker.finished.content", "150", "2340")
        assertEquals("Should handle large mutant counts", expected, result)
    }

    // --- parseMutationSummary: returns null when data is incomplete ---

    fun testReturnsNullWhenOutputHasNoMutantCounts() {
        val result = StrykerRunner.parseMutationSummary("All tests complete. No mutants generated.")

        assertNull("Should return null when output contains no survived/killed counts", result)
    }

    fun testReturnsNullWhenOnlySurvivedCountIsPresent() {
        val result = StrykerRunner.parseMutationSummary("5 survived")

        assertNull("Should return null when only survived count is present (no killed count)", result)
    }

    fun testReturnsNullWhenOnlyKilledCountIsPresent() {
        val result = StrykerRunner.parseMutationSummary("10 killed")

        assertNull("Should return null when only killed count is present (no survived count)", result)
    }

    fun testReturnsNullForEmptyOutput() {
        val result = StrykerRunner.parseMutationSummary("")

        assertNull("Should return null for empty output", result)
    }

    // --- parseMutationSummary: survived vs killed are not transposed ---

    fun testSurvivedAndKilledCountsAreNotTransposed() {
        val output = "3 survived\n7 killed"

        val result = StrykerRunner.parseMutationSummary(output)!!

        // Verify the numbers appear in the correct order relative to the message template
        val survivedFirst = StrykerBundle.message("notification.stryker.finished.content", "3", "7")
        val killedFirst = StrykerBundle.message("notification.stryker.finished.content", "7", "3")
        assertEquals("Survived and killed counts should not be transposed", survivedFirst, result)
        assertFalse("Result should not have counts transposed", result == killedFirst)
    }
}
