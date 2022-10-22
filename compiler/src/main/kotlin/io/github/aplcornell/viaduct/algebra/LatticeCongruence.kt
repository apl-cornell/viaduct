package io.github.aplcornell.viaduct.algebra

interface LatticeCongruence<A : Lattice<A>> {
    fun equals(first: A, second: A): Boolean
    fun lessThanOrEqualTo(first: A, second: A): Boolean
}

class FreeDistributiveLatticeCongruence<A>(
    private val congruence: List<FreeDistributiveLattice.LessThanOrEqualTo<A>>
) : LatticeCongruence<FreeDistributiveLattice<A>> {

    override fun equals(first: FreeDistributiveLattice<A>, second: FreeDistributiveLattice<A>) =
        lessThanOrEqualTo(first, second) && lessThanOrEqualTo(second, first)

    override fun lessThanOrEqualTo(
        first: FreeDistributiveLattice<A>,
        second: FreeDistributiveLattice<A>
    ): Boolean =
        first.lessThanOrEqualTo(
            second,
            congruence
        )

    /**
     * Return a new congruence that has the congruence relation of this and other.
     */
    operator fun plus(other: FreeDistributiveLatticeCongruence<A>): FreeDistributiveLatticeCongruence<A> =
        FreeDistributiveLatticeCongruence(
            this.congruence +
                other.congruence
        )

    companion object {
        private val EMPTY: FreeDistributiveLatticeCongruence<*> =
            FreeDistributiveLatticeCongruence<Any>(emptyList())

        @JvmStatic
        fun <A> empty(): FreeDistributiveLatticeCongruence<A> {
            @Suppress("UNCHECKED_CAST")
            return EMPTY as FreeDistributiveLatticeCongruence<A>
        }
    }
}
