package edu.cornell.cs.apl.viaduct.selection

import edu.cornell.cs.apl.viaduct.backends.aby.ABYProtocolFactory
import edu.cornell.cs.apl.viaduct.backends.cleartext.LocalProtocolFactory
import edu.cornell.cs.apl.viaduct.backends.cleartext.ReplicationProtocolFactory
import edu.cornell.cs.apl.viaduct.backends.commitment.CommitmentProtocolFactory
import edu.cornell.cs.apl.viaduct.backends.zkp.ZKPProtocolFactory
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode

/** Factory for Local, Replication, Commitment, ZKP, and ABY protocols. */
fun simpleProtocolFactory(
    program: ProgramNode,
    localFactory: LocalProtocolFactory = LocalProtocolFactory(program),
    replicationFactory: ReplicationProtocolFactory = ReplicationProtocolFactory(program),
    commitmentFactory: CommitmentProtocolFactory = CommitmentProtocolFactory(program),
    zkpFactory: ZKPProtocolFactory = ZKPProtocolFactory(program),
    abyFactory: ABYProtocolFactory = ABYProtocolFactory(program)
): ProtocolFactory {
    val factory = listOf(localFactory, replicationFactory, commitmentFactory, zkpFactory, abyFactory).unions()
    abyFactory.parentFactory = factory
    return factory
}
