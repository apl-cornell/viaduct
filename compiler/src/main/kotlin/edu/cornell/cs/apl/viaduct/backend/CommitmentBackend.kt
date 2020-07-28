package edu.cornell.cs.apl.viaduct.backend

import edu.cornell.cs.apl.viaduct.analysis.NameAnalysis
import edu.cornell.cs.apl.viaduct.analysis.TypeAnalysis
import edu.cornell.cs.apl.viaduct.protocols.CommitmentProtocol
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.ProtocolName
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BlockNode

class CommitmentBackend(
    val nameAnalysis: NameAnalysis,
    val typeAnalysis: TypeAnalysis
) : CppBuilder(), CppBackend {

    override val supportedProtocols: Set<ProtocolName>
        get() = setOf(CommitmentProtocol.protocolName)

    override val extraStartArguments: List<CppFormalDecl>
        get() = listOf()

    override fun extraFunctionArguments(protocol: Protocol): List<CppFormalDecl> = listOf()

    override fun buildProcessObject(
        protocol: Protocol,
        procName: CppIdentifier,
        funcName: CppIdentifier
    ): List<CppStatement> = TODO("CommitmentBackend")

    override fun compile(block: BlockNode, protocol: Protocol, host: Host): CppBlock = TODO("CommitmentBackend")
}
