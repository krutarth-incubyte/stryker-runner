package com.github.nicetester.stryker.action

import com.github.nicetester.stryker.StrykerBundle
import com.github.nicetester.stryker.service.MutationResultService
import com.github.nicetester.stryker.util.StrykerNotifications
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class ClearStrykerResultsAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val project = e.project ?: run {
            e.presentation.isEnabledAndVisible = false
            return
        }
        // Only show if there are results loaded
        val service = MutationResultService.getInstance(project)
        e.presentation.isEnabledAndVisible = service.reportTimestamp > 0L
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val service = MutationResultService.getInstance(project)

        // Clear in-memory results (removes annotations)
        service.clearResults()

        // Delete downloaded CI report from disk
        service.deleteCiReportDir()

        StrykerNotifications.notifyInfo(
            project,
            StrykerBundle.message("notification.clear.title"),
            StrykerBundle.message("notification.clear.content"),
        )
    }
}
