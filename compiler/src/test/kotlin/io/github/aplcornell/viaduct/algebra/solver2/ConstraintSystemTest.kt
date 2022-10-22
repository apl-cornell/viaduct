package io.github.apl_cornell.viaduct.algebra.solver2

import io.github.apl_cornell.viaduct.algebra.FreeDistributiveLattice
import io.github.apl_cornell.viaduct.algebra.FreeDistributiveLatticeCongruence
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.StringWriter

private typealias Constant = FreeDistributiveLattice<String>
private typealias Variable = String

private class IllegalFlowException(from: Constant, to: Constant) :
    IllegalArgumentException("Element $from does not flow to $to.")

private val ConstantBounds = Constant.bounds<String>()

/** Shorthand for creating constants. */
private fun c(element: String) = Constant(element)

/** Shorthand for creating constant terms. */
private fun t(constant: Constant): Term<Constant, Variable> = Term.constant(constant)

/** Shorthand for creating variable terms. */
private fun t(variable: Variable): Term<Constant, Variable> = Term.variable(variable)

/** Shorthand for creating flows to constraints. */
private infix fun Term<Constant, Variable>.flowsTo(
    that: Term<Constant, Variable>
): Constraint<Constant, Variable, IllegalFlowException> =
    this.flowsTo(that, ::IllegalFlowException)

private fun solve(
    vararg constraint: Constraint<Constant, Variable, IllegalFlowException>
): ConstraintSolution<Constant, Variable> =
    ConstraintSystem(listOf(*constraint), ConstantBounds, FreeDistributiveLatticeCongruence(listOf())).solution()

internal class ConstraintSystemTest {
    @Nested
    inner class AtomicTerms {
        @Test
        fun `constant flows to constant`() {
            solve(
                t(c("A")) flowsTo t(c("A"))
            )
        }

        @Test
        fun `constant not flows to constant`() {
            assertThrows<IllegalFlowException> {
                solve(
                    t(c("A")) flowsTo t(c("B"))
                )
            }
        }

        @Test
        fun `variable flows to constant`() {
            val solution = solve(
                t("x") flowsTo t(c("A"))
            )
            assertEquals(c("A"), solution("x"))
        }

        @Test
        fun `variable not flows to constant`() {
            assertThrows<IllegalFlowException> {
                solve(
                    t(c("A")) flowsTo t("x"),
                    t("x") flowsTo t(c("B"))
                )
            }
        }

        @Test
        fun `variable flows to self`() {
            val solution = solve(
                t("x") flowsTo t("x")
            )
            assertEquals(ConstantBounds.top, solution("x"))
        }

        @Test
        fun `variable flows to variable`() {
            val solution = solve(
                t("x") flowsTo t(c("A")),
                t("x") flowsTo t("y"),
                t("y") flowsTo t(c("B"))
            )
            assertEquals(c("A") meet c("B"), solution("x"))
            assertEquals(c("B"), solution("y"))
        }
    }

    @Nested
    inner class Join {
        @Test
        fun left() {
            val solution = solve(
                (t("x") join t("y")) flowsTo t(c("A")),
            )
            assertEquals(c("A"), solution("x"))
            assertEquals(c("A"), solution("y"))
        }

        @Test
        fun `right constant and constant`() {
            val solution = solve(
                t("x") flowsTo (t(c("A")) join t(c("B"))),
            )
            assertEquals(c("A") join c("B"), solution("x"))
        }

        @Test
        fun `right constant and variable`() {
            val solution = solve(
                t("x") flowsTo (t(c("A")) join t("y")),
                t("y") flowsTo t(c("B"))
            )
            assertEquals(c("A") join c("B"), solution("x"))
            assertEquals(c("B"), solution("y"))
        }

        @Test
        fun `right variable and variable`() {
            val solution = solve(
                t("x") flowsTo (t("y") join t("y")),
                t("y") flowsTo t(c("A"))
            )
            assertEquals(c("A"), solution("x"))
            assertEquals(c("A"), solution("y"))
        }
    }

    @Nested
    inner class Meet {
        @Test
        fun `left constant and constant`() {
            val solution = solve(
                t(c("A")) meet t(c("B")) flowsTo t("x"),
                t("x") flowsTo t(c("A"))
            )
            assertEquals(c("A"), solution("x"))
        }

        @Test
        fun `left constant and variable`() {
            val solution = solve(
                t(c("A")) meet t("x") flowsTo (t(c("A")) meet t(c("B")))
            )
            assertEquals(c("B"), solution("x"))
        }

        @Test
        fun `left variable and variable`() {
            val solution = solve(
                t("x") meet t("x") flowsTo t(c("A"))
            )
            assertEquals(c("A"), solution("x"))
        }

        @Test
        fun right() {
            val solution = solve(
                t("x") flowsTo (t("y") meet t("z")),
                t("y") flowsTo t(c("A")),
                t("z") flowsTo t(c("B"))
            )
            assertEquals(c("A") meet c("B"), solution("x"))
            assertEquals(c("A"), solution("y"))
            assertEquals(c("B"), solution("z"))
        }
    }

    @Nested
    inner class JoinAndMeet {
        @Test
        fun `constant variable flows to constant variable`() {
            val aAndB = c("A") meet c("B")
            val aAndC = c("A") meet c("C")
            val bOrC = c("B") join c("C")

            val solution = solve(
                t(c("A")) meet t("x") flowsTo (t(aAndB) join t("y")),
                t("y") flowsTo t(aAndC)

            )
            assertEquals(bOrC, solution("x"))
            assertEquals(aAndC, solution("y"))
        }
    }

    @Nested
    inner class IllegalTerms {
        @Test
        fun `left term`() {
            assertThrows<IllegalTermException> {
                (t("x") meet t("y")) flowsTo t(c("A"))
            }
        }

        @Test
        fun `right term`() {
            assertThrows<IllegalTermException> {
                t(c("A")) flowsTo (t("x") join t("y"))
            }
        }
    }

    @Nested
    inner class DotGraphOutput {
        private fun dotGraph(vararg constraint: Constraint<Constant, Variable, IllegalFlowException>): String {
            val system =
                ConstraintSystem(listOf(*constraint), ConstantBounds, FreeDistributiveLatticeCongruence(listOf()))
            val writer = StringWriter()
            system.exportDotGraph(writer)
            return writer.toString()
        }

        @Test
        fun `solvable system`() {
            println(
                dotGraph(
                    t("x") flowsTo t(c("A")),
                    t("y") flowsTo (t("x") join t(c("B"))),
                    t(c("A")) meet t("z") flowsTo t(c("A")),
                    t(c("A")) meet t("z") flowsTo (t(c("A")) join t("t"))
                )
            )
        }

        @Test
        fun `unsolvable system`() {
            println(
                dotGraph(
                    t(c("A")) flowsTo t("x"),
                    t("x") flowsTo t(c("B"))
                )
            )
        }
    }
}
