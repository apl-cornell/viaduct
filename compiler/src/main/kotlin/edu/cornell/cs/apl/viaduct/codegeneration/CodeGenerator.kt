package edu.cornell.cs.apl.viaduct.codegeneration

import com.squareup.kotlinpoet.CodeBlock
import edu.cornell.cs.apl.viaduct.selection.ProtocolCommunication
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AtomicExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.SimpleStatementNode

interface CodeGenerator {

    fun Guard(protocol: Protocol, expr: AtomicExpressionNode): CodeBlock

    fun SimpleStatement(protocol: Protocol, stmt: SimpleStatementNode): CodeBlock

    fun Send(
        sendingHost: Host,
        sender: LetNode,
        sendProtocol: Protocol,
        recvProtocol: Protocol,
        events: ProtocolCommunication
    ): CodeBlock

    fun Recieve(
        receivingHost: Host,
        sender: LetNode,
        sendProtocol: Protocol,
        recvProtocol: Protocol,
        events: ProtocolCommunication
    ): CodeBlock
}
