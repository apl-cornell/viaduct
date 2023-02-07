package io.github.aplcornell.viaduct.runtime

import io.github.aplcornell.viaduct.syntax.Host

interface ViaductGeneratedProgram {
    val hosts: Set<Host>
    fun main(host: Host, runtime: ViaductRuntime)
}
