package io.github.aplcornell.viaduct.selection

import io.github.aplcornell.viaduct.prettyprinting.Document
import io.github.aplcornell.viaduct.prettyprinting.PrettyPrintable
import io.github.aplcornell.viaduct.prettyprinting.braced
import io.github.aplcornell.viaduct.prettyprinting.times
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.toPersistentMap

typealias CostFeature = String

/** A commutative monoid that represents a notion of cost for a feature. */
interface CostMonoid<C : CostMonoid<C>> : PrettyPrintable {
    fun concat(other: C): C
    fun zero(): C
}

class IntegerCost(val cost: Int) : CostMonoid<IntegerCost> {
    companion object {
        fun zero(): IntegerCost = IntegerCost(0)
    }

    override fun concat(other: IntegerCost): IntegerCost =
        IntegerCost(this.cost + other.cost)

    override fun zero(): IntegerCost = IntegerCost.zero()

    override fun toDocument(): Document = Document(cost.toString())

    override fun toString(): String = toDocument().print()
}

/**
 * The cost of executing a piece of code or sending a message.
 * Consists of a map of features over some cost monoid.
 * */
data class Cost<C : CostMonoid<C>>(
    val features: PersistentMap<CostFeature, C>
) : CostMonoid<Cost<C>>, Map<CostFeature, C> by features, PrettyPrintable {
    override fun concat(other: Cost<C>): Cost<C> =
        Cost(
            this.features
                .mapValues { kv ->
                    kv.value.concat(other.features[kv.key] ?: kv.value.zero())
                }
                .plus(other.features.filterKeys { k -> !features.containsKey(k) })
                .toPersistentMap()
        )

    override fun zero(): Cost<C> =
        Cost(
            this.features.map { kv -> kv.key to kv.value.zero() }.toMap().toPersistentMap()
        )

    fun <D : CostMonoid<D>> map(f: (C) -> D): Cost<D> =
        Cost(
            this.features.map { kv -> kv.key to f(kv.value) }.toMap().toPersistentMap()
        )

    fun <D : CostMonoid<D>> featureMap(f: (CostFeature, C) -> D): Cost<D> =
        Cost(
            this.features.map { kv -> kv.key to f(kv.key, kv.value) }.toMap().toPersistentMap()
        )

    fun update(feature: CostFeature, cost: C): Cost<C> =
        Cost(features.put(feature, cost))

    override fun toDocument(): Document =
        features.map { kv -> Document(kv.key) * ":" * kv.value }.braced()
}
