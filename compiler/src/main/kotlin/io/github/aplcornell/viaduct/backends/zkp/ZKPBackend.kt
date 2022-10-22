package io.github.aplcornell.viaduct.backends.zkp

import io.github.aplcornell.viaduct.backends.Backend
import io.github.aplcornell.viaduct.codegeneration.CodeGenerator
import io.github.aplcornell.viaduct.codegeneration.CodeGeneratorContext
import io.github.aplcornell.viaduct.parsing.ProtocolParser
import io.github.aplcornell.viaduct.selection.ProtocolComposer
import io.github.aplcornell.viaduct.selection.ProtocolFactory
import io.github.aplcornell.viaduct.syntax.Protocol
import io.github.aplcornell.viaduct.syntax.ProtocolName
import io.github.aplcornell.viaduct.syntax.intermediate.ProgramNode

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
