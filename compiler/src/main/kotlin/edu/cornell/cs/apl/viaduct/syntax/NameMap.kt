package edu.cornell.cs.apl.viaduct.syntax

import edu.cornell.cs.apl.viaduct.errors.NameClashError
import edu.cornell.cs.apl.viaduct.errors.UndefinedNameError
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf

/** A persistent map from [Name]s to [Data]. */
internal class NameMap<N : Name, Data>
private constructor(
    private val map: PersistentMap<N, Pair<Data, SourceLocation>>
) {
    /** The empty map. */
    constructor() : this(persistentMapOf())

    /**
     * Returns the data associated with [name].
     *
     * @throws UndefinedNameError if [name] is not in the map.
     */
    operator fun get(name: Located<N>): Data {
        return map[name.value]?.first ?: throw UndefinedNameError(name)
    }

    /**
     * Returns whether there is a mapping for [name].
     */
    fun contains(name: Located<N>): Boolean {
        return map.containsKey(name.value)
    }

    /**
     * Returns the set of keys in the map.
     */
    val keys: Set<N>
        get() = map.keys

    /**
     * Returns the set of values in the map.
     */
    val values: Set<Data>
        get() = map.values.map { pair -> pair.first }.toSet()

    /**
     * Returns a new map where [name] is associated with [data].
     *
     * @throws NameClashError if [name] is already in the map.
     */
    fun put(name: Located<N>, data: Data): NameMap<N, Data> {
        val previousDeclaration = map[name.value]?.second
        if (previousDeclaration != null) {
            throw NameClashError(name.value, previousDeclaration, name.sourceLocation)
        }
        return NameMap(
            map.put(name.value, Pair(data, name.sourceLocation))
        )
    }
}
