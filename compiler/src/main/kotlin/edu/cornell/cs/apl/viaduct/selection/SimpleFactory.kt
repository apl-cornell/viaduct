package edu.cornell.cs.apl.viaduct.selection

import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode

/** Factory for Local, Replication and ABY protocols. */
fun simpleProtocolFactory(program: ProgramNode): ProtocolFactory {
    return unions(LocalFactory(program), ReplicationFactory(program), CommitmentFactory(program), ABYFactory(program))
}
