package io.github.aplcornell.viaduct.circuitbackends.aby

import io.github.aplcornell.viaduct.circuitbackends.Backend
import io.github.aplcornell.viaduct.circuitcodegeneration.CodeGenerator
import io.github.aplcornell.viaduct.circuitcodegeneration.CodeGeneratorContext
import io.github.aplcornell.viaduct.parsing.ProtocolParser
import io.github.aplcornell.viaduct.syntax.Protocol
import io.github.aplcornell.viaduct.syntax.ProtocolName

object ABYBackend : Backend {
    override val protocols: Set<ProtocolName>
        get() = setOf(ArithABY.protocolName, BoolABY.protocolName, YaoABY.protocolName)

    override val protocolParsers: Map<ProtocolName, ProtocolParser<Protocol>>
        get() = mapOf(
            ArithABY.protocolName to ArithABYProtocolParser,
            BoolABY.protocolName to BoolABYProtocolParser,
            YaoABY.protocolName to YaoABYProtocolParser,
        )

//    override fun protocolFactory(program: ProgramNode): ProtocolFactory = ABYProtocolFactory(program)

//    override val protocolComposer: ProtocolComposer
//        get() = ABYProtocolComposer

    override fun codeGenerator(context: CodeGeneratorContext): CodeGenerator = ABYCodeGenerator(context)
}
