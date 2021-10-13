package edu.cornell.cs.apl.viaduct.selection

import edu.cornell.cs.apl.viaduct.backends.aby.ABYFactory
import edu.cornell.cs.apl.viaduct.backends.cleartext.LocalFactory
import edu.cornell.cs.apl.viaduct.backends.cleartext.ReplicationFactory
import edu.cornell.cs.apl.viaduct.backends.commitment.CommitmentFactory
import edu.cornell.cs.apl.viaduct.backends.zkp.ZKPFactory
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode

/** Factory for Local, Replication, Commitment, ZKP, and ABY protocols. */
fun simpleProtocolFactory(
    program: ProgramNode,
    localFactory: LocalFactory = LocalFactory(program),
    replicationFactory: ReplicationFactory = ReplicationFactory(program),
    commitmentFactory: CommitmentFactory = CommitmentFactory(program),
    zkpFactory: ZKPFactory = ZKPFactory(program),
    abyFactory: ABYFactory = ABYFactory(program)
): ProtocolFactory {
    val factory = listOf(localFactory, replicationFactory, commitmentFactory, zkpFactory, abyFactory).unions()
    abyFactory.parentFactory = factory
    return factory
}
