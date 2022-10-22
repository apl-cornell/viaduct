package io.github.aplcornell.viaduct.backend

import io.github.aplcornell.viaduct.syntax.JumpLabel
import io.github.aplcornell.viaduct.syntax.intermediate.BreakNode

internal class LoopBreakSignal(val breakNode: BreakNode) : Exception() {
    val jumpLabel: JumpLabel
        get() = breakNode.jumpLabel.value
}
