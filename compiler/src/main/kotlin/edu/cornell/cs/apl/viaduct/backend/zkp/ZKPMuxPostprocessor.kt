package edu.cornell.cs.apl.viaduct.backend.zkp

import edu.cornell.cs.apl.viaduct.analysis.NameAnalysis
import edu.cornell.cs.apl.viaduct.passes.ProgramPostprocessor
import edu.cornell.cs.apl.viaduct.passes.mux
import edu.cornell.cs.apl.viaduct.protocols.ZKP
import edu.cornell.cs.apl.viaduct.syntax.FunctionName
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProcessDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import kotlinx.collections.immutable.persistentSetOf

object ZKPMuxPostprocessor : ProgramPostprocessor {
    override fun postprocess(splitProgram: ProgramNode): ProgramNode {
        val nameAnalysis = NameAnalysis.get(splitProgram)
        val zkpProcInfo =
            splitProgram
                .filterIsInstance<ProcessDeclarationNode>()
                .filter { procDecl -> procDecl.protocol.value is ZKP }
                .map { procDecl ->
                    Pair(procDecl.protocol.value, nameAnalysis.reachableFunctions(procDecl.body))
                }
                .unzip()

        val zkpProcesses: Set<Protocol> = zkpProcInfo.first.toSet()
        val zkpFunctions: Set<FunctionName> =
            zkpProcInfo.second.fold(persistentSetOf()) { acc, fset -> acc.addAll(fset) }
        return splitProgram.mux(zkpProcesses, zkpFunctions)
    }
}
