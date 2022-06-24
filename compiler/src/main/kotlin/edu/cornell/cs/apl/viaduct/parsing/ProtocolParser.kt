package edu.cornell.cs.apl.viaduct.parsing

import edu.cornell.cs.apl.viaduct.backends.cleartext.Local
import edu.cornell.cs.apl.viaduct.backends.cleartext.LocalProtocolParser
import edu.cornell.cs.apl.viaduct.errors.CompilationError
import edu.cornell.cs.apl.viaduct.errors.TypeMismatchError
import edu.cornell.cs.apl.viaduct.errors.UndefinedNameError
import edu.cornell.cs.apl.viaduct.syntax.ArgumentLabel
import edu.cornell.cs.apl.viaduct.syntax.NamedArguments
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.ProtocolName
import edu.cornell.cs.apl.viaduct.syntax.ProtocolNameNode
import edu.cornell.cs.apl.viaduct.syntax.ValueNode
import edu.cornell.cs.apl.viaduct.syntax.types.BooleanType
import edu.cornell.cs.apl.viaduct.syntax.types.HostSetType
import edu.cornell.cs.apl.viaduct.syntax.types.HostType
import edu.cornell.cs.apl.viaduct.syntax.types.IntegerType
import edu.cornell.cs.apl.viaduct.syntax.types.StringType
import edu.cornell.cs.apl.viaduct.syntax.values.BooleanValue
import edu.cornell.cs.apl.viaduct.syntax.values.HostSetValue
import edu.cornell.cs.apl.viaduct.syntax.values.HostValue
import edu.cornell.cs.apl.viaduct.syntax.values.IntegerValue
import edu.cornell.cs.apl.viaduct.syntax.values.StringValue
import edu.cornell.cs.apl.viaduct.syntax.values.Value
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf

/** A parser for protocols of type [P]. */
interface ProtocolParser<out P : Protocol> {
    /**
     * Parses [arguments] into a protocol of type [P].
     *
     * Note that calling [Protocol.arguments] on the returned value should result in a map equivalent to [arguments].
     * This function does not need to check for extra arguments; that is done automatically.
     *
     * @throws CompilationError if the arguments are malformed.
     */
    fun parse(arguments: ProtocolArguments): P
}

val defaultProtocolParsers: PersistentMap<ProtocolName, ProtocolParser<Protocol>> =
    persistentMapOf(Local.protocolName to LocalProtocolParser)

/** A map from argument labels to [Value]s. */
class ProtocolArguments internal constructor(private val arguments: NamedArguments<ValueNode>) {
    /** Used to check for unexpected arguments. See [assertHasNoExtraArguments]. */
    private val usedArguments: MutableSet<String> = mutableSetOf()

    /**
     * Returns the argument associated with label [label], automatically casting it to type [V].
     *
     * @throws UndefinedNameError if there is no argument with label [label].
     * @throws TypeMismatchError if the argument does not have type [V].
     */
    inline fun <reified V : Value> get(label: String): V {
        val argument = getArgument(label)
        val argumentValue = argument.value
        if (argumentValue is V) {
            return argumentValue
        } else {
            // TODO: this is super hacky.
            val expectedType =
                when (V::class) {
                    BooleanValue::class ->
                        BooleanType
                    IntegerValue::class ->
                        IntegerType
                    StringValue::class ->
                        StringType
                    HostValue::class ->
                        HostType
                    HostSetValue::class ->
                        HostSetType
                    else ->
                        throw RuntimeException("Unexpected value type ${V::class}.")
                }

            throw TypeMismatchError(argument, actualType = argumentValue.type, expectedType = expectedType)
        }
    }

    /**
     * Returns the argument associated with label [label].
     *
     * It is usually better to use [get].
     *
     * @throws UndefinedNameError if there is no argument with label [label].
     */
    fun getArgument(label: String): ValueNode {
        usedArguments.add(label)
        return arguments[ArgumentLabel(label)]
    }

    internal fun assertHasNoExtraArguments() {
        arguments.assertHasNoExtraArguments(usedArguments.map { ArgumentLabel(it) }.toSet())
    }
}

internal fun parseProtocol(
    protocolParsers: Map<ProtocolName, ProtocolParser<Protocol>>,
    protocolName: ProtocolNameNode,
    arguments: NamedArguments<ValueNode>
): Protocol {
    val parser = protocolParsers[protocolName.value] ?: throw UndefinedNameError(protocolName)
    val protocolArguments = ProtocolArguments(arguments)
    val protocol = parser.parse(protocolArguments)
    protocolArguments.assertHasNoExtraArguments()
    return protocol
}
