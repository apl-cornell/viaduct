package io.github.apl_cornell.viaduct.syntax

import io.github.apl_cornell.apl.prettyprinting.Document
import io.github.apl_cornell.apl.prettyprinting.PrettyPrintable
import io.github.apl_cornell.viaduct.syntax.types.FunctionType
import io.github.apl_cornell.viaduct.syntax.values.Value

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
 * For example, multiplication (conventionally) has higher precedence than addition, so
 * `x + y * z` is parsed as `x + (y * z)`.
 */
interface Precedence {
    /**
     * Determines the [Order] of this precedence with respect to [other].
     *
     * The result of `x.compareTo(y)` may be [Order.UNDETERMINED], in which case the order is determined by
     * `y.compareTo(x)`. If both of these are [Order.UNDETERMINED], then the precedences are not ordered, which is
     * valid.
     *
     * This design supports extensibility. Old operators do not need to know about new operators; their precedence can
     * (and should) compare as [Order.UNDETERMINED] to the precedence of operators they do not know about.
     * This way, newly added operators can declare their precedence with respect to existing operators without having
     * to change the code for existing operators.
     *
     * This function should satisfy the following properties:
     *
     * - If `x.compareTo(y)` returns [Order.LOWER], then
     *   `y.compareTo(x)` must return [Order.HIGHER] or [Order.UNDETERMINED].
     * - If `x.compareTo(y)` returns [Order.HIGHER], then
     *   `y.compareTo(x)` must return [Order.LOWER] or [Order.UNDETERMINED].
     *
     * However, this function is not required to be transitive or total. That is, not all operators are required to be
     * ordered with respect to each other. This design facilitates modularity
     * (see [Parsing Mixfix Operators](https://link.springer.com/chapter/10.1007/978-3-642-24452-0_5)).
     *
     * Note that two different objects implementing this interface can never denote the same precedence since [Order]
     * does not have an `EQUAL` option. If two operators have the same precedence, then [Operator.precedence] must
     * return the same object for both.
     */
    fun compareTo(other: Precedence): Order = Order.UNDETERMINED
}

/** The precedence that is higher than all other precedences. */
object HighestPrecedence : Precedence {
    override fun compareTo(other: Precedence): Order = Order.HIGHER
}

/** The precedence that is lower than all other precedences. */
object LowestPrecedence : Precedence {
    override fun compareTo(other: Precedence): Order = Order.LOWER
}

/** The result of comparing two [Precedence]s. */
enum class Order {
    LOWER, HIGHER, UNDETERMINED
}

/** A pure function from values to a value. */
interface Operator {
    /**
     * Determines the grouping of consecutive operators that have the same precedence.
     *
     * @see Associativity
     */
    val associativity: Associativity

    /**
     * Determines the order of this operator with respect to (a subset of) other operators.
     * Operators with higher precedence bind tighter than operators with lower precedence
     * (for example, multiplication has higher precedence than addition).
     *
     * @see Precedence
     */
    val precedence: Precedence

    /** The type of this operator. */
    val type: FunctionType

    /** In lieu of polymorphic types, have an optional list of alternative
     types to check against. */
    fun alternativeTypes(): List<FunctionType> = listOf()

    /** Computes the result of applying this operator to [arguments]. */
    fun apply(arguments: List<Value>): Value

    /** Shows this operator applied to [arguments]. */
    fun toDocument(arguments: List<PrettyPrintable>): Document
}

/**
 * Returns true when this operator has precedence higher than or equal to [other].
 *
 * This function is reflexive, that is, `x.bindsTighterThan(x)` returns true.
 * However, it is not necessarily transitive, and it does not order all operators.
 *
 * Two operators `x` and `y` have the same precedence if `x.bindsTighterThan(y)` and
 * `y.bindsTighterThan(x)`.
 */
fun Operator.bindsTighterThan(other: Operator): Boolean =
    this.precedence == other.precedence ||
        this.precedence.compareTo(other.precedence) == Order.HIGHER ||
        other.precedence.compareTo(this.precedence) == Order.LOWER

/**
 * An operator that is written before its operands.
 *
 * A prefix operator has an operand that comes after all its named parts.
 * For example, negation (`-x`) and if expressions (`if b then x else y`) are prefix operators.
 *
 * Prefix operators are right associative.
 */
interface PrefixOperator : Operator {
    override val associativity: Associativity
        get() = Associativity.RIGHT
}

/**
 * An operator that is written between its operands.
 *
 * An infix operator has an operand that comes before and an operand that comes after all its
 * named parts.
 * For example, addition (`x + y`) and the conditional operator (`b ? x : y`) are infix operators.
 */
interface InfixOperator : Operator

/**
 * An operator that is written after its operands.
 *
 * A postfix operator has an operand that comes before all its named parts.
 * For example, taking the factorial (`x!`) is a postfix operator.
 *
 * Postfix operators are left associative.
 */
interface PostfixOperator : Operator {
    override val associativity: Associativity
        get() = Associativity.LEFT
}

/**
 * An operator that surrounds its operands.
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

/**
 * An operator that takes a single argument.
 */
interface UnaryOperator : Operator {
    override fun apply(arguments: List<Value>): Value {
        checkArguments(arguments)
        return apply(arguments[0])
    }

    override fun toDocument(arguments: List<PrettyPrintable>): Document {
        checkArguments(arguments)
        return toDocument(arguments[0])
    }

    /** Computes the result of applying this operator to [argument]. */
    fun apply(argument: Value): Value

    /** Shows this operator applied to [argument]. */
    fun toDocument(argument: PrettyPrintable): Document
}

/**
 * An operator that takes two arguments.
 */
interface BinaryOperator : Operator {
    override fun apply(arguments: List<Value>): Value {
        checkArguments(arguments)
        return apply(arguments[0], arguments[1])
    }

    override fun toDocument(arguments: List<PrettyPrintable>): Document {
        checkArguments(arguments)
        return toDocument(arguments[0], arguments[1])
    }

    /** Computes the result of applying this operator to [argument1] and [argument2]. */
    fun apply(argument1: Value, argument2: Value): Value

    /** Shows this operator applied to [argument1] and [argument2]. */
    fun toDocument(argument1: PrettyPrintable, argument2: PrettyPrintable): Document
}

/** The number of arguments this operator takes. */
val Operator.arity: Int
    get() = type.arguments.size

/** Asserts that the correct number of arguments are passed to this operator. */
private fun Operator.checkArguments(arguments: List<*>) {
    require(arguments.size == this.arity) {
        "Operator takes ${this.arity} arguments but was given ${arguments.size}."
    }
}
