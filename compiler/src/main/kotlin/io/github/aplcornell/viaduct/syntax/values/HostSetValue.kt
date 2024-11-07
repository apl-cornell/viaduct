package io.github.aplcornell.viaduct.syntax.values

import io.github.aplcornell.viaduct.syntax.Host
import io.github.aplcornell.viaduct.syntax.HostNode
import io.github.aplcornell.viaduct.syntax.Name
import io.github.aplcornell.viaduct.syntax.NameMap
import io.github.aplcornell.viaduct.syntax.types.HostSetType
import io.github.aplcornell.viaduct.syntax.types.ValueType
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

    override fun toString(): String = hosts.map(Name::name).joinToString(separator = ", ", prefix = "{", postfix = "}")

    companion object {
        @JvmStatic
        operator fun invoke(hosts: List<HostNode>): HostSetValue {
            // Check for duplicates
            hosts.fold(NameMap<Host, Unit>()) { map, host -> map.put(host, Unit) }

            return HostSetValue(hosts.map { it.value }.sorted().toPersistentSet())
        }
    }
}
