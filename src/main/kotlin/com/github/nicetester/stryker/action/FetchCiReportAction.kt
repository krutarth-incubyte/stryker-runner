package com.github.nicetester.stryker.action

import com.github.nicetester.stryker.StrykerBundle
import com.github.nicetester.stryker.ci.GitHubArtifactClient
import com.github.nicetester.stryker.ci.GitUtil
import com.github.nicetester.stryker.model.ReportParser
import com.github.nicetester.stryker.service.MutationResultService
import com.github.nicetester.stryker.util.PathUtil
import com.github.nicetester.stryker.util.StrykerNotifications
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import java.io.File

class FetchCiReportAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val basePath = project.basePath ?: return

        val branch = GitUtil.getCurrentBranch(project)
        if (branch == null || branch == "HEAD" || branch == "main" || branch == "master") {
            StrykerNotifications.notifyWarning(
                project,
                StrykerBundle.message("notification.ci.fetch.title"),
                StrykerBundle.message("notification.ci.no.branch"),
            )
            return
        }

        val ownerRepo = GitUtil.getRemoteOwnerRepo(project)
        if (ownerRepo == null) {
            StrykerNotifications.notifyError(
                project,
                StrykerBundle.message("notification.ci.fetch.title"),
                StrykerBundle.message("notification.ci.no.remote"),
            )
            return
        }

        ProgressManager.getInstance().run(object : Task.Backgroundable(
            project,
            StrykerBundle.message("notification.ci.fetching", branch),
            true,
        ) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                indicator.text = StrykerBundle.message("notification.ci.fetching", branch)

                // Single CI report dir — delete previous before downloading
                val service = MutationResultService.getInstance(project)
                service.deleteCiReportDir()

                val downloadDir = File(basePath, MutationResultService.CI_REPORT_DIR)

                val result = GitHubArtifactClient.fetchMutationReportForBranch(ownerRepo, branch, downloadDir)

                when (result) {
                    is GitHubArtifactClient.FetchResult.Success -> {
                        val report = try {
                            ReportParser.parse(result.reportFile)
                        } catch (_: Exception) {
                            null
                        }

                        if (report != null && report.files.isNotEmpty()) {
                            val configDir = PathUtil.findNearestStrykerConfigDir(
                                result.reportFile.absolutePath, basePath
                            ) ?: PathUtil.getReportConfigDir(result.reportFile)

                            // Use CI run timestamp so local reports don't overwrite it
                            val loaded = service.setResultsIfNewer(
                                report.files, configDir, result.runTimestamp
                            )

                            if (loaded) {
                                StrykerNotifications.notifyInfo(
                                    project,
                                    StrykerBundle.message("notification.ci.fetch.title"),
                                    StrykerBundle.message(
                                        "notification.ci.fetch.success",
                                        result.prNumber,
                                        report.files.size,
                                        result.artifactName,
                                    ),
                                )
                            } else {
                                StrykerNotifications.notifyWarning(
                                    project,
                                    StrykerBundle.message("notification.ci.fetch.title"),
                                    StrykerBundle.message("notification.ci.fetch.not.newer"),
                                )
                            }
                        } else {
                            StrykerNotifications.notifyWarning(
                                project,
                                StrykerBundle.message("notification.ci.fetch.title"),
                                StrykerBundle.message("notification.ci.fetch.empty"),
                            )
                        }
                    }
                    is GitHubArtifactClient.FetchResult.Error -> {
                        StrykerNotifications.notifyError(
                            project,
                            StrykerBundle.message("notification.ci.fetch.title"),
                            result.message,
                        )
                    }
                }
            }
        })
    }
}
