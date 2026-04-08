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

    private val resultsByFile = mutableMapOf<String, List<MutantResult>>()

    // Tracks which report files have been auto-loaded, to avoid re-parsing on every file open
    private val loadedReportPaths = mutableSetOf<String>()

    fun tryMarkRunning(): Boolean = running.compareAndSet(false, true)

    fun markFinished() {
        running.set(false)
    }

    fun setResults(results: Map<String, List<MutantResult>>) {
        setResults(results, null)
    }

    fun setResults(results: Map<String, List<MutantResult>>, configDir: String?) {
        synchronized(resultsByFile) {
            resultsByFile.clear()
            resultsByFile.putAll(results)
            loadedReportPaths.clear()
        }
        strykerConfigDir = configDir
        restartAnalysis()
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
        restartAnalysis()
    }

    /**
     * Auto-discovers and loads a mutation report from disk for the given file.
     * Called by the annotator when no cached results exist.
     * Returns true if a report was found and loaded.
     */
    fun tryAutoLoadReport(absoluteFilePath: String): Boolean {
        val basePath = project.basePath ?: return false

        val reportFile = PathUtil.findNearestReportFile(absoluteFilePath, basePath) ?: return false
        val reportPath = reportFile.canonicalPath

        synchronized(resultsByFile) {
            if (reportPath in loadedReportPaths) return false
        }

        val report = try {
            ReportParser.parse(reportFile)
        } catch (_: Exception) {
            null
        } ?: return false

        val configDir = PathUtil.getReportConfigDir(reportFile)

        synchronized(resultsByFile) {
            loadedReportPaths.add(reportPath)
            resultsByFile.putAll(report.files)
        }
        strykerConfigDir = configDir
        return true
    }

    private fun restartAnalysis() {
        DaemonCodeAnalyzer.getInstance(project).restart()
    }

    companion object {
        @JvmStatic
        fun getInstance(project: Project): MutationResultService =
            project.getService(MutationResultService::class.java)
    }
}
