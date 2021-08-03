package edu.cornell.cs.apl.viaduct.codegeneration

import com.squareup.kotlinpoet.CodeBlock
import edu.cornell.cs.apl.viaduct.selection.ProtocolCommunication
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AtomicExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.SimpleStatementNode

interface CodeGenerator {
    val availableProtocols: Set<Protocol>

    fun Guard(protocol: Protocol, expr: AtomicExpressionNode): CodeBlock

    fun SimpleStatement(protocol: Protocol, stmt: SimpleStatementNode): CodeBlock

    fun Send(
        sender: LetNode,
        sendProtocol: Protocol,
        receiver: SimpleStatementNode,
        recvProtocol: Protocol,
        events: ProtocolCommunication
    ): CodeBlock

    fun Recieve(
        sender: LetNode,
        sendProtocol: Protocol,
        receiver: SimpleStatementNode,
        recvProtocol: Protocol,
        events: ProtocolCommunication
    ): CodeBlock
}
