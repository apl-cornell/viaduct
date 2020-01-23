package edu.cornell.cs.apl.viaduct.syntax

/**
 * A participant in the distributed computation.
 *
 * Hosts have inputs and outputs.
 * Hosts are location that can run (one or more) processes.
 */
data class Host(override val name: String) : Name, Comparable<Host> {
    override val nameCategory: String
        get() = "host"

    override fun compareTo(other: Host): Int =
        this.name.compareTo(other.name)
}
