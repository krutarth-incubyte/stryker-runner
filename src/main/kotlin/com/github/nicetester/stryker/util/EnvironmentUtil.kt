package com.github.nicetester.stryker.util

import com.intellij.execution.configurations.GeneralCommandLine
import java.io.IOException

object EnvironmentUtil {

    fun npxCommand(): String = if (isWindows()) "npx.cmd" else "npx"

    fun isNpxAvailable(): Boolean =
        try {
            val process = GeneralCommandLine(npxCommand(), "--version")
                .withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)
                .createProcess()
            process.waitFor()
            process.exitValue() == 0
        } catch (_: IOException) {
            false
        }

    fun isWindows(): Boolean =
        System.getProperty("os.name").lowercase().contains("win")
}
