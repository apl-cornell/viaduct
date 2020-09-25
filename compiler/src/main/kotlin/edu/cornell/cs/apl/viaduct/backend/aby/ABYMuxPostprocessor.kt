package edu.cornell.cs.apl.viaduct.backend.aby

import edu.cornell.cs.apl.viaduct.analysis.NameAnalysis
import edu.cornell.cs.apl.viaduct.passes.ProgramPostprocessor
import edu.cornell.cs.apl.viaduct.passes.mux
import edu.cornell.cs.apl.viaduct.protocols.ABY
import edu.cornell.cs.apl.viaduct.syntax.FunctionName
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProcessDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import kotlinx.collections.immutable.persistentSetOf

object ABYMuxPostprocessor : ProgramPostprocessor {
    override fun postprocess(splitProgram: ProgramNode): ProgramNode {
        val nameAnalysis = NameAnalysis.get(splitProgram)
        val abyProcInfo =
            splitProgram
                .filterIsInstance<ProcessDeclarationNode>()
                .filter { procDecl -> procDecl.protocol.value is ABY }
                .map { procDecl ->
                    Pair(procDecl.protocol.value, nameAnalysis.reachableFunctions(procDecl.body))
                }
                .unzip()

        val abyProcesses: Set<Protocol> = abyProcInfo.first.toSet()
        val abyFunctions: Set<FunctionName> =
            abyProcInfo.second.fold(persistentSetOf()) { acc, fset -> acc.addAll(fset) }
        return splitProgram.mux(abyProcesses, abyFunctions)
    }
}
