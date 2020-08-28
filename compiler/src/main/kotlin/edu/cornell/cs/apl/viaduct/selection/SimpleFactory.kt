package edu.cornell.cs.apl.viaduct.selection

import edu.cornell.cs.apl.viaduct.protocols.ABY
import edu.cornell.cs.apl.viaduct.protocols.Commitment
import edu.cornell.cs.apl.viaduct.protocols.Local
import edu.cornell.cs.apl.viaduct.protocols.Replication
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode

fun simpleProtocolFactory(program: ProgramNode): ProtocolFactory {
    return unions(LocalFactory(program), ReplicationFactory(program), CommitmentFactory(program), ABYFactory(program))
}

fun simpleProtocolCost(p: Protocol): Int {
    return when (p) {
        is Local -> 0
        is Commitment -> 1
        is Replication -> 1
        is ABY -> 10
        else -> 100
    }
}
