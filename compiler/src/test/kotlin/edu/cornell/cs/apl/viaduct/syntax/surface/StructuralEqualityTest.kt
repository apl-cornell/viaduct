package edu.cornell.cs.apl.viaduct.syntax.surface

import edu.cornell.cs.apl.viaduct.PositiveTestFileProvider
import edu.cornell.cs.apl.viaduct.parsing.SourceFile
import edu.cornell.cs.apl.viaduct.parsing.parse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import java.io.File

internal class StructuralEqualityTest {
    @ParameterizedTest
    @ArgumentsSource(PositiveTestFileProvider::class)
    fun `compares equals as equal`(file: File) {
        val source: SourceFile = SourceFile.from(file)
        assertStructurallyEquals(source.parse(), source.parse())
    }

    @Test
    fun `differentiates when not equal`() {
        val program1 = """
            fun main() {
                val x: int = 2 + 2;
            }
        """.trimIndent()

        val program2 = """
            fun main() {
                val x: int = 4;
            }
        """.trimIndent()

        assertStructurallyNotEquals(program1.parse(), program2.parse())
    }

    @Test
    fun `differentiates when arguments are not equal`() {
        val program1 = """
            fun main() {
                val a = Array[int](2);
            }
        """.trimIndent()

        val program2 = """
            fun main() {
                val a = Array[int](3);
            }
        """.trimIndent()

        assertStructurallyNotEquals(program1.parse(), program2.parse())
    }

    @Test
    fun `ignores source locations`() {
        val program1 = """
            fun main() {
                val x: int = 2 + 2;
            }
        """.trimIndent()

        val program2 = """
            fun  main ()  {

                val   x :  int   =    2  +  2 ;
             }
        """.trimIndent()

        assertStructurallyEquals(program1.parse(), program2.parse())
    }

    @ParameterizedTest
    @ArgumentsSource(PositiveTestFileProvider::class)
    fun `is smarter than object equality`(file: File) {
        val source: SourceFile = SourceFile.from(file)
        assertNotEquals(source.parse(), source.parse())
    }
}
