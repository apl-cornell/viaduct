package edu.cornell.cs.apl.viaduct.codegeneration

import com.squareup.kotlinpoet.CodeBlock
import edu.cornell.cs.apl.prettyprinting.joined
import edu.cornell.cs.apl.viaduct.selection.ProtocolCommunication
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AtomicExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DowngradeNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LiteralNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OperatorApplicationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutParameterInitializationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.QueryNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReadNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReceiveNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.SimpleStatementNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.UpdateNode

class PlainTextCodeGenerator(
    program: ProgramNode,
    override val availableProtocols: Set<Protocol>,
    private val host: Host
    //TODO - consider whether you need a runtime object here
) : AbstractCodeGenerator(program) {

    private fun genExpString(expr: ExpressionNode): String {
        return when (expr) {
            is LiteralNode -> expr.value.asDocument.print()

            is ReadNode -> expr.temporary.asDocument.print()

            is OperatorApplicationNode -> expr.operator.asDocument(expr.arguments).print()

            is QueryNode -> expr.variable.asDocument.print() + "." + expr.query.asDocument.print() + "(" + expr.arguments.joined().print() + ")"

            is DowngradeNode -> genExpString(expr.expression)

            is InputNode -> "runtime.input()"

            is ReceiveNode -> TODO()
        }
    }

    //TODO - figure out how to get type from expression node
    override fun genLet(protocol: Protocol, stmt: LetNode): CodeBlock {
        return CodeBlock.of(
            "val %L = %L",
            stmt.temporary.asDocument.print(),
            genExpString(stmt.value)
        )
    }

    override fun genDeclaration(protocol: Protocol, stmt: DeclarationNode): CodeBlock {
        return CodeBlock.of(
            "val %L = %T(%L)",
            stmt.name.asDocument.print(),
            stmt.className.asDocument.print(),
            stmt.arguments.joined().print()
        )
    }

    override fun genUpdate(protocol: Protocol, stmt: UpdateNode): CodeBlock {
        return CodeBlock.of(
            "%L.%L(%L)",
            stmt.variable.asDocument.print(),
            stmt.update.asDocument.print(),
            stmt.arguments.joined().print()
        )
    }


    override fun genOutParameter(protocol: Protocol, stmt: OutParameterInitializationNode): CodeBlock {
        TODO("is there anything special we have to do for out parameters?")
    }

    override fun genOutput(protocol: Protocol, stmt: OutputNode): CodeBlock {
        return CodeBlock.of(
            "runtime.output(%L)",
            genExpString(stmt.message)
        )
    }

    override fun genGuard(protocol: Protocol, expr: AtomicExpressionNode): CodeBlock {
        return CodeBlock.of(genExpString(expr))
    }

    override fun genSend(sender: LetNode,
                         sendProtocol: Protocol,
                         receiver: SimpleStatementNode,
                         recvProtocol: Protocol,
                         events: ProtocolCommunication
    ): CodeBlock {
        val sendBuilder = CodeBlock.builder()
    }

    override fun genRecieve(sender: LetNode,
                            sendProtocol: Protocol,
                            receiver: SimpleStatementNode,
                            recvProtocol: Protocol,
                            events: ProtocolCommunication
    ): CodeBlock {
        TODO("Not yet implemented")
    }
}
