package edu.cornell.cs.apl.viaduct.imp.ast2

/**
 * Determines how operators of the same precedence are grouped in the absence of parentheses.
 *
 * For example, the expression `x ~ y ~ z` with a corresponding associativity would be
 * interpreted as follows:
 *
 * - Left associative: `(x ~ y) ~ z`
 * - Right associative: `x ~ (y ~ z)`
 * - Non associative: syntax error.
 */
enum class Associativity {
    LEFT, RIGHT, NON
}

/** Pure functions from values to a value. */
interface Operator {
    /** Number of arguments the operator takes. */
    val arity: Int

    val associativity: Associativity

    /**
     * Determines the precedence of this operator with respect to (a subset of) other operators.
     *
     * This function must be reflexive, that is, `x.bindsTighterThan(x)` should return true.
     * However, it is not required to be transitive, and it does not have to order all operators.
     *
     * Two operators `x` and `y` have the same precedence if `x.bindsTighterThan(y)` and
     * `y.bindsTighterThan(x)`.
     */
    fun bindsTighterThan(other: Operator): Boolean
}

/**
 * Operators that are written before their operands.
 *
 * An operator is prefix if it has an operand that comes after all its named parts.
 * For example, negation, as in `-x`, and if expressions, as in `if b then x else y`, are prefix
 * operators.
 */
interface PrefixOperator : Operator {
    override val associativity: Associativity
        get() = Associativity.RIGHT
}

/**
 * Operators that are written between their operands.
 *
 * An operator is infix if it has an operand that comes before and an operand that comes after
 * all its named parts.
 * For example, addition, as in `x + y`, and the conditional operator, as in `b ? x : y`, are infix
 * operators.
 */
interface InfixOperator : Operator

/**
 * Operators that are written after their operands (e.g. factorial).
 *
 * An operator is postfix if has an operand that comes before all its named parts.
 * For example, taking the factorial, as in `x!`, is a prefix operator.
 */
interface PostfixOperator : Operator {
    override val associativity: Associativity
        get() = Associativity.LEFT
}

/**
 * Operators that surround their operands.
 *
 * An operator is closed if all its operands are contained between its named parts.
 * For example, a pair of parentheses, as in `(x)`, is a closed operator.
 */
interface ClosedOperator : Operator {
    override val associativity: Associativity
        get() = Associativity.NON
}
