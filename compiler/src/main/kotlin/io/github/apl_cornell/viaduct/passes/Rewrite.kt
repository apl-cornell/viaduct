package io.github.apl_cornell.viaduct.passes

import io.github.apl_cornell.viaduct.algebra.FreeDistributiveLattice
import io.github.apl_cornell.viaduct.security.ConfidentialityComponent
import io.github.apl_cornell.viaduct.security.HostPrincipal
import io.github.apl_cornell.viaduct.security.IntegrityComponent
import io.github.apl_cornell.viaduct.security.Label
import io.github.apl_cornell.viaduct.security.LabelAnd
import io.github.apl_cornell.viaduct.security.LabelBottom
import io.github.apl_cornell.viaduct.security.LabelConfidentiality
import io.github.apl_cornell.viaduct.security.LabelExpression
import io.github.apl_cornell.viaduct.security.LabelIntegrity
import io.github.apl_cornell.viaduct.security.LabelJoin
import io.github.apl_cornell.viaduct.security.LabelLiteral
import io.github.apl_cornell.viaduct.security.LabelMeet
import io.github.apl_cornell.viaduct.security.LabelOr
import io.github.apl_cornell.viaduct.security.LabelParameter
import io.github.apl_cornell.viaduct.security.LabelTop
import io.github.apl_cornell.viaduct.security.PolymorphicPrincipal

class Rewrite(private val rewrites: Map<PrincipalComponent, LabelConstant>) {

    /**
     * Given a map that maps element to expressions, rewrite by substitution.
     */
    fun rewrite(labelConstant: LabelConstant): LabelConstant =
        labelConstant.joinOfMeets.fold(FreeDistributiveLattice.bounds<PrincipalComponent>().bottom) { accOut, meet ->
            accOut.join(
                meet.fold(FreeDistributiveLattice.bounds<PrincipalComponent>().top) { accIn, e ->
                    accIn.meet(
                        when (e.principal) {
                            is HostPrincipal -> FreeDistributiveLattice(e)
                            is PolymorphicPrincipal -> rewrites[e]!!
                        }
                    )
                }
            )
        }

    /**
     * Given a label with polymorphic label and a rewrite map, return a label without polymorphic labels
     */
    fun rewrite(label: Label): Label =
        Label(rewrite(label.confidentialityComponent), rewrite(label.integrityComponent))

    fun rewrite(l: LabelExpression): LabelExpression =
        when (l) {
            is LabelParameter -> {
                // TODO: Catch nullptr exception or do a dedicated check?
                // Or we do a pass that checks for undeclared label parameters
                val confidentialityRewrite =
                    LabelConfidentiality(
                        rewrites[ConfidentialityComponent(PolymorphicPrincipal(l.name))]!!.joinOfMeets
                            .map { meet ->
                                meet.map { LabelLiteral(((it as ConfidentialityComponent).principal as HostPrincipal).host) }
                                    .reduceOrNull<LabelExpression, LabelExpression> { acc, e -> LabelAnd(acc, e) }
                                    ?: LabelTop
                            }
                            .reduceOrNull { acc, e -> LabelOr(acc, e) } ?: LabelBottom
                    )

                val integrityRewrite =
                    LabelIntegrity(
                        rewrites[IntegrityComponent(PolymorphicPrincipal(l.name))]!!.joinOfMeets
                            .map { meet ->
                                meet
                                    .map { LabelLiteral(((it as IntegrityComponent).principal as HostPrincipal).host) }
                                    .reduceOrNull<LabelExpression, LabelExpression> { acc, e -> LabelAnd(acc, e) }
                                    ?: LabelTop
                            }
                            .reduceOrNull { acc, e -> LabelOr(acc, e) } ?: LabelBottom
                    )

                LabelAnd(confidentialityRewrite, integrityRewrite)
            }

            is LabelAnd -> LabelAnd(rewrite(l.lhs), rewrite(l.rhs))
            is LabelOr -> LabelOr(rewrite(l.lhs), rewrite(l.rhs))
            is LabelJoin -> LabelJoin(rewrite(l.lhs), rewrite(l.rhs))
            is LabelMeet -> LabelMeet(rewrite(l.lhs), rewrite(l.rhs))
            is LabelConfidentiality -> LabelConfidentiality(rewrite(l.value))
            is LabelIntegrity -> LabelIntegrity(rewrite(l.value))
            else -> l
        }
}
