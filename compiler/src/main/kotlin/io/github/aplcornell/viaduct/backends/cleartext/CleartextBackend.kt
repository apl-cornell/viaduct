package io.github.aplcornell.viaduct.backends.cleartext

import io.github.aplcornell.viaduct.backends.Backend
import io.github.aplcornell.viaduct.codegeneration.CodeGenerator
import io.github.aplcornell.viaduct.codegeneration.CodeGeneratorContext
import io.github.aplcornell.viaduct.parsing.ProtocolParser
import io.github.aplcornell.viaduct.selection.ProtocolComposer
import io.github.aplcornell.viaduct.selection.ProtocolFactory
import io.github.aplcornell.viaduct.selection.unions
import io.github.aplcornell.viaduct.syntax.Protocol
import io.github.aplcornell.viaduct.syntax.ProtocolName
import io.github.aplcornell.viaduct.syntax.intermediate.ProgramNode
import io.github.aplcornell.viaduct.circuitcodegeneration.CodeGenerator as CircuitCodeGenerator
import io.github.aplcornell.viaduct.circuitcodegeneration.CodeGeneratorContext as CircuitCodeGeneratorContext

object CleartextBackend : Backend {
    override val protocols: Set<ProtocolName>
        get() = setOf(Local.protocolName, Replication.protocolName)

    override val protocolParsers: Map<ProtocolName, ProtocolParser<Protocol>>
        get() = mapOf(
            Local.protocolName to LocalProtocolParser,
            Replication.protocolName to ReplicationProtocolParser,
        )

    override fun protocolFactory(program: ProgramNode): ProtocolFactory =
        listOf(LocalProtocolFactory(program), ReplicationProtocolFactory(program)).unions()

    override val protocolComposer: ProtocolComposer
        get() = CleartextProtocolComposer

    override fun codeGenerator(context: CodeGeneratorContext): CodeGenerator = CleartextCodeGenerator(context)

    override fun circuitCodeGenerator(context: CircuitCodeGeneratorContext): CircuitCodeGenerator =
        CleartextCircuitCodeGenerator(context)
}
