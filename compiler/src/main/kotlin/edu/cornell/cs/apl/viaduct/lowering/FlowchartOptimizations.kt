package edu.cornell.cs.apl.viaduct.lowering

/* Instead of jumping to an empty block, jump to its successor instead. */
fun FlowchartProgram.removeEmptyBlocks(): FlowchartProgram {
    // find empty blocks with a unique successor
    val initEmptyBlockMap: Map<RegularBlockLabel, RegularBlockLabel> =
        this.blocks
            .filter { kv -> kv.value.statements.isEmpty() && kv.value.jump is Goto }
            .map { kv -> kv.key to (kv.value.jump as Goto).label }
            .toMap()

    // chain relabelings together
    val emptyBlockMap = mutableMapOf<RegularBlockLabel, RegularBlockLabel>()
    for (kv in initEmptyBlockMap) {
        var relabel = kv.value
        while (initEmptyBlockMap.containsKey(relabel)) {
            relabel = initEmptyBlockMap[relabel]!!
        }
        emptyBlockMap[kv.key] = relabel
    }

    return FlowchartProgram(
        this.blocks
            .filter { kv -> !emptyBlockMap.containsKey(kv.key) }
            .map { kv ->
                val renamedJump =
                    when (val jump = kv.value.jump) {
                        is Goto -> emptyBlockMap[jump.label]?.let { Goto(it) } ?: jump

                        is GotoIf -> {
                            val renamedThenLabel = emptyBlockMap[jump.thenLabel] ?: jump.thenLabel
                            val renamedElseLabel = emptyBlockMap[jump.elseLabel] ?: jump.elseLabel
                            jump.copy(thenLabel = renamedThenLabel, elseLabel = renamedElseLabel)
                        }

                        RegularHalt -> RegularHalt

                        // Kotlin compiler is too dumb to recognize that ResidualHalt
                        // is not a valid case, so we need this else case
                        else -> TODO()
                    }

                kv.key to kv.value.copy(jump = renamedJump)
            }
            .toMap()
    )
}
