package com.github.nicetester.stryker.annotator

import com.github.nicetester.stryker.model.MutantLocation
import com.github.nicetester.stryker.model.MutantResult
import com.github.nicetester.stryker.model.MutantStatus
import com.github.nicetester.stryker.service.MutationResultService
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class MutantAnnotatorTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        MutationResultService.getInstance(project).clearResults()
    }

    // --- formatMutantTooltip ---

    fun testFormatMutantTooltipContainsMutatorNameAndReplacement() {
        val mutant = MutantResult(
            id = "1",
            mutatorName = "ConditionalExpression",
            replacement = "false",
            location = MutantLocation(5, 10, 5, 20),
            status = MutantStatus.Survived,
        )

        val tooltip = MutantAnnotator.formatMutantTooltip(mutant)

        assertTrue("Tooltip should contain mutator name", tooltip.contains("ConditionalExpression"))
        assertTrue("Tooltip should contain replacement", tooltip.contains("false"))
    }

    fun testFormatMutantTooltipContainsReplacementOperator() {
        val mutant = MutantResult(
            id = "2",
            mutatorName = "ArithmeticOperator",
            replacement = "-",
            location = MutantLocation(10, 15, 10, 16),
            status = MutantStatus.Survived,
        )

        val tooltip = MutantAnnotator.formatMutantTooltip(mutant)

        assertTrue("Tooltip should contain mutator name", tooltip.contains("ArithmeticOperator"))
        assertTrue("Tooltip should contain replacement operator", tooltip.contains("-"))
    }

    // --- formatGutterTooltip ---

    fun testFormatGutterTooltipWithSingleMutant() {
        val mutant = MutantResult(
            id = "1",
            mutatorName = "BooleanLiteral",
            replacement = "true",
            location = MutantLocation(3, 1, 3, 5),
            status = MutantStatus.Survived,
        )

        val tooltip = MutantAnnotator.formatGutterTooltip(listOf(mutant))

        assertTrue("Gutter tooltip should contain mutator name", tooltip.contains("BooleanLiteral"))
        assertTrue("Gutter tooltip should contain replacement", tooltip.contains("true"))
    }

    fun testFormatGutterTooltipWithMultipleMutantsOnSameLine() {
        val mutant1 = MutantResult(
            id = "1",
            mutatorName = "ConditionalExpression",
            replacement = "||",
            location = MutantLocation(5, 1, 5, 10),
            status = MutantStatus.Survived,
        )
        val mutant2 = MutantResult(
            id = "2",
            mutatorName = "ArithmeticOperator",
            replacement = "+",
            location = MutantLocation(5, 15, 5, 16),
            status = MutantStatus.Survived,
        )

        val tooltip = MutantAnnotator.formatGutterTooltip(listOf(mutant1, mutant2))

        assertTrue("Should contain first mutator", tooltip.contains("ConditionalExpression"))
        assertTrue("Should contain second mutator", tooltip.contains("ArithmeticOperator"))
        assertTrue("Multiple mutants should be on separate lines", tooltip.contains("\n"))
    }

    fun testFormatGutterTooltipWithEmptyListReturnsEmpty() {
        val tooltip = MutantAnnotator.formatGutterTooltip(emptyList())

        assertEquals("", tooltip)
    }

    // --- MutantGutterIconRenderer ---

    fun testGutterIconRendererEqualityByLineAndMutants() {
        val mutant = MutantResult("1", "Test", "x", MutantLocation(1, 1, 1, 5), MutantStatus.Survived)

        val renderer1 = MutantGutterIconRenderer(4, listOf(mutant))
        val renderer2 = MutantGutterIconRenderer(4, listOf(mutant))

        assertEquals("Renderers with same line and mutants should be equal", renderer1, renderer2)
    }

    fun testGutterIconRendererInequalityByLine() {
        val mutant = MutantResult("1", "Test", "x", MutantLocation(1, 1, 1, 5), MutantStatus.Survived)

        val renderer1 = MutantGutterIconRenderer(4, listOf(mutant))
        val renderer2 = MutantGutterIconRenderer(7, listOf(mutant))

        assertFalse("Renderers on different lines should not be equal", renderer1 == renderer2)
    }

    fun testGutterIconRendererHashCodeConsistentWithEquals() {
        val mutant = MutantResult("1", "Test", "x", MutantLocation(1, 1, 1, 5), MutantStatus.Survived)

        val renderer1 = MutantGutterIconRenderer(4, listOf(mutant))
        val renderer2 = MutantGutterIconRenderer(4, listOf(mutant))

        assertEquals("Equal renderers should have same hashCode", renderer1.hashCode(), renderer2.hashCode())
    }

    fun testGutterIconRendererTooltipTextMatchesFormatGutterTooltip() {
        val mutant1 = MutantResult("1", "ConditionalExpression", "||", MutantLocation(5, 1, 5, 10), MutantStatus.Survived)
        val mutant2 = MutantResult("2", "ArithmeticOperator", "+", MutantLocation(5, 15, 5, 16), MutantStatus.Survived)
        val mutants = listOf(mutant1, mutant2)

        val renderer = MutantGutterIconRenderer(4, mutants)

        assertEquals(MutantAnnotator.formatGutterTooltip(mutants), renderer.tooltipText)
    }

    fun testGutterIconRendererHasIcon() {
        val mutant = MutantResult("1", "Test", "x", MutantLocation(1, 1, 1, 5), MutantStatus.Survived)
        val renderer = MutantGutterIconRenderer(0, listOf(mutant))

        assertNotNull("Gutter icon should not be null", renderer.icon)
    }

    // --- MutantAnnotator collectInformation ---

    fun testAnnotatorReturnsEmptyListWhenServiceHasNoMatchingFile() {
        val service = MutationResultService.getInstance(project)
        service.setResults(mapOf("some/other/file.ts" to listOf(
            MutantResult("1", "BooleanLiteral", "true", MutantLocation(1, 1, 1, 5), MutantStatus.Survived)
        )))

        val psiFile = myFixture.addFileToProject("src/app.ts", "const x = false;")
        myFixture.configureFromExistingVirtualFile(psiFile.virtualFile)

        val annotator = MutantAnnotator()
        val collected = annotator.collectInformation(psiFile, myFixture.editor, false)

        assertTrue("Should return empty list when no results match the file path", collected.isEmpty())
    }

    fun testAnnotatorDoAnnotatePassesThroughCollectedInfo() {
        val mutant = MutantResult("1", "BooleanLiteral", "true", MutantLocation(1, 1, 1, 5), MutantStatus.Survived)
        val annotator = MutantAnnotator()

        val result = annotator.doAnnotate(listOf(mutant))

        assertEquals(1, result.size)
        assertEquals("BooleanLiteral", result[0].mutatorName)
    }

    fun testAnnotatorReturnsEmptyListWhenNoResultsForFile() {
        val service = MutationResultService.getInstance(project)
        service.clearResults()

        val psiFile = myFixture.addFileToProject("src/other.ts", "const y = true;")
        myFixture.configureFromExistingVirtualFile(psiFile.virtualFile)

        val annotator = MutantAnnotator()
        val collected = annotator.collectInformation(psiFile, myFixture.editor, false)

        assertTrue("Should return empty list when no results for file", collected.isEmpty())
    }
}
