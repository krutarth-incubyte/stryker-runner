package com.github.nicetester.stryker.annotator

import com.github.nicetester.stryker.model.MutantResult
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.markup.GutterIconRenderer
import javax.swing.Icon

class MutantGutterIconRenderer(
    private val line: Int,
    private val mutants: List<MutantResult>,
) : GutterIconRenderer() {

    override fun getIcon(): Icon = AllIcons.General.Warning

    override fun getTooltipText(): String = MutantAnnotator.formatGutterTooltip(mutants)

    override fun equals(other: Any?): Boolean =
        other is MutantGutterIconRenderer && other.line == line && other.mutants == mutants

    override fun hashCode(): Int = 31 * line + mutants.hashCode()
}
