package io.github.apl_cornell.viaduct.security

import io.github.apl_cornell.apl.prettyprinting.Document
import io.github.apl_cornell.apl.prettyprinting.PrettyPrintable
import io.github.apl_cornell.apl.prettyprinting.plus
import io.github.apl_cornell.apl.prettyprinting.times
import io.github.apl_cornell.apl.prettyprinting.tupled
import io.github.apl_cornell.viaduct.algebra.FreeDistributiveLattice
import io.github.apl_cornell.viaduct.algebra.FreeDistributiveLatticeComponent
import io.github.apl_cornell.viaduct.syntax.Host
import io.github.apl_cornell.viaduct.syntax.LabelVariable


sealed class LabelExpression : PrettyPrintable {
    // TODO: put this in IFC check, Label -> SecurityLattice
    abstract fun interpret(): Label

    // TODO: put this in elaboration
    abstract fun rename(renamer: (String) -> String = { x -> x }): LabelExpression

    // TODO: delete
    abstract fun containsParameters(): Boolean
}

data class LabelLiteral(val name: Host) : LabelExpression() {
    override fun toDocument(): Document = name.toDocument()

    override fun interpret(): Label =
        Label(
            FreeDistributiveLattice(ConfidentialityComponent(HostPrincipal(name))),
            FreeDistributiveLattice(IntegrityComponent(HostPrincipal(name)))
        )

    override fun rename(renamer: (String) -> String): LabelExpression = this

    override fun containsParameters(): Boolean = false
}

data class LabelParameter(val name: LabelVariable) : LabelExpression() {
    override fun toDocument(): Document = name.toDocument()

    override fun interpret(): Label =
        Label(
            FreeDistributiveLattice(ConfidentialityComponent(PolymorphicPrincipal(name))),
            FreeDistributiveLattice(IntegrityComponent(PolymorphicPrincipal(name)))
        )

    override fun rename(renamer: (String) -> String): LabelExpression =
        LabelParameter(LabelVariable(renamer(name.name)))

    override fun containsParameters(): Boolean = true
}

data class LabelJoin(val lhs: LabelExpression, val rhs: LabelExpression) : LabelExpression() {
    override fun toDocument(): Document = listOf(lhs.toDocument() * Document("⊔") * rhs.toDocument()).tupled()

    override fun interpret(): Label =
        lhs.interpret().join(rhs.interpret())

    override fun rename(renamer: (String) -> String): LabelExpression =
        LabelJoin(lhs.rename(renamer), rhs.rename(renamer))

    override fun containsParameters(): Boolean = lhs.containsParameters() || rhs.containsParameters()
}

data class LabelMeet(val lhs: LabelExpression, val rhs: LabelExpression) : LabelExpression() {
    override fun toDocument(): Document = listOf(lhs.toDocument() * Document("⊓") * rhs.toDocument()).tupled()

    override fun interpret(): Label =
        lhs.interpret().meet(rhs.interpret())

    override fun rename(renamer: (String) -> String): LabelExpression =
        LabelMeet(lhs.rename(renamer), rhs.rename(renamer))

    override fun containsParameters(): Boolean = lhs.containsParameters() || rhs.containsParameters()
}

data class LabelAnd(val lhs: LabelExpression, val rhs: LabelExpression) : LabelExpression() {
    override fun toDocument(): Document = listOf(lhs.toDocument() * Document("&") * rhs.toDocument()).tupled()

    override fun interpret(): Label =
        lhs.interpret().and(rhs.interpret())

    override fun rename(renamer: (String) -> String): LabelExpression =
        LabelAnd(lhs.rename(renamer), rhs.rename(renamer))

    override fun containsParameters(): Boolean = lhs.containsParameters() || rhs.containsParameters()
}

data class LabelOr(val lhs: LabelExpression, val rhs: LabelExpression) : LabelExpression() {
    override fun toDocument(): Document = listOf(lhs.toDocument() * Document("|") * rhs.toDocument()).tupled()

    override fun interpret(): Label =
        lhs.interpret().or(rhs.interpret())

    override fun rename(renamer: (String) -> String): LabelExpression =
        LabelOr(lhs.rename(renamer), rhs.rename(renamer))

    override fun containsParameters(): Boolean = lhs.containsParameters() || rhs.containsParameters()
}

data class LabelConfidentiality(val value: LabelExpression) : LabelExpression() {
    override fun toDocument(): Document = value.toDocument() + Document("->")

    // Why do we need bounds to be separate?
    override fun interpret(): Label =
        value.interpret().confidentiality(FreeDistributiveLattice.bounds())

    override fun rename(renamer: (String) -> String): LabelExpression =
        LabelConfidentiality(value.rename(renamer))

    override fun containsParameters(): Boolean = value.containsParameters()
}

data class LabelIntegrity(val value: LabelExpression) : LabelExpression() {
    override fun toDocument(): Document = value.toDocument() + Document("<-")

    override fun interpret(): Label =
        value.interpret().integrity(FreeDistributiveLattice.bounds())

    override fun rename(renamer: (String) -> String): LabelExpression =
        LabelIntegrity(value.rename(renamer))

    override fun containsParameters(): Boolean = value.containsParameters()
}

object LabelBottom : LabelExpression() {
    override fun toDocument(): Document = Document("⊥")

    override fun interpret(): Label =
        SecurityLattice
            .Bounds<FreeDistributiveLatticeComponent>(FreeDistributiveLattice.bounds())
            .strongest


    override fun rename(renamer: (String) -> String): LabelExpression = this

    override fun containsParameters(): Boolean = false
}

object LabelTop : LabelExpression() {
    override fun toDocument(): Document = Document("⊤")

    override fun interpret(): Label =
        SecurityLattice
            .Bounds<FreeDistributiveLatticeComponent>(FreeDistributiveLattice.bounds())
            .weakest

    override fun rename(renamer: (String) -> String): LabelExpression = this

    override fun containsParameters(): Boolean = false
}
