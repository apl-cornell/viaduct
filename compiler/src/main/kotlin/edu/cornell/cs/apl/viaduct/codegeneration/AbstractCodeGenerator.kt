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

    override fun SimpleStatement(protocol: Protocol, stmt: SimpleStatementNode): CodeBlock {
        return when (stmt) {
            is LetNode -> Let(protocol, stmt)

            is DeclarationNode -> Declaration(protocol, stmt)

            is UpdateNode -> Update(protocol, stmt)

            is OutParameterInitializationNode -> OutParameterInitialization(protocol, stmt)

            is OutputNode -> Output(protocol, stmt)

            is SendNode -> throw IllegalInternalCommunicationError(stmt)
        }
    }

    abstract fun Let(protocol: Protocol, stmt: LetNode): CodeBlock

    abstract fun Declaration(protocol: Protocol, stmt: DeclarationNode): CodeBlock

    abstract fun Update(protocol: Protocol, stmt: UpdateNode): CodeBlock

    abstract fun OutParameterInitialization(protocol: Protocol, stmt: OutParameterInitializationNode): CodeBlock

    abstract fun Output(protocol: Protocol, stmt: OutputNode): CodeBlock
}

abstract class SingleProtocolCodeGenerator(
    program: ProgramNode,
    private val protocol: Protocol
) : AbstractCodeGenerator(program) {
    override val availableProtocols: Set<Protocol> =
        setOf(protocol)

    abstract fun Guard(expr: AtomicExpressionNode): CodeBlock

    override fun Guard(protocol: Protocol, expr: AtomicExpressionNode): CodeBlock =
        Guard(expr)

    abstract fun Let(stmt: LetNode): CodeBlock

    override fun Let(protocol: Protocol, stmt: LetNode): CodeBlock =
        Let(stmt)

    abstract fun Update(stmt: UpdateNode): CodeBlock

    override fun Update(protocol: Protocol, stmt: UpdateNode): CodeBlock =
        Update(stmt)

    abstract fun Output(stmt: OutputNode): CodeBlock

    override fun Output(protocol: Protocol, stmt: OutputNode): CodeBlock =
        Output(stmt)
}
