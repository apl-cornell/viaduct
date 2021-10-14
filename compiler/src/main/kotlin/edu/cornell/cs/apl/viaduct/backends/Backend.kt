package edu.cornell.cs.apl.viaduct.backends

import edu.cornell.cs.apl.viaduct.codegeneration.CodeGenerator
import edu.cornell.cs.apl.viaduct.codegeneration.CodeGeneratorContext
import edu.cornell.cs.apl.viaduct.codegeneration.unions
import edu.cornell.cs.apl.viaduct.selection.ProtocolFactory
import edu.cornell.cs.apl.viaduct.selection.unions
import edu.cornell.cs.apl.viaduct.syntax.ProtocolName
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import edu.cornell.cs.apl.viaduct.util.unions

/** A compiler extension that adds support for a cryptographic backend. */
interface Backend {
    /** Protocols added by this backend. */
    val protocols: Set<ProtocolName>

    fun protocolFactory(program: ProgramNode): ProtocolFactory

    fun codeGenerator(context: CodeGeneratorContext): CodeGenerator
}

/** Combines back ends for different protocols into one that supports all. */
fun Iterable<Backend>.unions(): Backend {
    // Ensure there are no duplicate protocols.
    mutableMapOf<ProtocolName, Backend>().apply {
        this@unions.forEach { backend ->
            backend.protocols.forEach { protocol ->
                val previous = this.put(protocol, backend)
                if (previous != null)
                    throw IllegalArgumentException(
                        """
                        Protocol ${protocol.name} is implemented by multiple backends:
                        ${'\t'}${previous::class}
                        ${'\t'}${backend::class}
                        """.trimIndent()
                    )
            }
        }
    }

    return object : Backend {
        private val backends = this@unions.toList()

        override val protocols: Set<ProtocolName>
            get() = backends.map { it.protocols }.unions()

        override fun protocolFactory(program: ProgramNode): ProtocolFactory =
            backends.map { it.protocolFactory(program) }.unions()

        override fun codeGenerator(context: CodeGeneratorContext): CodeGenerator =
            backends.map { it.protocols to it.codeGenerator(context) }.unions()
    }
}
