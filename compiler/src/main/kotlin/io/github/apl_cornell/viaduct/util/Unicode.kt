package io.github.apl_cornell.viaduct.util

import com.ibm.icu.text.BreakIterator

/**
 * Number of grapheme clusters (i.e. user-perceived characters) in the string.
 *
 * This is a better way to measure a string's length than using [CharSequence.length]
 */
fun CharSequence.graphemeClusterCount(): Int {
    val characterIterator = BreakIterator.getCharacterInstance()
    characterIterator.setText(this)
    characterIterator.first()

    var count = 0
    while (characterIterator.next() != BreakIterator.DONE) {
        count += 1
    }
    return count
}

/** A regular expression that recognizes Unicode line breaks. */
val unicodeLineBreak: Regex = Regex("\\R")
