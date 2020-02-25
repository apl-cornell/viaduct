package edu.cornell.cs.apl.viaduct.analysis

import edu.cornell.cs.apl.viaduct.algebra.MeetSemiLattice
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentHashSetOf
import kotlinx.collections.immutable.persistentSetOf

/** Sets with union form a bounded meet semi lattice. */
// TODO: we only need this to use [DataFlow]. Change [DataFlow] to take in a function?
internal data class SetWithUnion<T>(val elements: PersistentSet<T>) :
    MeetSemiLattice<SetWithUnion<T>>, Set<T> by elements {
    constructor(vararg elements: T) : this(persistentHashSetOf(*elements))

    override fun lessThanOrEqualTo(that: SetWithUnion<T>): Boolean =
        this.containsAll(that)

    override fun meet(that: SetWithUnion<T>): SetWithUnion<T> =
        // A slightly optimized set union.
        when {
            this.isEmpty() ->
                that
            that.isEmpty() ->
                this
            this.size >= that.size ->
                SetWithUnion(this.elements.addAll(that.elements))
            else ->
                SetWithUnion(that.elements.addAll(this.elements))
        }

    companion object {
        private val EMPTY = SetWithUnion(persistentSetOf<Nothing>())
        @Suppress("UNCHECKED_CAST")
        fun <T> top(): SetWithUnion<T> = EMPTY as SetWithUnion<T>
    }
}
