package io.github.aplcornell.viaduct.backends.aby

import io.github.aplcornell.viaduct.backends.Backend
import io.github.aplcornell.viaduct.codegeneration.CodeGenerator
import io.github.aplcornell.viaduct.codegeneration.CodeGeneratorContext
import io.github.aplcornell.viaduct.parsing.ProtocolParser
import io.github.aplcornell.viaduct.selection.ProtocolComposer
import io.github.aplcornell.viaduct.selection.ProtocolFactory
import io.github.aplcornell.viaduct.syntax.Protocol
import io.github.aplcornell.viaduct.syntax.ProtocolName
import io.github.aplcornell.viaduct.syntax.intermediate.ProgramNode
import io.github.aplcornell.viaduct.circuitcodegeneration.CodeGenerator as CircuitCodeGenerator
import io.github.aplcornell.viaduct.circuitcodegeneration.CodeGeneratorContext as CircuitCodeGeneratorContext

object ABYBackend : Backend {
    override val protocols: Set<ProtocolName>
        get() = setOf(ArithABY.protocolName, BoolABY.protocolName, YaoABY.protocolName)

    override val protocolParsers: Map<ProtocolName, ProtocolParser<Protocol>>
        get() = mapOf(
            ArithABY.protocolName to ArithABYProtocolParser,
            BoolABY.protocolName to BoolABYProtocolParser,
            YaoABY.protocolName to YaoABYProtocolParser,
        )

    override fun protocolFactory(program: ProgramNode): ProtocolFactory = ABYProtocolFactory(program)

    override val protocolComposer: ProtocolComposer
        get() = ABYProtocolComposer

    override fun codeGenerator(context: CodeGeneratorContext): CodeGenerator = ABYCodeGenerator(context)

    override fun circuitCodeGenerator(context: CircuitCodeGeneratorContext): CircuitCodeGenerator =
        ABYCircuitCodeGenerator(context)
}
