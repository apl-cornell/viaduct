package io.github.apl_cornell.viaduct.circuitbackends.cleartext

import io.github.apl_cornell.viaduct.circuitbackends.Backend
import io.github.apl_cornell.viaduct.circuitcodegeneration.CodeGenerator
import io.github.apl_cornell.viaduct.circuitcodegeneration.CodeGeneratorContext
import io.github.apl_cornell.viaduct.parsing.ProtocolParser
import io.github.apl_cornell.viaduct.syntax.Protocol
import io.github.apl_cornell.viaduct.syntax.ProtocolName

object CleartextBackend : Backend {
    override val protocols: Set<ProtocolName>
        get() = setOf(Local.protocolName, Replication.protocolName)

    override val protocolParsers: Map<ProtocolName, ProtocolParser<Protocol>>
        get() = mapOf(
            Local.protocolName to LocalProtocolParser,
            Replication.protocolName to ReplicationProtocolParser
        )

//    override fun protocolFactory(program: ProgramNode): ProtocolFactory =
//        listOf(LocalProtocolFactory(program), ReplicationProtocolFactory(program)).unions()

//    override val protocolComposer: ProtocolComposer
//        get() = CleartextProtocolComposer

    override fun codeGenerator(context: CodeGeneratorContext): CodeGenerator = CleartextCodeGenerator(context)
}
