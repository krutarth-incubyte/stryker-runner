package com.github.nicetester.stryker.action

import com.github.nicetester.stryker.model.MutantLocation
import com.github.nicetester.stryker.model.MutantResult
import com.github.nicetester.stryker.model.MutantStatus
import com.github.nicetester.stryker.service.MutationResultService
import com.intellij.openapi.actionSystem.ActionUiKind
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.testFramework.MapDataContext
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.io.File

class ClearStrykerResultsActionTest : BasePlatformTestCase() {

    private lateinit var action: ClearStrykerResultsAction
    private lateinit var service: MutationResultService

    override fun setUp() {
        super.setUp()
        action = ClearStrykerResultsAction()
        service = MutationResultService.getInstance(project)
        service.clearResults()
    }

    // --- visibility ---

    fun testActionIsHiddenWhenNoResultsAreLoaded() {
        val event = createActionEvent()

        action.update(event)

        assertFalse("Action should be hidden when no results are loaded", event.presentation.isEnabledAndVisible)
    }

    fun testActionIsVisibleWhenResultsAreLoaded() {
        service.setResults(mapOf("src/app.ts" to listOf(survivedMutant())), null, 1000L)
        val event = createActionEvent()

        action.update(event)

        assertTrue("Action should be visible when results are loaded", event.presentation.isEnabledAndVisible)
    }

    fun testActionBecomesHiddenAfterResultsAreCleared() {
        service.setResults(mapOf("src/app.ts" to listOf(survivedMutant())), null, 1000L)

        service.clearResults()

        val event = createActionEvent()
        action.update(event)
        assertFalse("Action should be hidden after results are cleared", event.presentation.isEnabledAndVisible)
    }

    // --- actionPerformed: clears results ---

    fun testClearActionRemovesAnnotationDataFromService() {
        service.setResults(mapOf("src/app.ts" to listOf(survivedMutant())), null, 1000L)
        val event = createActionEvent()

        action.actionPerformed(event)

        assertTrue("Results should be empty after clear", service.getResultsForFile("src/app.ts").isEmpty())
    }

    fun testClearActionResetsTimestampToZero() {
        service.setResults(mapOf("src/app.ts" to listOf(survivedMutant())), null, 5000L)
        val event = createActionEvent()

        action.actionPerformed(event)

        assertEquals("Timestamp should be 0 after clear", 0L, service.reportTimestamp)
    }

    fun testClearActionResetsConfigDir() {
        service.setResults(mapOf("src/app.ts" to listOf(survivedMutant())), "/some/config/dir", 1000L)
        val event = createActionEvent()

        action.actionPerformed(event)

        assertNull("Config dir should be null after clear", service.strykerConfigDir)
    }

    // --- actionPerformed: deletes CI report from disk ---

    fun testClearActionDeletesCiReportDirectoryFromDisk() {
        val ciDir = File(project.basePath!!, MutationResultService.CI_REPORT_DIR).also {
            it.mkdirs()
            File(it, "mutation.json").writeText("{}")
        }
        service.setResults(mapOf("src/app.ts" to listOf(survivedMutant())), null, 1000L)
        val event = createActionEvent()

        action.actionPerformed(event)

        assertFalse("CI report directory should be deleted after clear", ciDir.exists())
    }

    fun testClearActionDoesNotFailWhenCiReportDirectoryDoesNotExist() {
        service.setResults(mapOf("src/app.ts" to listOf(survivedMutant())), null, 1000L)
        val event = createActionEvent()

        // Should not throw even if CI dir doesn't exist
        action.actionPerformed(event)

        assertTrue("Results should still be cleared", service.getResultsForFile("src/app.ts").isEmpty())
    }

    // --- helpers ---

    private fun survivedMutant() =
        MutantResult("1", "BooleanLiteral", "true", MutantLocation(1, 1, 1, 5), MutantStatus.Survived)

    private fun createActionEvent(): AnActionEvent {
        val dataContext = MapDataContext().apply {
            put(CommonDataKeys.PROJECT, project)
        }
        return AnActionEvent.createEvent(dataContext, Presentation(), "", ActionUiKind.NONE, null)
    }
}
