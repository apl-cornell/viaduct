package io.github.apl_cornell.viaduct.passes

import io.github.apl_cornell.viaduct.algebra.FreeDistributiveLattice
import io.github.apl_cornell.viaduct.security.ConfidentialityComponent
import io.github.apl_cornell.viaduct.security.HostPrincipal
import io.github.apl_cornell.viaduct.security.IntegrityComponent
import io.github.apl_cornell.viaduct.security.Label
import io.github.apl_cornell.viaduct.security.LabelBottom
import io.github.apl_cornell.viaduct.security.LabelTop
import io.github.apl_cornell.viaduct.security.PolymorphicPrincipal
import io.github.apl_cornell.viaduct.security.Principal
import io.github.apl_cornell.viaduct.syntax.Host
import io.github.apl_cornell.viaduct.syntax.LabelVariable
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class RewriteTest {
    /*private val principalAlice = HostPrincipal(Host("alice"))
    private val principalBob = HostPrincipal(Host("bob"))
    private val principalChuck = HostPrincipal(Host("chuck"))
    private val principalA = PolymorphicPrincipal(LabelVariable("A"))
    private val principalB = PolymorphicPrincipal(LabelVariable("B"))
    private val principalC = PolymorphicPrincipal(LabelVariable("C"))*/
    private val principalAliceI = IntegrityComponent(HostPrincipal(Host("alice")) as Principal) as PrincipalComponent
    private val principalBobI = IntegrityComponent(HostPrincipal(Host("bob")) as Principal) as PrincipalComponent
    private val principalChuckI = IntegrityComponent(HostPrincipal(Host("chuck")) as Principal) as PrincipalComponent
    private val principalAI =
        IntegrityComponent(PolymorphicPrincipal(LabelVariable("A")) as Principal) as PrincipalComponent
    private val principalBI =
        IntegrityComponent(PolymorphicPrincipal(LabelVariable("B")) as Principal) as PrincipalComponent
    private val principalCI =
        IntegrityComponent(PolymorphicPrincipal(LabelVariable("C")) as Principal) as PrincipalComponent
    private val principalAliceC =
        ConfidentialityComponent(HostPrincipal(Host("alice")) as Principal) as PrincipalComponent
    private val principalBobC = ConfidentialityComponent(HostPrincipal(Host("bob")) as Principal) as PrincipalComponent
    private val principalChuckC =
        ConfidentialityComponent(HostPrincipal(Host("chuck")) as Principal) as PrincipalComponent
    private val principalAC =
        ConfidentialityComponent(PolymorphicPrincipal(LabelVariable("A")) as Principal) as PrincipalComponent
    private val principalBC =
        ConfidentialityComponent(PolymorphicPrincipal(LabelVariable("B")) as Principal) as PrincipalComponent
    private val principalCC =
        ConfidentialityComponent(PolymorphicPrincipal(LabelVariable("C")) as Principal) as PrincipalComponent
    private val aliceI = FreeDistributiveLattice(principalAliceI)
    private val bobI = FreeDistributiveLattice(principalBobI)
    private val chuckI = FreeDistributiveLattice(principalChuckI)

    //private val AI = FreeDistributiveLattice(principalAI)
    //private val BI = FreeDistributiveLattice(principalBI)
    //private val CI = FreeDistributiveLattice(principalCI)
    private val aliceC = FreeDistributiveLattice(principalAliceC)
    private val bobC = FreeDistributiveLattice(principalBobC)
    private val chuckC = FreeDistributiveLattice(principalChuckC)
    private val AC = FreeDistributiveLattice(principalAC)

    private val BC = FreeDistributiveLattice(principalBC)
    private val CC = FreeDistributiveLattice(principalCC)
    private val top = FreeDistributiveLattice.bounds<PrincipalComponent>().top
    private val bottom = FreeDistributiveLattice.bounds<PrincipalComponent>().bottom
    private val emptyRewrite = Rewrite(mapOf())
    private val easyRewrite = Rewrite(
        mapOf(
            (principalAC to aliceC), (principalAI to aliceI),
            (principalBC to bobC), (principalBI to bobI),
            (principalCC to chuckC), (principalCI to chuckI)
        )
    )
    private val hardRewrite = Rewrite(
        mapOf(
            (principalAC to top), (principalAI to bottom),
            (principalBC to aliceC.join(bobC)), (principalBI to aliceI.join(bobI)),
            (principalCC to chuckC.meet(bobC)), (principalCI to chuckI.meet(bobI))
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
        assertEquals(aliceC.join(bobC).meet(chuckC), emptyRewrite.rewrite(aliceC.join(bobC).meet(chuckC)))
        assertEquals(aliceC.join(bobC).meet(chuckC), easyRewrite.rewrite(aliceC.join(bobC).meet(chuckC)))
        assertEquals(aliceC.join(bobC).meet(chuckC), hardRewrite.rewrite(aliceC.join(bobC).meet(chuckC)))
        assertThrows<NullPointerException> { emptyRewrite.rewrite(AC) }
        assertEquals(aliceC, easyRewrite.rewrite(AC))
        assertEquals(top, hardRewrite.rewrite(AC))
        assertEquals(aliceC.join(bobC).meet(chuckC), easyRewrite.rewrite(AC.join(BC).meet(CC)))
        assertEquals(aliceC.join(bobC).meet(chuckC), hardRewrite.rewrite(AC.meet(BC).meet(chuckC)))
    }

    /* test on Label LabelConstants (FDL<PrincipalComponent>) */
    @Test
    fun testLabelExpression() {
        assertEquals(Label(top, top), emptyRewrite.rewrite(LabelTop).interpret())
        assertEquals(top, easyRewrite.rewrite(LabelTop).interpret())
        assertEquals(top, hardRewrite.rewrite(LabelTop).interpret())
        assertEquals(bottom, emptyRewrite.rewrite(LabelBottom).interpret())
        assertEquals(bottom, easyRewrite.rewrite(LabelBottom).interpret())
        assertEquals(bottom, hardRewrite.rewrite(LabelBottom).interpret())
        /*assertEquals(aliceC.join(bobC).meet(chuckC), emptyRewrite.rewrite(aliceC.join(bobC).meet(chuckC)))
        assertEquals(aliceC.join(bobC).meet(chuckC), easyRewrite.rewrite(aliceC.join(bobC).meet(chuckC)))
        assertEquals(aliceC.join(bobC).meet(chuckC), hardRewrite.rewrite(aliceC.join(bobC).meet(chuckC)))
        assertThrows<NullPointerException> { emptyRewrite.rewrite(AC) }
        assertEquals(aliceC, easyRewrite.rewrite(AC))
        assertEquals(top, hardRewrite.rewrite(AC))
        assertEquals(aliceC.join(bobC).meet(chuckC), easyRewrite.rewrite(AC.join(BC).meet(CC)))
        assertEquals(aliceC.join(bobC).meet(chuckC), hardRewrite.rewrite(AC.meet(BC).meet(chuckC)))*/
    }
}

