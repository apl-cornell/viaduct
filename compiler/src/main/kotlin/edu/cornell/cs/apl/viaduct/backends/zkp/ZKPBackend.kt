package edu.cornell.cs.apl.viaduct.backends.zkp

import edu.cornell.cs.apl.viaduct.backends.Backend
import edu.cornell.cs.apl.viaduct.codegeneration.CodeGenerator
import edu.cornell.cs.apl.viaduct.codegeneration.CodeGeneratorContext
import edu.cornell.cs.apl.viaduct.parsing.ProtocolParser
import edu.cornell.cs.apl.viaduct.selection.ProtocolComposer
import edu.cornell.cs.apl.viaduct.selection.ProtocolFactory
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.ProtocolName
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode

object ZKPBackend : Backend {
    override val protocols: Set<ProtocolName>
        get() = setOf(ZKP.protocolName)

    override val protocolParsers: Map<ProtocolName, ProtocolParser<Protocol>>
        get() = mapOf(ZKP.protocolName to ZKPProtocolParser)

    override fun protocolFactory(program: ProgramNode): ProtocolFactory = ZKPProtocolFactory(program)

    override val protocolComposer: ProtocolComposer
        get() = ZKPProtocolComposer

    override fun codeGenerator(context: CodeGeneratorContext): CodeGenerator = TODO()
}
