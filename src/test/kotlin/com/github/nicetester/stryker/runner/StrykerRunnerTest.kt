package com.github.nicetester.stryker.runner

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class StrykerRunnerTest : BasePlatformTestCase() {

    // --- parseMutationSummary tests ---

    fun testParseMutationSummaryExtractsSurvivedAndKilledCounts() {
        val output = """
            Mutant killed: src/app.ts:5:10
            Mutant survived: src/app.ts:12:4
            All tests complete
            3 survived
            7 killed
        """.trimIndent()

        val result = StrykerRunner.parseMutationSummary(output)

        assertNotNull(result)
        assertTrue("Summary should contain survived count", result!!.contains("3"))
        assertTrue("Summary should contain killed count", result.contains("7"))
    }

    fun testParseMutationSummaryReturnsNullWhenNoSurvivedOrKilledFound() {
        val output = "All tests complete. No mutants generated."

        val result = StrykerRunner.parseMutationSummary(output)

        assertNull(result)
    }

    fun testParseMutationSummaryReturnsNullWhenOnlySurvivedPresent() {
        val output = "5 survived"

        val result = StrykerRunner.parseMutationSummary(output)

        assertNull(result)
    }

    fun testParseMutationSummaryReturnsNullWhenOnlyKilledPresent() {
        val output = "10 killed"

        val result = StrykerRunner.parseMutationSummary(output)

        assertNull(result)
    }

    fun testParseMutationSummaryHandlesZeroCounts() {
        val output = "0 survived\n10 killed"

        val result = StrykerRunner.parseMutationSummary(output)

        assertNotNull(result)
        assertTrue("Summary should contain 0", result!!.contains("0"))
        assertTrue("Summary should contain 10", result.contains("10"))
    }

    fun testParseMutationSummaryIsCaseInsensitive() {
        val output = "3 Survived\n7 Killed"

        val result = StrykerRunner.parseMutationSummary(output)

        assertNotNull(result)
        assertTrue("Summary should contain survived count", result!!.contains("3"))
        assertTrue("Summary should contain killed count", result.contains("7"))
    }

    fun testParseMutationSummaryReturnsNullForEmptyOutput() {
        val result = StrykerRunner.parseMutationSummary("")

        assertNull(result)
    }

    fun testParseMutationSummaryHandlesLargeNumbers() {
        val output = "150 survived\n2340 killed"

        val result = StrykerRunner.parseMutationSummary(output)

        assertNotNull(result)
        assertTrue("Summary should contain 150", result!!.contains("150"))
        assertTrue("Summary should contain 2340", result.contains("2340"))
    }
}
