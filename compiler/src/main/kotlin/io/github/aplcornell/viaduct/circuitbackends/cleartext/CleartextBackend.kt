package io.github.aplcornell.viaduct.circuitbackends.cleartext

import io.github.aplcornell.viaduct.circuitbackends.Backend
import io.github.aplcornell.viaduct.circuitcodegeneration.CodeGenerator
import io.github.aplcornell.viaduct.circuitcodegeneration.CodeGeneratorContext
import io.github.aplcornell.viaduct.parsing.ProtocolParser
import io.github.aplcornell.viaduct.syntax.Protocol
import io.github.aplcornell.viaduct.syntax.ProtocolName

object CleartextBackend : Backend {
    override val protocols: Set<ProtocolName>
        get() = setOf(Local.protocolName, Replication.protocolName)

    override val protocolParsers: Map<ProtocolName, ProtocolParser<Protocol>>
        get() = mapOf(
            Local.protocolName to LocalProtocolParser,
            Replication.protocolName to ReplicationProtocolParser,
        )

//    override fun protocolFactory(program: ProgramNode): ProtocolFactory =
//        listOf(LocalProtocolFactory(program), ReplicationProtocolFactory(program)).unions()

//    override val protocolComposer: ProtocolComposer
//        get() = CleartextProtocolComposer

    override fun codeGenerator(context: CodeGeneratorContext): CodeGenerator = CleartextCodeGenerator(context)
}
