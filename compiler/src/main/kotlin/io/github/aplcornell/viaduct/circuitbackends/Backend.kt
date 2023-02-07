package io.github.aplcornell.viaduct.circuitbackends

import io.github.aplcornell.viaduct.circuitcodegeneration.CodeGenerator
import io.github.aplcornell.viaduct.circuitcodegeneration.CodeGeneratorContext
import io.github.aplcornell.viaduct.circuitcodegeneration.unions
import io.github.aplcornell.viaduct.parsing.ProtocolParser
import io.github.aplcornell.viaduct.syntax.Protocol
import io.github.aplcornell.viaduct.syntax.ProtocolName
import io.github.aplcornell.viaduct.util.unions

/** A compiler extension that adds support for a cryptographic backend. */
interface Backend {
    /** Protocols added by this backend. */
    val protocols: Set<ProtocolName>

    val protocolParsers: Map<ProtocolName, ProtocolParser<Protocol>>

//    fun protocolFactory(program: ProgramNode): ProtocolFactory

//    val protocolComposer: ProtocolComposer

    fun codeGenerator(context: CodeGeneratorContext): CodeGenerator
}

/** Combines back ends for different protocols into one that supports all. */
fun Iterable<Backend>.unions(): Backend {
    // Ensure there are no duplicate protocols.
    mutableMapOf<ProtocolName, Backend>().apply {
        this@unions.forEach { backend ->
            backend.protocols.forEach { protocol ->
                val previous = this.put(protocol, backend)
                if (previous != null) {
                    throw IllegalArgumentException(
                        """
                        Protocol ${protocol.name} is implemented by multiplecircuitbackends:
                        ${'\t'}${previous.name}
                        ${'\t'}${backend.name}
                        """.trimIndent(),
                    )
                }
            }
        }
    }

    return object : Backend {
        private val circuitbackends = this@unions.toList()

        override val protocols: Set<ProtocolName>
            get() = circuitbackends.map { it.protocols }.unions()

        override val protocolParsers: Map<ProtocolName, ProtocolParser<Protocol>> = run {
            // Ensure there are no missing or extra parsers
            circuitbackends.forEach { backend ->
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
            circuitbackends.map { it.protocolParsers }.unions()
        }

//        override fun protocolFactory(program: ProgramNode): ProtocolFactory =
//            circuitbackends.map { it.protocolFactory(program) }.unions()

//        override val protocolComposer: ProtocolComposer =
//            circuitbackends.map { it.protocols to it.protocolComposer }.unions().cached()

        override fun codeGenerator(context: CodeGeneratorContext): CodeGenerator =
            circuitbackends.map { it.protocols to it.codeGenerator(context) }.unions()
    }
}

/** Restricts the given back end to only use protocols satisfying [predicate]. */
fun Backend.filter(predicate: (ProtocolName) -> Boolean): Backend =
    object : Backend by this {
        override val protocols: Set<ProtocolName>
            get() = this@filter.protocols.filter(predicate).toSet()

//        override fun protocolFactory(program: ProgramNode): ProtocolFactory =
//            this@filter.protocolFactory(program).filter { predicate(it.protocolName) }
    }

/** Derives a name for this [Backend] to be used in error messages. */
private val Backend.name: String
    get() = this::class.toString()
