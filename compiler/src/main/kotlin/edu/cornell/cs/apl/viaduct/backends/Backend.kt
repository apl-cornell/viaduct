package edu.cornell.cs.apl.viaduct.backends

import edu.cornell.cs.apl.viaduct.codegeneration.CodeGenerator
import edu.cornell.cs.apl.viaduct.codegeneration.CodeGeneratorContext
import edu.cornell.cs.apl.viaduct.codegeneration.unions
import edu.cornell.cs.apl.viaduct.parsing.ProtocolParser
import edu.cornell.cs.apl.viaduct.selection.ProtocolComposer
import edu.cornell.cs.apl.viaduct.selection.ProtocolFactory
import edu.cornell.cs.apl.viaduct.selection.cached
import edu.cornell.cs.apl.viaduct.selection.filter
import edu.cornell.cs.apl.viaduct.selection.unions
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.ProtocolName
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import edu.cornell.cs.apl.viaduct.util.unions

/** A compiler extension that adds support for a cryptographic backend. */
interface Backend {
    /** Protocols added by this backend. */
    val protocols: Set<ProtocolName>

    val protocolParsers: Map<ProtocolName, ProtocolParser<Protocol>>

    fun protocolFactory(program: ProgramNode): ProtocolFactory

    val protocolComposer: ProtocolComposer

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
                        ${'\t'}${previous.name}
                        ${'\t'}${backend.name}
                        """.trimIndent()
                    )
            }
        }
    }

    return object : Backend {
        private val backends = this@unions.toList()

        override val protocols: Set<ProtocolName>
            get() = backends.map { it.protocols }.unions()

        override val protocolParsers: Map<ProtocolName, ProtocolParser<Protocol>> = run {
            // Ensure there are no missing or extra parsers
            backends.forEach { backend ->
                val protocols = backend.protocols
                val parsers = backend.protocolParsers
                val missingParsers = protocols - parsers.keys
                val extraParsers = parsers.keys - protocols
                if (missingParsers.isNotEmpty()) {
                    val missing = missingParsers.joinToString(", ") { it.name }
                    throw IllegalArgumentException("Missing parsers for $missing in ${backend.name}")
                }
                if (extraParsers.isNotEmpty()) {
                    val extra = extraParsers.joinToString(", ") { it.name }
                    throw IllegalArgumentException("Extraneous parsers for $extra in ${backend.name}")
                }
            }
            backends.map { it.protocolParsers }.unions()
        }

        override fun protocolFactory(program: ProgramNode): ProtocolFactory =
            backends.map { it.protocolFactory(program) }.unions()

        override val protocolComposer: ProtocolComposer =
            backends.map { it.protocols to it.protocolComposer }.unions().cached()

        override fun codeGenerator(context: CodeGeneratorContext): CodeGenerator =
            backends.map { it.protocols to it.codeGenerator(context) }.unions()
    }
}

/** Restricts the given back end to only use protocols satisfying [predicate]. */
fun Backend.filter(predicate: (ProtocolName) -> Boolean): Backend =
    object : Backend by this {
        override val protocols: Set<ProtocolName>
            get() = this@filter.protocols.filter(predicate).toSet()

        override fun protocolFactory(program: ProgramNode): ProtocolFactory =
            this@filter.protocolFactory(program).filter { predicate(it.protocolName) }
    }

/** Derives a name for this [Backend] to be used in error messages. */
private val Backend.name: String
    get() = this::class.toString()
