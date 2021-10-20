package edu.cornell.cs.apl.viaduct.backends.cleartext

import edu.cornell.cs.apl.viaduct.backends.Backend
import edu.cornell.cs.apl.viaduct.codegeneration.CodeGenerator
import edu.cornell.cs.apl.viaduct.codegeneration.CodeGeneratorContext
import edu.cornell.cs.apl.viaduct.codegeneration.PlainTextCodeGenerator
import edu.cornell.cs.apl.viaduct.selection.ProtocolComposer
import edu.cornell.cs.apl.viaduct.selection.ProtocolFactory
import edu.cornell.cs.apl.viaduct.selection.unions
import edu.cornell.cs.apl.viaduct.syntax.ProtocolName
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode

object CleartextBackend : Backend {
    override val protocols: Set<ProtocolName>
        get() = setOf(Local.protocolName, Replication.protocolName)

    override fun protocolFactory(program: ProgramNode): ProtocolFactory =
        listOf(LocalProtocolFactory(program), ReplicationProtocolFactory(program)).unions()

    override val protocolComposer: ProtocolComposer
        get() = CleartextProtocolComposer

    override fun codeGenerator(context: CodeGeneratorContext): CodeGenerator = PlainTextCodeGenerator(context)
}
