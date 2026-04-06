package com.github.nicetester.stryker.model

import com.google.gson.JsonParser
import java.io.File

object ReportParser {

    fun parse(reportFile: File): MutationReport? {
        if (!reportFile.exists()) return null

        val jsonText = reportFile.readText()
        return parseJson(jsonText)
    }

    fun parseJson(jsonText: String): MutationReport {
        val root = JsonParser.parseString(jsonText).asJsonObject
        val filesObject = root.getAsJsonObject("files")
        val filesMap = mutableMapOf<String, List<MutantResult>>()

        for ((filePath, fileElement) in filesObject.entrySet()) {
            val mutants = parseMutants(fileElement.asJsonObject.getAsJsonArray("mutants"))
            if (mutants.isNotEmpty()) {
                filesMap[filePath] = mutants
            }
        }

        return MutationReport(filesMap)
    }

    private fun parseMutants(mutantsArray: com.google.gson.JsonArray): List<MutantResult> =
        mutantsArray.map { element ->
            val obj = element.asJsonObject
            val location = obj.getAsJsonObject("location")
            val start = location.getAsJsonObject("start")
            val end = location.getAsJsonObject("end")

            MutantResult(
                id = obj.get("id").asString,
                mutatorName = obj.get("mutatorName").asString,
                replacement = obj.get("replacement").asString,
                location = MutantLocation(
                    startLine = start.get("line").asInt,
                    startColumn = start.get("column").asInt,
                    endLine = end.get("line").asInt,
                    endColumn = end.get("column").asInt,
                ),
                status = MutantStatus.fromString(obj.get("status").asString),
            )
        }
}
