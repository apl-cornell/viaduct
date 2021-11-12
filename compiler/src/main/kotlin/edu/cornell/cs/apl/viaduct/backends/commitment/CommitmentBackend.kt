package edu.cornell.cs.apl.viaduct.backends.commitment

import edu.cornell.cs.apl.viaduct.backends.Backend
import edu.cornell.cs.apl.viaduct.codegeneration.CodeGenerator
import edu.cornell.cs.apl.viaduct.codegeneration.CodeGeneratorContext
import edu.cornell.cs.apl.viaduct.codegeneration.CommitmentDispatchCodeGenerator
import edu.cornell.cs.apl.viaduct.parsing.ProtocolParser
import edu.cornell.cs.apl.viaduct.selection.ProtocolComposer
import edu.cornell.cs.apl.viaduct.selection.ProtocolFactory
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.ProtocolName
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode

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
