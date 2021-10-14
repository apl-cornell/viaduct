package edu.cornell.cs.apl.viaduct.backends.zkp

import edu.cornell.cs.apl.viaduct.backends.Backend
import edu.cornell.cs.apl.viaduct.codegeneration.CodeGenerator
import edu.cornell.cs.apl.viaduct.codegeneration.CodeGeneratorContext
import edu.cornell.cs.apl.viaduct.selection.ProtocolFactory
import edu.cornell.cs.apl.viaduct.syntax.ProtocolName
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode

object ZKPBackend : Backend {
    override val protocols: Set<ProtocolName>
        get() = setOf(ZKP.protocolName)

    override fun protocolFactory(program: ProgramNode): ProtocolFactory = ZKPFactory(program)

    override fun codeGenerator(context: CodeGeneratorContext): CodeGenerator = TODO()
}
