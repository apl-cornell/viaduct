package edu.cornell.cs.apl.viaduct.syntax

import edu.cornell.cs.apl.viaduct.syntax.types.OperatorType
import edu.cornell.cs.apl.viaduct.syntax.values.Value

/**
 * Determines how operators of the same [Precedence] are grouped in the absence of parentheses.
 *
 * For example, the expression `x ~ y ~ z` would be parsed as follows given the
 * corresponding associativity of `~`:
 *
 * - Left associative: `(x ~ y) ~ z`
 * - Right associative: `x ~ (y ~ z)`
 * - Non-associative: syntax error.
 */
enum class Associativity {
    LEFT, RIGHT, NON
}

/**
 * Determines the order of operations in the absence of parentheses.
 *
 * For example, conventionally, multiplication has higher precedence than addition, so
 * `x + y * z` is parsed as `x + (y * z)`.
 */
enum class Precedence {
    LOWER, EQUAL, HIGHER, UNDETERMINED
}

/** Pure functions from values to a value. */
interface Operator {
    /** Number of arguments the operator takes. */
    val arity: Int

    /**
     * Determines the grouping of consecutive operators that have the same precedence.
     *
     * @see Associativity
     */
    val associativity: Associativity

    /**
     * Determines the [Precedence] of this operator with respect to (a subset of) other operators.
     * Operators with higher precedence bind tighter than operators with lower precedence
     * (for example, multiplication has higher precedence than addition).
     *
     * The result of `x.comparePrecedenceTo(y)` might be [Precedence.UNDETERMINED], in which
     * case, the precedence is determined by `y.comparePrecedenceTo(x)`.
     * If both of these are [Precedence.UNDETERMINED], then the operators are not ordered, which
     * is valid.
     * This design supports extensibility. Old operators do not need to know about new
     * operators; they can (and should) return [Precedence.UNDETERMINED] for operators they
     * do not know about, this way, newly added operators can declare their precedence with
     * respect to existing operators.
     *
     * This function should satisfy the following properties:
     *
     * - `x.comparePrecedenceTo(x)` must return [Precedence.EQUAL]
     * - If `x.comparePrecedenceTo(y)` returns [Precedence.LOWER], then
     *   `y.comparePrecedenceTo(x)` must return [Precedence.HIGHER] or [Precedence.UNDETERMINED].
     * - If `x.comparePrecedenceTo(y)` returns [Precedence.EQUAL], then
     *   `y.comparePrecedenceTo(x)` must return [Precedence.EQUAL] or [Precedence.UNDETERMINED].
     * - If `x.comparePrecedenceTo(y)` returns [Precedence.HIGHER], then
     *   `y.comparePrecedenceTo(x)` must return [Precedence.LOWER] or [Precedence.UNDETERMINED].
     *
     * However, this function is not required to be transitive, and not all operators need to be
     * ordered.
     */
    fun comparePrecedenceTo(other: Operator): Precedence {
        return if (this == other) Precedence.EQUAL else Precedence.UNDETERMINED
    }

    /** Type of this operator. */
    val type: OperatorType

    /** The result of applying this operator to the given arguments. */
    fun apply(arguments: List<Value>): Value
}

/**
 * True when this operator has precedence higher than or equal to [other].
 *
 * This function is reflexive, that is, `x.bindsTighterThan(x)` returns true.
 * However, it is not necessarily transitive, and it does not order all operators.
 *
 * Two operators `x` and `y` have the same precedence if `x.bindsTighterThan(y)` and
 * `y.bindsTighterThan(x)`.
 */
fun Operator.bindsTighterThan(other: Operator): Boolean {
    val thisToOther = this.comparePrecedenceTo(other)
    val otherToThis = other.comparePrecedenceTo(this)
    return thisToOther == Precedence.EQUAL ||
        thisToOther == Precedence.HIGHER ||
        otherToThis == Precedence.EQUAL ||
        otherToThis == Precedence.LOWER
}

/**
 * Operators that are written before their operands.
 *
 * An operator is prefix if it has an operand that comes after all its named parts.
 * For example, negation (`-x`) and if expressions (`if b then x else y`) are prefix operators.
 *
 * Prefix operators are right associative.
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
 * For example, addition (`x + y`) and the conditional operator (`b ? x : y`) are infix operators.
 */
interface InfixOperator : Operator

/**
 * Operators that are written after their operands (e.g. factorial).
 *
 * An operator is postfix if has an operand that comes before all its named parts.
 * For example, taking the factorial (`x!`) is a prefix operator.
 *
 * Postfix operators are left associative.
 */
interface PostfixOperator : Operator {
    override val associativity: Associativity
        get() = Associativity.LEFT
}

/**
 * Operators that surround their operands.
 *
 * An operator is closed if all its operands are contained between its named parts.
 * For example, a pair of parentheses (`(x)`) is a closed operator.
 *
 * Closed operators are non-associative.
 */
interface ClosedOperator : Operator {
    override val associativity: Associativity
        get() = Associativity.NON
}
