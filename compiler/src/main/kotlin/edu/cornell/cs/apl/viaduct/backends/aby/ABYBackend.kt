package edu.cornell.cs.apl.viaduct.backends.aby

import edu.cornell.cs.apl.viaduct.backends.Backend
import edu.cornell.cs.apl.viaduct.codegeneration.CodeGenerator
import edu.cornell.cs.apl.viaduct.codegeneration.CodeGeneratorContext
import edu.cornell.cs.apl.viaduct.selection.ProtocolFactory
import edu.cornell.cs.apl.viaduct.syntax.ProtocolName
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode

object ABYBackend : Backend {
    override val protocols: Set<ProtocolName>
        get() = setOf(ArithABY.protocolName, BoolABY.protocolName, YaoABY.protocolName)

    override fun protocolFactory(program: ProgramNode): ProtocolFactory = ABYFactory(program)

    override fun codeGenerator(context: CodeGeneratorContext): CodeGenerator = TODO()
}
