package io.github.apl_cornell.viaduct.backends.commitment

import io.github.apl_cornell.viaduct.backends.Backend
import io.github.apl_cornell.viaduct.codegeneration.CodeGenerator
import io.github.apl_cornell.viaduct.codegeneration.CodeGeneratorContext
import io.github.apl_cornell.viaduct.parsing.ProtocolParser
import io.github.apl_cornell.viaduct.selection.ProtocolComposer
import io.github.apl_cornell.viaduct.selection.ProtocolFactory
import io.github.apl_cornell.viaduct.syntax.Protocol
import io.github.apl_cornell.viaduct.syntax.ProtocolName
import io.github.apl_cornell.viaduct.syntax.intermediate.ProgramNode

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
