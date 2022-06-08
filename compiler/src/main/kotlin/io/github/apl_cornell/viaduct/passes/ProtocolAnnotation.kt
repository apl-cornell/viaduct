package io.github.apl_cornell.viaduct.passes

import io.github.apl_cornell.viaduct.analysis.NameAnalysis
import io.github.apl_cornell.viaduct.selection.ProtocolAssignment
import io.github.apl_cornell.viaduct.syntax.Arguments
import io.github.apl_cornell.viaduct.syntax.Located
import io.github.apl_cornell.viaduct.syntax.intermediate.BlockNode
import io.github.apl_cornell.viaduct.syntax.intermediate.DeclarationNode
import io.github.apl_cornell.viaduct.syntax.intermediate.FunctionDeclarationNode
import io.github.apl_cornell.viaduct.syntax.intermediate.HostDeclarationNode
import io.github.apl_cornell.viaduct.syntax.intermediate.IfNode
import io.github.apl_cornell.viaduct.syntax.intermediate.InfiniteLoopNode
import io.github.apl_cornell.viaduct.syntax.intermediate.LetNode
import io.github.apl_cornell.viaduct.syntax.intermediate.ParameterNode
import io.github.apl_cornell.viaduct.syntax.intermediate.ProgramNode
import io.github.apl_cornell.viaduct.syntax.intermediate.StatementNode

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