package edu.cornell.cs.apl.viaduct.syntax

/**
 * A participant in the distributed computation.
 *
 * Hosts have inputs and outputs.
 * Hosts are location that can run (one or more) processes.
 */
data class Host(override val name: String) : Name {
    override val nameCategory: String
        get() = "host"
}
