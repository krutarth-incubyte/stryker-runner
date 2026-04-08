package com.github.nicetester.stryker.annotator

import com.github.nicetester.stryker.StrykerBundle
import com.github.nicetester.stryker.model.MutantResult
import com.github.nicetester.stryker.service.MutationResultService
import com.github.nicetester.stryker.util.PathUtil
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile

class MutantAnnotator : ExternalAnnotator<List<MutantResult>, List<MutantResult>>() {

    override fun collectInformation(file: PsiFile, editor: Editor, hasErrors: Boolean): List<MutantResult> {
        val project = file.project
        val filePath = file.virtualFile?.path ?: return emptyList()
        val service = MutationResultService.getInstance(project)

        // Try cached results first
        val configDir = service.strykerConfigDir
        val relativePath = resolveRelativePath(configDir, project, file)
        if (relativePath != null) {
            val cached = service.getSurvivedResultsForFile(relativePath)
            if (cached.isNotEmpty()) return cached
        }

        // Auto-discover and load from existing report on disk
        if (service.tryAutoLoadReport(filePath)) {
            val newConfigDir = service.strykerConfigDir
            val newRelativePath = resolveRelativePath(newConfigDir, project, file) ?: return emptyList()
            return service.getSurvivedResultsForFile(newRelativePath)
        }

        return emptyList()
    }

    override fun doAnnotate(collectedInfo: List<MutantResult>): List<MutantResult> = collectedInfo

    override fun apply(file: PsiFile, results: List<MutantResult>, holder: AnnotationHolder) {
        if (results.isEmpty()) return

        val document = file.viewProvider.document ?: return
        val mutantsByLine = results.groupBy { it.location.startLine }

        for ((line, mutants) in mutantsByLine) {
            val zeroBasedLine = line - 1
            if (zeroBasedLine < 0 || zeroBasedLine >= document.lineCount) continue

            for (mutant in mutants) {
                val range = computeAnnotationRange(document, zeroBasedLine, mutant)
                val tooltip = formatMutantTooltip(mutant)

                holder.newAnnotation(HighlightSeverity.WARNING, tooltip)
                    .range(range)
                    .gutterIconRenderer(MutantGutterIconRenderer(zeroBasedLine, mutants))
                    .create()
            }
        }
    }

    private fun computeAnnotationRange(document: Document, zeroBasedLine: Int, mutant: MutantResult): TextRange {
        val lineStartOffset = document.getLineStartOffset(zeroBasedLine)
        val lineEndOffset = document.getLineEndOffset(zeroBasedLine)
        val startColumn = (mutant.location.startColumn - 1).coerceAtLeast(0)
        val endColumn = (mutant.location.endColumn - 1).coerceAtLeast(startColumn)
        val startOffset = (lineStartOffset + startColumn).coerceAtMost(lineEndOffset)
        val endOffset = (lineStartOffset + endColumn).coerceAtMost(lineEndOffset)
        return TextRange(startOffset, maxOf(endOffset, startOffset))
    }

    private fun resolveRelativePath(configDir: String?, project: Project, file: PsiFile): String? {
        val filePath = file.virtualFile?.path ?: return null
        val basePath = configDir ?: project.basePath ?: return null
        val relative = PathUtil.computeRelativePath(basePath, filePath)
        return if (relative == filePath) null else relative
    }

    companion object {
        fun formatMutantTooltip(mutant: MutantResult): String =
            StrykerBundle.message(
                "annotator.mutant.tooltip",
                mutant.mutatorName,
                mutant.replacement,
            )

        fun formatGutterTooltip(mutants: List<MutantResult>): String =
            mutants.joinToString("\n") { formatMutantTooltip(it) }
    }
}
