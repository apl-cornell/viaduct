package io.github.aplcornell.viaduct.runtime

class CombinedRuntime(
    private val ioStrategy: IOStrategy,
    private val networkStrategy: NetworkStrategy
) : ViaductRuntime, IOStrategy by ioStrategy, NetworkStrategy by networkStrategy
