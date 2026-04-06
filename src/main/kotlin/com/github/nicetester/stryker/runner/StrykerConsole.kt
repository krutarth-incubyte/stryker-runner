package com.github.nicetester.stryker.runner

import com.github.nicetester.stryker.StrykerBundle
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.ui.ConsoleView
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.ContentFactory

object StrykerConsole {

    private const val TOOL_WINDOW_ID = "Stryker Output"

    fun getOrCreateConsole(project: Project): ConsoleView {
        val toolWindow = getOrCreateToolWindow(project)
        val existingConsole = findExistingConsole(toolWindow)
        if (existingConsole != null) return existingConsole

        return createAndAttachConsole(project, toolWindow)
    }

    private fun getOrCreateToolWindow(project: Project): ToolWindow {
        val manager = ToolWindowManager.getInstance(project)
        return manager.getToolWindow(TOOL_WINDOW_ID)
            ?: error("Tool window '$TOOL_WINDOW_ID' not registered in plugin.xml")
    }

    private fun findExistingConsole(toolWindow: ToolWindow): ConsoleView? {
        val contentManager = toolWindow.contentManager
        val existing = contentManager.findContent(
            StrykerBundle.message("console.tool.window.title")
        )
        return existing?.component as? ConsoleView
    }

    private fun createAndAttachConsole(project: Project, toolWindow: ToolWindow): ConsoleView {
        val console = TextConsoleBuilderFactory.getInstance()
            .createBuilder(project)
            .console

        val content = ContentFactory.getInstance().createContent(
            console.component,
            StrykerBundle.message("console.tool.window.title"),
            false
        )

        toolWindow.contentManager.removeAllContents(true)
        toolWindow.contentManager.addContent(content)

        return console
    }

    fun activateToolWindow(project: Project) {
        val manager = ToolWindowManager.getInstance(project)
        manager.getToolWindow(TOOL_WINDOW_ID)?.activate(null)
    }
}
