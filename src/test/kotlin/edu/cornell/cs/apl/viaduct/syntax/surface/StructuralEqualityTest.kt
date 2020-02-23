package edu.cornell.cs.apl.viaduct.syntax.surface

import edu.cornell.cs.apl.viaduct.ExampleFileProvider
import edu.cornell.cs.apl.viaduct.parsing.SourceFile
import edu.cornell.cs.apl.viaduct.parsing.parse
import java.io.File
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource

internal class StructuralEqualityTest {
    @ParameterizedTest
    @ArgumentsSource(ExampleFileProvider::class)
    fun `compares equals as equal`(file: File) {
        val source: SourceFile = SourceFile.from(file)
        assertStructurallyEquals(source.parse(), source.parse())
    }

    @Test
    fun `differentiates when not equal`() {
        val program1 = """
            process main {
                let mut x: int = 2 + 2;
            }
            """.trimIndent()

        val program2 = """
            process main {
                let mut x: int = 4;
            }
            """.trimIndent()

        assertStructurallyDiffers(program1.parse(), program2.parse())
    }

    @Test
    fun `ignores source locations`() {
        val program1 = """
            process main {
                let mut x: int = 2 + 2;
            }
            """.trimIndent()

        val program2 = """
            process  main  {

                let  mut   x :  int   =    2  +  2 ;
             }
            """.trimIndent()

        assertStructurallyEquals(program1.parse(), program2.parse())
    }

    @ParameterizedTest
    @ArgumentsSource(ExampleFileProvider::class)
    fun `is smarter than object equality`(file: File) {
        val source: SourceFile = SourceFile.from(file)
        assertNotEquals(source.parse(), source.parse())
    }
}

private fun assertStructurallyDiffers(expected: Node, actual: Node) {
    assertThrows<Throwable> { assertStructurallyEquals(expected, actual) }
}
