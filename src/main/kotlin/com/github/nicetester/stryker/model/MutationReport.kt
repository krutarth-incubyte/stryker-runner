package com.github.nicetester.stryker.model

enum class MutantStatus {
    Survived,
    Killed,
    Timeout,
    NoCoverage,
    CompileError,
    RuntimeError,
    Ignored;

    companion object {
        fun fromString(value: String): MutantStatus =
            entries.find { it.name.equals(value, ignoreCase = true) } ?: Ignored
    }
}

data class MutantLocation(
    val startLine: Int,
    val startColumn: Int,
    val endLine: Int,
    val endColumn: Int,
)

data class MutantResult(
    val id: String,
    val mutatorName: String,
    val replacement: String,
    val location: MutantLocation,
    val status: MutantStatus,
)

data class MutationReport(
    val files: Map<String, List<MutantResult>>,
)
