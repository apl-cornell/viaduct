package edu.cornell.cs.apl.viaduct.selection

import com.microsoft.z3.Context
import com.microsoft.z3.Global
import com.microsoft.z3.IntExpr
import com.microsoft.z3.IntNum
import com.microsoft.z3.Model
import com.microsoft.z3.Status
import com.uchuhimo.collections.BiMap
import com.uchuhimo.collections.toBiMap
import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.prettyprinting.PrettyPrintable
import edu.cornell.cs.apl.viaduct.analysis.NameAnalysis
import edu.cornell.cs.apl.viaduct.analysis.declarationNodes
import edu.cornell.cs.apl.viaduct.analysis.ifNodes
import edu.cornell.cs.apl.viaduct.analysis.infiniteLoopNodes
import edu.cornell.cs.apl.viaduct.analysis.letNodes
import edu.cornell.cs.apl.viaduct.analysis.main
import edu.cornell.cs.apl.viaduct.analysis.objectDeclarationArgumentNodes
import edu.cornell.cs.apl.viaduct.analysis.outputNodes
import edu.cornell.cs.apl.viaduct.analysis.parameterNodes
import edu.cornell.cs.apl.viaduct.analysis.updateNodes
import edu.cornell.cs.apl.viaduct.errors.NoHostDeclarationsError
import edu.cornell.cs.apl.viaduct.errors.NoProtocolIndexMapping
import edu.cornell.cs.apl.viaduct.errors.NoSelectionSolutionError
import edu.cornell.cs.apl.viaduct.errors.NoVariableSelectionSolutionError
import edu.cornell.cs.apl.viaduct.syntax.FunctionName
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.Variable
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.FunctionDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.Node
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import mu.KotlinLogging

private val logger = KotlinLogging.logger("Z3Selection")

enum class CostMode { MINIMIZE, MAXIMIZE }

/**
 * This class performs splitting by using Z3. It operates as follows:
 *
 * - First, it collects constraints on protocol selection from the [ProtocolFactory]. For each let or declaration,
 *      the factory outputs two things: first, it outputs a set of viable protocols for that variable. Second,
 *      it can output a number of custom constraints on selection for that variable which are forwarded to Z3.
 *      (For the simple factory, the custom constraints are trivial, as we have not yet constrained which protocols
 *      can talk to whom.)
 * - Second, it exports these constraints to Z3. The selection problem is encoded as follows:
 *      - We assign each possible viable protocol a unique integer index. Call this index i(p).
 *      - For each variable, we create a fresh integer constant. Call this constant c(v).
 *      - For each variable v with viable protocols P, we constrain that c(v) is contained in the image set of P under i.
 *      - For each variable v, we constrain c(v) relative to the custom constraints output by the factory.
 * - Third, we ask Z3 to optimize relative to a cost metric. The cost metric is provided by [costEstimator], which
 *   will represent cost using a set of features. At a high level, the cost estimator approximates cost by:
 *      - The cost of storing data in a protocol
 *      - The cost of executing computations in a protocol
 *      - Estimating the communication cost between one protocol reading data from another protocol
 */
private class Z3Selection(
    private val ctx: Context,
    private val program: ProgramNode,
    private val main: FunctionDeclarationNode,
    private val protocolFactory: ProtocolFactory,
    protocolComposer: ProtocolComposer,
    private val costEstimator: CostEstimator<IntegerCost>,
    private val costMode: CostMode,
    private val dumpMetadata: (Map<Node, PrettyPrintable>) -> Unit
) {
    private companion object {
        init {
            // Use old arithmetic solver to fix regression introduced in Z3 v4.8.9
            Global.setParameter("smt.arith.solver", "2")
        }
    }

    private val nameAnalysis = NameAnalysis.get(program)

    private val constraintGenerator =
        SelectionConstraintGenerator(program, protocolFactory, protocolComposer, costEstimator, ctx)

    private fun Cost<SymbolicCost>.featureSum(): SymbolicCost {
        val weights = costEstimator.featureWeights()
        return this.features.entries.fold(CostLiteral(0) as SymbolicCost) { acc, c ->
            CostAdd(acc, CostMul(CostLiteral(weights[c.key]!!.cost), c.value))
        }
    }

    private val reachableFunctions = run {
        val reachableFunctionNames = nameAnalysis.reachableFunctions(main)
        program.functions.filter { reachableFunctionNames.contains(it.name.value) }
    }

    private fun <T> reachableInstances(instances: Node.() -> List<T>): List<T> =
        reachableFunctions.flatMap(instances) + main.instances()

    private fun printMetadata(
        eval: (FunctionName, Variable) -> Protocol,
        model: Model,
        totalCostSymvar: IntExpr
    ) {
        val nodeCostFunc: (Node) -> Pair<Node, PrettyPrintable> = { node ->
            val symcost = constraintGenerator.symbolicCost(node)
            val nodeCostStr =
                symcost.featureSum().evaluate(eval) { cvar ->
                    val interpValue = model.getConstInterp(cvar.variable)
                    assert(interpValue != null)
                    (interpValue as IntNum).int
                }.toString()

            val nodeProtocolStr =
                when (node) {
                    is LetNode -> {
                        val enclosingFunc = nameAnalysis.enclosingFunctionName(node)
                        "protocol: ${eval(enclosingFunc, node.temporary.value).asDocument.print()}"
                    }

                    is DeclarationNode -> {
                        val enclosingFunc = nameAnalysis.enclosingFunctionName(node)
                        "protocol: ${eval(enclosingFunc, node.name.value).asDocument.print()}"
                    }

                    else -> ""
                }

            Pair(node, Document("cost: $nodeCostStr $nodeProtocolStr"))
        }

        val declarationNodes = reachableInstances { declarationNodes() }

        val letNodes = reachableInstances { letNodes() }

        val updateNodes = reachableInstances { updateNodes() }

        val outputNodes = reachableInstances { outputNodes() }

        val ifNodes = reachableInstances { ifNodes() }

        val loopNodes = reachableInstances { infiniteLoopNodes() }

        val totalCostMetadata =
            Document("total cost: ${(model.getConstInterp(totalCostSymvar) as IntNum).int}")

        val costMetadata: Map<Node, PrettyPrintable> =
            declarationNodes.asSequence().map { nodeCostFunc(it) }
                .plus(letNodes.map { nodeCostFunc(it) })
                .plus(updateNodes.map { nodeCostFunc(it) })
                .plus(outputNodes.map { nodeCostFunc(it) })
                .plus(ifNodes.map { nodeCostFunc(it) })
                .plus(loopNodes.map { nodeCostFunc(it) })
                .plus(reachableFunctions.map { nodeCostFunc(it) })
                .plus(nodeCostFunc(main))
                .plus(Pair(program, totalCostMetadata))
                .toMap()

        dumpMetadata(costMetadata)
    }

    /** Protocol selection. */
    fun select(): (FunctionName, Variable) -> Protocol {
        val solver = ctx.mkOptimize()
        val constraints = mutableSetOf<SelectionConstraint>()

        // select for functions first
        for (function in reachableFunctions) {
            constraints.addAll(constraintGenerator.getConstraints(function))
        }

        // then select for the main process
        constraints.addAll(constraintGenerator.getConstraints(main))

        // Build variable and protocol maps
        fun <T> associateFreshConstant(instances: Node.() -> List<T>): Map<T, IntExpr> =
            reachableInstances(instances).associateWith { (ctx.mkFreshConst("t", ctx.intSort)) as IntExpr }

        val letNodes = associateFreshConstant { letNodes() }
        val declarationNodes = associateFreshConstant { declarationNodes() }
        val objectDeclarationArgumentNodes = associateFreshConstant { objectDeclarationArgumentNodes() }
        val parameterNodes = associateFreshConstant { parameterNodes() }

        val pmap: BiMap<Protocol, Int> = run {
            // Compute all protocols relevant for the program
            val protocols = mutableSetOf<Protocol>()
            reachableInstances { letNodes() }.forEach { protocols.addAll(protocolFactory.viableProtocols(it)) }
            reachableInstances { declarationNodes() }.forEach { protocols.addAll(protocolFactory.viableProtocols(it)) }
            reachableInstances { parameterNodes() }.forEach { protocols.addAll(protocolFactory.viableProtocols(it)) }
            protocols.sorted().withIndex().associate { it.value to it.index }.toBiMap()
        }

        val varMap: BiMap<FunctionVariable, IntExpr> =
            (
                letNodes.mapKeys {
                    FunctionVariable(nameAnalysis.enclosingFunctionName(it.key), it.key.temporary.value)
                }
                ).plus(
                declarationNodes.mapKeys {
                    FunctionVariable(nameAnalysis.enclosingFunctionName(it.key), it.key.name.value)
                }
            ).plus(
                parameterNodes.mapKeys {
                    val functionName = nameAnalysis.functionDeclaration(it.key).name.value
                    FunctionVariable(functionName, it.key.name.value)
                }
            ).plus(
                objectDeclarationArgumentNodes.mapKeys {
                    FunctionVariable(nameAnalysis.enclosingFunctionName(it.key), it.key.name.value)
                }
            ).toBiMap()

        val pmapExpr = pmap.mapValues { ctx.mkInt(it.value) as IntExpr }.toBiMap()

        // load selection constraints into Z3
        val costVariables = mutableSetOf<CostVariable>()
        val hostVariables = mutableSetOf<HostVariable>()
        val guardVisibilityVariables = mutableSetOf<GuardVisibilityFlag>()
        for (constraint in constraints) {
            costVariables.addAll(constraint.costVariables())
            hostVariables.addAll(constraint.hostVariables())
            guardVisibilityVariables.addAll(constraint.guardVisibilityVariables())
            solver.Add(constraint.boolExpr(ctx, varMap, pmapExpr))
        }

        // make sure all cost variables are nonnegative
        for (costVariable in costVariables) {
            solver.Add(ctx.mkGe(costVariable.variable, ctx.mkInt(0)))
        }

        if (varMap.values.isNotEmpty()) {
            // load cost constraints into Z3; build integer expression to minimize
            val programCostFeatures: Cost<SymbolicCost> =
                reachableFunctions.fold(constraintGenerator.symbolicCost(main.body)) { acc, f ->
                    acc.concat(
                        constraintGenerator.symbolicCost(f.body)
                    )
                }

            val totalCost = programCostFeatures.featureSum()
            val costExpr = totalCost.arithExpr(ctx, varMap, pmapExpr)
            val totalCostSymvar = ctx.mkFreshConst("total_cost", ctx.intSort) as IntExpr

            solver.Add(ctx.mkEq(totalCostSymvar, costExpr))

            when (costMode) {
                CostMode.MINIMIZE -> solver.MkMinimize(totalCostSymvar)
                CostMode.MAXIMIZE -> solver.MkMaximize(totalCostSymvar)
            }

            val symvarCount = varMap.size + costVariables.size + hostVariables.size + guardVisibilityVariables.size

            logger.info { "number of symvars: $symvarCount" }
            logger.info { "cost mode set to $costMode" }

            if (solver.Check() == Status.SATISFIABLE) {
                val model = solver.model
                val interpMap: Map<FunctionVariable, Int> =
                    varMap.mapValues { e ->
                        (model.getConstInterp(e.value) as IntNum).int
                    }

                fun eval(f: FunctionName, v: Variable): Protocol {
                    val fvar = FunctionVariable(f, v)
                    return interpMap[fvar]?.let { protocolIndex ->
                        pmap.inverse[protocolIndex] ?: throw NoProtocolIndexMapping(protocolIndex)
                    } ?: throw NoVariableSelectionSolutionError(f, v)
                }

                printMetadata(::eval, model, totalCostSymvar)

                logger.info { "constraints satisfiable, extracted model" }

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
    protocolFactory: ProtocolFactory,
    protocolComposer: ProtocolComposer,
    costEstimator: CostEstimator<IntegerCost>,
    costMode: CostMode = CostMode.MINIMIZE,
    dumpMetadata: (Map<Node, PrettyPrintable>) -> Unit = {}
): (FunctionName, Variable) -> Protocol {
    if (program.hosts.isEmpty()) {
        throw NoHostDeclarationsError(program.sourceLocation.sourcePath)
    }

    return Context().use { context ->
        Z3Selection(
            context,
            program, program.main,
            protocolFactory, protocolComposer, costEstimator, costMode,
            dumpMetadata
        ).select()
    }
}
