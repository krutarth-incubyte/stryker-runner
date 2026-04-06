package com.github.nicetester.stryker.runner

import com.github.nicetester.stryker.StrykerBundle
import com.github.nicetester.stryker.model.ReportParser
import com.github.nicetester.stryker.service.MutationResultService
import com.github.nicetester.stryker.util.EnvironmentUtil
import com.github.nicetester.stryker.util.StrykerNotifications
import com.google.gson.JsonSyntaxException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.execution.ui.ConsoleView
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import java.io.File

class StrykerRunner(
    project: Project,
    private val mutatePattern: String,
    private val strykerConfigDir: String,
) : Task.Backgroundable(
    project,
    StrykerBundle.message("runner.progress.title"),
    true,
) {
    private val outputBuffer = StringBuilder()

    override fun run(indicator: ProgressIndicator) {
        val service = MutationResultService.getInstance(project)
        service.clearResults()
        try {
            indicator.text = StrykerBundle.message("runner.progress.indicator", mutatePattern)
            indicator.isIndeterminate = true

            val commandLine = buildCommandLine()
            val console = initializeConsole()
            val processHandler = OSProcessHandler(commandLine)

            attachConsoleAndCollectOutput(processHandler, console)
            processHandler.startNotify()
            processHandler.waitFor()

            handleExitCode(processHandler.exitCode)
        } finally {
            service.markFinished()
        }
    }

    private fun buildCommandLine(): GeneralCommandLine =
        GeneralCommandLine(EnvironmentUtil.npxCommand(), "stryker", "run", "--mutate", mutatePattern)
            .withWorkDirectory(strykerConfigDir)
            .withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)

    private fun initializeConsole(): ConsoleView {
        var console: ConsoleView? = null
        ApplicationManager.getApplication().invokeAndWait {
            console = StrykerConsole.getOrCreateConsole(project).also { it.clear() }
            StrykerConsole.activateToolWindow(project)
        }
        return console!!
    }

    private fun attachConsoleAndCollectOutput(processHandler: OSProcessHandler, console: ConsoleView) {
        console.attachToProcess(processHandler)
        processHandler.addProcessListener(object : ProcessListener {
            override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                if (outputType === ProcessOutputTypes.STDOUT || outputType === ProcessOutputTypes.STDERR) {
                    outputBuffer.append(event.text)
                }
            }
        })
    }

    private fun handleExitCode(exitCode: Int?) {
        when {
            exitCode == null -> StrykerNotifications.notifyError(
                project,
                StrykerBundle.message("notification.stryker.error.title"),
                StrykerBundle.message("notification.stryker.error.content", "unknown"),
            )
            exitCode == 0 -> {
                notifySuccess()
                parseAndStoreReport()
            }
            else -> StrykerNotifications.notifyError(
                project,
                StrykerBundle.message("notification.stryker.error.title"),
                StrykerBundle.message("notification.stryker.error.content", exitCode),
            )
        }
    }

    private fun notifySuccess() {
        val summary = parseMutationSummary(outputBuffer.toString())
        val content = summary ?: StrykerBundle.message("notification.stryker.finished.success")
        StrykerNotifications.notifyInfo(
            project,
            StrykerBundle.message("notification.stryker.finished.title"),
            content,
        )
    }

    private fun parseAndStoreReport() {
        val reportFile = findReportFile()

        if (reportFile == null || !reportFile.exists()) {
            StrykerNotifications.notifyWarning(
                project,
                StrykerBundle.message("notification.report.not.found.title"),
                StrykerBundle.message("notification.report.not.found.content"),
            )
            return
        }

        try {
            val report = ReportParser.parse(reportFile)
            if (report != null) {
                MutationResultService.getInstance(project).setResults(report.files, strykerConfigDir)
            }
        } catch (_: JsonSyntaxException) {
            StrykerNotifications.notifyError(
                project,
                StrykerBundle.message("notification.report.invalid.json.title"),
                StrykerBundle.message("notification.report.invalid.json.content"),
            )
        }
    }

    private fun findReportFile(): File? {
        // Check at stryker config dir (standard setup)
        val configDirReport = File(strykerConfigDir, REPORT_RELATIVE_PATH)
        if (configDirReport.exists()) return configDirReport

        // Walk up from the mutated file to find the nearest reports/ directory
        val mutatedFilePath = File(strykerConfigDir, mutatePattern.replace(GLOB_SUFFIX_PATTERN, ""))
        var dir: File? = if (mutatedFilePath.isDirectory) mutatedFilePath else mutatedFilePath.parentFile
        while (dir != null && dir.path.startsWith(strykerConfigDir)) {
            val candidate = File(dir, REPORT_RELATIVE_PATH)
            if (candidate.exists()) return candidate
            dir = dir.parentFile
        }

        return null
    }

    companion object {
        private const val REPORT_RELATIVE_PATH = "reports/mutation/mutation.json"
        private val GLOB_SUFFIX_PATTERN = Regex("""/\*\*.*""")
        private val SURVIVED_PATTERN = Regex("""(\d+)\s+survived""", RegexOption.IGNORE_CASE)
        private val KILLED_PATTERN = Regex("""(\d+)\s+killed""", RegexOption.IGNORE_CASE)

        fun parseMutationSummary(output: String): String? {
            val survived = SURVIVED_PATTERN.find(output)?.groupValues?.get(1)
            val killed = KILLED_PATTERN.find(output)?.groupValues?.get(1)

            if (survived != null && killed != null) {
                return StrykerBundle.message("notification.stryker.finished.content", survived, killed)
            }
            return null
        }
    }
}
