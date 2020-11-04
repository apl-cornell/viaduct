package edu.cornell.cs.apl.viaduct.selection

import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode

/** Factory for Local, Replication and ABY, and Commitment protocols. */
class SimpleProtocolFactory(
    program: ProgramNode,
    localFactory: LocalFactory = LocalFactory(program),
    replicationFactory: ReplicationFactory = ReplicationFactory(program),
    abyFactory: ABYFactory = ABYFactory(program),
    zkpFactory: ZKPFactory = ZKPFactory(program),
    commitmentFactory: CommitmentFactory = CommitmentFactory(program)
) : UnionProtocolFactory(localFactory, replicationFactory, abyFactory, zkpFactory, commitmentFactory)
