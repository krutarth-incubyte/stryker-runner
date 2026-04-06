package com.github.nicetester.stryker.action

import com.github.nicetester.stryker.StrykerBundle
import com.github.nicetester.stryker.runner.StrykerRunner
import com.github.nicetester.stryker.service.MutationResultService
import com.github.nicetester.stryker.util.EnvironmentUtil
import com.github.nicetester.stryker.util.PathUtil
import com.github.nicetester.stryker.util.StrykerNotifications
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.vfs.VirtualFile

class RunStrykerAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible = file != null && isActionTarget(file)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val basePath = project.basePath ?: return

        val service = MutationResultService.getInstance(project)
        if (!service.tryMarkRunning()) {
            StrykerNotifications.notifyWarning(
                project,
                StrykerBundle.message("notification.stryker.already.running.title"),
                StrykerBundle.message("notification.stryker.already.running.content"),
            )
            return
        }

        if (!EnvironmentUtil.isNpxAvailable()) {
            service.markFinished()
            StrykerNotifications.notifyError(
                project,
                StrykerBundle.message("notification.npx.not.found.title"),
                StrykerBundle.message("notification.npx.not.found.content"),
            )
            return
        }

        val strykerConfigDir = PathUtil.findNearestStrykerConfigDir(file.path, basePath)
        if (strykerConfigDir == null) {
            service.markFinished()
            StrykerNotifications.notifyError(
                project,
                StrykerBundle.message("notification.stryker.no.config.title"),
                StrykerBundle.message("notification.stryker.no.config.content"),
            )
            return
        }

        val mutatePattern = buildMutatePattern(strykerConfigDir, file)
        val runner = StrykerRunner(project, mutatePattern, strykerConfigDir)
        ProgressManager.getInstance().run(runner)
    }

    companion object {
        private val SUPPORTED_EXTENSIONS = setOf("js", "ts", "jsx", "tsx", "mjs", "mts")
        private const val FOLDER_GLOB_SUFFIX = "/**/*.{js,ts,jsx,tsx,mjs,mts}"

        fun isActionTarget(file: VirtualFile): Boolean =
            file.isDirectory || isSupportedFile(file)

        fun isSupportedFile(file: VirtualFile): Boolean =
            !file.isDirectory && file.extension?.lowercase() in SUPPORTED_EXTENSIONS

        fun buildMutatePattern(basePath: String, file: VirtualFile): String {
            val relativePath = PathUtil.computeRelativePath(basePath, file.path)
            if (file.isDirectory) {
                return relativePath + FOLDER_GLOB_SUFFIX
            }
            return relativePath
        }
    }
}
