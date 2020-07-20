package edu.cornell.cs.apl.viaduct.backend

import edu.cornell.cs.apl.viaduct.syntax.JumpLabel

class LoopBreakSignal(val jumpLabel: JumpLabel?): Exception() {
    constructor() : this(null)
}
