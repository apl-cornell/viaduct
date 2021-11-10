package edu.cornell.cs.apl.viaduct.selection

import edu.cornell.cs.apl.viaduct.errors.NoHostDeclarationsError
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode

class ProtocolSelection(
    private val solver: SelectionProblemSolver,
    private val protocolFactory: ProtocolFactory,
    private val protocolComposer: ProtocolComposer,
    private val costEstimator: CostEstimator<IntegerCost>
) {

    fun selectAssignment(program: ProgramNode): ProtocolAssignment {
        if (program.hosts.isEmpty()) {
            throw NoHostDeclarationsError(program.sourceLocation.sourcePath)
        }

        val constraintGenerator = SelectionConstraintGenerator(program, protocolFactory, protocolComposer, costEstimator)
        val selectionProblem = constraintGenerator.getSelectionProblem()
        return solver.solveSelectionProblem(selectionProblem)
    }
}
