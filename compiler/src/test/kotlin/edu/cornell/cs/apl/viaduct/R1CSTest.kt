package edu.cornell.cs.apl.viaduct

import edu.cornell.cs.apl.viaduct.backend.zkp.R1CSInstance
import edu.cornell.cs.apl.viaduct.backend.zkp.getOutputWire
import edu.cornell.cs.apl.viaduct.syntax.operators.Addition
import edu.cornell.cs.apl.viaduct.syntax.operators.EqualTo
import edu.cornell.cs.apl.viaduct.syntax.operators.Multiplication
import org.junit.jupiter.api.Test

internal class R1CSTest {

    fun mkInstance(isProver: Boolean): R1CSInstance {
        val instance = R1CSInstance(isProver)

        val o = instance.mkOutput(37)
        val tt = instance.mkPublicInput(2, 1)
        val five = instance.mkPublicInput(0, 7)
        val two = instance.mkPublicInput(1, 2)
        val w = if (isProver) (instance.mkInput(0, 5)) else (instance.mkDummy(0))
        val b =
            (EqualTo.getOutputWire(instance, listOf(
                    (Addition.getOutputWire(instance, listOf(Multiplication.getOutputWire(instance, listOf(w, five)), two))),
                    o)))

        instance.assertEquality(b, tt)
        return instance
    }

    fun testConstraint(f: (Boolean) -> R1CSInstance) {
        val i1 = f(true)
        val kp = i1.genKeypair()
        val pf = i1.makeProof(kp.proving_key)

        val i2 = f(false)
        val vrfy = i2.verifyProof(kp.verification_key, pf)
        assert(vrfy)
    }

    @Test
    fun `unpacking`() {
        testConstraint(::mkInstance)
    }
}
