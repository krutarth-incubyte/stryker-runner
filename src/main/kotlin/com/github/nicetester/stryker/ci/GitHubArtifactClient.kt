package com.github.nicetester.stryker.ci

import com.google.gson.JsonParser
import com.intellij.execution.configurations.GeneralCommandLine
import java.io.File
import java.io.IOException
import java.time.Instant

/**
 * Uses the `gh` CLI to interact with GitHub Actions artifacts.
 * Requires `gh` to be installed and authenticated.
 */
object GitHubArtifactClient {

    /**
     * Finds and downloads the mutation report artifact from the PR associated with the given branch.
     *
     * Flow:
     * 1. Find the PR number for the branch
     * 2. Get the latest completed workflow run for that PR (with its timestamp)
     * 3. Find an artifact containing "mutation"/"stryker" in its name
     * 4. Download and extract it
     * 5. Return the path to mutation.json + run timestamp for freshness comparison
     */
    fun fetchMutationReportForBranch(
        ownerRepo: String,
        branch: String,
        downloadDir: File,
    ): FetchResult {
        if (!isGhAvailable()) {
            return FetchResult.Error("GitHub CLI (gh) is not installed or not on PATH. Install it from https://cli.github.com")
        }

        val prNumber = findPrNumber(ownerRepo, branch)
            ?: return FetchResult.Error("No open PR found for branch '$branch' in $ownerRepo")

        val runInfo = findLatestWorkflowRun(ownerRepo, prNumber)
            ?: return FetchResult.Error("No completed workflow runs found for PR #$prNumber")

        val artifactName = findMutationArtifact(ownerRepo, runInfo.id)
            ?: return FetchResult.Error("No mutation report artifact found in workflow run #${runInfo.id}. Make sure your CI uploads an artifact containing 'mutation' in its name.")

        downloadDir.mkdirs()
        val downloaded = downloadArtifact(ownerRepo, artifactName, runInfo.id, downloadDir)
            ?: return FetchResult.Error("Failed to download artifact '$artifactName'")

        val mutationJson = findMutationJson(downloaded)
            ?: return FetchResult.Error("Downloaded artifact '$artifactName' does not contain a mutation.json file")

        return FetchResult.Success(mutationJson, prNumber, artifactName, runInfo.timestamp)
    }

    private fun findPrNumber(ownerRepo: String, branch: String): Int? {
        val output = runGh("pr", "view", branch, "--repo", ownerRepo, "--json", "number", "-q", ".number")
            ?: return null
        return output.trim().toIntOrNull()
    }

    private data class RunInfo(val id: Long, val timestamp: Long)

    private fun findLatestWorkflowRun(ownerRepo: String, prNumber: Int): RunInfo? {
        val runsJson = runGh(
            "api", "repos/$ownerRepo/actions/runs?event=pull_request&status=completed&per_page=10",
        ) ?: return null

        val runs = try {
            JsonParser.parseString(runsJson).asJsonObject.getAsJsonArray("workflow_runs")
        } catch (_: Exception) {
            return null
        }

        for (run in runs) {
            val runObj = run.asJsonObject
            val pullRequests = runObj.getAsJsonArray("pull_requests")
            for (pr in pullRequests) {
                if (pr.asJsonObject.get("number").asInt == prNumber) {
                    val id = runObj.get("id").asLong
                    val updatedAt = runObj.get("updated_at")?.asString
                    val timestamp = updatedAt?.let {
                        runCatching { Instant.parse(it).toEpochMilli() }.getOrNull()
                    } ?: System.currentTimeMillis()
                    return RunInfo(id, timestamp)
                }
            }
        }

        return null
    }

    private fun findMutationArtifact(ownerRepo: String, runId: Long): String? {
        val artifactsJson = runGh("api", "repos/$ownerRepo/actions/runs/$runId/artifacts") ?: return null

        val artifacts = try {
            JsonParser.parseString(artifactsJson).asJsonObject.getAsJsonArray("artifacts")
        } catch (_: Exception) {
            return null
        }

        for (artifact in artifacts) {
            val name = artifact.asJsonObject.get("name").asString
            if (name.contains("mutation", ignoreCase = true) || name.contains("stryker", ignoreCase = true)) {
                return name
            }
        }

        return null
    }

    private fun downloadArtifact(ownerRepo: String, artifactName: String, runId: Long, downloadDir: File): File? {
        runGh("run", "download", runId.toString(), "--repo", ownerRepo, "--name", artifactName, "--dir", downloadDir.absolutePath)
        return if (downloadDir.exists() && downloadDir.listFiles()?.isNotEmpty() == true) downloadDir else null
    }

    private fun findMutationJson(dir: File): File? =
        dir.walkTopDown().find { it.name == "mutation.json" && it.isFile }

    fun isGhAvailable(): Boolean =
        try {
            val process = GeneralCommandLine("gh", "--version")
                .withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)
                .createProcess()
            process.waitFor()
            process.exitValue() == 0
        } catch (_: IOException) {
            false
        }

    private fun runGh(vararg args: String): String? =
        try {
            val process = GeneralCommandLine("gh", *args)
                .withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)
                .createProcess()
            val output = process.inputStream.bufferedReader().readText().trim()
            process.waitFor()
            if (process.exitValue() == 0) output else null
        } catch (_: IOException) {
            null
        }

    sealed class FetchResult {
        data class Success(
            val reportFile: File,
            val prNumber: Int,
            val artifactName: String,
            val runTimestamp: Long,
        ) : FetchResult()
        data class Error(val message: String) : FetchResult()
    }
}
