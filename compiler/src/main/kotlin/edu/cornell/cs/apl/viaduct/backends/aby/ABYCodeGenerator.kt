package edu.cornell.cs.apl.viaduct.backends.aby

import com.github.apl_cornell.aby.SharingType
import com.squareup.kotlinpoet.CodeBlock
import com.sun.jdi.IntegerValue
import edu.cornell.cs.apl.viaduct.codegeneration.AbstractCodeGenerator
import edu.cornell.cs.apl.viaduct.codegeneration.CodeGeneratorContext
import edu.cornell.cs.apl.viaduct.codegeneration.getRole
import edu.cornell.cs.apl.viaduct.errors.CodeGenerationError
import edu.cornell.cs.apl.viaduct.selection.ProtocolCommunication
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AtomicExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclassificationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DowngradeNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.EndorsementNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LiteralNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OperatorApplicationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.PureExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.QueryNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReadNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.UpdateNode
import edu.cornell.cs.apl.viaduct.syntax.values.BooleanValue
import edu.cornell.cs.apl.viaduct.syntax.values.Value

class ABYCodeGenerator(
    context: CodeGeneratorContext
) : AbstractCodeGenerator(context) {

    private val bitLen = 32

    // TODO() - come up with a better way of storing the mapping : protocol -> ABYParty object names
    private fun protocolToAbyPartyCircuit(protocol: Protocol): CodeBlock =
        CodeBlock.of(
            "%L.getCircuitBuilder(%L)",
            "ABYParty${protocol.protocolName.name}",
            protocolToShareType(protocol)
        )

    private fun protocolToShareType(protocol: Protocol): SharingType =
        when (protocol) {
            is ArithABY -> SharingType.S_ARITH
            is BoolABY -> SharingType.S_BOOL
            is YaoABY -> SharingType.S_YAO
            else -> throw CodeGenerationError("unknown ABY protocol: ${protocol.toDocument().print()}")
        }

    private fun valueToCircuit(value: Value, isInput: Boolean = false, protocol: Protocol): CodeBlock =
        when (value) {
            is BooleanValue ->
                if (isInput) {
                    CodeBlock.of(
                        "%L.putINGate(%L.toInt().toBigInteger(), %L, %L)",
                        protocolToAbyPartyCircuit(protocol),
                        value.value,
                        bitLen,
                        getRole(protocol, context.host)
                    )
                } else {
                    CodeBlock.of(
                        "%L.putCONSGate(%L.toInt().toBigInteger(), %L)",
                        protocolToAbyPartyCircuit(protocol),
                        value.value,
                        bitLen
                    )
                }
            is IntegerValue ->
                if (isInput) {
                    CodeBlock.of(
                        "%L.putINGate(%L, %L, %L)",
                        protocolToAbyPartyCircuit(protocol),
                        value.value(),
                        bitLen,
                        getRole(protocol, context.host)
                    )
                } else {
                    CodeBlock.of(
                        "%L.putCONSGate(%L, %L)",
                        protocolToAbyPartyCircuit(protocol),
                        value.value(),
                        bitLen
                    )
                }
            else -> throw CodeGenerationError("unknown value type: ${value.toDocument().print()}")
        }

    override fun exp(protocol: Protocol, expr: ExpressionNode): CodeBlock =
        when (expr) {
            is LiteralNode -> valueToCircuit(expr.value, false, protocol)

            is ReadNode ->
                CodeBlock.of(
                    "%L",
                    context.kotlinName(expr.temporary.value, protocol)
                )

            is OperatorApplicationNode -> TODO()
            is QueryNode -> TODO()
            is DeclassificationNode -> exp(protocol, expr.expression)
            is EndorsementNode -> exp(protocol, expr.expression)
            is DowngradeNode -> exp(protocol, expr.expression)
            is InputNode ->
                throw CodeGenerationError("cannot perform I/O in non-Local protocol")
        }

    override fun let(protocol: Protocol, stmt: LetNode): CodeBlock =
        when (stmt.value) {
            is InputNode -> throw CodeGenerationError("cannot perform I/O in non-Local protocol")
            is PureExpressionNode -> {
                CodeBlock.of(
                    "val %N = %L",
                    context.kotlinName(stmt.temporary.value, protocol),
                    exp(protocol, stmt.value)
                )
            }
        }

    override fun update(protocol: Protocol, stmt: UpdateNode): CodeBlock {
        TODO("Not yet implemented")
    }

    override fun output(protocol: Protocol, stmt: OutputNode): CodeBlock =
        throw CodeGenerationError("cannot perform I/O in non-local protocol")

    override fun guard(protocol: Protocol, expr: AtomicExpressionNode): CodeBlock =
        throw CodeGenerationError("ABY: Cannot execute conditional guard")

    override fun send(
        sender: LetNode,
        sendProtocol: Protocol,
        receiveProtocol: Protocol,
        events: ProtocolCommunication
    ): CodeBlock {
        TODO("Not yet implemented")
    }

    override fun receive(
        sender: LetNode,
        sendProtocol: Protocol,
        receiveProtocol: Protocol,
        events: ProtocolCommunication
    ): CodeBlock {
        TODO("Not yet implemented")
    }
}
