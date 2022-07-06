package io.github.apl_cornell.viaduct.security

import io.github.apl_cornell.viaduct.prettyprinting.Document
import io.github.apl_cornell.viaduct.prettyprinting.PrettyPrintable
import io.github.apl_cornell.viaduct.prettyprinting.plus
import io.github.apl_cornell.viaduct.prettyprinting.times
import io.github.apl_cornell.viaduct.prettyprinting.tupled
import kotlinx.collections.immutable.persistentMapOf

sealed class LabelExpression : PrettyPrintable {
    abstract fun interpret(parameters: Map<String, Label> = persistentMapOf()): Label
    abstract fun rename(renamer: (String) -> String = { x -> x }): LabelExpression
    abstract fun containsParameters(): Boolean
}

data class LabelLiteral(val name: String) : LabelExpression() {
    override fun toDocument(): Document = Document(name)

    override fun interpret(parameters: Map<String, Label>): Label =
        Label(Principal(name))

    override fun rename(renamer: (String) -> String): LabelExpression = this

    override fun containsParameters(): Boolean = false
}

data class LabelParameter(val name: String) : LabelExpression() {
    override fun toDocument(): Document = Document(name)

    override fun interpret(parameters: Map<String, Label>): Label =
        parameters[name] ?: throw Exception("label parameter $name not found")

    override fun rename(renamer: (String) -> String): LabelExpression =
        LabelParameter(renamer(name))

    override fun containsParameters(): Boolean = true
}

data class LabelJoin(val lhs: LabelExpression, val rhs: LabelExpression) : LabelExpression() {
    override fun toDocument(): Document = listOf(lhs.toDocument() * Document("⊔") * rhs.toDocument()).tupled()

    override fun interpret(parameters: Map<String, Label>): Label =
        lhs.interpret(parameters).join(rhs.interpret(parameters))

    override fun rename(renamer: (String) -> String): LabelExpression =
        LabelJoin(lhs.rename(renamer), rhs.rename(renamer))

    override fun containsParameters(): Boolean = lhs.containsParameters() || rhs.containsParameters()
}

data class LabelMeet(val lhs: LabelExpression, val rhs: LabelExpression) : LabelExpression() {
    override fun toDocument(): Document = listOf(lhs.toDocument() * Document("⊓") * rhs.toDocument()).tupled()

    override fun interpret(parameters: Map<String, Label>): Label =
        lhs.interpret(parameters).meet(rhs.interpret(parameters))

    override fun rename(renamer: (String) -> String): LabelExpression =
        LabelMeet(lhs.rename(renamer), rhs.rename(renamer))

    override fun containsParameters(): Boolean = lhs.containsParameters() || rhs.containsParameters()
}

data class LabelAnd(val lhs: LabelExpression, val rhs: LabelExpression) : LabelExpression() {
    override fun toDocument(): Document = listOf(lhs.toDocument() * Document("&") * rhs.toDocument()).tupled()

    override fun interpret(parameters: Map<String, Label>): Label =
        lhs.interpret(parameters).and(rhs.interpret(parameters))

    override fun rename(renamer: (String) -> String): LabelExpression =
        LabelAnd(lhs.rename(renamer), rhs.rename(renamer))

    override fun containsParameters(): Boolean = lhs.containsParameters() || rhs.containsParameters()
}

data class LabelOr(val lhs: LabelExpression, val rhs: LabelExpression) : LabelExpression() {
    override fun toDocument(): Document = listOf(lhs.toDocument() * Document("|") * rhs.toDocument()).tupled()

    override fun interpret(parameters: Map<String, Label>): Label =
        lhs.interpret(parameters).or(rhs.interpret(parameters))

    override fun rename(renamer: (String) -> String): LabelExpression =
        LabelOr(lhs.rename(renamer), rhs.rename(renamer))

    override fun containsParameters(): Boolean = lhs.containsParameters() || rhs.containsParameters()
}

data class LabelConfidentiality(val value: LabelExpression) : LabelExpression() {
    override fun toDocument(): Document = value.toDocument() + Document("->")

    override fun interpret(parameters: Map<String, Label>): Label =
        value.interpret(parameters).confidentiality()

    override fun rename(renamer: (String) -> String): LabelExpression =
        LabelConfidentiality(value.rename(renamer))

    override fun containsParameters(): Boolean = value.containsParameters()
}

data class LabelIntegrity(val value: LabelExpression) : LabelExpression() {
    override fun toDocument(): Document = value.toDocument() + Document("<-")

    override fun interpret(parameters: Map<String, Label>): Label =
        value.interpret(parameters).integrity()

    override fun rename(renamer: (String) -> String): LabelExpression =
        LabelIntegrity(value.rename(renamer))

    override fun containsParameters(): Boolean = value.containsParameters()
}

object LabelBottom : LabelExpression() {
    override fun toDocument(): Document = Document("⊥")

    override fun interpret(parameters: Map<String, Label>): Label =
        Label.strongest

    override fun rename(renamer: (String) -> String): LabelExpression = this

    override fun containsParameters(): Boolean = false
}

object LabelTop : LabelExpression() {
    override fun toDocument(): Document = Document("⊤")

    override fun interpret(parameters: Map<String, Label>): Label =
        Label.weakest

    override fun rename(renamer: (String) -> String): LabelExpression = this

    override fun containsParameters(): Boolean = false
}
