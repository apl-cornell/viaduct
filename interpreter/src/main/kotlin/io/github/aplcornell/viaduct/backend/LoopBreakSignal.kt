package io.github.apl_cornell.viaduct.backend

import io.github.apl_cornell.viaduct.syntax.JumpLabel
import io.github.apl_cornell.viaduct.syntax.intermediate.BreakNode

internal class LoopBreakSignal(val breakNode: BreakNode) : Exception() {
    val jumpLabel: JumpLabel
        get() = breakNode.jumpLabel.value
}
