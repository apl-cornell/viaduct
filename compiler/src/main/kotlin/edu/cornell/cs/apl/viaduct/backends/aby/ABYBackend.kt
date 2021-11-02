package edu.cornell.cs.apl.viaduct.backends.aby

import edu.cornell.cs.apl.viaduct.backends.Backend
import edu.cornell.cs.apl.viaduct.codegeneration.CodeGenerator
import edu.cornell.cs.apl.viaduct.codegeneration.CodeGeneratorContext
import edu.cornell.cs.apl.viaduct.parsing.ProtocolParser
import edu.cornell.cs.apl.viaduct.selection.ProtocolComposer
import edu.cornell.cs.apl.viaduct.selection.ProtocolFactory
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.ProtocolName
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode

object ABYBackend : Backend {
    override val protocols: Set<ProtocolName>
        get() = setOf(ArithABY.protocolName, BoolABY.protocolName, YaoABY.protocolName)

    override val protocolParsers: Map<ProtocolName, ProtocolParser<Protocol>>
        get() = mapOf(
            ArithABY.protocolName to ArithABYProtocolParser,
            BoolABY.protocolName to BoolABYProtocolParser,
            YaoABY.protocolName to YaoABYProtocolParser
        )

    override fun protocolFactory(program: ProgramNode): ProtocolFactory = ABYProtocolFactory(program)

    override val protocolComposer: ProtocolComposer
        get() = ABYProtocolComposer

    override fun codeGenerator(context: CodeGeneratorContext): CodeGenerator = TODO()
}
