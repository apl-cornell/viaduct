package edu.cornell.cs.apl.viaduct.backend

import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.ProtocolName
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BlockNode

interface CppBackend {
    val supportedProtocols: Set<ProtocolName>

    val extraStartArguments: List<CppFormalDecl>

    fun extraFunctionArguments(protocol: Protocol): List<CppFormalDecl>

    fun buildProcessObject(protocol: Protocol, procName: CppIdentifier, funcName: CppIdentifier): List<CppStatement>

    fun compile(block: BlockNode, protocol: Protocol, host: Host): CppBlock
}
