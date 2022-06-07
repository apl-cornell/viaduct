package io.github.apl_cornell.viaduct.passes

import edu.cornell.cs.apl.viaduct.analysis.NameAnalysis
import edu.cornell.cs.apl.viaduct.selection.ProtocolAssignment
import edu.cornell.cs.apl.viaduct.syntax.Arguments
import edu.cornell.cs.apl.viaduct.syntax.Located
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BlockNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DelegationDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.FunctionDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.HostDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.IfNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InfiniteLoopNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ParameterNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.StatementNode

/** Annotate parameters, declarations, and let nodes with protocols. */
fun ProgramNode.annotateWithProtocols(assignment: ProtocolAssignment): ProgramNode =
    ProtocolAnnotator(this, assignment).run()

/** Annotate AST with protocol assignment. */
private class ProtocolAnnotator(val program: ProgramNode, val selection: ProtocolAssignment) {
    private val nameAnalysis = NameAnalysis.get(program)

    fun run(stmt: StatementNode): StatementNode {
        return when (stmt) {
            is LetNode -> {
                val enclosingFunction = nameAnalysis.enclosingFunctionName(stmt)
                LetNode(
                    stmt.name,
                    stmt.value,
                    Located(
                        selection.getAssignment(enclosingFunction, stmt.name.value),
                        stmt.sourceLocation
                    ),
                    stmt.sourceLocation
                )
            }

            is DeclarationNode -> {
                val enclosingFunction = nameAnalysis.enclosingFunctionName(stmt)
                DeclarationNode(
                    stmt.name,
                    stmt.objectType,
                    stmt.arguments,
                    Located(
                        selection.getAssignment(enclosingFunction, stmt.name.value),
                        stmt.sourceLocation
                    ),
                    stmt.sourceLocation
                )
            }

            is BlockNode ->
                BlockNode(
                    stmt.statements.map { run(it) },
                    stmt.sourceLocation
                )

            is IfNode ->
                IfNode(
                    stmt.guard,
                    run(stmt.thenBranch) as BlockNode,
                    run(stmt.elseBranch) as BlockNode,
                    stmt.sourceLocation
                )

            is InfiniteLoopNode ->
                InfiniteLoopNode(
                    run(stmt.body) as BlockNode,
                    stmt.jumpLabel,
                    stmt.sourceLocation
                )

            else -> stmt
        }
    }

    fun run(): ProgramNode =
        ProgramNode(
            program.declarations.map { decl ->
                when (decl) {
                    is HostDeclarationNode ->
                        decl

                    is DelegationDeclarationNode ->
                        decl

                    is FunctionDeclarationNode ->
                        FunctionDeclarationNode(
                            decl.name,
                            decl.pcLabel,
                            Arguments(
                                decl.parameters.map { param ->
                                    ParameterNode(
                                        param.name,
                                        param.parameterDirection,
                                        param.objectType,
                                        Located(
                                            selection.getAssignment(decl.name.value, param.name.value),
                                            param.sourceLocation
                                        ),
                                        param.sourceLocation
                                    )
                                },
                                decl.parameters.sourceLocation
                            ),
                            run(decl.body) as BlockNode,
                            decl.sourceLocation
                        )
                }
            },
            program.sourceLocation
        )
}
