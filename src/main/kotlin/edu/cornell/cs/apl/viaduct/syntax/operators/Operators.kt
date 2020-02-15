package edu.cornell.cs.apl.viaduct.syntax.operators

import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.prettyprinting.PrettyPrintable
import edu.cornell.cs.apl.prettyprinting.nested
import edu.cornell.cs.apl.prettyprinting.plus
import edu.cornell.cs.apl.prettyprinting.times
import edu.cornell.cs.apl.prettyprinting.tupled
import edu.cornell.cs.apl.viaduct.syntax.Associativity
import edu.cornell.cs.apl.viaduct.syntax.BinaryOperator
import edu.cornell.cs.apl.viaduct.syntax.InfixOperator
import edu.cornell.cs.apl.viaduct.syntax.Operator
import edu.cornell.cs.apl.viaduct.syntax.Precedence
import edu.cornell.cs.apl.viaduct.syntax.PrefixOperator
import edu.cornell.cs.apl.viaduct.syntax.UnaryOperator
import edu.cornell.cs.apl.viaduct.syntax.types.BooleanType
import edu.cornell.cs.apl.viaduct.syntax.types.FunctionType
import edu.cornell.cs.apl.viaduct.syntax.types.IntegerType
import edu.cornell.cs.apl.viaduct.syntax.values.BooleanValue
import edu.cornell.cs.apl.viaduct.syntax.values.IntegerValue
import edu.cornell.cs.apl.viaduct.syntax.values.Value
import kotlin.math.max
import kotlin.math.min

/** A unary prefix operator. */
abstract class UnaryPrefixOperator : UnaryOperator, PrefixOperator {
    final override fun comparePrecedenceTo(other: Operator): Precedence {
        if (other is UnaryPrefixOperator)
            return Precedence.EQUAL
        return Precedence.HIGHER
    }

    override fun asDocument(argument: PrettyPrintable): Document =
        Document(this.toString()) + argument
}

/** A binary prefix operator. */
abstract class BinaryPrefixOperator : BinaryOperator, PrefixOperator {
    final override fun asDocument(
        argument1: PrettyPrintable,
        argument2: PrettyPrintable
    ): Document =
        Document(this.toString()) + listOf(argument1, argument2).tupled().nested()
}

/** A binary infix operator. */
abstract class BinaryInfixOperator : BinaryOperator, InfixOperator {
    final override fun asDocument(
        argument1: PrettyPrintable,
        argument2: PrettyPrintable
    ): Document =
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

    final override fun comparePrecedenceTo(other: Operator): Precedence {
        if (other == this)
            return Precedence.EQUAL
        if (other is LogicalOperator)
            return Precedence.UNDETERMINED
        return Precedence.LOWER
    }

    final override val type: FunctionType
        get() = FunctionType(BooleanType, BooleanType, result = IntegerType)

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

    final override fun comparePrecedenceTo(other: Operator): Precedence {
        if (other is ArithmeticOperator)
            return Precedence.LOWER
        return super.comparePrecedenceTo(other)
    }

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
    override fun apply(left: Int, right: Int): Int {
        return left + right
    }

    override fun toString(): String {
        return "+"
    }
}

object Subtraction : ArithmeticOperator() {
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

object Multiplication : ArithmeticOperator() {
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

object Division : ArithmeticOperator() {
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

object Minimum : BinaryPrefixOperator() {
    // TODO: this is janky.
    override fun comparePrecedenceTo(other: Operator): Precedence {
        if (other is UnaryPrefixOperator || other is Minimum || other is Maximum)
            return Precedence.EQUAL
        return Precedence.HIGHER
    }

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
    override fun comparePrecedenceTo(other: Operator): Precedence {
        return Minimum.comparePrecedenceTo(
            other
        )
    }

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
