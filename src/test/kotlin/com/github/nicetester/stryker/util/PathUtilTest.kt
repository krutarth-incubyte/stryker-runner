package com.github.nicetester.stryker.util

import junit.framework.TestCase
import java.io.File

class PathUtilTest : TestCase() {

    private lateinit var tempDir: File

    override fun setUp() {
        super.setUp()
        tempDir = File(System.getProperty("java.io.tmpdir"), "stryker-pathutil-test-${System.nanoTime()}")
        tempDir.mkdirs()
    }

    override fun tearDown() {
        tempDir.deleteRecursively()
        super.tearDown()
    }

    // --- findNearestStrykerConfigDir ---

    fun testFindsConfigInSameDirectory() {
        val projectDir = File(tempDir, "project").apply { mkdirs() }
        File(projectDir, "stryker.conf.js").createNewFile()
        val sourceFile = File(projectDir, "app.ts").apply { createNewFile() }

        val result = PathUtil.findNearestStrykerConfigDir(sourceFile.path, projectDir.path)

        assertEquals(projectDir.canonicalPath, result)
    }

    fun testFindsConfigInParentDirectory() {
        val projectDir = File(tempDir, "project").apply { mkdirs() }
        File(projectDir, "stryker.conf.js").createNewFile()
        val srcDir = File(projectDir, "src").apply { mkdirs() }
        val sourceFile = File(srcDir, "app.ts").apply { createNewFile() }

        val result = PathUtil.findNearestStrykerConfigDir(sourceFile.path, projectDir.path)

        assertEquals(projectDir.canonicalPath, result)
    }

    fun testFindsConfigInMonorepoSubpackage() {
        val projectDir = File(tempDir, "monorepo").apply { mkdirs() }
        val appDir = File(projectDir, "apps/my-app").apply { mkdirs() }
        File(appDir, "stryker.config.json").createNewFile()
        val srcDir = File(appDir, "src/features").apply { mkdirs() }
        val sourceFile = File(srcDir, "auth.ts").apply { createNewFile() }

        val result = PathUtil.findNearestStrykerConfigDir(sourceFile.path, projectDir.path)

        assertEquals(appDir.canonicalPath, result)
    }

    fun testReturnsNullWhenNoConfigFound() {
        val projectDir = File(tempDir, "no-config").apply { mkdirs() }
        val srcDir = File(projectDir, "src").apply { mkdirs() }
        val sourceFile = File(srcDir, "app.ts").apply { createNewFile() }

        val result = PathUtil.findNearestStrykerConfigDir(sourceFile.path, projectDir.path)

        assertNull("Should return null when no stryker config exists", result)
    }

    fun testDoesNotSearchAboveProjectRoot() {
        val projectDir = File(tempDir, "project").apply { mkdirs() }
        // Config is ABOVE the project root — should not be found
        File(tempDir, "stryker.conf.js").createNewFile()
        val sourceFile = File(projectDir, "app.ts").apply { createNewFile() }

        val result = PathUtil.findNearestStrykerConfigDir(sourceFile.path, projectDir.path)

        assertNull("Should not find config above project root", result)
    }

    fun testFindsNearestConfigWhenMultipleExist() {
        val projectDir = File(tempDir, "monorepo").apply { mkdirs() }
        File(projectDir, "stryker.conf.js").createNewFile() // root config
        val appDir = File(projectDir, "apps/my-app").apply { mkdirs() }
        File(appDir, "stryker.conf.js").createNewFile() // app-level config (nearer)
        val srcDir = File(appDir, "src").apply { mkdirs() }
        val sourceFile = File(srcDir, "app.ts").apply { createNewFile() }

        val result = PathUtil.findNearestStrykerConfigDir(sourceFile.path, projectDir.path)

        assertEquals("Should find nearest (app-level) config", appDir.canonicalPath, result)
    }

    fun testHandlesDirectoryAsInput() {
        val projectDir = File(tempDir, "project").apply { mkdirs() }
        File(projectDir, "stryker.conf.mjs").createNewFile()
        val srcDir = File(projectDir, "src").apply { mkdirs() }

        val result = PathUtil.findNearestStrykerConfigDir(srcDir.path, projectDir.path)

        assertEquals(projectDir.canonicalPath, result)
    }

    fun testRecognizesAllConfigFileNames() {
        val configNames = listOf(
            "stryker.conf.js", "stryker.conf.mjs", "stryker.conf.cjs",
            "stryker.conf.json", "stryker.config.json",
            ".stryker.conf.js", ".stryker.conf.mjs", ".stryker.conf.cjs",
        )

        for (configName in configNames) {
            val projectDir = File(tempDir, "test-$configName").apply { mkdirs() }
            File(projectDir, configName).createNewFile()
            val sourceFile = File(projectDir, "app.ts").apply { createNewFile() }

            val result = PathUtil.findNearestStrykerConfigDir(sourceFile.path, projectDir.path)

            assertNotNull("Should find config file: $configName", result)
        }
    }
}
