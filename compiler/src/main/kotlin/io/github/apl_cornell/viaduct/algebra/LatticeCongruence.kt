package io.github.apl_cornell.viaduct.algebra

private typealias Congruence<A> = Pair<A, A>

interface LatticeCongruence<A : Lattice<A>> {
    fun equals(first: A, second: A): Boolean
    fun lessThanOrEqualTo(first: A, second: A): Boolean
}

class FreeDistributiveLatticeCongruence<A>(
    congruence: List<Congruence<FreeDistributiveLattice<A>>>
) : LatticeCongruence<FreeDistributiveLattice<A>> {
    private val foldedCongruence: Congruence<FreeDistributiveLattice<A>> =
        congruence.fold(Pair(FreeDistributiveLattice.bounds<A>().bottom, FreeDistributiveLattice.bounds<A>().bottom))
        { acc, element ->
            Pair(acc.first.meet(element.first), acc.second.join(element.second))
        }

    override fun equals(first: FreeDistributiveLattice<A>, second: FreeDistributiveLattice<A>) =
        if (foldedCongruence == null) first == second
        else (first.meet(foldedCongruence.first) == second.meet(foldedCongruence.first)) &&
            (first.join(foldedCongruence.second) == second.join(foldedCongruence.second))

    override fun lessThanOrEqualTo(first: FreeDistributiveLattice<A>, second: FreeDistributiveLattice<A>): Boolean =
        equals(first.join(second), second)

    /**
     * Return a new congruence that has the congruence relation of this and other.
     */
    operator fun plus(other: FreeDistributiveLatticeCongruence<A>): FreeDistributiveLatticeCongruence<A> =
        FreeDistributiveLatticeCongruence(listOf(this.foldedCongruence) + listOf(other.foldedCongruence))

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
