package edu.cornell.cs.apl.viaduct.backends.aby

import com.squareup.kotlinpoet.CodeBlock
import edu.cornell.cs.apl.viaduct.codegeneration.AbstractCodeGenerator
import edu.cornell.cs.apl.viaduct.codegeneration.CodeGeneratorContext
import edu.cornell.cs.apl.viaduct.errors.CodeGenerationError
import edu.cornell.cs.apl.viaduct.selection.ProtocolCommunication
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AtomicExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.UpdateNode

class ABYCodeGenerator(
    context: CodeGeneratorContext
) : AbstractCodeGenerator(context) {
    override fun exp(protocol: Protocol, expr: ExpressionNode): CodeBlock {
        TODO("Not yet implemented")
    }

    override fun let(protocol: Protocol, stmt: LetNode): CodeBlock {
        TODO("Not yet implemented")
    }
    /*

    val v : Circuit

    private val protocolCircuitType: Map<ProtocolName, ABYCircuitType> =
        mapOf(
            ArithABY.protocolName to ABYCircuitType.ARITH,
            BoolABY.protocolName to ABYCircuitType.BOOL,
            YaoABY.protocolName to ABYCircuitType.YAO
        )

    private fun valueToCircuit(circuitType: ABYCircuitType, value: Value, isInput: Boolean = false): CodeBlock =
        when (value) {
            is BooleanValue ->
            is IntegerValue ->
            else -> throw CodeGenerationError("unknown value type: ${value.toDocument().print()}")
        }

    override fun exp(circuitType: ABYCircuitType, protocol: Protocol, expr: ExpressionNode): CodeBlock =
        when (expr) {
            is LiteralNode ->

        }*/

    /*override fun let(protocol: Protocol, stmt: LetNode): CodeBlock =
        when (val rhs = stmt.value) {
            is InputNode -> throw CodeGenerationError("cannot perform I/O in non-local protocol")
            is PureExpressionNode -> {

            }
        }*/

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
