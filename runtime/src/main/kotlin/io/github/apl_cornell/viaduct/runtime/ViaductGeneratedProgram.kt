package io.github.apl_cornell.viaduct.runtime

import io.github.apl_cornell.viaduct.syntax.Host

interface ViaductGeneratedProgram {
    val hosts: Set<Host>
    fun main(host: Host, runtime: ViaductRuntime)
}
