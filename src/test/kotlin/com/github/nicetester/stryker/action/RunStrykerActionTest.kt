package com.github.nicetester.stryker.action

import com.github.nicetester.stryker.util.PathUtil
import com.intellij.openapi.actionSystem.ActionUiKind
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.MapDataContext
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class RunStrykerActionTest : BasePlatformTestCase() {

    private lateinit var action: RunStrykerAction

    override fun setUp() {
        super.setUp()
        action = RunStrykerAction()
    }

    private fun createActionEvent(file: VirtualFile? = null): AnActionEvent {
        val dataContext = MapDataContext().apply {
            put(CommonDataKeys.PROJECT, project)
            if (file != null) put(CommonDataKeys.VIRTUAL_FILE, file)
        }
        return AnActionEvent.createEvent(dataContext, Presentation(), "", ActionUiKind.NONE, null)
    }

    // --- isSupportedFile tests ---

    fun testIsSupportedFileReturnsTrueForJsFile() {
        val file = myFixture.configureByText("app.js", "").virtualFile
        assertTrue(RunStrykerAction.isSupportedFile(file))
    }

    fun testIsSupportedFileReturnsTrueForTsFile() {
        val file = myFixture.configureByText("app.ts", "").virtualFile
        assertTrue(RunStrykerAction.isSupportedFile(file))
    }

    fun testIsSupportedFileReturnsTrueForJsxFile() {
        val file = myFixture.configureByText("component.jsx", "").virtualFile
        assertTrue(RunStrykerAction.isSupportedFile(file))
    }

    fun testIsSupportedFileReturnsTrueForTsxFile() {
        val file = myFixture.configureByText("component.tsx", "").virtualFile
        assertTrue(RunStrykerAction.isSupportedFile(file))
    }

    fun testIsSupportedFileReturnsTrueForMjsFile() {
        val file = myFixture.configureByText("module.mjs", "").virtualFile
        assertTrue(RunStrykerAction.isSupportedFile(file))
    }

    fun testIsSupportedFileReturnsTrueForMtsFile() {
        val file = myFixture.configureByText("module.mts", "").virtualFile
        assertTrue(RunStrykerAction.isSupportedFile(file))
    }

    fun testIsSupportedFileReturnsFalseForJsonFile() {
        val file = myFixture.configureByText("package.json", "{}").virtualFile
        assertFalse(RunStrykerAction.isSupportedFile(file))
    }

    fun testIsSupportedFileReturnsFalseForMarkdownFile() {
        val file = myFixture.configureByText("README.md", "").virtualFile
        assertFalse(RunStrykerAction.isSupportedFile(file))
    }

    fun testIsSupportedFileReturnsFalseForPythonFile() {
        val file = myFixture.configureByText("script.py", "").virtualFile
        assertFalse(RunStrykerAction.isSupportedFile(file))
    }

    fun testIsSupportedFileReturnsFalseForDirectory() {
        val dir = myFixture.tempDirFixture.findOrCreateDir("src")
        assertFalse(RunStrykerAction.isSupportedFile(dir))
    }

    // --- PathUtil.computeRelativePath tests ---

    fun testComputeRelativePathStripsBasePath() {
        val result = PathUtil.computeRelativePath("/home/user/project", "/home/user/project/src/app.ts")
        assertEquals("src/app.ts", result)
    }

    fun testComputeRelativePathHandlesTrailingSlashOnBasePath() {
        val result = PathUtil.computeRelativePath("/home/user/project/", "/home/user/project/src/app.ts")
        assertEquals("src/app.ts", result)
    }

    fun testComputeRelativePathReturnsFullPathWhenBaseDoesNotMatch() {
        val result = PathUtil.computeRelativePath("/home/user/other-project", "/home/user/project/src/app.ts")
        assertEquals("/home/user/project/src/app.ts", result)
    }

    fun testComputeRelativePathHandlesFileAtProjectRoot() {
        val result = PathUtil.computeRelativePath("/home/user/project", "/home/user/project/app.ts")
        assertEquals("app.ts", result)
    }

    fun testComputeRelativePathHandlesDeeplyNestedFile() {
        val result = PathUtil.computeRelativePath("/project", "/project/src/features/auth/login.spec.ts")
        assertEquals("src/features/auth/login.spec.ts", result)
    }

    // --- Action visibility (update) tests ---

    fun testActionIsVisibleForTypeScriptFile() {
        val file = myFixture.configureByText("app.ts", "").virtualFile
        val event = createActionEvent(file)

        action.update(event)

        assertTrue(event.presentation.isEnabledAndVisible)
    }

    fun testActionIsVisibleForJavaScriptFile() {
        val file = myFixture.configureByText("index.js", "").virtualFile
        val event = createActionEvent(file)

        action.update(event)

        assertTrue(event.presentation.isEnabledAndVisible)
    }

    fun testActionIsHiddenForUnsupportedFile() {
        val file = myFixture.configureByText("data.json", "{}").virtualFile
        val event = createActionEvent(file)

        action.update(event)

        assertFalse(event.presentation.isEnabledAndVisible)
    }

    fun testActionIsHiddenWhenNoFileSelected() {
        val event = createActionEvent()

        action.update(event)

        assertFalse(event.presentation.isEnabledAndVisible)
    }

    // --- isActionTarget tests ---

    fun testIsActionTargetReturnsTrueForDirectory() {
        val dir = myFixture.tempDirFixture.findOrCreateDir("src")
        assertTrue(RunStrykerAction.isActionTarget(dir))
    }

    fun testIsActionTargetReturnsTrueForJsFile() {
        val file = myFixture.configureByText("app.js", "").virtualFile
        assertTrue(RunStrykerAction.isActionTarget(file))
    }

    fun testIsActionTargetReturnsFalseForJsonFile() {
        val file = myFixture.configureByText("config.json", "{}").virtualFile
        assertFalse(RunStrykerAction.isActionTarget(file))
    }

    fun testIsActionTargetReturnsFalseForMarkdownFile() {
        val file = myFixture.configureByText("README.md", "").virtualFile
        assertFalse(RunStrykerAction.isActionTarget(file))
    }

    fun testIsActionTargetReturnsFalseForPngFile() {
        val file = myFixture.configureByText("image.png", "").virtualFile
        assertFalse(RunStrykerAction.isActionTarget(file))
    }

    fun testIsActionTargetReturnsFalseForCssFile() {
        val file = myFixture.configureByText("styles.css", "").virtualFile
        assertFalse(RunStrykerAction.isActionTarget(file))
    }

    // --- buildMutatePattern tests ---

    fun testBuildMutatePatternReturnsRelativePathForFile() {
        val file = myFixture.configureByText("app.ts", "").virtualFile
        val basePath = file.parent.path

        val result = RunStrykerAction.buildMutatePattern(basePath, file)

        assertEquals("app.ts", result)
    }

    fun testBuildMutatePatternAppendsFolderGlobForDirectory() {
        val dir = myFixture.tempDirFixture.findOrCreateDir("src")
        val basePath = dir.parent.path

        val result = RunStrykerAction.buildMutatePattern(basePath, dir)

        assertEquals("src/**/*.{js,ts,jsx,tsx,mjs,mts}", result)
    }

    fun testBuildMutatePatternHandlesNestedDirectory() {
        val dir = myFixture.tempDirFixture.findOrCreateDir("src/features/auth")
        val basePath = dir.parent.parent.parent.path

        val result = RunStrykerAction.buildMutatePattern(basePath, dir)

        assertEquals("src/features/auth/**/*.{js,ts,jsx,tsx,mjs,mts}", result)
    }

    fun testBuildMutatePatternHandlesNestedFile() {
        myFixture.tempDirFixture.findOrCreateDir("src/utils")
        val file = myFixture.addFileToProject("src/utils/helper.ts", "").virtualFile
        val basePath = file.parent.parent.parent.path

        val result = RunStrykerAction.buildMutatePattern(basePath, file)

        assertEquals("src/utils/helper.ts", result)
    }

    // --- Action visibility for folders ---

    fun testActionIsVisibleForDirectory() {
        val dir = myFixture.tempDirFixture.findOrCreateDir("src")
        val event = createActionEvent(dir)

        action.update(event)

        assertTrue(event.presentation.isEnabledAndVisible)
    }

    fun testActionIsHiddenForMarkdownFile() {
        val file = myFixture.configureByText("notes.md", "").virtualFile
        val event = createActionEvent(file)

        action.update(event)

        assertFalse(event.presentation.isEnabledAndVisible)
    }

    fun testActionIsHiddenForPngFile() {
        val file = myFixture.configureByText("logo.png", "").virtualFile
        val event = createActionEvent(file)

        action.update(event)

        assertFalse(event.presentation.isEnabledAndVisible)
    }

    fun testActionIsHiddenForCssFile() {
        val file = myFixture.configureByText("styles.css", "").virtualFile
        val event = createActionEvent(file)

        action.update(event)

        assertFalse(event.presentation.isEnabledAndVisible)
    }
}
