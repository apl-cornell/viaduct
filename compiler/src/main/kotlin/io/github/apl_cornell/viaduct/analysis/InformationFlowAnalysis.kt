package io.github.apl_cornell.viaduct.analysis

import io.github.apl_cornell.viaduct.algebra.FreeDistributiveLattice
import io.github.apl_cornell.viaduct.algebra.FreeDistributiveLatticeCongruence
import io.github.apl_cornell.viaduct.attributes.Tree
import io.github.apl_cornell.viaduct.attributes.attribute
import io.github.apl_cornell.viaduct.errors.InformationFlowError
import io.github.apl_cornell.viaduct.errors.InsecureDataFlowError
import io.github.apl_cornell.viaduct.security.Component
import io.github.apl_cornell.viaduct.security.ConfidentialityComponent
import io.github.apl_cornell.viaduct.security.IntegrityComponent
import io.github.apl_cornell.viaduct.security.Label
import io.github.apl_cornell.viaduct.security.LabelAnd
import io.github.apl_cornell.viaduct.security.LabelBottom
import io.github.apl_cornell.viaduct.security.LabelComponent
import io.github.apl_cornell.viaduct.security.LabelConfidentiality
import io.github.apl_cornell.viaduct.security.LabelExpression
import io.github.apl_cornell.viaduct.security.LabelIntegrity
import io.github.apl_cornell.viaduct.security.LabelJoin
import io.github.apl_cornell.viaduct.security.LabelLiteral
import io.github.apl_cornell.viaduct.security.LabelMeet
import io.github.apl_cornell.viaduct.security.LabelOr
import io.github.apl_cornell.viaduct.security.LabelParameter
import io.github.apl_cornell.viaduct.security.LabelTop
import io.github.apl_cornell.viaduct.security.PolymorphicPrincipal
import io.github.apl_cornell.viaduct.security.Principal
import io.github.apl_cornell.viaduct.security.SecurityLattice
import io.github.apl_cornell.viaduct.security.solver2.Constraint
import io.github.apl_cornell.viaduct.security.solver2.ConstraintSolution
import io.github.apl_cornell.viaduct.security.solver2.ConstraintSystem
import io.github.apl_cornell.viaduct.security.solver2.Term
import io.github.apl_cornell.viaduct.security.solver2.confidentialityFlowsTo
import io.github.apl_cornell.viaduct.security.solver2.flowsTo
import io.github.apl_cornell.viaduct.security.solver2.integrityFlowsTo
import io.github.apl_cornell.viaduct.security.solver2.term
import io.github.apl_cornell.viaduct.syntax.DelegationKind
import io.github.apl_cornell.viaduct.syntax.HasSourceLocation
import io.github.apl_cornell.viaduct.syntax.HostTrustConfiguration
import io.github.apl_cornell.viaduct.syntax.Variable
import io.github.apl_cornell.viaduct.syntax.intermediate.AssertionNode
import io.github.apl_cornell.viaduct.syntax.intermediate.BlockNode
import io.github.apl_cornell.viaduct.syntax.intermediate.BreakNode
import io.github.apl_cornell.viaduct.syntax.intermediate.DeclarationNode
import io.github.apl_cornell.viaduct.syntax.intermediate.DeclassificationNode
import io.github.apl_cornell.viaduct.syntax.intermediate.DowngradeNode
import io.github.apl_cornell.viaduct.syntax.intermediate.EndorsementNode
import io.github.apl_cornell.viaduct.syntax.intermediate.ExpressionArgumentNode
import io.github.apl_cornell.viaduct.syntax.intermediate.ExpressionNode
import io.github.apl_cornell.viaduct.syntax.intermediate.FunctionArgumentNode
import io.github.apl_cornell.viaduct.syntax.intermediate.FunctionCallNode
import io.github.apl_cornell.viaduct.syntax.intermediate.FunctionDeclarationNode
import io.github.apl_cornell.viaduct.syntax.intermediate.IfNode
import io.github.apl_cornell.viaduct.syntax.intermediate.InfiniteLoopNode
import io.github.apl_cornell.viaduct.syntax.intermediate.InputNode
import io.github.apl_cornell.viaduct.syntax.intermediate.LetNode
import io.github.apl_cornell.viaduct.syntax.intermediate.LiteralNode
import io.github.apl_cornell.viaduct.syntax.intermediate.Node
import io.github.apl_cornell.viaduct.syntax.intermediate.ObjectDeclarationArgumentNode
import io.github.apl_cornell.viaduct.syntax.intermediate.ObjectReferenceArgumentNode
import io.github.apl_cornell.viaduct.syntax.intermediate.OperatorApplicationNode
import io.github.apl_cornell.viaduct.syntax.intermediate.OutParameterArgumentNode
import io.github.apl_cornell.viaduct.syntax.intermediate.OutParameterConstructorInitializerNode
import io.github.apl_cornell.viaduct.syntax.intermediate.OutParameterExpressionInitializerNode
import io.github.apl_cornell.viaduct.syntax.intermediate.OutParameterInitializationNode
import io.github.apl_cornell.viaduct.syntax.intermediate.OutputNode
import io.github.apl_cornell.viaduct.syntax.intermediate.ParameterNode
import io.github.apl_cornell.viaduct.syntax.intermediate.ProgramNode
import io.github.apl_cornell.viaduct.syntax.intermediate.QueryNode
import io.github.apl_cornell.viaduct.syntax.intermediate.ReadNode
import io.github.apl_cornell.viaduct.syntax.intermediate.StatementNode
import io.github.apl_cornell.viaduct.syntax.intermediate.UpdateNode
import io.github.apl_cornell.viaduct.syntax.intermediate.VariableDeclarationNode
import java.io.Writer
import io.github.apl_cornell.viaduct.algebra.solver2.Term as AlgebraTerm
import io.github.apl_cornell.viaduct.syntax.LabelVariable as LabelVariableName

private typealias PrincipalComponent = Component<Principal>
private typealias LabelConstant = FreeDistributiveLattice<PrincipalComponent>
private typealias LabelTerm = Term<LabelConstant, LabelVariable>
private typealias LabelConstraint = Constraint<LabelConstant, LabelVariable, InformationFlowError>
private typealias LabelConstraintSystem = ConstraintSystem<LabelConstant, LabelVariable, InformationFlowError>
private typealias Solution = ConstraintSolution<LabelConstant, LabelVariable>
private typealias DelegationContext = FreeDistributiveLatticeCongruence<Component<Principal>>

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

        /** Label of a literal node. */
        data class Literal(val node: LiteralNode) : Data() {

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

        data class PolymorphicVariable(val variable: LabelVariableName, val node: FunctionCallNode) : Data() {
            override fun toString(): String = variable.toDocument().print()
        }
    }
}

/** Associates [Variable]s with their [Label]s. */
class InformationFlowAnalysis private constructor(
    private val tree: Tree<Node, ProgramNode>,
    private val nameAnalysis: NameAnalysis
) {
    private val FunctionDeclarationNode.constraintSystem: LabelConstraintSystem by attribute {
        constraints()
    }

    private val FunctionDeclarationNode.solution: Solution by attribute {
        constraintSystem.solution()
    }

    val trustConfiguration: HostTrustConfiguration = HostTrustConfiguration(tree.root)

    // private val solution by lazy { constraintSystem.solution() }

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
        get() =
            when (this) {
                is FunctionDeclarationNode ->
                    if (this.pcLabel != null) {
                        term(this.pcLabel.value.interpret())
                    } else {
                        term(LabelVariable.PC(pathName))
                    }

                else -> term(LabelVariable.PC(pathName))
            }

    /** The [LabelTerm] that represents the label of the [Variable] declared by this node. */
    // TODO: incorporate label annotations
    private val VariableDeclarationNode.labelTerm: LabelTerm
        get() =
            when (this) {
                is ParameterNode -> {
                    term((objectType.labelArguments!!.first().value.interpret()))
                }

                is LetNode -> {
                    term(LabelVariable.Data.Declaration(this))
                }

                is DeclarationNode -> {
                    if (objectType.labelArguments == null) {
                        term(LabelVariable.Data.Declaration(this))
                    } else {
                        term((objectType.labelArguments.first().value.interpret()))
                    }
                }

                is ObjectDeclarationArgumentNode -> {
                    term(LabelVariable.Data.Declaration(this))
                }
            }

    /** Returns constraints asserting that [this] node with the given label can flow to a location with label [to]. */
    private infix fun Pair<HasSourceLocation, LabelTerm>.flowsTo(toLabel: LabelTerm): Sequence<LabelConstraint> {
        val constraints: Iterable<LabelConstraint> =
            second.flowsTo(toLabel, LabelConstant.bounds()) { to, from ->
                InsecureDataFlowError(this.first, from, to, (first as Node).delegationContext)
            }
        return constraints.asSequence()
    }

    private infix fun Pair<HasSourceLocation, LabelTerm>.confidentialityFlowsTo(toLabel: LabelTerm): Sequence<LabelConstraint> {
        val constraints: Iterable<LabelConstraint> =
            second.confidentialityFlowsTo(toLabel, LabelConstant.bounds()) { to, from ->
                InsecureDataFlowError(this.first, from, to, (first as Node).delegationContext)
            }
        return constraints.asSequence()
    }

    private infix fun Pair<HasSourceLocation, LabelTerm>.integrityFlowsTo(toLabel: LabelTerm): Sequence<LabelConstraint> {
        val constraints: Iterable<LabelConstraint> =
            second.integrityFlowsTo(toLabel, LabelConstant.bounds()) { to, from ->
                InsecureDataFlowError(this.first, from, to, (first as Node).delegationContext)
            }
        return constraints.asSequence()
    }

    /** Returns constraints asserting that the pc at [this] node flows to node with label [nodeLabel]. */
    private fun Node.pcFlowsTo(
        nodeLabel: LabelTerm
    ): Sequence<LabelConstraint> {
        val constraints: Iterable<LabelConstraint> =
            pcTerm.flowsTo(nodeLabel, LabelConstant.bounds()) { to, from ->
                InsecureDataFlowError(this, from, to, delegationContext)
            }
        return constraints.asSequence()
    }

    private fun LabelExpression.interpretAsVariable(node: FunctionCallNode): LabelTerm =
        when (this) {
            is LabelParameter -> {
                term(LabelVariable.Data.PolymorphicVariable(this.name, node))
            }

            is LabelLiteral -> {
                term(interpret())
            }

            is LabelConfidentiality -> {
                value.interpretAsVariable(node).confidentiality(
                    AlgebraTerm.Bounds(FreeDistributiveLattice.bounds())
                )
            }

            is LabelIntegrity -> {
                value.interpretAsVariable(node).integrity(
                    AlgebraTerm.Bounds(FreeDistributiveLattice.bounds())
                )
            }

            is LabelJoin -> {
                lhs.interpretAsVariable(node).join(rhs.interpretAsVariable(node))
            }

            is LabelMeet -> {
                lhs.interpretAsVariable(node).meet(rhs.interpretAsVariable(node))
            }

            is LabelOr -> {
                lhs.interpretAsVariable(node).or(rhs.interpretAsVariable(node))
            }

            is LabelAnd -> {
                lhs.interpretAsVariable(node).and(rhs.interpretAsVariable(node))
            }

            is LabelBottom -> {
                term(
                    SecurityLattice
                        .Bounds<LabelComponent>(FreeDistributiveLattice.bounds())
                        .strongest
                )
            }

            is LabelTop -> {
                term(
                    SecurityLattice
                        .Bounds<LabelComponent>(FreeDistributiveLattice.bounds())
                        .weakest
                )
            }
        }

    /** Returns constraints that need to hold for [this] expression to flow to [outputLabel]. */
    private infix fun ExpressionNode.flowsTo(outputLabel: LabelTerm): Sequence<LabelConstraint> =

        when (this) {
            is LiteralNode -> {
                val literalVariable: Pair<HasSourceLocation, LabelTerm> =
                    (this to term(LabelVariable.Data.Literal(this)))
                literalVariable flowsTo outputLabel
            }

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
                    this@flowsTo.pcFlowsTo(declarationLabel)

                    // TODO: return label should be based on the query.
                    //  For example, Array.length leaks the label on the size but not the data.
                    yieldAll((this@flowsTo to declarationLabel) flowsTo outputLabel)
                }
            }

            is DowngradeNode -> {
                val from: LabelTerm =
                    if (fromLabel == null)
                        term(LabelVariable.Data.DeclassificationFrom(this as DeclassificationNode))
                    else
                        term(fromLabel!!.value.interpret())

                val to: LabelTerm =
                    if (toLabel == null)
                        term(LabelVariable.Data.EndorsementTo(this as EndorsementNode))
                    else
                        term(toLabel!!.value.interpret())

                sequence {
                    yieldAll(this@flowsTo.pcFlowsTo(to))
                    yieldAll((expression to from) flowsTo from.swap())
                    yieldAll((expression to to) flowsTo to.swap())
                    yieldAll((this@flowsTo to to) flowsTo outputLabel)
                    // TODO: expression should flows to from label. it is causing rewrite map bug currently
                    when (this@flowsTo) {
                        is DeclassificationNode ->
                            yieldAll((expression to from) integrityFlowsTo to)

                        is EndorsementNode ->
                            yieldAll((expression to from) confidentialityFlowsTo to)
                    }
                }
            }

            is InputNode -> {
                val hostLabel: LabelTerm = term(LabelLiteral(host.value).interpret())

                // Host learns the current pc
                sequence {
                    yieldAll(pcFlowsTo(hostLabel))
                    yieldAll((this@flowsTo to hostLabel) flowsTo outputLabel)
                }
            }
        }

    /**
     * Generate information flow constraints for a statement.
     */
    private fun StatementNode.constraints(): Sequence<LabelConstraint> =
        when (this) {
            is LetNode -> {
                sequence {
                    // expression flows to l
                    yieldAll(value.flowsTo(labelTerm))
                    // pc flows to l
                    yieldAll(pcFlowsTo(labelTerm))
                }
            }

            is DeclarationNode -> {
                sequence {
                    // arguments flow to l
                    yieldAll(
                        arguments.flatMap {
                            it flowsTo labelTerm
                        }
                    )
                    // pc flows to l
                    yieldAll(pcFlowsTo(labelTerm))
                }
            }

            is UpdateNode -> {
                val labelTerm = nameAnalysis.declaration(this@constraints).labelTerm
                sequence {
                    // arguments flow to the label of variable being updated
                    yieldAll(
                        arguments.flatMap {
                            it flowsTo labelTerm
                        }
                    )
                    // pc flows to label of updated variables
                    yieldAll(pcFlowsTo(labelTerm))
                }
                // TODO: consult the method signature. There may be constraints on the pc or the arguments.
            }

            is OutParameterInitializationNode -> {
                val labelTerm = nameAnalysis.declaration(this@constraints).labelTerm
                sequence {
                    // pc flows to label of the out variable
                    yieldAll(pcFlowsTo(labelTerm))
                    when (val initializer = this@constraints.initializer) {
                        is OutParameterExpressionInitializerNode -> {
                            // the label of expression flows to the label of out variable
                            yieldAll(initializer.expression flowsTo labelTerm)
                        }

                        is OutParameterConstructorInitializerNode -> {
                            // the constructor arguments flows to the out variable label
                            yieldAll(
                                initializer.arguments.flatMap {
                                    it flowsTo labelTerm
                                }
                            )
                        }
                    }
                }
            }

            is FunctionCallNode -> {
                val functionDeclaration = nameAnalysis.declaration(this)

                sequence {
                    // pc flows to function
                    // TODO: use proper exception class
                    yieldAll(pcFlowsTo(functionDeclaration.pcLabel!!.value.interpretAsVariable(this@constraints)))
                    // arguments flows to parameters (and the reverse direction for out parameters)
                    yieldAll(
                        arguments.flatMap {
                            // parameter of the callsite
                            val parameter = nameAnalysis.parameter(it)
                            // parameters are interpreted as label variables, literals are still literals
                            val parameterLabel: LabelTerm =
                                parameter.objectType.labelArguments!!.first().value
                                    .interpretAsVariable(this@constraints)

                            when (it) {
                                is ExpressionArgumentNode -> {
                                    it.expression flowsTo parameterLabel
                                }

                                is ObjectReferenceArgumentNode -> {
                                    (it to nameAnalysis.declaration(it).labelTerm) flowsTo
                                        parameterLabel
                                }

                                is ObjectDeclarationArgumentNode -> {
                                    (parameter to parameterLabel).flowsTo(it.labelTerm)
                                }

                                is OutParameterArgumentNode -> {
                                    (parameter to parameterLabel).flowsTo(nameAnalysis.declaration(it).labelTerm)
                                }
                            }
                        }
                    )
                    // parameters satisfy function IFC constraints, where polymorphic variables
                    // in the function declaration are interpreted as existential variables
                    yieldAll(
                        functionDeclaration.labelConstraints.flatMap {
                            // this has to be IFC delegations
                            assert(it.delegationKind == DelegationKind.IFC)
                            (this@constraints to it.from.value.interpretAsVariable(this@constraints)) flowsTo
                                it.to.value.interpretAsVariable(this@constraints)
                        }
                    )
                }
            }

            is OutputNode -> {
                val outputLabel: LabelTerm = term(LabelLiteral(host.value).interpret())
                sequence {
                    // pc flows to the output host
                    yieldAll(pcFlowsTo(outputLabel))
                    // the content of output flows to the output host
                    yieldAll(this@constraints.message flowsTo outputLabel)
                }
            }

            is IfNode -> {
                val thenPC = thenBranch.pcTerm
                val elsePC = elseBranch.pcTerm
                sequence {
                    // check guard label flows to then and else PC
                    yieldAll(guard flowsTo thenPC)
                    yieldAll(guard flowsTo elsePC)
                    // this pc flows to pc of branches
                    yieldAll(pcFlowsTo(thenPC))
                    yieldAll(pcFlowsTo(elsePC))
                    // check then and else with guard label as PC
                    yieldAll(thenBranch.constraints())
                    yieldAll(elseBranch.constraints())
                }
            }

            is InfiniteLoopNode -> {
                sequence {
                    // this pc flows the pc of body, which is also equal to the label of b
                    yieldAll(pcFlowsTo(body.pcTerm))
                    // check body
                    yieldAll(body.constraints())
                }
            }

            is BreakNode -> {
                val loopPC = nameAnalysis.correspondingLoop(this).body.pcTerm
                // pc flows to pc of body of the loop
                pcFlowsTo(loopPC)
            }

            is AssertionNode -> {
                // Everybody must execute assertions, so [condition] must be public and trusted.
                // TODO: can we do any better? This seems almost impossible to achieve...
                condition flowsTo
                    term(SecurityLattice.Bounds<LabelConstant>(LabelConstant.bounds()).bottom)
            }

            is BlockNode -> {
                sequence {
                    statements.forEach {
                        yieldAll(it.constraints())
                    }
                }
            }
        }

    /**
     * Return a [LabelConstraintSystem] given a function
     */
    private fun FunctionDeclarationNode.constraints(): LabelConstraintSystem {
        return ConstraintSystem(
            body.constraints().asIterable(),
            FreeDistributiveLattice.bounds(),
            this.body.delegationContext
        )
    }

    private val Node.delegationContext: DelegationContext
        get() = trustConfiguration.congruence +
            FreeDistributiveLatticeCongruence(
                nameAnalysis.enclosingFunction(this)
                    .labelConstraints.flatMap { it.congruences() }
            ) +
            FreeDistributiveLatticeCongruence(
                nameAnalysis.enclosingFunction(this).labelParameters.map {
                    Pair(
                        FreeDistributiveLattice(
                            IntegrityComponent(
                                PolymorphicPrincipal(it.value)
                            )
                        ),
                        FreeDistributiveLattice(
                            ConfidentialityComponent(
                                PolymorphicPrincipal(
                                    it.value
                                )
                            )
                        )
                    )
                }
            )

    /** Returns the inferred security label of the [Variable] defined by [node]. */
    fun label(node: VariableDeclarationNode): Label =
        nameAnalysis.enclosingFunction(node as Node).solution.evaluate(node.labelTerm)

    /** Returns the label of a label parameter of a function being called */
    fun label(functionCall: FunctionCallNode, labelParameter: LabelVariableName): Label =
        nameAnalysis.enclosingFunction(functionCall)
            .solution.evaluate(term(LabelVariable.Data.PolymorphicVariable(labelParameter, functionCall)))

    /** Returns the inferred security label of function arguments. */
    fun label(node: FunctionArgumentNode): Label {
        val solution = nameAnalysis.enclosingFunction(node as Node).solution
        return when (node) {
            is ExpressionArgumentNode -> {
                when (val expr = node.expression) {
                    is LiteralNode -> {
                        solution.evaluate(term(LabelVariable.Data.Literal(expr)))
                    }

                    is ReadNode -> {
                        nameAnalysis.enclosingFunction(expr).solution
                        solution.evaluate(nameAnalysis.declaration(expr).labelTerm)
                    }
                }
            }

            is ObjectReferenceArgumentNode -> {
                solution.evaluate(nameAnalysis.declaration(node).labelTerm)
            }

            is ObjectDeclarationArgumentNode -> {
                solution.evaluate(node.labelTerm)
            }

            is OutParameterArgumentNode -> {
                solution.evaluate(nameAnalysis.declaration(node).labelTerm)
            }
        }
    }

    /**
     * Asserts that the program does not violate information flow security.
     *
     * @throws InformationFlowError if the program has insecure information flow.
     */
    fun check() {
        // Force thunk to ensure there is a solution to the constraints.
        tree.root.functions.forEach {
            it.solution
        }
    }

    /** Outputs a DOT representation of the program's constraint graph to [output]. */
    fun exportConstraintGraph(output: Writer) {
        tree.root.functions.forEach {
            it.constraintSystem.exportDotGraph(output)
        }
    }

    companion object : AnalysisProvider<InformationFlowAnalysis> {
        private fun construct(program: ProgramNode) =
            InformationFlowAnalysis(program.tree, NameAnalysis.get(program))

        override fun get(program: ProgramNode): InformationFlowAnalysis = program.cached(::construct)
    }
}
