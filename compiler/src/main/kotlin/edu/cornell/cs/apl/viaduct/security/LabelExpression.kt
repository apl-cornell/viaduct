package edu.cornell.cs.apl.viaduct.security

import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.prettyprinting.PrettyPrintable
import edu.cornell.cs.apl.prettyprinting.plus
import edu.cornell.cs.apl.prettyprinting.times
import edu.cornell.cs.apl.prettyprinting.tupled
import kotlinx.collections.immutable.persistentMapOf

sealed class LabelExpression : PrettyPrintable {
    abstract fun interpret(parameters: Map<String, Label> = persistentMapOf()): Label
    abstract fun rename(renamer: (String) -> String = { x -> x }): LabelExpression
}

data class LabelLiteral(val name: String) : LabelExpression() {
    override val asDocument: Document
        get() = Document(name)

    override fun interpret(parameters: Map<String, Label>): Label =
        Label(Principal(name))

    override fun rename(renamer: (String) -> String): LabelExpression = this
}

data class LabelParameter(val name: String) : LabelExpression() {
    override val asDocument: Document
        get() = Document(name)

    override fun interpret(parameters: Map<String, Label>): Label =
        parameters[name] ?: throw Exception("label parameter $name not found")

    override fun rename(renamer: (String) -> String): LabelExpression =
        LabelParameter(renamer(name))
}

data class LabelJoin(val lhs: LabelExpression, val rhs: LabelExpression) : LabelExpression() {
    override val asDocument: Document
        get() = listOf(lhs.asDocument * Document("⊔") * rhs.asDocument).tupled()

    override fun interpret(parameters: Map<String, Label>): Label =
        lhs.interpret(parameters).join(rhs.interpret(parameters))

    override fun rename(renamer: (String) -> String): LabelExpression =
        LabelJoin(lhs.rename(renamer), rhs.rename(renamer))
}

data class LabelMeet(val lhs: LabelExpression, val rhs: LabelExpression) : LabelExpression() {
    override val asDocument: Document
        get() = listOf(lhs.asDocument * Document("⊓") * rhs.asDocument).tupled()

    override fun interpret(parameters: Map<String, Label>): Label =
        lhs.interpret(parameters).meet(rhs.interpret(parameters))

    override fun rename(renamer: (String) -> String): LabelExpression =
        LabelMeet(lhs.rename(renamer), rhs.rename(renamer))
}

data class LabelAnd(val lhs: LabelExpression, val rhs: LabelExpression) : LabelExpression() {
    override val asDocument: Document
        get() = listOf(lhs.asDocument * Document("&") * rhs.asDocument).tupled()

    override fun interpret(parameters: Map<String, Label>): Label =
        lhs.interpret(parameters).and(rhs.interpret(parameters))

    override fun rename(renamer: (String) -> String): LabelExpression =
        LabelAnd(lhs.rename(renamer), rhs.rename(renamer))
}

data class LabelOr(val lhs: LabelExpression, val rhs: LabelExpression) : LabelExpression() {
    override val asDocument: Document
        get() = listOf(lhs.asDocument * Document("|") * rhs.asDocument).tupled()

    override fun interpret(parameters: Map<String, Label>): Label =
        lhs.interpret(parameters).or(rhs.interpret(parameters))

    override fun rename(renamer: (String) -> String): LabelExpression =
        LabelOr(lhs.rename(renamer), rhs.rename(renamer))
}

data class LabelConfidentiality(val value: LabelExpression) : LabelExpression() {
    override val asDocument: Document
        get() = value.asDocument + Document("->")

    override fun interpret(parameters: Map<String, Label>): Label =
        value.interpret(parameters).confidentiality()

    override fun rename(renamer: (String) -> String): LabelExpression =
        LabelConfidentiality(value.rename(renamer))
}

data class LabelIntegrity(val value: LabelExpression) : LabelExpression() {
    override val asDocument: Document
        get() = value.asDocument + Document("<-")

    override fun interpret(parameters: Map<String, Label>): Label =
        value.interpret(parameters).integrity()

    override fun rename(renamer: (String) -> String): LabelExpression =
        LabelIntegrity(value.rename(renamer))
}

object LabelBottom : LabelExpression() {
    override val asDocument: Document
        get() = Document("⊥")

    override fun interpret(parameters: Map<String, Label>): Label =
        Label.strongest

    override fun rename(renamer: (String) -> String): LabelExpression = this
}

object LabelTop : LabelExpression() {
    override val asDocument: Document
        get() = Document("⊤")

    override fun interpret(parameters: Map<String, Label>): Label =
        Label.weakest

    override fun rename(renamer: (String) -> String): LabelExpression = this
}
