package edu.cornell.cs.apl.viaduct.selection

import com.microsoft.z3.Context
import com.microsoft.z3.IntExpr
import com.microsoft.z3.IntNum
import com.microsoft.z3.Status
import com.uchuhimo.collections.BiMap
import com.uchuhimo.collections.toBiMap
import edu.cornell.cs.apl.viaduct.analysis.NameAnalysis
import edu.cornell.cs.apl.viaduct.analysis.declarationNodes
import edu.cornell.cs.apl.viaduct.analysis.ifNodes
import edu.cornell.cs.apl.viaduct.analysis.letNodes
import edu.cornell.cs.apl.viaduct.analysis.objectDeclarationArgumentNodes
import edu.cornell.cs.apl.viaduct.analysis.outputNodes
import edu.cornell.cs.apl.viaduct.analysis.updateNodes
import edu.cornell.cs.apl.viaduct.errors.NoHostDeclarationsError
import edu.cornell.cs.apl.viaduct.errors.NoProtocolIndexMapping
import edu.cornell.cs.apl.viaduct.errors.NoSelectionSolutionError
import edu.cornell.cs.apl.viaduct.errors.NoVariableSelectionSolutionError
import edu.cornell.cs.apl.viaduct.syntax.FunctionName
import edu.cornell.cs.apl.viaduct.syntax.HostTrustConfiguration
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.Variable
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.IfNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.Node
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ObjectDeclarationArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ParameterNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProcessDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.UpdateNode
import edu.cornell.cs.apl.viaduct.util.unions

/**
 * This class performs splitting by using Z3. It operates as follows:
 * - First, it collects constraints on protocol selection from the [ProtocolFactory]. For each let or declaration,
 *      the factory outputs two things: first, it outputs a set of viable protocols for that variable. Second,
 *      it can output a number of custom constraints on selection for that variable which are forwarded to Z3.
 *      (For the simple factory, the custom constraints are trivial, as we have not yet constrained which protocols
 *      can talk to who.)
 * - Second, it exports these constraints to Z3. The selection problem is encoded as follows:
 *      - We assign each possible viable protocol a unique integer index. Call this index i(p).
 *      - For each variable, we create a fresh integer constant. Call this constant c(v).
 *      - For each variable v with viable protocols P, we constrain that c(v) is contained in the image set of P under i.
 *      - For each variable v, we constrain c(v) relative to the custom constraints output by the factory.
 * - Third, we ask Z3 to optimize relative to a cost metric. For now, the cost metric is the sum of costs of all protocols
 *       selected. This is particularly naive, as it regards queries/declassifies as having a cost, even though it is
 *       likely free in all backends.
 * - Finally, we ask Z3 for a model, which we may convert into a function of type Variable -> Protocol.
 *
 */

private class Z3Selection(
    private val program: ProgramNode,
    private val main: ProcessDeclarationNode,
    private val protocolFactory: ProtocolFactory,
    private val costEstimator: CostEstimator<IntegerCost>,
    private val ctx: Context
) {
    private val nameAnalysis = NameAnalysis.get(program)

    private val constraintGenerator = ConstraintGenerator(program, protocolFactory, costEstimator, ctx)
    private val hostTrustConfiguration = HostTrustConfiguration(program)

    init {
        if (this.hostTrustConfiguration.isEmpty()) {
            throw NoHostDeclarationsError(program.sourceLocation.sourcePath)
        }
    }

    private fun Node.selectionConstraints(): Set<SelectionConstraint> =
        constraintGenerator.selectionConstraints(this)
            .union(this.children.map { it.selectionConstraints() }.unions())

    /** Generate both selection and cost constraints. */
    private fun Node.constraints(): Set<SelectionConstraint> =
        constraintGenerator.selectionConstraints(this)
            .union(constraintGenerator.costConstraints(this)
                .union(this.children.map { it.constraints() }.unions()))

    /** Protocol selection. */
    fun select(): (FunctionName, Variable) -> Protocol {
        val solver = ctx.mkOptimize()
        val constraints = mutableSetOf<SelectionConstraint>()

        val reachableFunctionNames = nameAnalysis.reachableFunctions(main)
        val reachableFunctions = program.functions.filter { f -> reachableFunctionNames.contains(f.name.value) }

        for (function in reachableFunctions) {
            // select for function parameters first
            for (parameter in function.parameters) {
                constraints.add(
                    VariableIn(
                        Pair(function.name.value, parameter.name.value),
                        constraintGenerator.viableProtocols(parameter)
                    )
                )
                constraints.add(protocolFactory.constraint(parameter))
            }

            // then select for function bodies
            constraints.addAll(function.body.selectionConstraints())
        }

        constraints.addAll(main.constraints())

        // build variable and protocol maps
        val letNodes: Map<LetNode, IntExpr> =
            reachableFunctions
                .flatMap { f -> f.letNodes() }
                .plus(main.letNodes())
                .map { letNode: LetNode ->
                    letNode to (ctx.mkFreshConst("t", ctx.intSort) as IntExpr)
                }.toMap()

        val declarationNodes: Map<DeclarationNode, IntExpr> =
            reachableFunctions
                .flatMap { f -> f.declarationNodes() }
                .plus(main.declarationNodes())
                .map { decl: DeclarationNode ->
                    decl to (ctx.mkFreshConst("t", ctx.intSort) as IntExpr)
                }.toMap()

        val objectDeclarationArgumentNodes: Map<ObjectDeclarationArgumentNode, IntExpr> =
            reachableFunctions
                .flatMap { f -> f.objectDeclarationArgumentNodes() }
                .plus(main.objectDeclarationArgumentNodes())
                .map { decl ->
                    decl to (ctx.mkFreshConst("t", ctx.intSort) as IntExpr)
                }.toMap()

        val parameterNodes: Map<ParameterNode, IntExpr> =
            reachableFunctions.flatMap { function ->
                function.parameters.map { parameter ->
                    parameter to (ctx.mkFreshConst("t", ctx.intSort) as IntExpr)
                }
            }.toMap()

        val updateNodes: List<UpdateNode> =
            reachableFunctions.flatMap { f -> f.updateNodes() }.plus(main.updateNodes())

        val outputNodes: List<OutputNode> =
            reachableFunctions.flatMap { f -> f.outputNodes() }.plus(main.outputNodes())

        val ifNodes: List<IfNode> =
            reachableFunctions.flatMap { f -> f.ifNodes() }.plus(main.ifNodes())

        val pmap: BiMap<Protocol, Int> =
            protocolFactory.protocols().map { it.protocol }.toSet().withIndex().map {
                it.value to it.index
            }.toMap().toBiMap()

        val varMap: BiMap<FunctionVariable, IntExpr> =
            (letNodes.mapKeys {
                Pair(nameAnalysis.enclosingFunctionName(it.key), it.key.temporary.value)
            }.plus(
                declarationNodes.mapKeys {
                    Pair(nameAnalysis.enclosingFunctionName(it.key), it.key.name.value)
                }
            ).plus(
                parameterNodes.mapKeys {
                    val functionName = nameAnalysis.functionDeclaration(it.key).name.value
                    Pair(functionName, it.key.name.value)
                }
            ).plus(
                objectDeclarationArgumentNodes.mapKeys {
                    Pair(nameAnalysis.enclosingFunctionName(it.key), it.key.name.value)
                }
            )).toBiMap()

        // tally all costs associated with storage (declaration nodes) and computations (let nodes and update nodes)
        val zeroSymbolicCost = costEstimator.zeroCost().map { CostLiteral(0) }
        val programCost =
            letNodes.keys.fold(zeroSymbolicCost) { acc, letNode -> acc.concat(constraintGenerator.symbolicCost(letNode)) }
                .concat(
                    declarationNodes.keys.fold(zeroSymbolicCost) { acc, decl -> acc.concat(constraintGenerator.symbolicCost(decl)) }
                )
                .concat(
                    parameterNodes.keys.fold(zeroSymbolicCost) { acc, param -> acc.concat(constraintGenerator.symbolicCost(param)) }
                )
                .concat(
                    updateNodes.fold(zeroSymbolicCost) { acc, update -> acc.concat(constraintGenerator.symbolicCost(update)) }
                )
                .concat(
                    outputNodes.fold(zeroSymbolicCost) { acc, output -> acc.concat(constraintGenerator.symbolicCost(output)) }
                )
                .concat(
                    ifNodes.fold(zeroSymbolicCost) { acc, ifNode -> acc.concat(constraintGenerator.symbolicCost(ifNode)) }
                )

        val pmapExpr = pmap.mapValues { ctx.mkInt(it.value) as IntExpr }.toBiMap()

        // load selection constraints into Z3
        for (constraint in constraints) {
            solver.Add(constraint.boolExpr(ctx, varMap, pmapExpr))
        }

        if (varMap.values.isNotEmpty()) {
            // load cost constraints into Z3; build integer expression to minimize
            val weights = costEstimator.featureWeights()
            val cost =
                programCost.features.entries
                    .fold(CostLiteral(0) as SymbolicCost) { acc, c ->
                        CostAdd(acc, CostMul(CostLiteral(weights[c.key]!!.cost), c.value))
                    }
                    .arithExpr(ctx, varMap, pmapExpr)

            solver.MkMinimize(cost)

            if (solver.Check() == Status.SATISFIABLE) {
                val model = solver.model

                val interpMap: Map<FunctionVariable, Int> =
                    varMap.mapValues { e ->
                        (model.getConstInterp(e.value) as IntNum).int
                    }

                fun eval(f: FunctionName, v: Variable): Protocol {
                    val fvar = Pair(f, v)
                    return interpMap[fvar]?.let { protocolIndex ->
                        pmap.inverse[protocolIndex] ?: throw NoProtocolIndexMapping(protocolIndex)
                    } ?: throw NoVariableSelectionSolutionError(f, v)
                }
                return ::eval
            } else {
                throw NoSelectionSolutionError()
            }
        } else {
            return { f: FunctionName, v: Variable ->
                throw NoVariableSelectionSolutionError(f, v)
            }
        }
    }
}

fun selectProtocolsWithZ3(
    program: ProgramNode,
    main: ProcessDeclarationNode,
    protocolFactory: ProtocolFactory,
    costEstimator: CostEstimator<IntegerCost>
): (FunctionName, Variable) -> Protocol {
    val ctx = Context()
    val ret =
        Z3Selection(program, main, protocolFactory, costEstimator, ctx).select()
    ctx.close()
    return ret
}
