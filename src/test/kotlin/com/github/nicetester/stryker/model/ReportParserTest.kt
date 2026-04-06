package com.github.nicetester.stryker.model

import com.google.gson.JsonSyntaxException
import junit.framework.TestCase
import java.io.File

class ReportParserTest : TestCase() {

    // --- parseJson: valid report with survived and killed mutants ---

    fun testParseJsonReturnsAllMutants() {
        val json = """
        {
          "schemaVersion": "1.0",
          "thresholds": { "high": 80, "low": 60 },
          "files": {
            "src/utils/math.ts": {
              "language": "typescript",
              "source": "const add = (a, b) => a + b;",
              "mutants": [
                {
                  "id": "1",
                  "mutatorName": "ConditionalExpression",
                  "replacement": "false",
                  "location": { "start": { "line": 5, "column": 10 }, "end": { "line": 5, "column": 20 } },
                  "status": "Survived"
                },
                {
                  "id": "2",
                  "mutatorName": "ArithmeticOperator",
                  "replacement": "-",
                  "location": { "start": { "line": 10, "column": 15 }, "end": { "line": 10, "column": 16 } },
                  "status": "Killed"
                }
              ]
            }
          }
        }
        """.trimIndent()

        val report = ReportParser.parseJson(json)

        assertEquals(1, report.files.size)
        val mutants = report.files["src/utils/math.ts"]!!
        assertEquals(2, mutants.size)
        assertEquals(MutantStatus.Survived, mutants[0].status)
        assertEquals(MutantStatus.Killed, mutants[1].status)
    }

    fun testParseJsonExtractsMutantFields() {
        val json = """
        {
          "files": {
            "src/app.ts": {
              "language": "typescript",
              "source": "",
              "mutants": [
                {
                  "id": "42",
                  "mutatorName": "BooleanLiteral",
                  "replacement": "true",
                  "location": { "start": { "line": 3, "column": 5 }, "end": { "line": 3, "column": 10 } },
                  "status": "Survived"
                }
              ]
            }
          }
        }
        """.trimIndent()

        val report = ReportParser.parseJson(json)
        val mutant = report.files["src/app.ts"]!![0]

        assertEquals("42", mutant.id)
        assertEquals("BooleanLiteral", mutant.mutatorName)
        assertEquals("true", mutant.replacement)
        assertEquals(3, mutant.location.startLine)
        assertEquals(5, mutant.location.startColumn)
        assertEquals(3, mutant.location.endLine)
        assertEquals(10, mutant.location.endColumn)
        assertEquals(MutantStatus.Survived, mutant.status)
    }

    // --- parseJson: multiple files ---

    fun testParseJsonHandlesMultipleFiles() {
        val json = """
        {
          "files": {
            "src/a.ts": {
              "language": "typescript",
              "source": "",
              "mutants": [
                {
                  "id": "1",
                  "mutatorName": "StringLiteral",
                  "replacement": "\"\"",
                  "location": { "start": { "line": 1, "column": 1 }, "end": { "line": 1, "column": 5 } },
                  "status": "Survived"
                }
              ]
            },
            "src/b.ts": {
              "language": "typescript",
              "source": "",
              "mutants": [
                {
                  "id": "2",
                  "mutatorName": "BlockStatement",
                  "replacement": "{}",
                  "location": { "start": { "line": 7, "column": 1 }, "end": { "line": 7, "column": 10 } },
                  "status": "Killed"
                }
              ]
            }
          }
        }
        """.trimIndent()

        val report = ReportParser.parseJson(json)

        assertEquals(2, report.files.size)
        assertTrue(report.files.containsKey("src/a.ts"))
        assertTrue(report.files.containsKey("src/b.ts"))
    }

    // --- parseJson: empty files map ---

    fun testParseJsonHandlesEmptyFilesMap() {
        val json = """
        {
          "files": {}
        }
        """.trimIndent()

        val report = ReportParser.parseJson(json)

        assertTrue(report.files.isEmpty())
    }

    // --- parseJson: empty mutants array for a file ---

    fun testParseJsonExcludesFilesWithEmptyMutantsArray() {
        val json = """
        {
          "files": {
            "src/empty.ts": {
              "language": "typescript",
              "source": "",
              "mutants": []
            }
          }
        }
        """.trimIndent()

        val report = ReportParser.parseJson(json)

        assertTrue("Files with no mutants should not appear", report.files.isEmpty())
    }

    // --- parseJson: invalid JSON throws ---

    fun testParseJsonThrowsOnInvalidJson() {
        try {
            ReportParser.parseJson("not valid json{{{")
            fail("Expected JsonSyntaxException for invalid JSON")
        } catch (_: JsonSyntaxException) {
            // expected
        }
    }

    fun testParseJsonThrowsOnEmptyString() {
        try {
            ReportParser.parseJson("")
            fail("Expected exception for empty string")
        } catch (_: Exception) {
            // expected - empty string is not valid JSON
        }
    }

    // --- parse(File): file does not exist ---

    fun testParseReturnsNullWhenFileDoesNotExist() {
        val nonExistent = File("/tmp/stryker-test-nonexistent-${System.nanoTime()}.json")

        val result = ReportParser.parse(nonExistent)

        assertNull("Should return null when report file does not exist", result)
    }

    // --- parse(File): valid file ---

    fun testParseReadsFileAndReturnsMutationReport() {
        val tmpFile = File.createTempFile("stryker-report-", ".json")
        try {
            tmpFile.writeText("""
            {
              "files": {
                "src/app.ts": {
                  "language": "typescript",
                  "source": "",
                  "mutants": [
                    {
                      "id": "1",
                      "mutatorName": "ConditionalExpression",
                      "replacement": "false",
                      "location": { "start": { "line": 5, "column": 10 }, "end": { "line": 5, "column": 20 } },
                      "status": "Survived"
                    }
                  ]
                }
              }
            }
            """.trimIndent())

            val report = ReportParser.parse(tmpFile)

            assertNotNull(report)
            assertEquals(1, report!!.files.size)
            assertEquals("ConditionalExpression", report.files["src/app.ts"]!![0].mutatorName)
        } finally {
            tmpFile.delete()
        }
    }

    // --- MutantStatus ---

    fun testMutantStatusFromStringSurvived() {
        assertEquals(MutantStatus.Survived, MutantStatus.fromString("Survived"))
    }

    fun testMutantStatusFromStringKilled() {
        assertEquals(MutantStatus.Killed, MutantStatus.fromString("Killed"))
    }

    fun testMutantStatusFromStringTimeout() {
        assertEquals(MutantStatus.Timeout, MutantStatus.fromString("Timeout"))
    }

    fun testMutantStatusFromStringNoCoverage() {
        assertEquals(MutantStatus.NoCoverage, MutantStatus.fromString("NoCoverage"))
    }

    fun testMutantStatusFromStringUnknownReturnsIgnored() {
        assertEquals(MutantStatus.Ignored, MutantStatus.fromString("SomeUnknownStatus"))
    }

    fun testMutantStatusFromStringIsCaseInsensitive() {
        assertEquals(MutantStatus.Survived, MutantStatus.fromString("survived"))
        assertEquals(MutantStatus.Killed, MutantStatus.fromString("KILLED"))
    }

    // --- parseJson: multiple mutants on same line ---

    fun testParseJsonHandlesMultipleMutantsOnSameLine() {
        val json = """
        {
          "files": {
            "src/app.ts": {
              "language": "typescript",
              "source": "",
              "mutants": [
                {
                  "id": "1",
                  "mutatorName": "ConditionalExpression",
                  "replacement": "true",
                  "location": { "start": { "line": 5, "column": 1 }, "end": { "line": 5, "column": 10 } },
                  "status": "Survived"
                },
                {
                  "id": "2",
                  "mutatorName": "BooleanLiteral",
                  "replacement": "false",
                  "location": { "start": { "line": 5, "column": 15 }, "end": { "line": 5, "column": 20 } },
                  "status": "Survived"
                }
              ]
            }
          }
        }
        """.trimIndent()

        val report = ReportParser.parseJson(json)
        val mutants = report.files["src/app.ts"]!!

        assertEquals(2, mutants.size)
        assertEquals("ConditionalExpression", mutants[0].mutatorName)
        assertEquals("BooleanLiteral", mutants[1].mutatorName)
    }

    // --- parseJson: preserves all status types ---

    fun testParseJsonPreservesAllStatusTypes() {
        val json = """
        {
          "files": {
            "src/app.ts": {
              "language": "typescript",
              "source": "",
              "mutants": [
                {
                  "id": "1",
                  "mutatorName": "A",
                  "replacement": "x",
                  "location": { "start": { "line": 1, "column": 1 }, "end": { "line": 1, "column": 5 } },
                  "status": "Survived"
                },
                {
                  "id": "2",
                  "mutatorName": "B",
                  "replacement": "y",
                  "location": { "start": { "line": 2, "column": 1 }, "end": { "line": 2, "column": 5 } },
                  "status": "Killed"
                },
                {
                  "id": "3",
                  "mutatorName": "C",
                  "replacement": "z",
                  "location": { "start": { "line": 3, "column": 1 }, "end": { "line": 3, "column": 5 } },
                  "status": "Timeout"
                },
                {
                  "id": "4",
                  "mutatorName": "D",
                  "replacement": "w",
                  "location": { "start": { "line": 4, "column": 1 }, "end": { "line": 4, "column": 5 } },
                  "status": "NoCoverage"
                }
              ]
            }
          }
        }
        """.trimIndent()

        val report = ReportParser.parseJson(json)
        val mutants = report.files["src/app.ts"]!!

        assertEquals(4, mutants.size)
        assertEquals(MutantStatus.Survived, mutants[0].status)
        assertEquals(MutantStatus.Killed, mutants[1].status)
        assertEquals(MutantStatus.Timeout, mutants[2].status)
        assertEquals(MutantStatus.NoCoverage, mutants[3].status)
    }
}
