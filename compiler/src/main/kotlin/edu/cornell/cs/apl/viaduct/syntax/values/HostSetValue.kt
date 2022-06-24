package edu.cornell.cs.apl.viaduct.syntax.values

import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.HostNode
import edu.cornell.cs.apl.viaduct.syntax.Name
import edu.cornell.cs.apl.viaduct.syntax.NameMap
import edu.cornell.cs.apl.viaduct.syntax.types.HostSetType
import edu.cornell.cs.apl.viaduct.syntax.types.ValueType
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.toPersistentSet

/** A set of hosts. */
class HostSetValue private constructor(val hosts: PersistentSet<Host>) : Value(), PersistentSet<Host> by hosts {
    constructor(hosts: Set<Host>) : this(hosts.sorted().toPersistentSet())

    override val type: ValueType
        get() = HostSetType

    override fun equals(other: Any?): Boolean {
        return other is HostSetValue && this.hosts == other.hosts
    }

    override fun hashCode(): Int {
        return hosts.hashCode()
    }

    override fun toString(): String =
        hosts.map(Name::name).joinToString(separator = ", ", prefix = "{", postfix = "}")

    companion object {
        @JvmStatic
        operator fun invoke(hosts: List<HostNode>): HostSetValue {
            // Check for duplicates
            hosts.fold(NameMap<Host, Unit>()) { map, host -> map.put(host, Unit) }

            return HostSetValue(hosts.map { it.value }.sorted().toPersistentSet())
        }
    }
}
