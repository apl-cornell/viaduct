package edu.cornell.cs.apl.viaduct.selection

import edu.cornell.cs.apl.viaduct.backends.aby.ABYFactory
import edu.cornell.cs.apl.viaduct.backends.cleartext.LocalFactory
import edu.cornell.cs.apl.viaduct.backends.cleartext.ReplicationFactory
import edu.cornell.cs.apl.viaduct.backends.commitment.CommitmentFactory
import edu.cornell.cs.apl.viaduct.backends.zkp.ZKPFactory
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode

/** Factory for Local, Replication and ABY, and Commitment protocols. */
class SimpleProtocolFactory(
    program: ProgramNode,
    localFactory: LocalFactory = LocalFactory(program),
    replicationFactory: ReplicationFactory = ReplicationFactory(program),
    abyFactory: ABYFactory = ABYFactory(program),
    zkpFactory: ZKPFactory = ZKPFactory(program),
    commitmentFactory: CommitmentFactory = CommitmentFactory(program)
) : UnionProtocolFactory(localFactory, replicationFactory, abyFactory, zkpFactory, commitmentFactory) {
    init {
        abyFactory.parentFactory = this
    }
}
