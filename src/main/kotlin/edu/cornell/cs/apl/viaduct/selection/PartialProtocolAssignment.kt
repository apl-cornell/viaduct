package edu.cornell.cs.apl.viaduct.selection

import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.Variable
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf

/**
 * A mapping from a subset of the variables in the program to protocols.
 *
 * This class is used during protocol search to keep track of the protocols selected "so far".
 * It incrementally computes the estimated cost of the protocol assignment, which is essential
 * for making the search fast enough to be practical.
 */
abstract class PartialProtocolAssignment private constructor(
    private val costEstimator: CostEstimator,
    private val protocolAssignment: PersistentMap<Variable, Protocol>
) : Map<Variable, Protocol> by protocolAssignment {
    /** Compute `hashCode()` once and store it for later. */
    private val hashCode: Int by lazy { protocolAssignment.hashCode() }

    constructor(costEstimator: CostEstimator) : this(costEstimator, persistentMapOf())

    /**
     * Returns the result of associating the specified [variable] with the specified [protocol].
     *
     * If the variable is already assigned to a protocol, the old protocol is replaced by the new
     * protocol.
     */
    abstract fun set(variable: Variable, protocol: Protocol): PartialProtocolAssignment

    /**
     * Estimated cost of running the program with this protocol assignment.
     *
     * Note that since the protocol assignment is partial, the cost only covers a portion of the
     * program.
     */
    abstract val cost: Cost

    override fun equals(other: Any?): Boolean {
        return other is PartialProtocolAssignment &&
            this.protocolAssignment == other.protocolAssignment
    }

    override fun hashCode(): Int = hashCode
}
