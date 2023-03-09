package io.github.aplcornell.viaduct.passes

import io.github.aplcornell.viaduct.algebra.FreeDistributiveLattice
import io.github.aplcornell.viaduct.analysis.HostTrustConfiguration
import io.github.aplcornell.viaduct.security.ConfidentialityComponent
import io.github.aplcornell.viaduct.security.HostPrincipal
import io.github.aplcornell.viaduct.security.IntegrityComponent
import io.github.aplcornell.viaduct.security.Label
import io.github.aplcornell.viaduct.security.LabelAnd
import io.github.aplcornell.viaduct.security.LabelBottom
import io.github.aplcornell.viaduct.security.LabelConfidentiality
import io.github.aplcornell.viaduct.security.LabelExpression
import io.github.aplcornell.viaduct.security.LabelIntegrity
import io.github.aplcornell.viaduct.security.LabelJoin
import io.github.aplcornell.viaduct.security.LabelLiteral
import io.github.aplcornell.viaduct.security.LabelMeet
import io.github.aplcornell.viaduct.security.LabelOr
import io.github.aplcornell.viaduct.security.LabelParameter
import io.github.aplcornell.viaduct.security.LabelTop
import io.github.aplcornell.viaduct.security.PolymorphicPrincipal
import io.github.aplcornell.viaduct.security.interpret

class Rewrite(
    private val rewrites: Map<PrincipalComponent, LabelConstant>,
    private val hostTrustConfiguration: HostTrustConfiguration,
) {

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
                        },
                    )
                },
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
                interpret(
                    Label(
                        rewrites[ConfidentialityComponent(PolymorphicPrincipal(l.name))]!!,
                        rewrites[IntegrityComponent(PolymorphicPrincipal(l.name))]!!,
                    ),
                    hostTrustConfiguration,
                )
            }

            is LabelAnd -> LabelAnd(rewrite(l.lhs), rewrite(l.rhs))
            is LabelOr -> LabelOr(rewrite(l.lhs), rewrite(l.rhs))
            is LabelJoin -> LabelJoin(rewrite(l.lhs), rewrite(l.rhs))
            is LabelMeet -> LabelMeet(rewrite(l.lhs), rewrite(l.rhs))
            is LabelConfidentiality -> LabelConfidentiality(rewrite(l.value))
            is LabelIntegrity -> LabelIntegrity(rewrite(l.value))
            is LabelBottom -> l
            is LabelTop -> l
            is LabelLiteral -> l
        }
}
