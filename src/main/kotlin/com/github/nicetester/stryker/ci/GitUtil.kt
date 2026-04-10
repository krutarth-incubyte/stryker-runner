package com.github.nicetester.stryker.ci

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.project.Project
import java.io.IOException

object GitUtil {

    fun getCurrentBranch(project: Project): String? {
        val basePath = project.basePath ?: return null
        return runGit(basePath, "rev-parse", "--abbrev-ref", "HEAD")
    }

    fun getRemoteOwnerRepo(project: Project): String? {
        val basePath = project.basePath ?: return null
        val remoteUrl = runGit(basePath, "remote", "get-url", "origin") ?: return null
        return parseOwnerRepo(remoteUrl)
    }

    fun parseOwnerRepo(remoteUrl: String): String? {
        // Handles both SSH and HTTPS formats:
        // git@github.com:owner/repo.git -> owner/repo
        // https://github.com/owner/repo.git -> owner/repo
        val sshMatch = Regex("""git@github\.com:(.+/.+?)(?:\.git)?$""").find(remoteUrl.trim())
        if (sshMatch != null) return sshMatch.groupValues[1]

        val httpsMatch = Regex("""https://github\.com/(.+/.+?)(?:\.git)?$""").find(remoteUrl.trim())
        if (httpsMatch != null) return httpsMatch.groupValues[1]

        return null
    }

    private fun runGit(workingDir: String, vararg args: String): String? =
        try {
            val process = GeneralCommandLine("git", *args)
                .withWorkDirectory(workingDir)
                .withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)
                .createProcess()
            val output = process.inputStream.bufferedReader().readText().trim()
            process.waitFor()
            if (process.exitValue() == 0 && output.isNotEmpty()) output else null
        } catch (_: IOException) {
            null
        }
}
