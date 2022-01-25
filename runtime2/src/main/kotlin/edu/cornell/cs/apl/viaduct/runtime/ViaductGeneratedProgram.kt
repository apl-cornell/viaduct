package edu.cornell.cs.apl.viaduct.runtime

import edu.cornell.cs.apl.viaduct.syntax.Host

interface ViaductGeneratedProgram {
    val programName: String
    val hosts: Set<Host>
    fun main(host: Host, runtime: Runtime)
}
