package io.github.apl_cornell.viaduct.backends.zkp

import io.github.apl_cornell.viaduct.backends.Backend
import io.github.apl_cornell.viaduct.codegeneration.CodeGenerator
import io.github.apl_cornell.viaduct.codegeneration.CodeGeneratorContext
import io.github.apl_cornell.viaduct.parsing.ProtocolParser
import io.github.apl_cornell.viaduct.selection.ProtocolComposer
import io.github.apl_cornell.viaduct.selection.ProtocolFactory
import io.github.apl_cornell.viaduct.syntax.Protocol
import io.github.apl_cornell.viaduct.syntax.ProtocolName
import io.github.apl_cornell.viaduct.syntax.intermediate.ProgramNode

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
