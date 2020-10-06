package edu.cornell.cs.apl.viaduct.selection

import edu.cornell.cs.apl.viaduct.analysis.NameAnalysis
import edu.cornell.cs.apl.viaduct.analysis.functionCallNodes
import edu.cornell.cs.apl.viaduct.protocols.Local
import edu.cornell.cs.apl.viaduct.syntax.intermediate.IfNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LiteralNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReadNode

/** Factory for Local, Replication and ABY protocols. */
class SimpleProtocolFactory(
    private val program: ProgramNode,
    private val localFactory: LocalFactory = LocalFactory(program),
    private val replicationFactory: ReplicationFactory = ReplicationFactory(program),
    private val abyFactory: ABYFactory = ABYFactory(program)
) : UnionProtocolFactory(localFactory, replicationFactory, abyFactory) {
    private val nameAnalysis = NameAnalysis.get(program)

    // if there is an MPC protocol participating in a branch of a conditional,
    // make sure that the guard is not computed in a Local protocol,
    // since the MPC protocol will treat it as an input value and not as a replicated cleartext value.
    override fun constraint(node: IfNode): SelectionConstraint {
        return when (val guard = node.guard) {
            is LiteralNode -> super.constraint(node)

            is ReadNode -> {
                val functionName = nameAnalysis.enclosingFunctionName(node)
                val variables = nameAnalysis.variables(node)
                val mpcProtocols = abyFactory.protocols.map { it.protocol }.toSet()

                val plaintextCondition =
                    // all protocols participate in function calls
                    if (node.functionCallNodes().isNotEmpty()) {
                        Literal(true)
                    } else {
                        variables
                            .map { v -> VariableIn(v, mpcProtocols) }
                            .fold<SelectionConstraint, SelectionConstraint>(Literal(false)) { acc, constr ->
                                Or(acc, constr)
                            }
                    }

                val guardNotMPCInput =
                    Not(VariableIn(
                        FunctionVariable(functionName, guard.temporary.value),
                        super.viableProtocols(nameAnalysis.declaration(guard))
                            .filterIsInstance<Local>()
                            .toSet()
                    ))

                val mpcPlaintextGuardConstraint = Implies(plaintextCondition, guardNotMPCInput)

                And(super.constraint(node), mpcPlaintextGuardConstraint)
            }
        }
    }
}
