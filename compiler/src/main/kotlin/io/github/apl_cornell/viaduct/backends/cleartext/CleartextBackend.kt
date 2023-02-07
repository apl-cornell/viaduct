package io.github.apl_cornell.viaduct.backends.cleartext

import io.github.apl_cornell.viaduct.backends.Backend
import io.github.apl_cornell.viaduct.codegeneration.CodeGenerator
import io.github.apl_cornell.viaduct.codegeneration.CodeGeneratorContext
import io.github.apl_cornell.viaduct.parsing.ProtocolParser
import io.github.apl_cornell.viaduct.selection.ProtocolComposer
import io.github.apl_cornell.viaduct.selection.ProtocolFactory
import io.github.apl_cornell.viaduct.selection.unions
import io.github.apl_cornell.viaduct.syntax.Protocol
import io.github.apl_cornell.viaduct.syntax.ProtocolName
import io.github.apl_cornell.viaduct.syntax.intermediate.ProgramNode
import io.github.apl_cornell.viaduct.circuitcodegeneration.CodeGenerator as CircuitCodeGenerator
import io.github.apl_cornell.viaduct.circuitcodegeneration.CodeGeneratorContext as CircuitCodeGeneratorContext

object CleartextBackend : Backend {
    override val protocols: Set<ProtocolName>
        get() = setOf(Local.protocolName, Replication.protocolName)

    override val protocolParsers: Map<ProtocolName, ProtocolParser<Protocol>>
        get() = mapOf(
            Local.protocolName to LocalProtocolParser,
            Replication.protocolName to ReplicationProtocolParser
        )

    override fun protocolFactory(program: ProgramNode): ProtocolFactory =
        listOf(LocalProtocolFactory(program), ReplicationProtocolFactory(program)).unions()

    override val protocolComposer: ProtocolComposer
        get() = CleartextProtocolComposer

    override fun codeGenerator(context: CodeGeneratorContext): CodeGenerator = CleartextCodeGenerator(context)

    override fun circuitCodeGenerator(context: CircuitCodeGeneratorContext): CircuitCodeGenerator =
        CleartextCircuitCodeGenerator(context)
}
