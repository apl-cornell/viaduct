package edu.cornell.cs.apl.viaduct

import com.microsoft.z3.Context
import org.junit.jupiter.api.Test

internal class Z3Test {
    @Test
    fun `z3 works`() {
        val ctx = Context()
        ctx.close()
    }
}
