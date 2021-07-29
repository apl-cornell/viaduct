package edu.cornell.cs.apl.viaduct.codegeneration

import com.squareup.kotlinpoet.CodeBlock
import edu.cornell.cs.apl.viaduct.errors.IllegalInternalCommunicationError
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AtomicExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutParameterInitializationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.SendNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.SimpleStatementNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.UpdateNode

abstract class AbstractCodeGenerator(
    val program: ProgramNode
) : CodeGenerator {

    override fun genSimpleStatement(protocol: Protocol, stmt: SimpleStatementNode): CodeBlock {
        when (stmt) {
            is LetNode -> genLet(protocol, stmt)

            is DeclarationNode -> genDeclaration(protocol, stmt)

            is UpdateNode -> genUpdate(protocol, stmt)

            is OutParameterInitializationNode -> genOutParameter(protocol, stmt)

            is OutputNode -> genOutput(protocol, stmt)

            is SendNode -> throw IllegalInternalCommunicationError(stmt)
        }

        throw IllegalArgumentException("unknown statement type")
    }

    abstract fun genLet(protocol: Protocol, stmt: LetNode): CodeBlock

    abstract fun genDeclaration(protocol: Protocol, stmt: DeclarationNode): CodeBlock

    abstract fun genUpdate(protocol: Protocol, stmt: UpdateNode): CodeBlock

    abstract fun genOutParameter(protocol: Protocol, stmt: OutParameterInitializationNode): CodeBlock

    abstract fun genOutput(protocol: Protocol, stmt: OutputNode): CodeBlock
}

abstract class SingleProtocolCodeGenerator(
    program: ProgramNode,
    private val protocol: Protocol
) : AbstractCodeGenerator(program) {
    override val availableProtocols: Set<Protocol> =
        setOf(protocol)

    abstract fun genGuard(expr: AtomicExpressionNode): CodeBlock

    override fun genGuard(protocol: Protocol, expr: AtomicExpressionNode): CodeBlock =
        genGuard(expr)

    abstract fun genLet(stmt: LetNode): CodeBlock

    override fun genLet(protocol: Protocol, stmt: LetNode): CodeBlock =
        genLet(stmt)

    abstract fun genUpdate(stmt: UpdateNode): CodeBlock

    override fun genUpdate(protocol: Protocol, stmt: UpdateNode): CodeBlock =
        genUpdate(stmt)

    abstract fun genOutput(stmt: OutputNode): CodeBlock

    override fun genOutput(protocol: Protocol, stmt: OutputNode): CodeBlock =
        genOutput(stmt)
}
