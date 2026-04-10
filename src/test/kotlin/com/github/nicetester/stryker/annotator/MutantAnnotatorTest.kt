package com.github.nicetester.stryker.annotator

import com.github.nicetester.stryker.model.MutantLocation
import com.github.nicetester.stryker.model.MutantResult
import com.github.nicetester.stryker.model.MutantStatus
import com.github.nicetester.stryker.service.MutationResultService
import com.github.nicetester.stryker.util.PathUtil
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

    fun testAnnotatorReturnsSurvivedMutantsForMatchingFile() {
        val psiFile = myFixture.addFileToProject("src/calculator.ts", "const add = (a, b) => a + b;")
        myFixture.configureFromExistingVirtualFile(psiFile.virtualFile)

        // Derive configDir from the actual virtual file path (two levels up from "src/calculator.ts")
        // so that PathUtil.computeRelativePath produces "src/calculator.ts" as the key
        val configDir = psiFile.virtualFile.parent.parent.path
        val service = MutationResultService.getInstance(project)
        service.setResults(
            mapOf("src/calculator.ts" to listOf(
                survivedMutant("1", "ArithmeticOperator", "+", MutantLocation(1, 24, 1, 25)),
            )),
            configDir,
            System.currentTimeMillis(),
        )

        val annotator = MutantAnnotator()
        val collected = annotator.collectInformation(psiFile, myFixture.editor, false)

        assertEquals("Should return survived mutants when file path matches service key", 1, collected.size)
        assertEquals("ArithmeticOperator", collected[0].mutatorName)
    }

    fun testAnnotatorOnlyReturnsSurvivedMutantsNotKilledOnes() {
        val psiFile = myFixture.addFileToProject("src/filter.ts", "const negate = (x) => !x;")
        myFixture.configureFromExistingVirtualFile(psiFile.virtualFile)

        val configDir = psiFile.virtualFile.parent.parent.path
        val service = MutationResultService.getInstance(project)
        service.setResults(
            mapOf("src/filter.ts" to listOf(
                survivedMutant("1", "LogicalOperator", "&&", MutantLocation(1, 1, 1, 5)),
                MutantResult("2", "UnaryOperator", "", MutantLocation(1, 23, 1, 24), MutantStatus.Killed),
            )),
            configDir,
            System.currentTimeMillis(),
        )

        val annotator = MutantAnnotator()
        val collected = annotator.collectInformation(psiFile, myFixture.editor, false)

        assertEquals("Should return only survived mutants, not killed ones", 1, collected.size)
        assertEquals("LogicalOperator", collected[0].mutatorName)
    }

    // --- MutantGutterIconRenderer: icon identity ---

    fun testGutterIconRendererReturnsWarningIcon() {
        val mutant = survivedMutant("1", "Test", "x", MutantLocation(1, 1, 1, 5))
        val renderer = MutantGutterIconRenderer(0, listOf(mutant))

        val icon = renderer.icon

        assertNotNull(icon)
        // Warning icon should be 16x16 — verifies it's the expected icon type, not a placeholder
        assertEquals("Warning gutter icon should be 16px wide", 16, icon.iconWidth)
        assertEquals("Warning gutter icon should be 16px tall", 16, icon.iconHeight)
    }

    // --- helpers ---

    private fun survivedMutant(id: String, mutatorName: String, replacement: String, location: MutantLocation) =
        MutantResult(id, mutatorName, replacement, location, MutantStatus.Survived)
}
