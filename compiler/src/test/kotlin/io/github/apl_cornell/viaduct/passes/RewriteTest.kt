package io.github.apl_cornell.viaduct.passes

import io.github.apl_cornell.viaduct.algebra.FreeDistributiveLattice
import io.github.apl_cornell.viaduct.security.ConfidentialityComponent
import io.github.apl_cornell.viaduct.security.HostPrincipal
import io.github.apl_cornell.viaduct.security.IntegrityComponent
import io.github.apl_cornell.viaduct.security.LabelAnd
import io.github.apl_cornell.viaduct.security.LabelBottom
import io.github.apl_cornell.viaduct.security.LabelJoin
import io.github.apl_cornell.viaduct.security.LabelLiteral
import io.github.apl_cornell.viaduct.security.LabelMeet
import io.github.apl_cornell.viaduct.security.LabelOr
import io.github.apl_cornell.viaduct.security.LabelParameter
import io.github.apl_cornell.viaduct.security.LabelTop
import io.github.apl_cornell.viaduct.security.PolymorphicPrincipal
import io.github.apl_cornell.viaduct.security.Principal
import io.github.apl_cornell.viaduct.syntax.Host
import io.github.apl_cornell.viaduct.syntax.LabelVariable
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class RewriteTest {
    private fun h(s: String) = Host(s)
    private fun p(s: String) = LabelVariable(s)
    private fun hp(s: String) = HostPrincipal(h(s))
    private fun pp(s: String) = PolymorphicPrincipal(p(s))
    private fun hpi(s: String) = IntegrityComponent(hp(s) as Principal) as PrincipalComponent
    private fun hpc(s: String) = ConfidentialityComponent(hp(s) as Principal) as PrincipalComponent
    private fun ppi(s: String) = IntegrityComponent(pp(s) as Principal) as PrincipalComponent
    private fun ppc(s: String) = ConfidentialityComponent(pp(s) as Principal) as PrincipalComponent

    private fun fhpi(s: String) = FreeDistributiveLattice(hpi(s))
    private fun fhpc(s: String) = FreeDistributiveLattice(hpc(s))
    private fun fppi(s: String) = FreeDistributiveLattice(ppi(s))
    private fun fppc(s: String) = FreeDistributiveLattice(ppc(s))

    private fun hl(s: String) = LabelLiteral(h(s))
    private fun pl(s: String) = LabelParameter(p(s))

    private val top = FreeDistributiveLattice.bounds<PrincipalComponent>().top
    private val bottom = FreeDistributiveLattice.bounds<PrincipalComponent>().bottom
    private val emptyRewrite = Rewrite(mapOf())
    private val easyRewrite = Rewrite(
        mapOf(
            (ppc("A") to fhpc("alice")), (ppi("A") to fhpi("alice")),
            (ppc("B") to fhpc("bob")), (ppi("B") to fhpi("bob")),
            (ppc("C") to fhpc("chuck")), (ppi("C") to fhpi("chuck"))
        )
    )
    private val hardRewrite = Rewrite(
        mapOf(
            (ppc("A") to top), (ppi("A") to bottom),
            (ppc("B") to fhpc("alice").join(fhpc("bob"))), (ppi("B") to fhpi("alice").meet(fhpi("bob"))),
            (ppc("C") to fhpc("chuck")), (ppi("C") to fhpi("chuck"))
        )
    )

    /* test on Label LabelConstants (FDL<PrincipalComponent>) */
    @Test
    fun testLabelConstant() {
        assertEquals(top, emptyRewrite.rewrite(top))
        assertEquals(top, easyRewrite.rewrite(top))
        assertEquals(top, hardRewrite.rewrite(top))
        assertEquals(bottom, emptyRewrite.rewrite(bottom))
        assertEquals(bottom, easyRewrite.rewrite(bottom))
        assertEquals(bottom, hardRewrite.rewrite(bottom))
        assertEquals(
            fhpc("alice").join(fhpc("bob")).meet(fhpc("chuck")),
            emptyRewrite.rewrite(fhpc("alice").join(fhpc("bob")).meet(fhpc("chuck")))
        )
        assertEquals(
            fhpc("alice").join(fhpc("bob")).meet(fhpc("chuck")),
            easyRewrite.rewrite(fhpc("alice").join(fhpc("bob")).meet(fhpc("chuck")))
        )
        assertEquals(
            fhpc("alice").join(fhpc("bob")).meet(fhpc("chuck")),
            hardRewrite.rewrite(fhpc("alice").join(fhpc("bob")).meet(fhpc("chuck")))
        )
        assertThrows<NullPointerException> { emptyRewrite.rewrite(fppc("A")) }
        assertEquals(fhpc("alice"), easyRewrite.rewrite(fppc("A")))
        assertEquals(top, hardRewrite.rewrite(fppc("A")))
        assertEquals(
            fhpc("alice").join(fhpc("bob")).meet(fhpc("chuck")),
            easyRewrite.rewrite(fppc("A").join(fppc("B")).meet(fppc("C")))
        )
        assertEquals(
            fhpc("chuck"),
            hardRewrite.rewrite(fppc("A").join(fppc("B")).meet(fppc("C")))
        )
        assertEquals(
            fhpc("alice").join(fhpc("bob")).meet(fhpc("chuck")),
            hardRewrite.rewrite(fppc("A").meet(fppc("B")).meet(fppc("C")))
        )
        assertEquals(
            fhpi("alice").meet(fhpi("bob")).meet(fhpi("chuck")),
            hardRewrite.rewrite(fppi("A").join(fppi("B")).meet(fppi("C")))
        )
    }

    /* test on Label LabelConstants (FDL<PrincipalComponent>) */
    @Test
    fun testLabelExpression() {
        assertEquals(top, emptyRewrite.rewrite(LabelTop).interpret().confidentialityComponent)
        assertEquals(top, easyRewrite.rewrite(LabelTop).interpret().confidentialityComponent)
        assertEquals(top, hardRewrite.rewrite(LabelTop).interpret().confidentialityComponent)
        assertEquals(bottom, emptyRewrite.rewrite(LabelBottom).interpret().confidentialityComponent)
        assertEquals(bottom, easyRewrite.rewrite(LabelBottom).interpret().confidentialityComponent)
        assertEquals(bottom, hardRewrite.rewrite(LabelBottom).interpret().confidentialityComponent)
        assertEquals(
            fhpc("alice").join(fhpc("bob")).meet(fhpc("chuck")),
            emptyRewrite.rewrite(
                LabelAnd(LabelOr(hl("alice"), hl("bob")), hl("chuck"))
            ).interpret().confidentialityComponent
        )
        assertEquals(
            fhpc("alice").join(fhpc("bob")).meet(fhpc("chuck")),
            easyRewrite.rewrite(
                LabelAnd(
                    LabelOr(hl("alice"), hl("bob")),
                    hl("chuck")
                )
            ).interpret().confidentialityComponent
        )
        assertEquals(
            fhpc("alice").join(fhpc("bob")).meet(fhpc("chuck")),
            hardRewrite.rewrite(
                LabelAnd(
                    LabelOr(hl("alice"), hl("bob")),
                    hl("chuck")
                )
            ).interpret().confidentialityComponent
        )
        assertThrows<NullPointerException> { emptyRewrite.rewrite(pl("A")) }
        assertEquals(fhpc("alice"), easyRewrite.rewrite(pl("A")).interpret().confidentialityComponent)
        assertEquals(top, hardRewrite.rewrite(pl("A")).interpret().confidentialityComponent)
        assertEquals(bottom, hardRewrite.rewrite(pl("A")).interpret().integrityComponent)

        assertEquals(
            fhpi("alice").meet(fhpi("bob")).meet(fhpi("chuck")),
            hardRewrite.rewrite(LabelAnd(LabelOr(pl("A"), pl("B")), pl("C"))).interpret().integrityComponent
        )
        assertEquals(
            fhpi("alice").meet(fhpi("bob")).meet(fhpi("chuck")),
            hardRewrite.rewrite(LabelMeet(LabelJoin(pl("A"), pl("B")), pl("C"))).interpret().integrityComponent
        )

        assertEquals(
            fhpc("alice").join(fhpc("bob")).join(fhpc("chuck")),
            hardRewrite.rewrite(LabelOr(LabelAnd(pl("A"), pl("B")), pl("C"))).interpret().confidentialityComponent
        )
        assertEquals(
            fhpc("alice").join(fhpc("bob")).join(fhpc("chuck")),
            hardRewrite.rewrite(LabelMeet(LabelJoin(pl("A"), pl("B")), pl("C"))).interpret().confidentialityComponent
        )

        assertEquals(
            fhpi("chuck"),
            hardRewrite.rewrite(LabelOr(LabelAnd(pl("A"), pl("B")), pl("C"))).interpret().integrityComponent
        )
        assertEquals(
            fhpi("chuck"),
            hardRewrite.rewrite(LabelJoin(LabelMeet(pl("A"), pl("B")), pl("C"))).interpret().integrityComponent
        )

        assertEquals(
            fhpc("chuck"),
            hardRewrite.rewrite(LabelAnd(LabelOr(pl("A"), pl("B")), pl("C"))).interpret().confidentialityComponent
        )
        assertEquals(
            fhpc("chuck"),
            hardRewrite.rewrite(LabelJoin(LabelMeet(pl("A"), pl("B")), pl("C"))).interpret().confidentialityComponent
        )
    }
}
