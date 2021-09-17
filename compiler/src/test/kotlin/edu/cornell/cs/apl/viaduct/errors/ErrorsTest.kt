package edu.cornell.cs.apl.viaduct.errors

import edu.cornell.cs.apl.viaduct.NegativeTestFileProvider
import edu.cornell.cs.apl.viaduct.analysis.main
import edu.cornell.cs.apl.viaduct.parsing.SourceFile
import edu.cornell.cs.apl.viaduct.parsing.isBlankOrUnderline
import edu.cornell.cs.apl.viaduct.parsing.parse
import edu.cornell.cs.apl.viaduct.passes.check
import edu.cornell.cs.apl.viaduct.passes.elaborated
import edu.cornell.cs.apl.viaduct.selection.CostMode
import edu.cornell.cs.apl.viaduct.selection.SimpleCostEstimator
import edu.cornell.cs.apl.viaduct.selection.SimpleCostRegime
import edu.cornell.cs.apl.viaduct.selection.SimpleProtocolComposer
import edu.cornell.cs.apl.viaduct.selection.SimpleProtocolFactory
import edu.cornell.cs.apl.viaduct.selection.selectProtocolsWithZ3
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

internal class ErrorsTest {
    @ParameterizedTest
    @ArgumentsSource(NegativeTestFileProvider::class)
    fun `erroneous example files throw the expected compilation error`(file: File) {
        assertThrows(expectedError(file).java) { run(file) }
    }

    @ParameterizedTest
    @ArgumentsSource(NegativeTestFileProvider::class)
    fun `error messages end in a single blank line`(file: File) {
        try {
            run(file)
            assert(false)
        } catch (e: CompilationError) {
            val messageLines = e.toString().split(Regex("\\R"))
            assertTrue(isBlankOrUnderline(messageLines.last())) {
                "Error message should end in a blank line."
            }
            assertFalse(isBlankOrUnderline(messageLines[messageLines.size - 2])) {
                "Error message should have no more than one blank line at the end."
            }
        }
    }
}

/** Parses, checks, interprets, and splits a program. */
private fun run(file: File) {
    val program = SourceFile.from(file).parse().elaborated()
    program.check()
    selectProtocolsWithZ3(
        program, program.main,
        SimpleProtocolFactory(program), SimpleProtocolComposer,
        SimpleCostEstimator(SimpleProtocolComposer, SimpleCostRegime.LAN),
        CostMode.MINIMIZE
    )
    // TODO: interpret
}

/** Returns the subclass of [CompilationError] that running [file] is supposed to throw. */
private fun expectedError(file: File): KClass<CompilationError> {
    val comment = file.useLines { it.first() }
    val expectedErrorName = comment.removeSurrounding("/*", "*/").trim()
    val packageName = CompilationError::class.java.packageName
    val kClass = Class.forName("$packageName.$expectedErrorName").kotlin

    assert(kClass.isSubclassOf(CompilationError::class))
    assert(!kClass.isAbstract)

    @Suppress("UNCHECKED_CAST")
    return kClass as KClass<CompilationError>
}
