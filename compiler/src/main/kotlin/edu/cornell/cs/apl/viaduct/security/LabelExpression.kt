package edu.cornell.cs.apl.viaduct.security

import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.prettyprinting.PrettyPrintable
import edu.cornell.cs.apl.prettyprinting.plus
import edu.cornell.cs.apl.prettyprinting.times
import edu.cornell.cs.apl.prettyprinting.tupled
import kotlinx.collections.immutable.persistentMapOf

sealed class LabelExpression : PrettyPrintable {
    abstract fun interpret(parameters: Map<String, Label> = persistentMapOf()): Label
}

data class LabelLiteral(val name: String) : LabelExpression() {
    override val asDocument: Document
        get() = Document(name)

    override fun interpret(parameters: Map<String, Label>): Label =
        Label(Principal(name))
}

data class LabelParameter(val name: String) : LabelExpression() {
    override val asDocument: Document
        get() = Document(name)

    override fun interpret(parameters: Map<String, Label>): Label =
        parameters[name] ?: throw Exception("label parameter $name not found")
}

data class LabelJoin(val lhs: LabelExpression, val rhs: LabelExpression) : LabelExpression() {
    override val asDocument: Document
        get() = listOf(lhs.asDocument * Document("⊔") * rhs.asDocument).tupled()

    override fun interpret(parameters: Map<String, Label>): Label =
        lhs.interpret(parameters).join(rhs.interpret(parameters))
}

data class LabelMeet(val lhs: LabelExpression, val rhs: LabelExpression) : LabelExpression() {
    override val asDocument: Document
        get() = listOf(lhs.asDocument * Document("⊓") * rhs.asDocument).tupled()

    override fun interpret(parameters: Map<String, Label>): Label =
        lhs.interpret(parameters).meet(rhs.interpret(parameters))
}

data class LabelAnd(val lhs: LabelExpression, val rhs: LabelExpression) : LabelExpression() {
    override val asDocument: Document
        get() = listOf(lhs.asDocument * Document("&") * rhs.asDocument).tupled()

    override fun interpret(parameters: Map<String, Label>): Label =
        lhs.interpret(parameters).and(rhs.interpret(parameters))
}

data class LabelOr(val lhs: LabelExpression, val rhs: LabelExpression) : LabelExpression() {
    override val asDocument: Document
        get() = listOf(lhs.asDocument * Document("|") * rhs.asDocument).tupled()

    override fun interpret(parameters: Map<String, Label>): Label =
        lhs.interpret(parameters).or(rhs.interpret(parameters))
}

data class LabelConfidentiality(val value: LabelExpression) : LabelExpression() {
    override val asDocument: Document
        get() = value.asDocument + Document("->")

    override fun interpret(parameters: Map<String, Label>): Label =
        value.interpret(parameters).confidentiality()
}

data class LabelIntegrity(val value: LabelExpression) : LabelExpression() {
    override val asDocument: Document
        get() = value.asDocument + Document("<-")

    override fun interpret(parameters: Map<String, Label>): Label =
        value.interpret(parameters).integrity()
}

object LabelBottom : LabelExpression() {
    override val asDocument: Document
        get() = Document("⊥")

    override fun interpret(parameters: Map<String, Label>): Label =
        Label.strongest
}

object LabelTop : LabelExpression() {
    override val asDocument: Document
        get() = Document("⊤")

    override fun interpret(parameters: Map<String, Label>): Label =
        Label.weakest
}
