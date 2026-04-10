package com.github.nicetester.stryker.ci

import junit.framework.TestCase
import java.io.File

class GitHubArtifactClientTest : TestCase() {

    // --- parseWorkflowRunsJson ---

    fun testFindsRunMatchingPrNumber() {
        val json = workflowRunsJson(prNumber = 42, runId = 1001, updatedAt = "2024-01-15T10:00:00Z")

        val result = GitHubArtifactClient.parseWorkflowRunsJson(json, prNumber = 42)

        assertNotNull(result)
        assertEquals(1001L, result!!.id)
    }

    fun testRunTimestampIsParsedFromUpdatedAt() {
        val json = workflowRunsJson(prNumber = 42, runId = 1001, updatedAt = "2024-01-15T10:00:00Z")

        val result = GitHubArtifactClient.parseWorkflowRunsJson(json, prNumber = 42)

        assertNotNull(result)
        assertTrue("Timestamp should be epoch millis > 0", result!!.timestamp > 0L)
        assertEquals(1705312800000L, result.timestamp)
    }

    fun testReturnsNullWhenNoPrMatchesRequestedNumber() {
        val json = workflowRunsJson(prNumber = 99, runId = 1001, updatedAt = "2024-01-15T10:00:00Z")

        val result = GitHubArtifactClient.parseWorkflowRunsJson(json, prNumber = 42)

        assertNull("Should return null when no run matches the PR number", result)
    }

    fun testReturnsNullWhenWorkflowRunsArrayIsEmpty() {
        val json = """{"workflow_runs": []}"""

        val result = GitHubArtifactClient.parseWorkflowRunsJson(json, prNumber = 42)

        assertNull("Should return null when workflow_runs is empty", result)
    }

    fun testReturnsNullForInvalidJson() {
        val result = GitHubArtifactClient.parseWorkflowRunsJson("not valid json", prNumber = 42)

        assertNull("Should return null for invalid JSON", result)
    }

    fun testReturnsNullWhenPullRequestsArrayIsEmpty() {
        val json = """
        {
          "workflow_runs": [
            {
              "id": 1001,
              "updated_at": "2024-01-15T10:00:00Z",
              "pull_requests": []
            }
          ]
        }
        """.trimIndent()

        val result = GitHubArtifactClient.parseWorkflowRunsJson(json, prNumber = 42)

        assertNull("Should return null when pull_requests array is empty", result)
    }

    fun testFindsCorrectRunWhenMultipleRunsExist() {
        val json = """
        {
          "workflow_runs": [
            {
              "id": 2000,
              "updated_at": "2024-01-15T10:00:00Z",
              "pull_requests": [{"number": 10}]
            },
            {
              "id": 3000,
              "updated_at": "2024-01-16T10:00:00Z",
              "pull_requests": [{"number": 42}]
            }
          ]
        }
        """.trimIndent()

        val result = GitHubArtifactClient.parseWorkflowRunsJson(json, prNumber = 42)

        assertNotNull(result)
        assertEquals(3000L, result!!.id)
    }

    // --- parseArtifactsJson ---

    fun testFindsMutationArtifactByExactName() {
        val json = artifactsJson("mutation-report")

        val result = GitHubArtifactClient.parseArtifactsJson(json)

        assertEquals("mutation-report", result)
    }

    fun testFindsMutationArtifactWithMutationInName() {
        val json = artifactsJson("stryker-mutation-results")

        val result = GitHubArtifactClient.parseArtifactsJson(json)

        assertEquals("stryker-mutation-results", result)
    }

    fun testFindsMutationArtifactWithStrykerInName() {
        val json = artifactsJson("stryker-output")

        val result = GitHubArtifactClient.parseArtifactsJson(json)

        assertEquals("stryker-output", result)
    }

    fun testArtifactMatchingIsCaseInsensitive() {
        val json = artifactsJson("Mutation-Report")

        val result = GitHubArtifactClient.parseArtifactsJson(json)

        assertEquals("Mutation-Report", result)
    }

    fun testReturnsNullWhenNoArtifactMatchesMutationOrStryker() {
        val json = artifactsJson("coverage-report")

        val result = GitHubArtifactClient.parseArtifactsJson(json)

        assertNull("Should return null when no artifact contains 'mutation' or 'stryker'", result)
    }

    fun testReturnsNullWhenArtifactsArrayIsEmpty() {
        val json = """{"artifacts": []}"""

        val result = GitHubArtifactClient.parseArtifactsJson(json)

        assertNull("Should return null when artifacts array is empty", result)
    }

    fun testReturnsFirstMatchingArtifactWhenMultipleMatch() {
        val json = """
        {
          "artifacts": [
            {"name": "test-results"},
            {"name": "mutation-report"},
            {"name": "stryker-output"}
          ]
        }
        """.trimIndent()

        val result = GitHubArtifactClient.parseArtifactsJson(json)

        assertEquals("mutation-report", result)
    }

    fun testSkipsNonMatchingArtifactsBeforeFindingMatch() {
        val json = """
        {
          "artifacts": [
            {"name": "coverage"},
            {"name": "build-logs"},
            {"name": "mutation-results"}
          ]
        }
        """.trimIndent()

        val result = GitHubArtifactClient.parseArtifactsJson(json)

        assertEquals("mutation-results", result)
    }

    fun testReturnsNullForInvalidArtifactsJson() {
        val result = GitHubArtifactClient.parseArtifactsJson("{bad json")

        assertNull("Should return null for invalid JSON", result)
    }

    // --- findMutationJsonInDir ---

    fun testFindsMutationJsonAtTopLevel() {
        val dir = createTempDir()
        try {
            val expected = File(dir, "mutation.json").apply { writeText("{}") }

            val result = GitHubArtifactClient.findMutationJsonInDir(dir)

            assertNotNull(result)
            assertEquals(expected.canonicalPath, result!!.canonicalPath)
        } finally {
            dir.deleteRecursively()
        }
    }

    fun testFindsMutationJsonInNestedSubdirectory() {
        val dir = createTempDir()
        try {
            val subDir = File(dir, "reports/mutation").apply { mkdirs() }
            val expected = File(subDir, "mutation.json").apply { writeText("{}") }

            val result = GitHubArtifactClient.findMutationJsonInDir(dir)

            assertNotNull(result)
            assertEquals(expected.canonicalPath, result!!.canonicalPath)
        } finally {
            dir.deleteRecursively()
        }
    }

    fun testReturnsNullWhenNoMutationJsonExists() {
        val dir = createTempDir()
        try {
            File(dir, "other-file.json").writeText("{}")

            val result = GitHubArtifactClient.findMutationJsonInDir(dir)

            assertNull("Should return null when no mutation.json exists in the directory", result)
        } finally {
            dir.deleteRecursively()
        }
    }

    fun testIgnoresDirectoriesNamedMutationJson() {
        val dir = createTempDir()
        try {
            File(dir, "mutation.json").mkdirs() // directory, not file

            val result = GitHubArtifactClient.findMutationJsonInDir(dir)

            assertNull("Should ignore directories named mutation.json", result)
        } finally {
            dir.deleteRecursively()
        }
    }

    // --- helpers ---

    private fun workflowRunsJson(prNumber: Int, runId: Long, updatedAt: String) = """
        {
          "workflow_runs": [
            {
              "id": $runId,
              "updated_at": "$updatedAt",
              "pull_requests": [{"number": $prNumber}]
            }
          ]
        }
    """.trimIndent()

    private fun artifactsJson(name: String) = """
        {
          "artifacts": [
            {"name": "$name"}
          ]
        }
    """.trimIndent()
}
