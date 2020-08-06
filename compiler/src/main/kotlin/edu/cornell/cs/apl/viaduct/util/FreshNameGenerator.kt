package edu.cornell.cs.apl.viaduct.util

import java.lang.Integer.max

/** Generates distinct names. Never generates the same name twice. */
class FreshNameGenerator(
    names: Set<String> // the initial set of names that will populate the name map
) {
    private val freshNameMap: MutableMap<String, Int> = mutableMapOf()

    init {
        for (name: String in names) {
            getFreshName(name)
        }
    }

    constructor() : this(setOf())

    /** Returns a new name derived from base. */
    fun getFreshName(input: String): String {
        val regex = Regex("_([0-9]+)\\z")
        val match: MatchResult? = regex.find(input)

        val base: String
        val n: Int
        if (match == null) {
            base = input
            n = freshNameMap[base] ?: 0
        } else { // strip suffix "_[num]" from base to avoid collisions
            val parsedNum: Int = match.groupValues[1].toInt()
            println(parsedNum)
            base = input.substring(0, match.range.first)
            n = max(freshNameMap[base] ?: 0, parsedNum)
        }

        freshNameMap[base] = n + 1
        return if (n > 0) "${base}_$n" else base
    }
}
