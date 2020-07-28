package edu.cornell.cs.apl.viaduct.backend

import edu.cornell.cs.apl.viaduct.syntax.JumpLabel
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BreakNode

internal class LoopBreakSignal(val breakNode: BreakNode) : Exception() {
    val jumpLabel: JumpLabel = breakNode.jumpLabel.value
}
