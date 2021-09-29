package edu.cornell.cs.apl.viaduct.codegeneration

import com.squareup.kotlinpoet.CodeBlock
import edu.cornell.cs.apl.viaduct.errors.IllegalInternalCommunicationError
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AtomicExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutParameterInitializationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.SendNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.SimpleStatementNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.UpdateNode

abstract class AbstractCodeGenerator : CodeGenerator {

    override fun simpleStatement(protocol: Protocol, stmt: SimpleStatementNode): CodeBlock {
        return when (stmt) {
            is LetNode -> let(protocol, stmt)

            is DeclarationNode -> declaration(protocol, stmt)

            is UpdateNode -> update(protocol, stmt)

            is OutParameterInitializationNode -> outParameterInitialization(protocol, stmt)

            is OutputNode -> output(protocol, stmt)

            is SendNode -> throw IllegalInternalCommunicationError(stmt)
        }
    }

    abstract fun let(protocol: Protocol, stmt: LetNode): CodeBlock

    abstract fun declaration(protocol: Protocol, stmt: DeclarationNode): CodeBlock

    abstract fun update(protocol: Protocol, stmt: UpdateNode): CodeBlock

    abstract fun outParameterInitialization(protocol: Protocol, stmt: OutParameterInitializationNode): CodeBlock

    abstract fun output(protocol: Protocol, stmt: OutputNode): CodeBlock
}

abstract class SingleProtocolCodeGenerator : AbstractCodeGenerator() {

    abstract fun guard(expr: AtomicExpressionNode): CodeBlock

    override fun guard(protocol: Protocol, expr: AtomicExpressionNode): CodeBlock =
        guard(expr)

    abstract fun let(stmt: LetNode): CodeBlock

    override fun let(protocol: Protocol, stmt: LetNode): CodeBlock =
        let(stmt)

    abstract fun update(stmt: UpdateNode): CodeBlock

    override fun update(protocol: Protocol, stmt: UpdateNode): CodeBlock =
        update(stmt)

    abstract fun output(stmt: OutputNode): CodeBlock

    override fun output(protocol: Protocol, stmt: OutputNode): CodeBlock =
        output(stmt)
}
