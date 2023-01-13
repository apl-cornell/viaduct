package io.github.aplcornell.viaduct.security.solver

import io.github.aplcornell.viaduct.algebra.FreeDistributiveLattice
import io.github.aplcornell.viaduct.algebra.FreeDistributiveLatticeCongruence
import io.github.aplcornell.viaduct.security.SecurityLattice
import org.junit.jupiter.api.AssertionFailureBuilder.assertionFailure
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.StringWriter
import io.github.aplcornell.viaduct.algebra.solver.Term as ComponentTerm

private typealias Constant = FreeDistributiveLattice<String>
private typealias Variable = String

private class IllegalFlowException(from: SecurityLattice<Constant>, to: SecurityLattice<Constant>) :
    IllegalArgumentException("Element $from does not flow to $to.")

private val ConstantBounds = Constant.bounds<String>()

private val SecurityBounds = SecurityLattice.Bounds(ConstantBounds)

/** Shorthand for creating constants. */
private fun c(element: String) = SecurityLattice(Constant(element))

/** Shorthand for creating constant terms. */
private fun t(constant: SecurityLattice<Constant>): Term<Constant, Variable> = term(constant)

/** Shorthand for creating variable terms. */
private fun t(variable: Variable): Term<Constant, Variable> = term(variable)

/** Shorthand for getting the confidentiality of a constant. */
private fun SecurityLattice<Constant>.c() = this.confidentiality(ConstantBounds)

/** Shorthand for getting the integrity of a constant. */
private fun SecurityLattice<Constant>.i() = this.integrity(ConstantBounds)

/** Shorthand for getting the confidentiality of a term. */
private val Term<Constant, Variable>.c
    get() = this.confidentiality(ComponentTerm.Bounds(ConstantBounds))

/** Shorthand for getting the integrity of a term. */
private val Term<Constant, Variable>.i
    get() = this.integrity(ComponentTerm.Bounds(ConstantBounds))

/** Shorthand for creating flows to constraints. */
private infix fun Term<Constant, Variable>.flowsTo(
    that: Term<Constant, Variable>,
): Iterable<Constraint<Constant, Variable, IllegalFlowException>> =
    this.flowsTo(that, ConstantBounds, ::IllegalFlowException)

private fun solve(
    vararg constraint: Iterable<Constraint<Constant, Variable, IllegalFlowException>>,
): ConstraintSolution<Constant, Variable> =
    ConstraintSystem(constraint.flatMap { it }, ConstantBounds, FreeDistributiveLatticeCongruence(listOf())).solution()

private fun assertEquals(expected: SecurityLattice<Constant>, actual: SecurityLattice<Constant>) {
    fun equals(expected: Constant, actual: Constant): Boolean =
        expected.lessThanOrEqualTo(actual, listOf()) && actual.lessThanOrEqualTo(expected, listOf())

    val confidentiality = equals(expected.confidentialityComponent, actual.confidentialityComponent)
    val integrity = equals(expected.integrityComponent, actual.integrityComponent)
    if (!(confidentiality && integrity)) {
        assertionFailure().expected(expected).actual(actual).buildAndThrow()
    }
}

internal class ConstraintSystemTest {
    @Nested
    inner class Inference {
        @Test
        fun `constant flows to constant`() {
            solve(
                t(c("A")) flowsTo t(c("A")),
            )
        }

        @Test
        fun `constant not flows to constant`() {
            assertThrows<IllegalFlowException> {
                solve(
                    t(c("A")) flowsTo t(c("B")),
                )
            }
        }

        @Test
        fun `constant flows to variable`() {
            val solution = solve(
                t(c("A")) flowsTo t("x"),
            )
            assertEquals(c("A").c(), solution("x"))
        }

        @Test
        fun `variable flows to constant`() {
            val solution = solve(
                t("x") flowsTo t(c("A")),
            )
            assertEquals(c("A").i(), solution("x"))
        }

        @Test
        fun `variable not flows to constant`() {
            assertThrows<IllegalFlowException> {
                solve(
                    t(c("A")) flowsTo t("x"),
                    t("x") flowsTo t(c("B")),
                )
            }
        }

        @Test
        fun `variable flows to self`() {
            val solution = solve(
                t("x") flowsTo t("x"),
            )
            assertEquals(SecurityBounds.weakest, solution("x"))
        }

        @Test
        fun `variable flows to variable`() {
            val solution = solve(
                t("x") flowsTo t(c("A")),
                t("x") flowsTo t("y"),
                t("y") flowsTo t(c("B")),
            )
            assertEquals((c("A") meet c("B")).i(), solution("x"))
            assertEquals(c("B").i(), solution("y"))
        }
    }

    @Nested
    inner class DotGraphOutput {
        private fun dotGraph(vararg constraint: Iterable<Constraint<Constant, Variable, IllegalFlowException>>): String {
            val system =
                ConstraintSystem(constraint.flatMap { it }, ConstantBounds, FreeDistributiveLatticeCongruence(listOf()))
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
                    t(c("A")) meet t("z") flowsTo (t(c("A")) join t("t")),
                ),
            )
        }

        @Test
        fun `unsolvable system`() {
            println(
                dotGraph(
                    t(c("A")) flowsTo t("x"),
                    t("x") flowsTo t(c("B")),
                ),
            )
        }
    }
}
