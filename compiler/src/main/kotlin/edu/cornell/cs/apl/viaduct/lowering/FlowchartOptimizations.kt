package edu.cornell.cs.apl.viaduct.lowering

import java.util.LinkedList
import java.util.Queue

/* Instead of jumping to an empty block, jump to its successor instead. */
fun FlowchartProgram.removeEmptyBlocks(): FlowchartProgram {
    // find empty blocks with a unique successor
    val emptyBlockMap: Map<RegularBlockLabel, RegularBlockLabel> =
        this.blocks
            .filter { kv -> kv.value.statements.isEmpty() && kv.value.jump is Goto }
            .map { kv -> kv.key to (kv.value.jump as Goto).label }
            .toMap()

    // chain relabelings together
    val jumpRelabelMap = mutableMapOf<RegularBlockLabel, RegularBlockLabel>()
    val blockRelableMap = mutableMapOf<RegularBlockLabel, RegularBlockLabel>()
    for (kv in emptyBlockMap) {
        var relabel = kv.value
        while (emptyBlockMap.containsKey(relabel)) {
            relabel = emptyBlockMap[relabel]!!
        }
        jumpRelabelMap[kv.key] = relabel
    }

    // if entry point is removed, new entry point is the block it points to
    if (jumpRelabelMap.containsKey(ENTRY_POINT_LABEL)) {
        val newEntryPointLabel = jumpRelabelMap[ENTRY_POINT_LABEL]!!
        jumpRelabelMap[newEntryPointLabel] = ENTRY_POINT_LABEL
        blockRelableMap[newEntryPointLabel] = ENTRY_POINT_LABEL
    }

    val newBlocks =
        this.blocks
            .filter { kv -> !emptyBlockMap.containsKey(kv.key) }
            .map { kv ->
                val renamedJump =
                    when (val jump = kv.value.jump) {
                        is Goto -> jumpRelabelMap[jump.label]?.let { Goto(it) } ?: jump

                        is GotoIf -> {
                            val renamedThenLabel = jumpRelabelMap[jump.thenLabel] ?: jump.thenLabel
                            val renamedElseLabel = jumpRelabelMap[jump.elseLabel] ?: jump.elseLabel
                            jump.copy(thenLabel = renamedThenLabel, elseLabel = renamedElseLabel)
                        }

                        RegularHalt -> RegularHalt

                        // Kotlin compiler is too dumb to recognize that ResidualHalt
                        // is not a valid case, so we need this else case
                        else -> TODO()
                    }

                val newLabel = blockRelableMap[kv.key] ?: kv.key

                newLabel to kv.value.copy(jump = renamedJump)
            }
            .toMap()

    return FlowchartProgram(newBlocks)
}

/** Inline "linear" set of blocks
 * e.g. a CFG A -> B -> C should just be one block A */
fun FlowchartProgram.inlineBlocks(): FlowchartProgram {
    val newBlocks = mutableMapOf<RegularBlockLabel, LoweredBasicBlock<RegularBlockLabel>>()

    val worklist: Queue<RegularBlockLabel> = LinkedList()
    worklist.add(ENTRY_POINT_LABEL)

    while (worklist.isNotEmpty()) {
        val label = worklist.remove()
        var curBlock = this.block(label)!!
        val curStmts = mutableListOf<LoweredStatement>()

        while (true) {
            curStmts.addAll(curBlock.statements)

            val jump = curBlock.jump
            if (jump is Goto) {
                // if the current block is the only predecessor for the successor,
                // inline the successor
                if (this.predecessorMap[jump.label]!!.size == 1) {
                    curBlock = this.block(jump.label)!!

                } else break
            } else break
        }

        newBlocks[label] = LoweredBasicBlock(curStmts, curBlock.jump)
        worklist.addAll(
            curBlock.successors().filter { !newBlocks.containsKey(it) }
        )
    }

    return FlowchartProgram(newBlocks)
}

fun FlowchartProgram.optimize(): FlowchartProgram {
    return this.removeEmptyBlocks().inlineBlocks()
}
