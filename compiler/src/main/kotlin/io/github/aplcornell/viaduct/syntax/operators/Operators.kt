package io.github.aplcornell.viaduct.syntax.operators

import io.github.aplcornell.viaduct.prettyprinting.Document
import io.github.aplcornell.viaduct.prettyprinting.PrettyPrintable
import io.github.aplcornell.viaduct.prettyprinting.nested
import io.github.aplcornell.viaduct.prettyprinting.plus
import io.github.aplcornell.viaduct.prettyprinting.times
import io.github.aplcornell.viaduct.prettyprinting.tupled
import io.github.aplcornell.viaduct.syntax.Associativity
import io.github.aplcornell.viaduct.syntax.BinaryOperator
import io.github.aplcornell.viaduct.syntax.HighestPrecedence
import io.github.aplcornell.viaduct.syntax.InfixOperator
import io.github.aplcornell.viaduct.syntax.LowestPrecedence
import io.github.aplcornell.viaduct.syntax.Order
import io.github.aplcornell.viaduct.syntax.Precedence
import io.github.aplcornell.viaduct.syntax.PrefixOperator
import io.github.aplcornell.viaduct.syntax.UnaryOperator
import io.github.aplcornell.viaduct.syntax.types.BooleanType
import io.github.aplcornell.viaduct.syntax.types.FunctionType
import io.github.aplcornell.viaduct.syntax.types.IntegerType
import io.github.aplcornell.viaduct.syntax.values.BooleanValue
import io.github.aplcornell.viaduct.syntax.values.IntegerValue
import io.github.aplcornell.viaduct.syntax.values.Value
import kotlin.math.max
import kotlin.math.min

/** The precedence of [LogicalOperator]s. */
object LogicalOperatorPrecedence : Precedence

/** The precedence of [ComparisonOperator]s. */
object ComparisonOperatorPrecedence : Precedence {
    override fun compareTo(other: Precedence): Order {
        return if (other is LogicalOperatorPrecedence) {
            Order.HIGHER
        } else {
            super.compareTo(other)
        }
    }
}

/** The precedence of [Addition] and [Subtraction]. */
object AdditiveOperatorPrecedence : Precedence {
    override fun compareTo(other: Precedence): Order {
        return if (other is LogicalOperatorPrecedence || other is ComparisonOperatorPrecedence) {
            Order.HIGHER
        } else {
            super.compareTo(other)
        }
    }
}

/** The precedence of [Multiplication] and [Division]. */
object MultiplicativeOperatorPrecedence : Precedence {
    override fun compareTo(other: Precedence): Order {
        return if (other is LogicalOperatorPrecedence || other is ComparisonOperatorPrecedence || other is AdditiveOperatorPrecedence) {
            Order.HIGHER
        } else {
            super.compareTo(other)
        }
    }
}

/** A unary prefix operator. */
abstract class UnaryPrefixOperator : UnaryOperator, PrefixOperator {
    final override val precedence: Precedence
        get() = HighestPrecedence

    override fun toDocument(argument: PrettyPrintable): Document =
        Document(this.toString()) + argument
}

/** A binary prefix operator. */
abstract class BinaryPrefixOperator : BinaryOperator, PrefixOperator {
    final override fun toDocument(argument1: PrettyPrintable, argument2: PrettyPrintable): Document =
        Document(this.toString()) + listOf(argument1, argument2).tupled().nested()
}

/** A binary infix operator. */
abstract class BinaryInfixOperator : BinaryOperator, InfixOperator {
    final override fun toDocument(argument1: PrettyPrintable, argument2: PrettyPrintable): Document =
        argument1 * this.toString() * argument2
}

/** An infix operator that takes two numbers and returns a number. */
abstract class ArithmeticOperator : BinaryInfixOperator() {
    final override val associativity: Associativity
        get() = Associativity.LEFT

    final override val type: FunctionType
        get() = FunctionType(IntegerType, IntegerType, result = IntegerType)

    final override fun apply(argument1: Value, argument2: Value): Value {
        val arg1 = argument1 as IntegerValue
        val arg2 = argument2 as IntegerValue
        return IntegerValue(apply(arg1.value, arg2.value))
    }

    abstract fun apply(left: Int, right: Int): Int
}

/** An infix operator that takes two booleans and returns a boolean. */
abstract class LogicalOperator : BinaryInfixOperator() {
    final override val associativity: Associativity
        get() = Associativity.LEFT

    final override val precedence: Precedence
        get() = LogicalOperatorPrecedence

    final override val type: FunctionType
        get() = FunctionType(BooleanType, BooleanType, result = BooleanType)

    final override fun apply(argument1: Value, argument2: Value): Value {
        val arg1 = argument1 as BooleanValue
        val arg2 = argument2 as BooleanValue
        return BooleanValue(apply(arg1.value, arg2.value))
    }

    abstract fun apply(left: Boolean, right: Boolean): Boolean
}

/** An infix operator that takes two numbers and returns a boolean. */
abstract class ComparisonOperator : BinaryInfixOperator() {
    final override val associativity: Associativity
        get() = Associativity.NON

    final override val precedence: Precedence
        get() = ComparisonOperatorPrecedence

    final override val type: FunctionType
        get() = FunctionType(IntegerType, IntegerType, result = BooleanType)

    final override fun apply(argument1: Value, argument2: Value): Value {
        val arg1 = argument1 as IntegerValue
        val arg2 = argument2 as IntegerValue
        return BooleanValue(apply(arg1.value, arg2.value))
    }

    abstract fun apply(left: Int, right: Int): Boolean
}

// Arithmetic Operators

object Negation : UnaryPrefixOperator() {
    override val type: FunctionType
        get() = FunctionType(IntegerType, result = IntegerType)

    override fun apply(argument: Value): Value {
        return IntegerValue(-(argument as IntegerValue).value)
    }

    override fun toString(): String {
        return "-"
    }
}

object Addition : ArithmeticOperator() {
    override val precedence: Precedence
        get() = AdditiveOperatorPrecedence

    override fun apply(left: Int, right: Int): Int {
        return left + right
    }

    override fun toString(): String {
        return "+"
    }
}

object Subtraction : ArithmeticOperator() {
    override val precedence: Precedence
        get() = AdditiveOperatorPrecedence

    override fun apply(left: Int, right: Int): Int {
        return left - right
    }

    override fun toString(): String {
        return "-"
    }
}

object Multiplication : ArithmeticOperator() {
    override val precedence: Precedence
        get() = MultiplicativeOperatorPrecedence

    override fun apply(left: Int, right: Int): Int {
        return left * right
    }

    override fun toString(): String {
        return "*"
    }
}

object Division : ArithmeticOperator() {
    override val precedence: Precedence
        get() = MultiplicativeOperatorPrecedence

    override fun apply(left: Int, right: Int): Int {
        return left / right
    }

    override fun toString(): String {
        return "/"
    }
}

object Minimum : BinaryPrefixOperator() {
    override val precedence: Precedence
        get() = HighestPrecedence

    override val type: FunctionType
        get() = FunctionType(IntegerType, IntegerType, result = IntegerType)

    override fun apply(argument1: Value, argument2: Value): Value {
        val arg1 = argument1 as IntegerValue
        val arg2 = argument2 as IntegerValue
        return IntegerValue(min(arg1.value, arg2.value))
    }

    override fun toString(): String {
        return "min"
    }
}

object Maximum : BinaryPrefixOperator() {
    override val precedence: Precedence
        get() = HighestPrecedence

    override val type: FunctionType
        get() = Minimum.type

    override fun apply(argument1: Value, argument2: Value): Value {
        val arg1 = argument1 as IntegerValue
        val arg2 = argument2 as IntegerValue
        return IntegerValue(max(arg1.value, arg2.value))
    }

    override fun toString(): String {
        return "max"
    }
}

// Logical Operators

object Not : UnaryPrefixOperator() {
    override val type: FunctionType
        get() = FunctionType(BooleanType, result = BooleanType)

    override fun apply(argument: Value): Value {
        return BooleanValue(!(argument as BooleanValue).value)
    }

    override fun toString(): String {
        return "!"
    }
}

object And : LogicalOperator() {
    override fun apply(left: Boolean, right: Boolean): Boolean {
        return left && right
    }

    override fun toString(): String {
        return "&&"
    }
}

object Or : LogicalOperator() {
    override fun apply(left: Boolean, right: Boolean): Boolean {
        return left || right
    }

    override fun toString(): String {
        return "||"
    }
}

// Comparison Operators

object EqualTo : ComparisonOperator() {
    override fun apply(left: Int, right: Int): Boolean {
        return left == right
    }

    override fun toString(): String {
        return "=="
    }
}

object ExclusiveOr : ComparisonOperator() {
    override fun apply(left: Int, right: Int): Boolean {
        return left != right
    }

    override fun toString(): String {
        return "!="
    }
}

object LessThan : ComparisonOperator() {
    override fun apply(left: Int, right: Int): Boolean {
        return left < right
    }

    override fun toString(): String {
        return "<"
    }
}

object LessThanOrEqualTo : ComparisonOperator() {
    override fun apply(left: Int, right: Int): Boolean {
        return left <= right
    }

    override fun toString(): String {
        return "<="
    }
}

object GreaterThan : ComparisonOperator() {
    override fun apply(left: Int, right: Int): Boolean {
        return left > right
    }

    override fun toString(): String {
        return ">"
    }
}

object GreaterThanOrEqualTo : ComparisonOperator() {
    override fun apply(left: Int, right: Int): Boolean {
        return left >= right
    }

    override fun toString(): String {
        return ">="
    }
}

/**
 * Cases on the first argument; evaluates to the second argument if the first argument is `true`, and to the third
 * argument if the first argument is `false`.
 */
object Mux : InfixOperator {
    override val precedence: Precedence
        get() = LowestPrecedence

    override val associativity: Associativity
        get() = Associativity.NON

    override val type: FunctionType
        get() = FunctionType(BooleanType, IntegerType, IntegerType, result = IntegerType)

    override fun alternativeTypes(): List<FunctionType> =
        listOf(FunctionType(BooleanType, BooleanType, BooleanType, result = BooleanType))

    override fun apply(arguments: List<Value>): Value {
        return if ((arguments[0] as BooleanValue).value) arguments[1] else arguments[2]
    }

    override fun toDocument(arguments: List<PrettyPrintable>): Document {
        return Document("mux") + listOf(arguments[0], arguments[1], arguments[2]).tupled().nested()
    }
}
