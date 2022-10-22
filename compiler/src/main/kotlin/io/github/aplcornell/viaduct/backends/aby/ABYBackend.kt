package io.github.apl_cornell.viaduct.backends.aby

import io.github.apl_cornell.viaduct.backends.Backend
import io.github.apl_cornell.viaduct.codegeneration.CodeGenerator
import io.github.apl_cornell.viaduct.codegeneration.CodeGeneratorContext
import io.github.apl_cornell.viaduct.parsing.ProtocolParser
import io.github.apl_cornell.viaduct.selection.ProtocolComposer
import io.github.apl_cornell.viaduct.selection.ProtocolFactory
import io.github.apl_cornell.viaduct.syntax.Protocol
import io.github.apl_cornell.viaduct.syntax.ProtocolName
import io.github.apl_cornell.viaduct.syntax.intermediate.ProgramNode

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

    override fun codeGenerator(context: CodeGeneratorContext): CodeGenerator = ABYCodeGenerator(context)
}
