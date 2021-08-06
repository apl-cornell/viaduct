package edu.cornell.cs.apl.viaduct.codegeneration

import com.squareup.kotlinpoet.CodeBlock
import edu.cornell.cs.apl.prettyprinting.joined
import edu.cornell.cs.apl.viaduct.selection.ProtocolCommunication
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
    override val availableProtocols: Set<Protocol>
) : AbstractCodeGenerator(program) {

    private fun expString(expr: ExpressionNode): String {
        return when (expr) {
            is LiteralNode -> expr.value.toString()

            is ReadNode -> expr.temporary.value.name

            is OperatorApplicationNode -> expr.operator.asDocument(expr.arguments).print()

            is QueryNode -> expr.variable.value.name + "." + expr.query.value.name + "(" + expr.arguments.joined().print() + ")"

            is DowngradeNode -> expString(expr)

            is InputNode -> "runtime.input()"

            is ReceiveNode -> TODO()
        }
    }

    override fun Let(protocol: Protocol, stmt: LetNode): CodeBlock {
        return CodeBlock.of(
            "val %L = %L",
            stmt.temporary.value.name,

            // TODO - create type translation function : Viaduct type -> Kotlin type
            // this.translateType(this.typeAnalysis.type(stmt)),
            expString(stmt.value)
        )
    }

    override fun Declaration(protocol: Protocol, stmt: DeclarationNode): CodeBlock {
        return CodeBlock.of(
            "val %L = %T(%L)",
            stmt.name.value.name,
            stmt.className.value.name,
            stmt.arguments.joined().print()
        )
    }

    override fun Update(protocol: Protocol, stmt: UpdateNode): CodeBlock {
        return CodeBlock.of(
            "%L.%L(%L)",
            stmt.variable.value.name,
            stmt.update.value.name,
            stmt.arguments.joined().print()
        )
    }

    override fun OutParameter(protocol: Protocol, stmt: OutParameterInitializationNode): CodeBlock {
        TODO("is there anything special we have to do for out parameters?")
    }

    override fun Output(protocol: Protocol, stmt: OutputNode): CodeBlock {
        return CodeBlock.of(
            "runtime.output(%L)",
            expString(stmt.message)
        )
    }

    override fun Guard(protocol: Protocol, expr: AtomicExpressionNode): CodeBlock {
        return CodeBlock.of(expString(expr))
    }

    override fun Send(
        sender: LetNode,
        sendProtocol: Protocol,
        receiver: SimpleStatementNode,
        recvProtocol: Protocol,
        events: ProtocolCommunication
    ): CodeBlock {
        val sendBuilder = CodeBlock.builder()
        if (sendProtocol != recvProtocol) {
            TODO("not yet implemented")
        }
        return sendBuilder.build()
    }

    override fun Recieve(
        sender: LetNode,
        sendProtocol: Protocol,
        receiver: SimpleStatementNode,
        recvProtocol: Protocol,
        events: ProtocolCommunication
    ): CodeBlock {
        TODO("Not yet implemented")
    }
}
