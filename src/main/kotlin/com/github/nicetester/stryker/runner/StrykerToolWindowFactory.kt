package com.github.nicetester.stryker.runner

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory

class StrykerToolWindowFactory : ToolWindowFactory, DumbAware {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        // Content is created on demand by StrykerConsole when a run starts
    }

    override fun shouldBeAvailable(project: Project): Boolean = true
}
