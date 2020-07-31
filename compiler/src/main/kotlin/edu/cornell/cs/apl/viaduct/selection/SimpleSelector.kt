package edu.cornell.cs.apl.viaduct.selection

import edu.cornell.cs.apl.viaduct.protocols.ABY
import edu.cornell.cs.apl.viaduct.protocols.Local
import edu.cornell.cs.apl.viaduct.protocols.Replication
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode

fun simpleSelector(program: ProgramNode): ProtocolSelector {
    return unions(LocalSelector(program), ReplicationSelector(program), ABYSelector(program))
}

fun simpleProtocolCost(p: Protocol): Int {
    return when (p) {
        is Local -> 0
        is Replication -> 1
        is ABY -> 2
        else -> 10
    }
}
