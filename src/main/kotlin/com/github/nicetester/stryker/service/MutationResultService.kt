package com.github.nicetester.stryker.service

import com.github.nicetester.stryker.model.MutantResult
import com.github.nicetester.stryker.model.MutantStatus
import com.github.nicetester.stryker.model.ReportParser
import com.github.nicetester.stryker.util.PathUtil
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

@Service(Service.Level.PROJECT)
class MutationResultService(private val project: Project) {

    private val running = AtomicBoolean(false)

    val isRunning: Boolean get() = running.get()

    @Volatile
    var strykerConfigDir: String? = null
        private set

    /** Epoch millis of the report currently loaded. 0 means nothing loaded. */
    @Volatile
    var reportTimestamp: Long = 0L
        private set

    private val resultsByFile = mutableMapOf<String, List<MutantResult>>()

    // Tracks which report files have been auto-loaded, to avoid re-parsing on every file open
    private val loadedReportPaths = mutableSetOf<String>()

    fun tryMarkRunning(): Boolean = running.compareAndSet(false, true)

    fun markFinished() {
        running.set(false)
    }

    fun setResults(results: Map<String, List<MutantResult>>) {
        setResults(results, null, System.currentTimeMillis())
    }

    fun setResults(results: Map<String, List<MutantResult>>, configDir: String?) {
        setResults(results, configDir, System.currentTimeMillis())
    }

    fun setResults(results: Map<String, List<MutantResult>>, configDir: String?, timestamp: Long) {
        synchronized(resultsByFile) {
            resultsByFile.clear()
            resultsByFile.putAll(results)
            loadedReportPaths.clear()
        }
        strykerConfigDir = configDir
        reportTimestamp = timestamp
        restartAnalysis()
    }

    /**
     * Loads results only if the given timestamp is newer than what's currently loaded.
     * Used by auto-discovery and CI fetch to avoid overwriting a fresher report.
     * Returns true if the results were accepted.
     */
    fun setResultsIfNewer(
        results: Map<String, List<MutantResult>>,
        configDir: String?,
        timestamp: Long,
    ): Boolean {
        if (timestamp <= reportTimestamp) return false
        setResults(results, configDir, timestamp)
        return true
    }

    fun getResultsForFile(filePath: String): List<MutantResult> =
        synchronized(resultsByFile) {
            resultsByFile[filePath] ?: emptyList()
        }

    fun getSurvivedResultsForFile(filePath: String): List<MutantResult> =
        getResultsForFile(filePath).filter { it.status == MutantStatus.Survived }

    fun clearResults() {
        synchronized(resultsByFile) {
            resultsByFile.clear()
            loadedReportPaths.clear()
        }
        strykerConfigDir = null
        reportTimestamp = 0L
        restartAnalysis()
    }

    /**
     * Auto-discovers and loads a mutation report from disk for the given file.
     * Only loads if the report file is newer than the currently loaded report.
     * Returns true if a report was found and loaded.
     */
    fun tryAutoLoadReport(absoluteFilePath: String): Boolean {
        val basePath = project.basePath ?: return false

        val reportFile = PathUtil.findNearestReportFile(absoluteFilePath, basePath) ?: return false
        val reportPath = reportFile.canonicalPath
        val fileTimestamp = reportFile.lastModified()

        synchronized(resultsByFile) {
            if (reportPath in loadedReportPaths) return false
        }

        // Skip if we already have a newer or equal report loaded
        if (fileTimestamp <= reportTimestamp) return false

        val report = try {
            ReportParser.parse(reportFile)
        } catch (_: Exception) {
            null
        } ?: return false

        val configDir = PathUtil.getReportConfigDir(reportFile)

        return setResultsIfNewer(report.files, configDir, fileTimestamp).also { loaded ->
            if (loaded) {
                synchronized(resultsByFile) { loadedReportPaths.add(reportPath) }
            }
        }
    }

    /** Deletes the single CI report download directory under .stryker-runner/ci-report/ */
    fun deleteCiReportDir() {
        val basePath = project.basePath ?: return
        File(basePath, CI_REPORT_DIR).deleteRecursively()
    }

    private fun restartAnalysis() {
        DaemonCodeAnalyzer.getInstance(project).restart()
    }

    companion object {
        const val CI_REPORT_DIR = ".stryker-runner/ci-report"

        @JvmStatic
        fun getInstance(project: Project): MutationResultService =
            project.getService(MutationResultService::class.java)
    }
}
