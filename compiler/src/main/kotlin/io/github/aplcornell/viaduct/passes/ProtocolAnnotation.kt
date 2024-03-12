package io.github.aplcornell.viaduct.passes

import io.github.aplcornell.viaduct.analysis.NameAnalysis
import io.github.aplcornell.viaduct.selection.ProtocolAssignment
import io.github.aplcornell.viaduct.syntax.Arguments
import io.github.aplcornell.viaduct.syntax.Located
import io.github.aplcornell.viaduct.syntax.intermediate.BlockNode
import io.github.aplcornell.viaduct.syntax.intermediate.DeclarationNode
import io.github.aplcornell.viaduct.syntax.intermediate.DelegationDeclarationNode
import io.github.aplcornell.viaduct.syntax.intermediate.FunctionDeclarationNode
import io.github.aplcornell.viaduct.syntax.intermediate.HostDeclarationNode
import io.github.aplcornell.viaduct.syntax.intermediate.IfNode
import io.github.aplcornell.viaduct.syntax.intermediate.InfiniteLoopNode
import io.github.aplcornell.viaduct.syntax.intermediate.LetNode
import io.github.aplcornell.viaduct.syntax.intermediate.ParameterNode
import io.github.aplcornell.viaduct.syntax.intermediate.ProgramNode
import io.github.aplcornell.viaduct.syntax.intermediate.StatementNode

/** Annotate parameters, declarations, and let nodes with protocols. */
fun ProgramNode.annotateWithProtocols(assignment: ProtocolAssignment): ProgramNode = ProtocolAnnotator(this, assignment).run()

/** Annotate AST with protocol assignment. */
private class ProtocolAnnotator(val program: ProgramNode, val selection: ProtocolAssignment) {
    private val nameAnalysis = program.analyses.get<NameAnalysis>()

    fun run(stmt: StatementNode): StatementNode {
        return when (stmt) {
            is LetNode -> {
                val enclosingFunction = nameAnalysis.enclosingFunctionName(stmt)
                LetNode(
                    stmt.name,
                    stmt.value,
                    Located(
                        selection.getAssignment(enclosingFunction, stmt.name.value),
                        stmt.sourceLocation,
                    ),
                    stmt.sourceLocation,
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
                        stmt.sourceLocation,
                    ),
                    stmt.sourceLocation,
                )
            }

            is BlockNode ->
                BlockNode(
                    stmt.statements.map { run(it) },
                    stmt.sourceLocation,
                )

            is IfNode ->
                IfNode(
                    stmt.guard,
                    run(stmt.thenBranch) as BlockNode,
                    run(stmt.elseBranch) as BlockNode,
                    stmt.sourceLocation,
                )

            is InfiniteLoopNode ->
                InfiniteLoopNode(
                    run(stmt.body) as BlockNode,
                    stmt.jumpLabel,
                    stmt.sourceLocation,
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
                            decl.labelParameters,
                            Arguments(
                                decl.parameters.map { param ->
                                    ParameterNode(
                                        param.name,
                                        param.parameterDirection,
                                        param.objectType,
                                        Located(
                                            selection.getAssignment(decl.name.value, param.name.value),
                                            param.sourceLocation,
                                        ),
                                        param.sourceLocation,
                                    )
                                },
                                decl.parameters.sourceLocation,
                            ),
                            decl.labelConstraints,
                            decl.pcLabel,
                            run(decl.body) as BlockNode,
                            decl.sourceLocation,
                        )
                }
            },
            program.sourceLocation,
        )
}
