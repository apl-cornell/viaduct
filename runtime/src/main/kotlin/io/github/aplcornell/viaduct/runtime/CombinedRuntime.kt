package io.github.apl_cornell.viaduct.runtime

class CombinedRuntime(
    private val ioStrategy: IOStrategy,
    private val networkStrategy: NetworkStrategy
) : ViaductRuntime, IOStrategy by ioStrategy, NetworkStrategy by networkStrategy
