package io.github.aplcornell.viaduct.util

/** Generates distinct names. Never generates the same name twice. */
class FreshNameGenerator(initialNames: Set<String>) {
    /**
     * The keys are all the names returned by [getFreshName].
     * The value mapped to a name is the first suffix that should be tried to distinguish that name.
     */
    private val returnedNames: MutableMap<String, Int> = mutableMapOf()

    init {
        for (name in initialNames) {
            getFreshName(name)
        }
    }

    constructor() : this(setOf())

    /**
     * Returns a new name derived from [base]. The return name will be different from all previously returned names.
     *
     * If this is the first time [base] is passed to this function, then returns [base].
     */
    fun getFreshName(base: String): String {
        var suffix = returnedNames[base] ?: 0
        var proposedName: String

        do {
            proposedName = makeName(base, suffix)
            suffix += 1
        } while (returnedNames.containsKey(proposedName))

        returnedNames[base] = suffix
        returnedNames[proposedName] = 1
        return proposedName
    }

    private fun makeName(base: String, suffix: Int): String =
        if (suffix == 0) base else "${base}_$suffix"
}
