package com.github.nicetester.stryker.util

import java.io.File

object PathUtil {

    private val STRYKER_CONFIG_NAMES = listOf(
        "stryker.conf.js",
        "stryker.conf.mjs",
        "stryker.conf.cjs",
        "stryker.conf.json",
        "stryker.config.json",
        ".stryker.conf.js",
        ".stryker.conf.mjs",
        ".stryker.conf.cjs",
    )

    fun computeRelativePath(basePath: String, filePath: String): String {
        val normalizedBase = basePath.trimEnd('/')
        val normalizedFile = filePath.trimEnd('/')
        return if (normalizedFile.startsWith(normalizedBase)) {
            normalizedFile.removePrefix(normalizedBase).trimStart('/')
        } else {
            normalizedFile
        }
    }

    fun findNearestStrykerConfigDir(filePath: String, projectBasePath: String): String? {
        var dir = File(filePath).let { if (it.isDirectory) it else it.parentFile }
        val projectRoot = File(projectBasePath).canonicalPath
        while (dir != null && dir.canonicalPath.startsWith(projectRoot)) {
            if (STRYKER_CONFIG_NAMES.any { File(dir, it).exists() }) {
                return dir.canonicalPath
            }
            dir = dir.parentFile
        }
        return null
    }

    private const val REPORT_RELATIVE_PATH = "reports/mutation/mutation.json"

    fun findNearestReportFile(filePath: String, projectBasePath: String): File? {
        var dir = File(filePath).let { if (it.isDirectory) it else it.parentFile }
        val projectRoot = File(projectBasePath).canonicalPath
        while (dir != null && dir.canonicalPath.startsWith(projectRoot)) {
            val candidate = File(dir, REPORT_RELATIVE_PATH)
            if (candidate.exists()) return candidate
            dir = dir.parentFile
        }
        return null
    }

    fun getReportConfigDir(reportFile: File): String {
        // The report is at <configDir>/reports/mutation/mutation.json — go up 3 levels
        return reportFile.parentFile.parentFile.parentFile.canonicalPath
    }
}
