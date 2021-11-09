package edu.cornell.cs.apl.viaduct.analysis

import edu.cornell.cs.apl.attributes.Tree
import edu.cornell.cs.apl.attributes.attribute
import edu.cornell.cs.apl.prettyprinting.PrettyPrintable
import edu.cornell.cs.apl.viaduct.errors.ConfidentialityChangingEndorsementError
import edu.cornell.cs.apl.viaduct.errors.InformationFlowError
import edu.cornell.cs.apl.viaduct.errors.InsecureControlFlowError
import edu.cornell.cs.apl.viaduct.errors.InsecureDataFlowError
import edu.cornell.cs.apl.viaduct.errors.IntegrityChangingDeclassificationError
import edu.cornell.cs.apl.viaduct.errors.LabelMismatchError
import edu.cornell.cs.apl.viaduct.errors.MalleableDowngradeError
import edu.cornell.cs.apl.viaduct.security.Label
import edu.cornell.cs.apl.viaduct.security.LabelParameter
import edu.cornell.cs.apl.viaduct.security.solver.AtomicLabelTerm
import edu.cornell.cs.apl.viaduct.security.solver.ConstraintSolution
import edu.cornell.cs.apl.viaduct.security.solver.ConstraintSolver
import edu.cornell.cs.apl.viaduct.security.solver.LabelConstant
import edu.cornell.cs.apl.viaduct.security.solver.LabelTerm
import edu.cornell.cs.apl.viaduct.security.solver.LabelVariable
import edu.cornell.cs.apl.viaduct.syntax.FunctionName
import edu.cornell.cs.apl.viaduct.syntax.HasSourceLocation
import edu.cornell.cs.apl.viaduct.syntax.ObjectVariable
import edu.cornell.cs.apl.viaduct.syntax.Temporary
import edu.cornell.cs.apl.viaduct.syntax.Variable
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AssertionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BlockNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BreakNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclassificationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DowngradeNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.EndorsementNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ExpressionArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.FunctionArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.FunctionCallNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.FunctionDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.IfNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InfiniteLoopNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LiteralNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.Node
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ObjectDeclaration
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ObjectDeclarationArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ObjectReferenceArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OperatorApplicationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutParameterArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutParameterConstructorInitializerNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutParameterExpressionInitializerNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutParameterInitializationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ParameterNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.QueryNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReadNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.StatementNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.UpdateNode
import edu.cornell.cs.apl.viaduct.util.FreshNameGenerator
import kotlinx.collections.immutable.persistentMapOf
import mu.KotlinLogging
import java.io.Writer
import java.util.LinkedList

private val logger = KotlinLogging.logger("InformationFlowAnalysis")

/** Associates [Variable]s with their [Label]s. */
class InformationFlowAnalysis private constructor(
    private val tree: Tree<Node, ProgramNode>,
    private val nameAnalysis: NameAnalysis
) {
    private val nameGenerator = FreshNameGenerator()

    private val constraintSolverMap: MutableMap<FunctionName, ConstraintSolver<InformationFlowError>> = mutableMapOf()
    private val solutionMap: MutableMap<FunctionName, ConstraintSolution> = mutableMapOf()
    private val parameterVariableMap: MutableMap<FunctionName, Pair<FunctionName, Map<ObjectVariable, LabelVariable>>> =
        mutableMapOf()
    private val pcVariableMap: MutableMap<String, Pair<FunctionName, LabelVariable>> = mutableMapOf()
    private val functionPcVariableMap: MutableMap<FunctionName, Pair<FunctionName, LabelVariable>> = mutableMapOf()
    private val worklist = LinkedList<FunctionDeclarationNode>()

    private fun constraintSolver(function: FunctionName): ConstraintSolver<InformationFlowError> =
        constraintSolverMap.getValue(function)

    private fun constraintSolver(expr: ExpressionNode): ConstraintSolver<InformationFlowError> =
        constraintSolver(nameAnalysis.enclosingFunctionName(expr))

    private fun constraintSolver(stmt: StatementNode): ConstraintSolver<InformationFlowError> =
        constraintSolver(nameAnalysis.enclosingFunctionName(stmt))

    private fun constraintSolver(arg: FunctionArgumentNode): ConstraintSolver<InformationFlowError> =
        constraintSolver(nameAnalysis.enclosingFunctionName(arg))

    private fun constraintSolver(parameter: ParameterNode): ConstraintSolver<InformationFlowError> =
        constraintSolver(nameAnalysis.functionDeclaration(parameter).name.value)

    /** The (fresh) variable that stands in for the [Label] of the result of this expression. */
    private val ExpressionNode.labelVariable: LabelVariable by attribute {
        constraintSolver(this).addNewVariable(PrettyNodeWrapper(this))
    }

    /** The [LabelTerm] representing the [Label] of the temporary defined by this node. */
    private val LetNode.temporaryLabel: LabelVariable by attribute {
        constraintSolver(this).addNewVariable(PrettyNodeWrapper(this))
    }

    private val variableLabelMap: MutableMap<Node, AtomicLabelTerm> = mutableMapOf()

    private fun ObjectDeclaration.variableLabel(
        parameterMap: Map<String, Label> = persistentMapOf()
    ): AtomicLabelTerm =
        variableLabelMap.getOrPut(
            this.declarationAsNode
        ) {
            if (labelArguments == null || this.declarationAsNode is ObjectDeclarationArgumentNode) {
                when (val declaration = this.declarationAsNode) {
                    is DeclarationNode ->
                        constraintSolver(declaration).addNewVariable(PrettyNodeWrapper(this.name))

                    is ParameterNode ->
                        constraintSolver(declaration).addNewVariable(PrettyNodeWrapper(this.name))

                    is ObjectDeclarationArgumentNode ->
                        constraintSolver(declaration).addNewVariable(PrettyNodeWrapper(this.name))

                    else -> throw Exception("Impossible case: Unknown ObjectDeclaration type")
                }
            } else {
                // TODO: this is hacky. How do we know it's the first label, for example?
                LabelConstant(labelArguments!![0].value.interpret(parameterMap))
            }
        }

    /**
     * Name of the PC label at a particular node.
     */
    private val Node.pathName: String by attribute {
        val parent = tree.parent(this)
        when {
            parent == null -> "program"

            this is FunctionDeclarationNode -> "${parent.pathName}.${name.value.name}"

            parent is IfNode && this is BlockNode -> "${parent.pathName}.if.${tree.childIndex(this)}"

            this is InfiniteLoopNode -> "${parent.pathName}.loop"

            else -> parent.pathName
        }
    }

    /**
     * Adds a constraint asserting that [node] with label [nodeLabel] can flow to a location
     * with label [to].
     */
    private fun assertFlowsTo(
        solver: ConstraintSolver<InformationFlowError>,
        node: HasSourceLocation,
        nodeLabel: AtomicLabelTerm,
        to: LabelTerm
    ) =
        solver.addFlowsToConstraint(nodeLabel, to) { actualNodeLabel, toLabel ->
            InsecureDataFlowError(node, actualNodeLabel, toLabel)
        }

    /**
     * Adds a constraint asserting that [node] with label [nodeLabel] is equal to a location
     * with label [to].
     */
    private fun assertEqualsTo(
        solver: ConstraintSolver<InformationFlowError>,
        node: HasSourceLocation,
        nodeLabel: AtomicLabelTerm,
        to: AtomicLabelTerm
    ) =
        solver.addEqualToConstraint(nodeLabel, to) { actualNodeLabel, toLabel ->
            InsecureDataFlowError(node, actualNodeLabel, toLabel)
        }

    private fun flowsTo(
        solver: ConstraintSolver<InformationFlowError>,
        expr: ExpressionNode,
        to: LabelTerm
    ) = assertFlowsTo(solver, expr, expr.labelVariable, to)

    private fun pcFlowsTo(
        solver: ConstraintSolver<InformationFlowError>,
        pc: AtomicLabelTerm,
        node: HasSourceLocation,
        nodeLabel: AtomicLabelTerm
    ) {
        solver.addFlowsToConstraint(pc, nodeLabel) { pcLabel, actualNodeLabel ->
            InsecureControlFlowError(node, actualNodeLabel, pcLabel)
        }
    }

    /**
     * Create a PC label variable and save into global map.
     */
    private fun createPCVariable(
        solver: ConstraintSolver<InformationFlowError>,
        function: FunctionName,
        path: String
    ): LabelVariable {
        val pc = solver.addNewVariable(nameGenerator.getFreshName(path))
        pcVariableMap[path] = Pair(function, pc)
        return pc
    }

    /**
     * Create a PC label variable for a statement.
     */
    private fun createPCVariable(
        solver: ConstraintSolver<InformationFlowError>,
        stmt: StatementNode
    ): LabelVariable =
        createPCVariable(solver, nameAnalysis.enclosingFunctionName(stmt), stmt.pathName)

    /** Returns the inferred security label of the [Temporary] defined by [node]. */
    fun label(node: LetNode): Label =
        solutionMap.getValue(nameAnalysis.enclosingFunctionName(node)).getValue(node.temporaryLabel)

    /** Returns the inferred security label of the [ObjectVariable] declared by [node]. */
    fun label(node: DeclarationNode): Label =
        when (val label = node.variableLabel()) {
            is LabelConstant -> label.value

            is LabelVariable ->
                solutionMap.getValue(nameAnalysis.enclosingFunctionName(node)).getValue(label)

            else -> throw Error("impossible case")
        }

    /** Returns the inferred security label of the [ObjectVariable] declared by [node]. */
    fun label(node: ParameterNode): Label {
        val function = nameAnalysis.functionDeclaration(node)
        val (labelFunction, labelParamMap) = parameterVariableMap.getValue(function.name.value)
        return solutionMap.getValue(labelFunction).getValue(labelParamMap.getValue(node.name.value))
    }

    /** Returns the inferred security label of the [ObjectVariable] declared by [node]. */
    fun label(node: ObjectDeclarationArgumentNode): Label {
        val labelVar = nameAnalysis.asObjectDeclaration(node).variableLabel()
        return labelVar.getValue(solutionMap.getValue(nameAnalysis.enclosingFunctionName(node)))
    }

    /** Returns the inferred security label of the result of [node]. */
    fun label(node: ExpressionNode): Label =
        node.labelVariable.getValue(solutionMap.getValue(nameAnalysis.enclosingFunctionName(node)))

    /** Returns the label of the program counter at the [node]'s program point. */
    fun pcLabel(node: Node): Label {
        val path = node.pathName
        val (pcFunction, pcVariable) = pcVariableMap.getValue(path)
        return solutionMap.getValue(pcFunction).getValue(pcVariable)
    }

    /**
     * Generate information flow constraints for an expression.
     */
    fun ExpressionNode.check(
        solver: ConstraintSolver<InformationFlowError>,
        parameterMap: Map<String, Label>,
        pcLabel: AtomicLabelTerm
    ) {
        when (this) {
            is LiteralNode -> Unit

            is ReadNode -> {
                val temporaryLabel = nameAnalysis.declaration(this).temporaryLabel
                assertFlowsTo(solver, temporary, temporaryLabel, this.labelVariable)
            }

            is OperatorApplicationNode -> {
                for (argument in this.arguments) {
                    argument.check(solver, parameterMap, pcLabel)
                    flowsTo(solver, argument, this.labelVariable)
                }
            }

            is QueryNode -> {
                val variableLabel = nameAnalysis.declaration(this).variableLabel(parameterMap)

                for (argument in this.arguments) {
                    argument.check(solver, parameterMap, pcLabel)
                    flowsTo(solver, argument, variableLabel)
                }

                pcFlowsTo(solver, pcLabel, this.variable, variableLabel)

                // TODO: return label should be based on the query.
                //  For example, Array.length leaks the label on the size but not the data.
                assertFlowsTo(solver, variable, variableLabel, this.labelVariable)
            }

            is DowngradeNode -> {
                this.expression.check(solver, parameterMap, pcLabel)

                val from =
                    fromLabel?.let {
                        LabelConstant(it.value.interpret(parameterMap))
                    } ?: expression.labelVariable

                val to = toLabel?.let {
                    LabelConstant(it.value.interpret(parameterMap))
                } ?: this.labelVariable

                pcFlowsTo(solver, pcLabel, this, to)

                // From label must match the expression label if it is specified
                if (this.fromLabel != null) {
                    solver.addEqualToConstraint(this.expression.labelVariable, from) { actual, expected ->
                        LabelMismatchError(this.expression, actual, expected)
                    }
                }

                /* Don't need this PC check anymore I think? it's not in the rules in the paper
                solver.addFlowsToConstraint(from, pcLabel.swap().join(to.value)) { _, _ ->
                    MalleableDowngradeError(this)
                }
                */

                when (this) {
                    is DeclassificationNode -> {
                        // Don't downgrade integrity
                        solver.addEqualToConstraint(from.integrity(), to.integrity()) { fromLabel, toLabel ->
                            IntegrityChangingDeclassificationError(this, fromLabel, toLabel)
                        }

                        // Nonmalleability
                        val toConst = to as LabelConstant
                        solver.addFlowsToConstraint(from, from.swap().join(toConst.value)) { _, _ ->
                            MalleableDowngradeError(this)
                        }
                    }

                    is EndorsementNode -> {
                        // Don't downgrade confidentiality
                        solver.addEqualToConstraint(from.confidentiality(), to.confidentiality()) { fromLabel, toLabel ->
                            ConfidentialityChangingEndorsementError(this, fromLabel, toLabel)
                        }

                        // Nonmalleability
                        val fromConst = from as LabelConstant
                        solver.addFlowsToConstraint(from, to.join(fromConst.value.swap())) { _, _ ->
                            MalleableDowngradeError(this)
                        }
                    }
                }

                assertFlowsTo(solver, this, to, this.labelVariable)
            }

            is InputNode -> {
                val hostLabel =
                    LabelConstant(nameAnalysis.declaration(this).authority.value.interpret())

                // Host learns the current pc
                pcFlowsTo(solver, pcLabel, this.host, hostLabel)

                assertFlowsTo(solver, this.host, hostLabel, this.labelVariable)
            }
        }
    }

    /**
     * Generate information flow constraints for a statement.
     */
    fun StatementNode.check(
        solver: ConstraintSolver<InformationFlowError>,
        parameterMap: Map<String, Label>,
        pcLabel: AtomicLabelTerm
    ) {
        when (this) {
            is LetNode -> {
                this.value.check(solver, parameterMap, pcLabel)
                flowsTo(solver, this.value, this.temporaryLabel)
            }

            is DeclarationNode -> {
                val varLabel = this.variableLabel(parameterMap)
                for (argument in this.arguments) {
                    argument.check(solver, parameterMap, pcLabel)
                    assertFlowsTo(solver, argument, argument.labelVariable, varLabel)
                }
                pcFlowsTo(solver, pcLabel, this.name, varLabel)
            }

            is UpdateNode -> {
                val variableLabel = nameAnalysis.declaration(this).variableLabel(parameterMap)
                for (argument in this.arguments) {
                    argument.check(solver, parameterMap, pcLabel)
                    flowsTo(solver, argument, variableLabel)
                }
                pcFlowsTo(solver, pcLabel, this.variable, variableLabel)
                // TODO: consult the method signature. There may be constraints on the pc or the arguments.
            }

            is OutParameterInitializationNode -> {
                val variableLabel = nameAnalysis.declaration(this).variableLabel(parameterMap)
                pcFlowsTo(solver, pcLabel, this.name, variableLabel)
                when (val initializer = this.initializer) {
                    is OutParameterConstructorInitializerNode -> {
                        for (argument in initializer.arguments) {
                            argument.check(solver, parameterMap, pcLabel)
                            flowsTo(solver, argument, variableLabel)
                        }
                    }

                    is OutParameterExpressionInitializerNode -> {
                        initializer.expression.check(solver, parameterMap, pcLabel)
                        flowsTo(solver, initializer.expression, variableLabel)
                    }
                }
            }

            is FunctionCallNode -> {
                // assert argument labels and PC match the called function's labels
                if (solutionMap.containsKey(this.name.value)) {
                    val funcDecl = nameAnalysis.declaration(this)
                    val funcPcVariable = pcVariableMap.getValue(funcDecl.pathName)
                    val parameterVariables = parameterVariableMap.getValue(this.name.value)

                    val functionPcLabel = solutionMap.getValue(funcPcVariable.first).getValue(funcPcVariable.second)
                    val parameterSolution = solutionMap.getValue(parameterVariables.first)
                    val parameterLabels =
                        parameterVariables.second
                            .map { kv -> Pair(kv.key, parameterSolution.getValue(kv.value)) }
                            .toMap()

                    assertEqualsTo(
                        solver,
                        this,
                        pcLabel,
                        LabelConstant(functionPcLabel)
                    )

                    for (argument in this.arguments) {
                        val parameterLabel: Label =
                            parameterLabels.getValue(nameAnalysis.parameter(argument).name.value)
                        val argumentLabel =
                            when (argument) {
                                is ExpressionArgumentNode -> {
                                    argument.expression.check(solver, parameterMap, pcLabel)
                                    argument.expression.labelVariable
                                }

                                is ObjectReferenceArgumentNode ->
                                    nameAnalysis.declaration(argument).variableLabel()

                                is ObjectDeclarationArgumentNode ->
                                    nameAnalysis.asObjectDeclaration(argument).variableLabel()

                                is OutParameterArgumentNode ->
                                    nameAnalysis.declaration(argument).variableLabel()
                            }

                        assertEqualsTo(
                            solver,
                            argument,
                            LabelConstant(parameterLabel),
                            argumentLabel
                        )
                    }
                } else { // add function to worklist
                    val enclosingFunction = nameAnalysis.enclosingFunctionName(this)
                    val argumentLabelMap =
                        this.arguments.associate { argument ->
                            val parameter = nameAnalysis.parameter(argument)
                            val argumentVariable =
                                when (argument) {
                                    is ExpressionArgumentNode -> {
                                        argument.expression.check(solver, parameterMap, pcLabel)
                                        argument.expression.labelVariable
                                    }

                                    is ObjectReferenceArgumentNode ->
                                        nameAnalysis.declaration(argument).variableLabel()

                                    is ObjectDeclarationArgumentNode ->
                                        nameAnalysis.asObjectDeclaration(argument).variableLabel()

                                    is OutParameterArgumentNode ->
                                        nameAnalysis.declaration(argument).variableLabel()
                                }

                            Pair(parameter.name.value, argumentVariable)
                        }

                    val parameterVariables =
                        // no complex expressions with label parameters
                        this.arguments.associate { argument ->
                            val parameter = nameAnalysis.parameter(argument)
                            val parameterVariable =
                                solver.addNewVariable(nameGenerator.getFreshName(parameter.name.value.name))
                            val argumentLabel = argumentLabelMap.getValue(parameter.name.value)

                            assertEqualsTo(
                                solver,
                                argument,
                                parameterVariable,
                                argumentLabel
                            )

                            if (parameter.labelArguments != null) {
                                val labelBoundExpr = parameter.labelArguments[0].value
                                val labelBound =
                                    when {
                                        labelBoundExpr is LabelParameter ->
                                            argumentLabelMap.getValue(ObjectVariable(labelBoundExpr.name))

                                        !labelBoundExpr.containsParameters() ->
                                            LabelConstant(labelBoundExpr.interpret())

                                        // no complex expressions with label parameters
                                        else -> throw Error("no complex label expressions with parameters in function signatures")
                                    }

                                if (argument is ObjectDeclarationArgumentNode) {
                                    assertEqualsTo(
                                        solver,
                                        argument,
                                        argumentLabel,
                                        labelBound
                                    )
                                } else {
                                    assertFlowsTo(
                                        solver,
                                        argument,
                                        argumentLabel,
                                        labelBound
                                    )
                                }
                            }

                            Pair(parameter.name.value, parameterVariable)
                        }

                    val functionDecl = nameAnalysis.declaration(this)
                    val functionPc =
                        solver.addNewVariable(nameGenerator.getFreshName("${this.name.value}.pc"))

                    assertEqualsTo(solver, this, functionPc, pcLabel)

                    if (functionDecl.pcLabel != null) {
                        val labelBound = functionDecl.pcLabel.value

                        when {
                            labelBound is LabelParameter -> {
                                assertFlowsTo(
                                    solver,
                                    this,
                                    pcLabel,
                                    argumentLabelMap.getValue(ObjectVariable(labelBound.name))
                                )
                            }

                            !labelBound.containsParameters() -> {
                                assertFlowsTo(
                                    solver,
                                    this,
                                    pcLabel,
                                    LabelConstant(labelBound.interpret())
                                )
                            }

                            // no complex expressions with label parameters
                            else -> throw Error("no complex label expressions with parameters in function signatures")
                        }
                    }

                    pcVariableMap[functionDecl.pathName] = Pair(enclosingFunction, functionPc)
                    functionPcVariableMap[this.name.value] = Pair(enclosingFunction, functionPc)
                    parameterVariableMap[this.name.value] = Pair(enclosingFunction, parameterVariables)
                    worklist.add(nameAnalysis.declaration(this))
                }
            }

            is OutputNode -> {
                this.message.check(solver, parameterMap, pcLabel)

                val hostLabel =
                    LabelConstant(nameAnalysis.declaration(this).authority.value.interpret())
                pcFlowsTo(solver, pcLabel, this.host, hostLabel)
                flowsTo(solver, this.message, hostLabel)
            }

            is IfNode -> {
                this.guard.check(solver, parameterMap, pcLabel)
                val thenPc = createPCVariable(solver, this.thenBranch)
                pcFlowsTo(solver, pcLabel, this, thenPc)
                flowsTo(solver, this.guard, thenPc)
                this.thenBranch.check(solver, parameterMap, thenPc)

                val elsePc = createPCVariable(solver, this.elseBranch)
                pcFlowsTo(solver, pcLabel, this, elsePc)
                flowsTo(solver, this.guard, elsePc)
                this.elseBranch.check(solver, parameterMap, elsePc)
            }

            is InfiniteLoopNode -> {
                val loopPc = createPCVariable(solver, this)
                pcFlowsTo(solver, pcLabel, this, loopPc)
                this.body.check(solver, parameterMap, loopPc)
            }

            is BreakNode -> {
                val loopPath = nameAnalysis.correspondingLoop(this).pathName
                val loopPc = pcVariableMap.getValue(loopPath).second
                pcFlowsTo(solver, pcLabel, this, loopPc)
            }

            is AssertionNode -> {
                // Everybody must execute assertions, so [condition] must be public and trusted.
                // TODO: can we do any better? This seems almost impossible to achieve...
                flowsTo(solver, this.condition, LabelConstant(Label.bottom))
            }

            is BlockNode -> {
                for (child in statements) {
                    child.check(solver, parameterMap, pcLabel)
                }
            }
        }
    }

    /**
     * Asserts that the program does not violate information flow security, and throws (a subclass
     * of) [InformationFlowError] otherwise.
     */
    fun check() {
        for (function in tree.root.functions) {
            constraintSolverMap[function.name.value] = ConstraintSolver()
        }

        val mainFunction = tree.root.main.name.value
        val mainSolver = ConstraintSolver<InformationFlowError>()
        constraintSolverMap[mainFunction] = mainSolver

        var numLabelVariables = 0

        val mainPc = mainSolver.addNewVariable(nameGenerator.getFreshName(tree.root.main.body.pathName))
        pcVariableMap[tree.root.main.body.pathName] = Pair(mainFunction, mainPc)
        tree.root.main.body.check(mainSolver, persistentMapOf(), mainPc)
        solutionMap[mainFunction] = mainSolver.solve()
        numLabelVariables += mainSolver.variableCount()

        while (worklist.isNotEmpty()) {
            val currentFunction = worklist.remove()

            // initialize constraint solver
            val solver = constraintSolverMap.getValue(currentFunction.name.value)

            // get formal parameter labels
            val (solFunction, solVariableMap) =
                parameterVariableMap.getValue(currentFunction.name.value)
            val solution = solutionMap.getValue(solFunction)
            val parameterMap =
                solVariableMap
                    .map { kv -> Pair(kv.key.name, solution.getValue(kv.value)) }
                    .toMap()

            // get PC label
            val (pcSolFunction, pcSolVariable) =
                functionPcVariableMap.getValue(currentFunction.name.value)
            val pcSolution = solutionMap.getValue(pcSolFunction).getValue(pcSolVariable)

            // add constraints for function body
            currentFunction.body.check(solver, parameterMap, LabelConstant(pcSolution))

            // get solution
            solutionMap[currentFunction.name.value] = solver.solve()
            numLabelVariables += solver.variableCount()
        }

        logger.info { "number of label variables: $numLabelVariables" }
    }

    /** Outputs a DOT representation of the program's constraint graph to [output]. */
    fun exportConstraintGraph(output: Writer) {
        constraintSolverMap.getValue(nameAnalysis.enclosingFunctionName(tree.root.main.body)).exportDotGraph(output)
    }

    companion object : AnalysisProvider<InformationFlowAnalysis> {
        private fun construct(program: ProgramNode) = InformationFlowAnalysis(program.tree, NameAnalysis.get(program))

        override fun get(program: ProgramNode): InformationFlowAnalysis = program.cached(::construct)
    }
}

/**
 * A wrapper around AST nodes whose only job is to pretty print the node when [toString] is called.
 * Instances of this class are used as [LabelVariable] labels in the [ConstraintSolver] so we get
 * more readable debug output.
 */
private class PrettyNodeWrapper(private val node: PrettyPrintable) {
    // TODO: colors?
    override fun toString(): String = node.asDocument().print()
}
