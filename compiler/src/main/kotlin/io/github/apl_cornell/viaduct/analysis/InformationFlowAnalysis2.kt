package edu.cornell.cs.apl.viaduct.analysis

import edu.cornell.cs.apl.attributes.Tree
import edu.cornell.cs.apl.attributes.attribute
import edu.cornell.cs.apl.viaduct.algebra.FreeDistributiveLattice
import edu.cornell.cs.apl.viaduct.errors.InformationFlowError
import edu.cornell.cs.apl.viaduct.security.Label
import edu.cornell.cs.apl.viaduct.security.Principal
import edu.cornell.cs.apl.viaduct.security.solver2.Constraint
import edu.cornell.cs.apl.viaduct.security.solver2.ConstraintSystem
import edu.cornell.cs.apl.viaduct.security.solver2.Term
import edu.cornell.cs.apl.viaduct.security.solver2.flowsTo
import edu.cornell.cs.apl.viaduct.security.solver2.term
import edu.cornell.cs.apl.viaduct.syntax.HasSourceLocation
import edu.cornell.cs.apl.viaduct.syntax.Variable
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BlockNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclassificationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.EndorsementNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.FunctionArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.FunctionDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.IfNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InfiniteLoopNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LiteralNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.Node
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OperatorApplicationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.QueryNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReadNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.VariableDeclarationNode
import java.io.Writer

private typealias LabelConstant = FreeDistributiveLattice<Principal>
private typealias LabelTerm = Term<LabelConstant, LabelVariable>
private typealias LabelConstraint = Constraint<LabelConstant, LabelVariable, InformationFlowError>

/** We infer labels for specific nodes in the program, so we need constraint variables only for those nodes. */
private sealed class LabelVariable {
    /** Label of the program counter at [path]. */
    data class PC(val path: String) : LabelVariable() {
        override fun toString(): String = path
    }

    /** Label of a piece of data. */
    sealed class Data : LabelVariable() {
        /** Label of the [Variable] declared by [node]. */
        data class Declaration(val node: VariableDeclarationNode) : Data() {
            override fun toString(): String = node.name.value.name
        }

        /** Label of a function argument. */
        data class Argument(val node: FunctionArgumentNode) : Data() {
            init {
                require(node !is VariableDeclarationNode)
            }

            override fun toString(): String = node.toDocument().print()
        }

        /** Label of the input to [DeclassificationNode]. */
        data class DeclassificationFrom(val node: DeclassificationNode) : Data() {
            override fun toString(): String = node.toDocument().print()
        }

        /** Label of the output of [EndorsementNode]. */
        data class EndorsementTo(val node: EndorsementNode) : Data() {
            override fun toString(): String = node.toDocument().print()
        }
    }
}

/** Associates [Variable]s with their [Label]s. */
class InformationFlowAnalysis2 private constructor(
    private val tree: Tree<Node, ProgramNode>,
    private val nameAnalysis: NameAnalysis
) {
    private val constraintSystem: ConstraintSystem<LabelConstant, LabelVariable, InformationFlowError> = TODO()

    private val solution by lazy { constraintSystem.solution() }

    /** Name of the PC label at a particular node. */
    private val Node.pathName: String by attribute {
        val parent = tree.parent(this)
        when {
            parent == null -> "program"

            this is FunctionDeclarationNode -> "${parent.pathName}.${name.value.name}"

            this is IfNode -> "${parent.pathName}.${tree.childIndex(this)}.if"

            parent is IfNode && this is BlockNode -> "${parent.pathName}.${tree.childIndex(this)}"

            this is InfiniteLoopNode -> "${parent.pathName}.${tree.childIndex(this)}.loop"

            else -> parent.pathName
        }
    }

    /** The [LabelTerm] that represents the label of the program counter at this node. */
    private val Node.pcTerm: LabelTerm
        get() = term(LabelVariable.PC(pathName))

    /** The [LabelTerm] that represents the label of the [Variable] declared by this node. */
    // TODO: incorporate label annotations
    private val VariableDeclarationNode.labelTerm: LabelTerm
        get() = term(LabelVariable.Data.Declaration(this))

    /** Returns constraints asserting that [this] node with the given label can flow to a location with label [to]. */
    private infix fun Pair<HasSourceLocation, LabelTerm>.flowsTo(to: LabelTerm): Sequence<LabelConstraint> {
        val constraints: Iterable<LabelConstraint> =
            second.flowsTo(to, LabelConstant.bounds()) { _, _ ->
                TODO("${this.first}")
                // TODO: handle illegal terms
                // InsecureDataFlowError(this.first, actualNodeLabel, toLabel)
            }
        return constraints.asSequence()
    }

    /** Returns constraints asserting that the pc at [this] node flows to [node] with label [nodeLabel]. */
    private fun Node.pcFlowsTo(node: HasSourceLocation, nodeLabel: LabelTerm): Sequence<LabelConstraint> =
        TODO("$node: $nodeLabel")

    /** Returns constraints that need to hold for [this] expression to flow to [outputLabel]. */
    private infix fun ExpressionNode.flowsTo(outputLabel: LabelTerm): Sequence<LabelConstraint> =
        when (this) {
            is LiteralNode ->
                sequenceOf()

            is ReadNode -> {
                val declarationLabel = nameAnalysis.declaration(this).labelTerm
                (this to declarationLabel) flowsTo outputLabel
            }

            is OperatorApplicationNode -> {
                sequence {
                    arguments.forEach { argument -> yieldAll(argument flowsTo outputLabel) }
                }
            }

            is QueryNode -> {
                val declarationLabel = nameAnalysis.declaration(this).labelTerm

                sequence {
                    arguments.forEach { yieldAll(it flowsTo declarationLabel) }
                    this@flowsTo.pcFlowsTo(this@flowsTo.variable, declarationLabel)

                    // TODO: return label should be based on the query.
                    //  For example, Array.length leaks the label on the size but not the data.
                    yieldAll((this@flowsTo to declarationLabel) flowsTo outputLabel)
                }
            }

//            is DowngradeNode -> {
//                val from =
//                    fromLabel?.let {
//                        LabelConstant(it.value.interpret(parameterMap))
//                    } ?: expression.labelVariable
//
//                val to = toLabel?.let {
//                    LabelConstant(it.value.interpret(parameterMap))
//                } ?: this.labelVariable
//
//                pcFlowsTo(solver, pcLabel, this, to)
//
//                // From label must match the expression label if it is specified
//                if (this.fromLabel != null) {
//                    solver.addEqualToConstraint(this.expression.labelVariable, from) { actual, expected ->
//                        LabelMismatchError(this.expression, actual, expected)
//                    }
//                }
//
//                when (this) {
//                    is DeclassificationNode -> {
//                        // Don't downgrade integrity
//                        solver.addEqualToConstraint(from.integrity(), to.integrity()) { fromLabel, toLabel ->
//                            IntegrityChangingDeclassificationError(this, fromLabel, toLabel)
//                        }
//
//                        // Nonmalleability
//                        val toConst = to as LabelConstant
//                        solver.addFlowsToConstraint(from, from.swap().join(toConst.value)) { _, _ ->
//                            MalleableDowngradeError(this)
//                        }
//                    }
//
//                    is EndorsementNode -> {
//                        // Don't downgrade confidentiality
//                        solver.addEqualToConstraint(
//                            from.confidentiality(),
//                            to.confidentiality()
//                        ) { fromLabel, toLabel ->
//                            ConfidentialityChangingEndorsementError(this, fromLabel, toLabel)
//                        }
//
//                        // Nonmalleability
//                        val fromConst = from as LabelConstant
//                        solver.addFlowsToConstraint(from, to.join(fromConst.value.swap())) { _, _ ->
//                            MalleableDowngradeError(this)
//                        }
//                    }
//                }
//
//                assertFlowsTo(solver, this, to, this.labelVariable)
//            }

//            is InputNode -> {
//                val hostLabel =
//                    LabelConstant(nameAnalysis.declaration(this).authority.value.interpret())
//
//                // Host learns the current pc
//                pcFlowsTo(solver, pcLabel, this.host, hostLabel)
//
//                assertFlowsTo(solver, this.host, hostLabel, this.labelVariable)
//            }
            else -> TODO()
        }

    /**
     * Generate information flow constraints for a statement.
     */
//    fun StatementNode.check(
//        solver: ConstraintSolver<InformationFlowError>,
//        parameterMap: Map<String, Label>,
//        pcLabel: AtomicLabelTerm
//    ) {
//        when (this) {
//            is LetNode -> {
//                this.value.check(solver, parameterMap, pcLabel)
//                flowsTo(solver, this.value, this.temporaryLabel)
//            }
//
//            is DeclarationNode -> {
//                val varLabel = this.variableLabel(parameterMap)
//                for (argument in this.arguments) {
//                    argument.check(solver, parameterMap, pcLabel)
//                    assertFlowsTo(solver, argument, argument.labelVariable, varLabel)
//                }
//                pcFlowsTo(solver, pcLabel, this.name, varLabel)
//            }
//
//            is UpdateNode -> {
//                val variableLabel = nameAnalysis.declaration(this).variableLabel(parameterMap)
//                for (argument in this.arguments) {
//                    argument.check(solver, parameterMap, pcLabel)
//                    flowsTo(solver, argument, variableLabel)
//                }
//                pcFlowsTo(solver, pcLabel, this.variable, variableLabel)
//                // TODO: consult the method signature. There may be constraints on the pc or the arguments.
//            }
//
//            is OutParameterInitializationNode -> {
//                val variableLabel = nameAnalysis.declaration(this).variableLabel(parameterMap)
//                pcFlowsTo(solver, pcLabel, this.name, variableLabel)
//                when (val initializer = this.initializer) {
//                    is OutParameterConstructorInitializerNode -> {
//                        for (argument in initializer.arguments) {
//                            argument.check(solver, parameterMap, pcLabel)
//                            flowsTo(solver, argument, variableLabel)
//                        }
//                    }
//
//                    is OutParameterExpressionInitializerNode -> {
//                        initializer.expression.check(solver, parameterMap, pcLabel)
//                        flowsTo(solver, initializer.expression, variableLabel)
//                    }
//                }
//            }
//
//            is FunctionCallNode -> {
//                // assert argument labels and PC match the called function's labels
//                if (solutionMap.containsKey(this.name.value)) {
//                    val funcDecl = nameAnalysis.declaration(this)
//                    val funcPcVariable = pcVariableMap.getValue(funcDecl.pathName)
//                    val parameterVariables = parameterVariableMap.getValue(this.name.value)
//
//                    val functionPcLabel = solutionMap.getValue(funcPcVariable.first).getValue(funcPcVariable.second)
//                    val parameterSolution = solutionMap.getValue(parameterVariables.first)
//                    val parameterLabels =
//                        parameterVariables.second
//                            .map { kv -> Pair(kv.key, parameterSolution.getValue(kv.value)) }
//                            .toMap()
//
//                    assertEqualsTo(
//                        solver,
//                        this,
//                        pcLabel,
//                        LabelConstant(functionPcLabel)
//                    )
//
//                    for (argument in this.arguments) {
//                        val parameterLabel: Label =
//                            parameterLabels.getValue(nameAnalysis.parameter(argument).name.value)
//                        val argumentLabel =
//                            when (argument) {
//                                is ExpressionArgumentNode -> {
//                                    argument.expression.check(solver, parameterMap, pcLabel)
//                                    argument.expression.labelVariable
//                                }
//
//                                is ObjectReferenceArgumentNode ->
//                                    nameAnalysis.declaration(argument).variableLabel()
//
//                                is ObjectDeclarationArgumentNode ->
//                                    argument.variableLabel()
//
//                                is OutParameterArgumentNode ->
//                                    nameAnalysis.declaration(argument).variableLabel()
//                            }
//
//                        assertEqualsTo(
//                            solver,
//                            argument,
//                            LabelConstant(parameterLabel),
//                            argumentLabel
//                        )
//                    }
//                } else { // add function to worklist
//                    val enclosingFunction = nameAnalysis.enclosingFunctionName(this)
//                    val argumentLabelMap =
//                        this.arguments.associate { argument ->
//                            val parameter = nameAnalysis.parameter(argument)
//                            val argumentVariable =
//                                when (argument) {
//                                    is ExpressionArgumentNode -> {
//                                        argument.expression.check(solver, parameterMap, pcLabel)
//                                        argument.expression.labelVariable
//                                    }
//
//                                    is ObjectReferenceArgumentNode ->
//                                        nameAnalysis.declaration(argument).variableLabel()
//
//                                    is ObjectDeclarationArgumentNode ->
//                                        argument.variableLabel()
//
//                                    is OutParameterArgumentNode ->
//                                        nameAnalysis.declaration(argument).variableLabel()
//                                }
//
//                            Pair(parameter.name.value, argumentVariable)
//                        }
//
//                    val parameterVariables =
//                        // no complex expressions with label parameters
//                        this.arguments.associate { argument ->
//                            val parameter = nameAnalysis.parameter(argument)
//                            val parameterVariable =
//                                solver.addNewVariable(nameGenerator.getFreshName(parameter.name.value.name))
//                            val argumentLabel = argumentLabelMap.getValue(parameter.name.value)
//
//                            assertEqualsTo(
//                                solver,
//                                argument,
//                                parameterVariable,
//                                argumentLabel
//                            )
//
//                            if (parameter.objectType.labelArguments != null) {
//                                val labelBoundExpr = parameter.objectType.labelArguments[0].value
//                                val labelBound =
//                                    when {
//                                        labelBoundExpr is LabelParameter ->
//                                            argumentLabelMap.getValue(ObjectVariable(labelBoundExpr.name))
//
//                                        !labelBoundExpr.containsParameters() ->
//                                            LabelConstant(labelBoundExpr.interpret())
//
//                                        // no complex expressions with label parameters
//                                        else -> throw Error("no complex label expressions with parameters in function signatures")
//                                    }
//
//                                if (argument is ObjectDeclarationArgumentNode) {
//                                    assertEqualsTo(
//                                        solver,
//                                        argument,
//                                        argumentLabel,
//                                        labelBound
//                                    )
//                                } else {
//                                    assertFlowsTo(
//                                        solver,
//                                        argument,
//                                        argumentLabel,
//                                        labelBound
//                                    )
//                                }
//                            }
//
//                            Pair(parameter.name.value, parameterVariable)
//                        }
//
//                    val functionDecl = nameAnalysis.declaration(this)
//                    val functionPc =
//                        solver.addNewVariable(nameGenerator.getFreshName("${this.name.value}.pc"))
//
//                    assertEqualsTo(solver, this, functionPc, pcLabel)
//
//                    if (functionDecl.pcLabel != null) {
//                        val labelBound = functionDecl.pcLabel.value
//
//                        when {
//                            labelBound is LabelParameter -> {
//                                assertFlowsTo(
//                                    solver,
//                                    this,
//                                    pcLabel,
//                                    argumentLabelMap.getValue(ObjectVariable(labelBound.name))
//                                )
//                            }
//
//                            !labelBound.containsParameters() -> {
//                                assertFlowsTo(
//                                    solver,
//                                    this,
//                                    pcLabel,
//                                    LabelConstant(labelBound.interpret())
//                                )
//                            }
//
//                            // no complex expressions with label parameters
//                            else -> throw Error("no complex label expressions with parameters in function signatures")
//                        }
//                    }
//
//                    pcVariableMap[functionDecl.pathName] = Pair(enclosingFunction, functionPc)
//                    functionPcVariableMap[this.name.value] = Pair(enclosingFunction, functionPc)
//                    parameterVariableMap[this.name.value] = Pair(enclosingFunction, parameterVariables)
//                    worklist.add(nameAnalysis.declaration(this))
//                }
//            }
//
//            is OutputNode -> {
//                this.message.check(solver, parameterMap, pcLabel)
//
//                val hostLabel =
//                    LabelConstant(nameAnalysis.declaration(this).authority.value.interpret())
//                pcFlowsTo(solver, pcLabel, this.host, hostLabel)
//                flowsTo(solver, this.message, hostLabel)
//            }
//
//            is IfNode -> {
//                this.guard.check(solver, parameterMap, pcLabel)
//                val thenPc = createPCVariable(solver, this.thenBranch)
//                pcFlowsTo(solver, pcLabel, this, thenPc)
//                flowsTo(solver, this.guard, thenPc)
//                this.thenBranch.check(solver, parameterMap, thenPc)
//
//                val elsePc = createPCVariable(solver, this.elseBranch)
//                pcFlowsTo(solver, pcLabel, this, elsePc)
//                flowsTo(solver, this.guard, elsePc)
//                this.elseBranch.check(solver, parameterMap, elsePc)
//            }
//
//            is InfiniteLoopNode -> {
//                val loopPc = createPCVariable(solver, this)
//                pcFlowsTo(solver, pcLabel, this, loopPc)
//                this.body.check(solver, parameterMap, loopPc)
//            }
//
//            is BreakNode -> {
//                val loopPath = nameAnalysis.correspondingLoop(this).pathName
//                val loopPc = pcVariableMap.getValue(loopPath).second
//                pcFlowsTo(solver, pcLabel, this, loopPc)
//            }
//
//            is AssertionNode -> {
//                // Everybody must execute assertions, so [condition] must be public and trusted.
//                // TODO: can we do any better? This seems almost impossible to achieve...
//                flowsTo(solver, this.condition, LabelConstant(Label.bottom))
//            }
//
//            is BlockNode -> {
//                for (child in statements) {
//                    child.check(solver, parameterMap, pcLabel)
//                }
//            }
//        }
//    }

    /** Returns the inferred security label of the [Variable] defined by [node]. */
    fun label(node: VariableDeclarationNode): Label =
        TODO("$node")

    /**
     * Asserts that the program does not violate information flow security.
     *
     * @throws InformationFlowError if the program has insecure information flow.
     */
    fun check() {
        // Force thunk to ensure there is a solution to the constraints.
        solution
    }

    /** Outputs a DOT representation of the program's constraint graph to [output]. */
    fun exportConstraintGraph(output: Writer) {
        constraintSystem.exportDotGraph(output)
    }

    companion object : AnalysisProvider<InformationFlowAnalysis2> {
        private fun construct(program: ProgramNode) =
            InformationFlowAnalysis2(program.tree, NameAnalysis.get(program))

        override fun get(program: ProgramNode): InformationFlowAnalysis2 = program.cached(::construct)
    }
}
