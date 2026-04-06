package com.github.nicetester.stryker.service

import com.github.nicetester.stryker.model.MutantResult
import com.github.nicetester.stryker.model.MutantStatus
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import java.util.concurrent.atomic.AtomicBoolean

@Service(Service.Level.PROJECT)
class MutationResultService(private val project: Project) {

    private val running = AtomicBoolean(false)

    val isRunning: Boolean get() = running.get()

    @Volatile
    var strykerConfigDir: String? = null
        private set

    private val resultsByFile = mutableMapOf<String, List<MutantResult>>()

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
        }
        strykerConfigDir = null
        restartAnalysis()
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
