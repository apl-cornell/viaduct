package edu.cornell.cs.apl.viaduct.syntax

import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.prettyprinting.nested
import edu.cornell.cs.apl.prettyprinting.plus
import edu.cornell.cs.apl.prettyprinting.times
import edu.cornell.cs.apl.prettyprinting.tupled
import edu.cornell.cs.apl.viaduct.security.Label
import edu.cornell.cs.apl.viaduct.syntax.values.HostSetValue
import edu.cornell.cs.apl.viaduct.syntax.values.HostValue
import edu.cornell.cs.apl.viaduct.syntax.values.Value
import edu.cornell.cs.apl.viaduct.util.asComparable

/**
 * An abstract location where computations can be placed.
 *
 * A protocol simultaneously names a location and determines the (cryptographic) mechanism for
 * executing the code placed at that location.
 *
 * A protocol is a [ProtocolName] applied to a sequence of named arguments.
 * The name and the arguments uniquely determine the protocol (see [Protocol.equals]).
 */
abstract class Protocol : Name, Comparable<Protocol> {
    companion object {
        const val INTERNAL_INPUT = "INTERNAL_INPUT"
        const val INTERNAL_OUTPUT = "INTERNAL_OUTPUT"
    }

    /** The name of the (cryptographic) protocol. */
    abstract val protocolName: ProtocolName

    /** The named arguments applied to [protocolName]. */
    abstract val arguments: Map<String, Value>

    /** Returns the authority label of this protocol given the authority labels of the participating hosts. */
    abstract fun authority(hostTrustConfiguration: HostTrustConfiguration): Label

    /** Hosts involved in this protocol. */
    val hosts: HostSetValue by lazy {
        val hosts = mutableSetOf<Host>()
        this.arguments.values.forEach {
            when (it) {
                is HostValue ->
                    hosts.add(it.value)
                is HostSetValue ->
                    hosts.addAll(it)
            }
        }
        HostSetValue(hosts)
    }

    final override val name: String
        get() = this.asDocument.print()

    final override val nameCategory: String
        get() = "protocol"

    // TODO: make this final once we remove [Ideal]
    override val asDocument: Document
        get() {
            val sortedArguments = arguments.toSortedMap().entries
            val printedArguments = sortedArguments.map { Document(it.key) * "=" * it.value }
            return protocolName + printedArguments.tupled().nested()
        }

    final override fun equals(other: Any?): Boolean =
        other is Protocol && this.protocolName == other.protocolName && this.arguments == other.arguments

    final override fun hashCode(): Int =
        Pair(protocolName, arguments).hashCode()

    final override fun compareTo(other: Protocol): Int {
        if (protocolName != other.protocolName) {
            return protocolName.name.compareTo(other.protocolName.name)
        } else {
            assert(arguments.keys == other.arguments.keys)

            val sortedKeys: List<String> = arguments.keys.sorted()
            for (key: String in sortedKeys) {
                val hostCmp: Int =
                    // only compare host and hostSets
                    when (val thisValue = arguments[key]) {
                        is HostValue -> {
                            val otherValue: HostValue = other.arguments[key] as HostValue
                            thisValue.value.compareTo(otherValue.value)
                        }

                        is HostSetValue -> {
                            val otherValue: HostSetValue = other.arguments[key] as HostSetValue
                            thisValue.hosts.asComparable().compareTo(otherValue.hosts)
                        }

                        else -> 0
                    }

                // values are equal; check next pair of values
                if (hostCmp == 0) {
                    continue
                } else {
                    return hostCmp
                }
            }

            // all keys are equal
            return 0
        }
    }

    val internalInputPorts: Map<Host, InputPort> by lazy {
        hosts
            .map { h -> Pair(h, InputPort(this, h, INTERNAL_INPUT)) }
            .toMap()
    }

    val internalOutputPorts: Map<Host, OutputPort> by lazy {
        hosts
            .map { h -> Pair(h, OutputPort(this, h, INTERNAL_OUTPUT)) }
            .toMap()
    }
}
