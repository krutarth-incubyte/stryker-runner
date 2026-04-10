package com.github.nicetester.stryker.service

import com.github.nicetester.stryker.model.MutantLocation
import com.github.nicetester.stryker.model.MutantResult
import com.github.nicetester.stryker.model.MutantStatus
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.io.File

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

    // --- setResultsIfNewer ---

    fun testAcceptsResultsWhenNothingIsLoadedYet() {
        val result = service.setResultsIfNewer(
            mapOf("src/app.ts" to listOf(makeMutant("1"))),
            configDir = null,
            timestamp = 1000L,
        )

        assertTrue("Should accept results when nothing is loaded", result)
        assertEquals(1, service.getResultsForFile("src/app.ts").size)
        assertEquals(1000L, service.reportTimestamp)
    }

    fun testRejectsResultsOlderThanCurrentlyLoaded() {
        service.setResults(mapOf("src/app.ts" to listOf(makeMutant("1", "Current"))), null, 2000L)

        val accepted = service.setResultsIfNewer(
            mapOf("src/app.ts" to listOf(makeMutant("2", "Stale"))),
            configDir = null,
            timestamp = 1000L,
        )

        assertFalse("Should reject results older than current timestamp", accepted)
        assertEquals("Current", service.getResultsForFile("src/app.ts")[0].mutatorName)
    }

    fun testRejectsResultsWithSameTimestampAsCurrentlyLoaded() {
        service.setResults(mapOf("src/app.ts" to listOf(makeMutant("1", "Current"))), null, 1000L)

        val accepted = service.setResultsIfNewer(
            mapOf("src/app.ts" to listOf(makeMutant("2", "SameTime"))),
            configDir = null,
            timestamp = 1000L,
        )

        assertFalse("Should reject results with equal timestamp (strict newer-than check)", accepted)
        assertEquals("Current", service.getResultsForFile("src/app.ts")[0].mutatorName)
    }

    fun testAcceptsResultsNewerThanCurrentlyLoaded() {
        service.setResults(mapOf("src/app.ts" to listOf(makeMutant("1", "Old"))), null, 1000L)

        val accepted = service.setResultsIfNewer(
            mapOf("src/app.ts" to listOf(makeMutant("2", "Fresh"))),
            configDir = null,
            timestamp = 2000L,
        )

        assertTrue("Should accept results newer than current timestamp", accepted)
        assertEquals("Fresh", service.getResultsForFile("src/app.ts")[0].mutatorName)
        assertEquals(2000L, service.reportTimestamp)
    }

    fun testCiReportDoesNotOverwriteFresherLocalReport() {
        // Simulate: local Stryker just ran and produced a fresh report
        service.setResults(mapOf("src/app.ts" to listOf(makeMutant("1", "LocalFresh"))), null, 3000L)

        // CI report is older (was generated before the local run)
        val ciAccepted = service.setResultsIfNewer(
            mapOf("src/app.ts" to listOf(makeMutant("2", "CiStale"))),
            configDir = null,
            timestamp = 2000L,
        )

        assertFalse("CI report should not overwrite a fresher local report", ciAccepted)
        assertEquals("LocalFresh", service.getResultsForFile("src/app.ts")[0].mutatorName)
    }

    fun testLocalReportOverwritesStaleCiReport() {
        // Simulate: CI report loaded first
        service.setResults(mapOf("src/app.ts" to listOf(makeMutant("1", "CiOld"))), null, 1000L)

        // Local run completes later with a fresher report
        val localAccepted = service.setResultsIfNewer(
            mapOf("src/app.ts" to listOf(makeMutant("2", "LocalFresh"))),
            configDir = null,
            timestamp = 2000L,
        )

        assertTrue("Fresh local report should overwrite stale CI report", localAccepted)
        assertEquals("LocalFresh", service.getResultsForFile("src/app.ts")[0].mutatorName)
    }

    // --- tryAutoLoadReport ---

    fun testTryAutoLoadReturnsFalseWhenNoReportExists() {
        val fakePath = "/tmp/nonexistent-project-${System.nanoTime()}/src/app.ts"

        val loaded = service.tryAutoLoadReport(fakePath)

        assertFalse("Should return false when no report file exists", loaded)
    }

    fun testTryAutoLoadFindsAndLoadsReportFromDisk() {
        // Create the report inside project.basePath so PathUtil.findNearestReportFile can locate it
        val basePath = project.basePath!!
        val reportDir = File(basePath, "reports/mutation").also { it.mkdirs() }
        val reportFile = writeSampleReport(File(reportDir, "mutation.json"))
        val sourceFile = File(basePath, "src/app.ts").also { it.parentFile.mkdirs(); it.createNewFile() }
        try {
            val loaded = service.tryAutoLoadReport(sourceFile.path)

            assertTrue("Should load report when mutation.json exists near the source file", loaded)
            assertTrue("Timestamp should be set after loading", service.reportTimestamp > 0L)
        } finally {
            reportFile.delete()
            reportDir.deleteRecursively()
            sourceFile.delete()
        }
    }

    fun testTryAutoLoadDoesNotReloadAlreadyLoadedReport() {
        val basePath = project.basePath!!
        val reportDir = File(basePath, "reports/mutation").also { it.mkdirs() }
        val reportFile = writeSampleReport(File(reportDir, "mutation.json"))
        val sourceFile = File(basePath, "src/app.ts").also { it.parentFile.mkdirs(); it.createNewFile() }
        try {
            service.tryAutoLoadReport(sourceFile.path)
            val firstTimestamp = service.reportTimestamp

            val loadedAgain = service.tryAutoLoadReport(sourceFile.path)

            assertFalse("Should not reload a report that was already loaded from the same path", loadedAgain)
            assertEquals("Timestamp should not change on second attempt", firstTimestamp, service.reportTimestamp)
        } finally {
            reportFile.delete()
            reportDir.deleteRecursively()
            sourceFile.delete()
        }
    }

    fun testTryAutoLoadDoesNotOverwriteFresherInMemoryReport() {
        // Load a very fresh report into memory first
        service.setResults(mapOf("src/app.ts" to listOf(makeMutant("1", "FreshMemory"))), null, Long.MAX_VALUE)

        val basePath = project.basePath!!
        val reportDir = File(basePath, "reports/mutation").also { it.mkdirs() }
        val reportFile = writeSampleReport(File(reportDir, "mutation.json"))
        val sourceFile = File(basePath, "src/app.ts").also { it.parentFile.mkdirs(); it.createNewFile() }
        try {
            val loaded = service.tryAutoLoadReport(sourceFile.path)

            assertFalse("Auto-discovery should not overwrite a fresher in-memory report", loaded)
            assertEquals("FreshMemory", service.getResultsForFile("src/app.ts").firstOrNull()?.mutatorName)
        } finally {
            reportFile.delete()
            reportDir.deleteRecursively()
            sourceFile.delete()
        }
    }

    // --- deleteCiReportDir ---

    fun testDeleteCiReportDirRemovesDirectoryFromDisk() {
        val ciDir = File(project.basePath!!, MutationResultService.CI_REPORT_DIR).also {
            it.mkdirs()
            File(it, "mutation.json").writeText("{}")
        }
        assertTrue("CI report dir should exist before deletion", ciDir.exists())

        service.deleteCiReportDir()

        assertFalse("CI report dir should be gone after deletion", ciDir.exists())
    }

    fun testDeleteCiReportDirDoesNothingWhenDirectoryDoesNotExist() {
        val ciDir = File(project.basePath!!, MutationResultService.CI_REPORT_DIR)
        assertFalse("CI report dir should not exist", ciDir.exists())

        service.deleteCiReportDir() // Should not throw

        assertFalse("CI report dir should still not exist", ciDir.exists())
    }

    // --- reportTimestamp resets on clear ---

    fun testClearResultsResetsTimestampToZero() {
        service.setResults(mapOf("src/app.ts" to listOf(makeMutant("1"))), null, 5000L)
        assertEquals(5000L, service.reportTimestamp)

        service.clearResults()

        assertEquals("Timestamp should be 0 after clear", 0L, service.reportTimestamp)
    }

    // --- helpers ---

    private fun writeSampleReport(file: File): File {
        file.writeText("""
            {
              "files": {
                "src/app.ts": {
                  "mutants": [
                    {
                      "id": "1",
                      "mutatorName": "BooleanLiteral",
                      "replacement": "true",
                      "location": {"start": {"line": 1, "column": 1}, "end": {"line": 1, "column": 5}},
                      "status": "Survived"
                    }
                  ]
                }
              }
            }
        """.trimIndent())
        return file
    }
}
