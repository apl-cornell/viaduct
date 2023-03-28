package io.github.aplcornell.viaduct.analysis

/**
 * A class that computes information about a [Node].
 * The information should be completely determined given the [Node].
 * That is, an [Analysis] should act like a pure function from [Node]
 * to some metadata.
 *
 * It is common to need the same analysis at multiple places in a compiler.
 * For example, type information might be useful for different passes.
 * Recompute type information over and over again would be inefficient.
 * @see AnalysisProvider for a way to cache [Analysis] instances.
 */
interface Analysis<Node>
