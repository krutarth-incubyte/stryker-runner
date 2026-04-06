package com.github.nicetester.stryker.service

import com.github.nicetester.stryker.model.MutantLocation
import com.github.nicetester.stryker.model.MutantResult
import com.github.nicetester.stryker.model.MutantStatus
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class MutationResultServiceTest : BasePlatformTestCase() {

    private lateinit var service: MutationResultService

    override fun setUp() {
        super.setUp()
        service = MutationResultService.getInstance(project)
        service.markFinished()
        service.clearResults()
    }

    fun testServiceIsRegisteredAndObtainableFromProject() {
        assertNotNull("MutationResultService should be obtainable from project", service)
    }

    fun testGetInstanceReturnsSameInstanceForSameProject() {
        val second = MutationResultService.getInstance(project)
        assertSame("Should return the same project-scoped service instance", service, second)
    }

    // --- tryMarkRunning / markFinished ---

    fun testIsRunningIsFalseByDefault() {
        assertFalse("isRunning should be false initially", service.isRunning)
    }

    fun testTryMarkRunningReturnsTrueAndSetsIsRunning() {
        assertTrue("tryMarkRunning should return true when not running", service.tryMarkRunning())
        assertTrue("isRunning should be true after tryMarkRunning", service.isRunning)
    }

    fun testTryMarkRunningReturnsFalseWhenAlreadyRunning() {
        service.tryMarkRunning()
        assertFalse("tryMarkRunning should return false when already running", service.tryMarkRunning())
    }

    fun testMarkFinishedSetsIsRunningToFalse() {
        service.tryMarkRunning()
        service.markFinished()
        assertFalse("isRunning should be false after markFinished()", service.isRunning)
    }

    fun testTryMarkRunningWorksAfterMarkFinished() {
        service.tryMarkRunning()
        service.markFinished()
        assertTrue("tryMarkRunning should work again after markFinished", service.tryMarkRunning())
        service.markFinished()
    }

    // --- setResults / getResultsForFile / clearResults ---

    private fun makeMutant(id: String, mutatorName: String = "Test", status: MutantStatus = MutantStatus.Survived): MutantResult =
        MutantResult(id, mutatorName, "x", MutantLocation(1, 1, 1, 5), status)

    fun testSetResultsStoresResultsRetrievableByFile() {
        val mutant = makeMutant("1", "BooleanLiteral")

        service.setResults(mapOf("src/app.ts" to listOf(mutant)))

        val results = service.getResultsForFile("src/app.ts")
        assertEquals(1, results.size)
        assertEquals("BooleanLiteral", results[0].mutatorName)
    }

    fun testGetResultsForFileReturnsEmptyListForUnknownFile() {
        service.setResults(mapOf("src/app.ts" to listOf(makeMutant("1"))))

        val results = service.getResultsForFile("src/nonexistent.ts")

        assertTrue("Should return empty list for unknown file", results.isEmpty())
    }

    fun testClearResultsRemovesAllStoredResults() {
        service.setResults(mapOf(
            "src/a.ts" to listOf(makeMutant("1")),
            "src/b.ts" to listOf(makeMutant("2")),
        ))

        service.clearResults()

        assertTrue("Results for a.ts should be empty after clear", service.getResultsForFile("src/a.ts").isEmpty())
        assertTrue("Results for b.ts should be empty after clear", service.getResultsForFile("src/b.ts").isEmpty())
    }

    fun testSetResultsReplacesExistingResults() {
        service.setResults(mapOf("src/app.ts" to listOf(makeMutant("1", "OldMutator"))))

        service.setResults(mapOf("src/app.ts" to listOf(makeMutant("2", "NewMutator"))))

        val results = service.getResultsForFile("src/app.ts")
        assertEquals(1, results.size)
        assertEquals("NewMutator", results[0].mutatorName)
    }

    fun testSetResultsWithMultipleFilesStoresAll() {
        service.setResults(mapOf(
            "src/a.ts" to listOf(makeMutant("1")),
            "src/b.ts" to listOf(makeMutant("2"), makeMutant("3")),
        ))

        assertEquals(1, service.getResultsForFile("src/a.ts").size)
        assertEquals(2, service.getResultsForFile("src/b.ts").size)
    }

    fun testSetResultsClearsPreviousFilesThatAreNoLongerPresent() {
        service.setResults(mapOf("src/old.ts" to listOf(makeMutant("1"))))

        service.setResults(mapOf("src/new.ts" to listOf(makeMutant("2"))))

        assertTrue("Old file results should be gone", service.getResultsForFile("src/old.ts").isEmpty())
        assertEquals(1, service.getResultsForFile("src/new.ts").size)
    }

    // --- getSurvivedResultsForFile ---

    fun testGetSurvivedResultsForFileFiltersToSurvivedOnly() {
        service.setResults(mapOf("src/app.ts" to listOf(
            makeMutant("1", "A", MutantStatus.Survived),
            makeMutant("2", "B", MutantStatus.Killed),
            makeMutant("3", "C", MutantStatus.Timeout),
            makeMutant("4", "D", MutantStatus.Survived),
        )))

        val survived = service.getSurvivedResultsForFile("src/app.ts")

        assertEquals(2, survived.size)
        assertEquals("A", survived[0].mutatorName)
        assertEquals("D", survived[1].mutatorName)
    }

    fun testGetSurvivedResultsForFileReturnsEmptyWhenNoSurvived() {
        service.setResults(mapOf("src/app.ts" to listOf(
            makeMutant("1", "A", MutantStatus.Killed),
            makeMutant("2", "B", MutantStatus.Timeout),
        )))

        val survived = service.getSurvivedResultsForFile("src/app.ts")

        assertTrue("Should be empty when no survived mutants", survived.isEmpty())
    }

    // --- strykerConfigDir ---

    fun testSetResultsWithConfigDirStoresConfigDir() {
        service.setResults(mapOf("src/app.ts" to listOf(makeMutant("1"))), "/path/to/config")

        assertEquals("/path/to/config", service.strykerConfigDir)
    }

    fun testClearResultsClearsConfigDir() {
        service.setResults(mapOf("src/app.ts" to listOf(makeMutant("1"))), "/path/to/config")

        service.clearResults()

        assertNull("strykerConfigDir should be null after clear", service.strykerConfigDir)
    }
}
