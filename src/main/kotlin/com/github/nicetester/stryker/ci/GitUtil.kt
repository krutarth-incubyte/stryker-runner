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
        val url = remoteUrl.trim()

        // Standard SSH: git@github.com:owner/repo.git
        // Org alias SSH: org-12345@github.com:owner/repo.git
        val sshMatch = Regex("""[^@]+@github\.com:(.+/.+?)(?:\.git)?$""").find(url)
        if (sshMatch != null) return sshMatch.groupValues[1]

        // HTTPS: https://github.com/owner/repo.git
        val httpsMatch = Regex("""https://github\.com/(.+/.+?)(?:\.git)?$""").find(url)
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
