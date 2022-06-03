package io.github.apl_cornell.viaduct.errors

import io.github.apl_cornell.viaduct.NegativeTestFileProvider
import io.github.apl_cornell.viaduct.backends.DefaultCombinedBackend
import io.github.apl_cornell.viaduct.parsing.SourceFile
import io.github.apl_cornell.viaduct.parsing.isBlankOrUnderline
import io.github.apl_cornell.viaduct.passes.compile
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
        assertThrows(expectedError(file).java) { compile(file) }
    }

    @ParameterizedTest
    @ArgumentsSource(NegativeTestFileProvider::class)
    fun `error messages end in a single blank line`(file: File) {
        try {
            compile(file)
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

/** Parses, checks, compiles a program. */
private fun compile(file: File) {
    SourceFile.from(file).compile(DefaultCombinedBackend)
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
