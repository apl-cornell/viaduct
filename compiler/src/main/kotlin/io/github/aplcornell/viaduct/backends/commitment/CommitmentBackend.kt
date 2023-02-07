package io.github.aplcornell.viaduct.backends.commitment

import io.github.aplcornell.viaduct.backends.Backend
import io.github.aplcornell.viaduct.codegeneration.CodeGenerator
import io.github.aplcornell.viaduct.codegeneration.CodeGeneratorContext
import io.github.aplcornell.viaduct.parsing.ProtocolParser
import io.github.aplcornell.viaduct.selection.ProtocolComposer
import io.github.aplcornell.viaduct.selection.ProtocolFactory
import io.github.aplcornell.viaduct.syntax.Protocol
import io.github.aplcornell.viaduct.syntax.ProtocolName
import io.github.aplcornell.viaduct.syntax.intermediate.ProgramNode

object CommitmentBackend : Backend {
    override val protocols: Set<ProtocolName>
        get() = setOf(Commitment.protocolName)

    override val protocolParsers: Map<ProtocolName, ProtocolParser<Protocol>>
        get() = mapOf(Commitment.protocolName to CommitmentProtocolParser)

    override fun protocolFactory(program: ProgramNode): ProtocolFactory = CommitmentProtocolFactory(program)

    override val protocolComposer: ProtocolComposer
        get() = CommitmentProtocolComposer

    override fun codeGenerator(context: CodeGeneratorContext): CodeGenerator = CommitmentDispatchCodeGenerator(context)
}
