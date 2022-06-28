package io.github.apl_cornell.viaduct.algebra

import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentSet

private typealias Meet<A> = PersistentSet<A>
private typealias JoinOfMeets<A> = PersistentSet<Meet<A>>

/**
 * The free distributive lattice over an arbitrary set `A` of elements. In addition to lattice
 * identities, the following hold:
 *
 * `a /\ (b \/ c) == (a /\ b) \/ (a /\ c)`
 *
 * `a \/ (b /\ c) == (a \/ b) /\ (a \/ c)`
 */
class FreeDistributiveLattice<A> private constructor(joinOfMeets: JoinOfMeets<A>) :
    HeytingAlgebra<FreeDistributiveLattice<A>> {
    private val joinOfMeets = removeRedundant(joinOfMeets)

    constructor(element: A) : this(persistentSetOf(persistentSetOf(element)))

    override fun join(that: FreeDistributiveLattice<A>): FreeDistributiveLattice<A> {
        return FreeDistributiveLattice(joinOfMeets.addAll(that.joinOfMeets))
    }

    override fun meet(that: FreeDistributiveLattice<A>): FreeDistributiveLattice<A> {
        var candidates: JoinOfMeets<A> = persistentSetOf()
        for (meet1 in joinOfMeets) {
            for (meet2 in that.joinOfMeets) {
                candidates = candidates.add(meet1.addAll(meet2))
            }
        }
        return FreeDistributiveLattice(candidates)
    }

    /**
     * Returns the relative pseudocomplement of `this` relative to `that`. the relative
     * pseudocomplement is greatest x s.t. `this & x <= that`.
     *
     *
     * How does this work? we are dealing with constraints of the form `(A1 | ... | Am) & x
     * <= B1 | ... | Bn`
     *
     *
     * which can be rewritten as `(A1&x) | ... | (Am&x) <= B1 | ... | Bn`
     *
     *
     * This inequality only holds true if every meet on the left can be "covered" on the right s.t.
     * a meet on the right side is a subset of the meet on the left side. For every meet on the left
     * Ai, we complement it with every meet on the right Bj. because we want the greatest solution, we
     * join these complements together, arriving at an upper bound for x: `x <= Ci1 | ... | Cin`
     *
     *
     * where `Cij = Bj \ Ai`.
     *
     *
     * But we have to do the same process for all meets on the left, so we get m upper bounds.
     * these have to be all simultaneously satisfied, so we take the meet of the upper bounds: `x = (C11 | ... | C1n) & ... & (Cm1 | ... | Cmn)`
     *
     *
     * The algorithm below computes exactly this solution.
     */
    override fun imply(that: FreeDistributiveLattice<A>): FreeDistributiveLattice<A> {
        var result = bounds<A>().top
        joinOfMeets.forEach { thisMeet ->
            val newJoinOfMeets =
                that.joinOfMeets.map { thatMeet -> thatMeet.removeAll(thisMeet) }.toPersistentSet()
            result = result.meet(FreeDistributiveLattice(newJoinOfMeets))
        }
        return result
    }

    override fun equals(other: Any?): Boolean =
        other is FreeDistributiveLattice<*> && this.joinOfMeets == other.joinOfMeets

    override fun hashCode(): Int =
        joinOfMeets.hashCode()

    override fun toString(): String {
        fun meetToString(meet: Meet<A>): String {
            val elements = meet.map { it.toString() }.sorted()
            val body = elements.joinToString(" \u2227 ")
            return if (meet.size > 1) "($body)" else body
        }

        return when (this) {
            bounds<A>().top ->
                "\u22A4"

            bounds<A>().bottom ->
                "\u22A5"

            else -> {
                val meets = joinOfMeets.map { meetToString(it) }.sorted()
                val body = meets.joinToString(" \u2228 ")
                if (joinOfMeets.size > 1) "($body)" else body
            }
        }
    }

    companion object {
        private object Bounds : BoundedLattice<FreeDistributiveLattice<Nothing>> {
            override val bottom: FreeDistributiveLattice<Nothing> =
                FreeDistributiveLattice(persistentSetOf())

            override val top: FreeDistributiveLattice<Nothing> =
                FreeDistributiveLattice(persistentSetOf(persistentSetOf()))
        }

        @Suppress("UNCHECKED_CAST")
        fun <A> bounds(): BoundedLattice<FreeDistributiveLattice<A>> =
            Bounds as BoundedLattice<FreeDistributiveLattice<A>>

        /** Remove redundant meets according to [isRedundant]. */
        private fun <A> removeRedundant(joinOfMeets: JoinOfMeets<A>): JoinOfMeets<A> {
            return joinOfMeets.filter { meet -> !isRedundant(joinOfMeets, meet) }.toPersistentSet()
        }

        /**
         * Given `m_1 \/ m_2 \/ ... \/ m_n`, if any `m_i` is a strict subset of `m_j`,
         * then `m_j` is redundant.
         */
        private fun <A> isRedundant(joinOfMeets: JoinOfMeets<A>, j: Meet<A>): Boolean =
            joinOfMeets.any { it != j && j.containsAll(it) }
    }
}
