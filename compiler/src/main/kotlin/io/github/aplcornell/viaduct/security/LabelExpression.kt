package io.github.aplcornell.viaduct.security

import io.github.aplcornell.viaduct.algebra.FreeDistributiveLattice
import io.github.aplcornell.viaduct.analysis.HostTrustConfiguration
import io.github.aplcornell.viaduct.prettyprinting.Document
import io.github.aplcornell.viaduct.prettyprinting.PrettyPrintable
import io.github.aplcornell.viaduct.prettyprinting.plus
import io.github.aplcornell.viaduct.prettyprinting.times
import io.github.aplcornell.viaduct.prettyprinting.tupled
import io.github.aplcornell.viaduct.syntax.Host
import io.github.aplcornell.viaduct.syntax.LabelVariable

sealed class LabelExpression : PrettyPrintable {
    // TODO: put this in IFC check, Label -> SecurityLattice
    abstract fun interpret(): Label

    // TODO: put this in elaboration
    abstract fun rename(renamer: (String) -> String = { x -> x }): LabelExpression
}

data class LabelLiteral(val name: Host) : LabelExpression() {
    override fun toDocument(): Document = name.toDocument()

    override fun interpret(): Label =
        name.label

    override fun rename(renamer: (String) -> String): LabelExpression = this
}

data class LabelParameter(val name: LabelVariable) : LabelExpression() {
    override fun toDocument(): Document = name.toDocument()

    override fun interpret(): Label =
        name.label

    override fun rename(renamer: (String) -> String): LabelExpression =
        LabelParameter(LabelVariable(renamer(name.name)))
}

data class LabelJoin(val lhs: LabelExpression, val rhs: LabelExpression) : LabelExpression() {
    override fun toDocument(): Document = listOf(lhs.toDocument() * Document("⊔") * rhs.toDocument()).tupled()

    override fun interpret(): Label =
        lhs.interpret().join(rhs.interpret())

    override fun rename(renamer: (String) -> String): LabelExpression =
        LabelJoin(lhs.rename(renamer), rhs.rename(renamer))
}

data class LabelMeet(val lhs: LabelExpression, val rhs: LabelExpression) : LabelExpression() {
    override fun toDocument(): Document = listOf(lhs.toDocument() * Document("⊓") * rhs.toDocument()).tupled()

    override fun interpret(): Label =
        lhs.interpret().meet(rhs.interpret())

    override fun rename(renamer: (String) -> String): LabelExpression =
        LabelMeet(lhs.rename(renamer), rhs.rename(renamer))
}

data class LabelAnd(val lhs: LabelExpression, val rhs: LabelExpression) : LabelExpression() {
    override fun toDocument(): Document = listOf(lhs.toDocument() * Document("&") * rhs.toDocument()).tupled()

    override fun interpret(): Label =
        lhs.interpret().and(rhs.interpret())

    override fun rename(renamer: (String) -> String): LabelExpression =
        LabelAnd(lhs.rename(renamer), rhs.rename(renamer))
}

data class LabelOr(val lhs: LabelExpression, val rhs: LabelExpression) : LabelExpression() {
    override fun toDocument(): Document = listOf(lhs.toDocument() * Document("|") * rhs.toDocument()).tupled()

    override fun interpret(): Label =
        lhs.interpret().or(rhs.interpret())

    override fun rename(renamer: (String) -> String): LabelExpression =
        LabelOr(lhs.rename(renamer), rhs.rename(renamer))
}

data class LabelConfidentiality(val value: LabelExpression) : LabelExpression() {
    override fun toDocument(): Document = value.toDocument() + Document("->")

    // Why do we need bounds to be separate?
    override fun interpret(): Label =
        value.interpret().confidentiality()

    override fun rename(renamer: (String) -> String): LabelExpression =
        LabelConfidentiality(value.rename(renamer))
}

data class LabelIntegrity(val value: LabelExpression) : LabelExpression() {
    override fun toDocument(): Document = value.toDocument() + Document("<-")

    override fun interpret(): Label =
        value.interpret().integrity()

    override fun rename(renamer: (String) -> String): LabelExpression =
        LabelIntegrity(value.rename(renamer))
}

object LabelBottom : LabelExpression() {
    override fun toDocument(): Document = Document("⊥")

    override fun interpret(): Label =
        SecurityLattice
            .Bounds<LabelComponent>(FreeDistributiveLattice.bounds())
            .strongest

    override fun rename(renamer: (String) -> String): LabelExpression = this
}

object LabelTop : LabelExpression() {
    override fun toDocument(): Document = Document("⊤")

    override fun interpret(): Label =
        SecurityLattice
            .Bounds<LabelComponent>(FreeDistributiveLattice.bounds())
            .weakest

    override fun rename(renamer: (String) -> String): LabelExpression = this
}

fun interpret(label: Label, trustConfiguration: HostTrustConfiguration): LabelExpression {
    val con = label.confidentialityComponent.joinOfMeets
        .map { meet ->
            meet.filterIsInstance<ConfidentialityComponent<Principal>>()
                .map {
                    when (val principal = it.principal) {
                        is HostPrincipal -> LabelLiteral(principal.host)
                        is PolymorphicPrincipal -> LabelParameter(principal.labelVariable)
                    }
                }.reduceOrNull { acc, e -> LabelAnd(acc, e) } ?: LabelTop
        }.reduceOrNull { acc, e -> LabelOr(acc, e) } ?: LabelBottom
    val int = label.integrityComponent.joinOfMeets
        .map { meet ->
            meet.filterIsInstance<IntegrityComponent<Principal>>()
                .map {
                    when (val principal = it.principal) {
                        is HostPrincipal -> LabelLiteral(principal.host)
                        is PolymorphicPrincipal -> LabelParameter(principal.labelVariable)
                    }
                }.reduceOrNull { acc, e -> LabelAnd(acc, e) } ?: LabelTop
        }.reduceOrNull { acc, e -> LabelOr(acc, e) } ?: LabelBottom
    val result = LabelAnd(LabelConfidentiality(con), LabelIntegrity(int))
    // remove components that are not on the same side of label projection and assert
    // that it is still correct
    // TODO: Prove this is correct or think of new label model
    assert(trustConfiguration.equals(label, result.interpret()))
    return result
}
