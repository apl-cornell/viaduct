package edu.cornell.cs.apl.viaduct.syntax

import edu.cornell.cs.apl.viaduct.syntax.types.BooleanType
import edu.cornell.cs.apl.viaduct.syntax.types.IntegerType
import edu.cornell.cs.apl.viaduct.syntax.types.OperatorType
import edu.cornell.cs.apl.viaduct.syntax.values.BooleanValue
import edu.cornell.cs.apl.viaduct.syntax.values.IntegerValue
import edu.cornell.cs.apl.viaduct.syntax.values.Value
import kotlinx.collections.immutable.persistentListOf
import kotlin.math.min

/**
 * A prefix operator that takes a single argument.
 */
private interface UnaryPrefixOperator : PrefixOperator, UnaryOperator {
    override fun comparePrecedenceTo(other: Operator): Precedence {
        if (other is UnaryPrefixOperator)
            return Precedence.EQUAL
        return Precedence.HIGHER
    }
}

/**
 * An infix operator that takes two arguments.
 */
private interface BinaryInfixOperator : InfixOperator, BinaryOperator

/**
 * Left associative operators.
 */
private interface LeftAssociativeOperator : Operator {
    override val associativity: Associativity
        get() = Associativity.LEFT
}

/**
 * An infix operator that takes two numbers and returns a number.
 */
private interface ArithmeticOperator : BinaryInfixOperator, LeftAssociativeOperator {
    override val type: OperatorType
        get() = OperatorType(persistentListOf(IntegerType, IntegerType), IntegerType)

    override fun apply(arguments: List<Value>): Value {
        val arg1 = arguments[0] as IntegerValue
        val arg2 = arguments[1] as IntegerValue
        return IntegerValue(apply(arg1.value, arg2.value))
    }

    fun apply(left: Int, right: Int): Int
}

/**
 * An infix operator that takes two booleans and returns a boolean.
 */
private interface LogicalOperator : BinaryInfixOperator, LeftAssociativeOperator {
    override fun comparePrecedenceTo(other: Operator): Precedence {
        if (other == this)
            return Precedence.EQUAL
        if (other is LogicalOperator)
            return Precedence.UNDETERMINED
        return Precedence.LOWER
    }

    override val type: OperatorType
        get() = OperatorType(persistentListOf(BooleanType, BooleanType), IntegerType)

    override fun apply(arguments: List<Value>): Value {
        val arg1 = arguments[0] as BooleanValue
        val arg2 = arguments[1] as BooleanValue
        return BooleanValue(apply(arg1.value, arg2.value))
    }

    fun apply(left: Boolean, right: Boolean): Boolean
}

/**
 * An infix operator that takes two numbers and returns a boolean.
 */
private interface ComparisonOperator : BinaryInfixOperator {
    override val associativity: Associativity
        get() = Associativity.NON

    override fun comparePrecedenceTo(other: Operator): Precedence {
        if (other is ArithmeticOperator)
            return Precedence.LOWER
        return super.comparePrecedenceTo(other)
    }

    override val type: OperatorType
        get() = OperatorType(persistentListOf(IntegerType, IntegerType), BooleanType)

    override fun apply(arguments: List<Value>): Value {
        val arg1 = arguments[0] as IntegerValue
        val arg2 = arguments[1] as IntegerValue
        return BooleanValue(apply(arg1.value, arg2.value))
    }

    fun apply(left: Int, right: Int): Boolean
}

// Arithmetic Operators

object Negation : UnaryPrefixOperator {
    override val type: OperatorType
        get() = OperatorType(persistentListOf(IntegerType), IntegerType)

    override fun apply(arguments: List<Value>): Value {
        return IntegerValue(-(arguments[0] as IntegerValue).value)
    }

    override fun toString(): String {
        return "-"
    }
}

object Addition : ArithmeticOperator {
    override fun apply(left: Int, right: Int): Int {
        return left + right
    }

    override fun toString(): String {
        return "+"
    }
}

object Subtraction : ArithmeticOperator {
    override fun comparePrecedenceTo(other: Operator): Precedence {
        return Addition.comparePrecedenceTo(other)
    }

    override fun apply(left: Int, right: Int): Int {
        return left - right
    }

    override fun toString(): String {
        return "-"
    }
}

object Multiplication : ArithmeticOperator {
    override fun comparePrecedenceTo(other: Operator): Precedence {
        if (other is Addition || other is Subtraction) {
            return Precedence.HIGHER
        }
        return super.comparePrecedenceTo(other)
    }

    override fun apply(left: Int, right: Int): Int {
        return left * right
    }

    override fun toString(): String {
        return "*"
    }
}

object Division : ArithmeticOperator {
    override fun comparePrecedenceTo(other: Operator): Precedence {
        return Multiplication.comparePrecedenceTo(
            other
        )
    }

    override fun apply(left: Int, right: Int): Int {
        return left / right
    }

    override fun toString(): String {
        return "/"
    }
}

object Minimum : ArithmeticOperator {
    // TODO: this is janky.
    override fun comparePrecedenceTo(other: Operator): Precedence {
        if (other is UnaryPrefixOperator || other is Minimum || other is Maximum)
            return Precedence.EQUAL
        return Precedence.HIGHER
    }

    override fun apply(left: Int, right: Int): Int {
        return min(left, right)
    }

    override fun toString(): String {
        return "min"
    }
}

object Maximum : ArithmeticOperator {
    override fun comparePrecedenceTo(other: Operator): Precedence {
        return Minimum.comparePrecedenceTo(other)
    }

    override fun apply(left: Int, right: Int): Int {
        return min(left, right)
    }

    override fun toString(): String {
        return "max"
    }
}

// Logical Operators

object Not : UnaryPrefixOperator {
    override val type: OperatorType
        get() = OperatorType(persistentListOf(BooleanType), BooleanType)

    override fun apply(arguments: List<Value>): Value {
        return BooleanValue(!(arguments[0] as BooleanValue).value)
    }

    override fun toString(): String {
        return "!"
    }
}

object And : LogicalOperator {
    override fun apply(left: Boolean, right: Boolean): Boolean {
        return left && right
    }

    override fun toString(): String {
        return "&&"
    }
}

object Or : LogicalOperator {
    override fun apply(left: Boolean, right: Boolean): Boolean {
        return left || right
    }

    override fun toString(): String {
        return "||"
    }
}

// Comparison Operators

object EqualTo : ComparisonOperator {
    override fun apply(left: Int, right: Int): Boolean {
        return left == right
    }

    override fun toString(): String {
        return "=="
    }
}

object LessThan : ComparisonOperator {
    override fun apply(left: Int, right: Int): Boolean {
        return left < right
    }

    override fun toString(): String {
        return "<"
    }
}

object LessThanOrEqualTo : ComparisonOperator {
    override fun apply(left: Int, right: Int): Boolean {
        return left <= right
    }

    override fun toString(): String {
        return "<="
    }
}
