package edu.cornell.cs.apl.viaduct.backends.commitment

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.asClassName
import edu.cornell.cs.apl.viaduct.analysis.NameAnalysis
import edu.cornell.cs.apl.viaduct.analysis.TypeAnalysis
import edu.cornell.cs.apl.viaduct.codegeneration.AbstractCodeGenerator
import edu.cornell.cs.apl.viaduct.codegeneration.CodeGeneratorContext
import edu.cornell.cs.apl.viaduct.codegeneration.receiveReplicated
import edu.cornell.cs.apl.viaduct.codegeneration.typeTranslator
import edu.cornell.cs.apl.viaduct.errors.CodeGenerationError
import edu.cornell.cs.apl.viaduct.runtime.commitment.Commitment
import edu.cornell.cs.apl.viaduct.runtime.commitment.Committed
import edu.cornell.cs.apl.viaduct.selection.CommunicationEvent
import edu.cornell.cs.apl.viaduct.selection.ProtocolCommunication
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.ProtocolProjection
import edu.cornell.cs.apl.viaduct.syntax.datatypes.Get
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AtomicExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DowngradeNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LiteralNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OperatorApplicationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.QueryNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReadNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.UpdateNode
import edu.cornell.cs.apl.viaduct.syntax.types.ImmutableCellType
import edu.cornell.cs.apl.viaduct.syntax.types.MutableCellType
import edu.cornell.cs.apl.viaduct.syntax.types.VectorType
import edu.cornell.cs.apl.viaduct.backends.commitment.Commitment as CommitmentProtocol

internal class CommitmentHolderGenerator(
    context: CodeGeneratorContext
) : AbstractCodeGenerator(context) {
    private val typeAnalysis: TypeAnalysis = TypeAnalysis.get(context.program)
    private val nameAnalysis: NameAnalysis = NameAnalysis.get(context.program)
    override fun exp(protocol: Protocol, expr: ExpressionNode): CodeBlock =
        when (expr) {
            is LiteralNode -> CodeBlock.of(
                "%M(%L).%M()",
                MemberName(Committed.Companion::class.asClassName(), "fake"),
                expr.value,
                MemberName(Committed.Companion::class.asClassName(), "commitment")
            )

            is ReadNode -> CodeBlock.of(
                "%L",
                context.kotlinName(expr.temporary.value, protocol)
            )

            is DowngradeNode -> exp(protocol, expr.expression)

            is QueryNode ->
                when (typeAnalysis.type(nameAnalysis.declaration(expr))) {
                    is VectorType -> {
                        when (expr.query.value) {
                            is Get -> CodeBlock.of(
                                "%N[%L]",
                                context.kotlinName(expr.variable.value),
                                cleartextExp(protocol, expr.arguments.first())
                            )
                            else -> throw CodeGenerationError("unknown vector query", expr)
                        }
                    }

                    is ImmutableCellType -> {
                        when (expr.query.value) {
                            is Get -> CodeBlock.of(context.kotlinName(expr.variable.value))
                            else -> throw CodeGenerationError("unknown query", expr)
                        }
                    }

                    is MutableCellType -> {
                        when (expr.query.value) {
                            is Get -> CodeBlock.of(context.kotlinName(expr.variable.value))
                            else -> throw CodeGenerationError("unknown query", expr)
                        }
                    }

                    else -> throw CodeGenerationError("unknown AST object", expr)
                }

            is OperatorApplicationNode ->
                throw CodeGenerationError("Commitment: cannot perform operations on committed values")

            is InputNode ->
                throw CodeGenerationError("Commitment: cannot perform I/O in non-local protocol")
        }

    override fun let(protocol: Protocol, stmt: LetNode): CodeBlock =
        CodeBlock.of(
            "var %N = %L",
            context.kotlinName(stmt.temporary.value, protocol),
            exp(protocol, stmt.value)
        )

    override fun update(protocol: Protocol, stmt: UpdateNode): CodeBlock =
        when (typeAnalysis.type(nameAnalysis.declaration(stmt))) {
            is VectorType ->
                when (stmt.update.value) {
                    is edu.cornell.cs.apl.viaduct.syntax.datatypes.Set ->
                        CodeBlock.of(
                            "%N[%L] = %L",
                            context.kotlinName(stmt.variable.value),
                            cleartextExp(protocol, stmt.arguments[0]),
                            exp(protocol, stmt.arguments[1])
                        )
                    else -> throw CodeGenerationError("Commitment: cannot modify commitments")
                }
            is MutableCellType ->
                when (stmt.update.value) {
                    is edu.cornell.cs.apl.viaduct.syntax.datatypes.Set ->
                        CodeBlock.of(
                            "%N = %L",
                            context.kotlinName(stmt.variable.value),
                            exp(protocol, stmt.arguments[0])
                        )
                    else -> throw CodeGenerationError("Commitment: cannot modify commitments")
                }
            else -> throw CodeGenerationError("Commitment: unknown object to update")
        }

    override fun output(protocol: Protocol, stmt: OutputNode): CodeBlock =
        throw CodeGenerationError("Commitment: cannot perform I/O in non-local protocol")

    override fun guard(protocol: Protocol, expr: AtomicExpressionNode): CodeBlock =
        throw CodeGenerationError("Commitment: cannot use committed value as a guard")

    override fun send(
        sender: LetNode,
        sendProtocol: Protocol,
        receiveProtocol: Protocol,
        events: ProtocolCommunication
    ): CodeBlock {
        val sendBuilder = CodeBlock.builder()

        // here, the interpreter checks for the available protocols, is this necessary here?
        val relevantEvents: List<CommunicationEvent> =
            events.getProjectionSends(
                ProtocolProjection(sendProtocol, context.host),
                CommitmentProtocol.OPEN_COMMITMENT_OUTPUT
            ).toList()

        for (event in relevantEvents) {
            if (event.send.host != event.recv.host) {
                sendBuilder.addStatement(
                    "%L",
                    context.send(
                        CodeBlock.of("%L", context.kotlinName(sender.temporary.value, sendProtocol)),
                        event.recv.host
                    )
                )
            }
        }
        return sendBuilder.build()
    }

    override fun receive(
        sender: LetNode,
        sendProtocol: Protocol,
        receiveProtocol: Protocol,
        events: ProtocolCommunication
    ): CodeBlock {

        val receiveBuilder = CodeBlock.builder()
        val projection = ProtocolProjection(receiveProtocol, context.host)
        if (sendProtocol != receiveProtocol) {
            when {
                events.any { event -> event.recv.id == CommitmentProtocol.CLEARTEXT_INPUT } -> {
                    val relevantEvents =
                        events.getHostReceives(
                            projection.host,
                            CommitmentProtocol.CLEARTEXT_INPUT
                        )

                    receiveBuilder.addStatement(
                        "val %N = %L",
                        context.kotlinName(sender.temporary.value, receiveProtocol),
                        receiveReplicated(
                            sender,
                            sendProtocol,
                            relevantEvents,
                            context,
                            typeAnalysis
                        ),
                    )
                }

                else -> { // create commitment
                    if (context.host !in sendProtocol.hosts) {
                        receiveBuilder.addStatement(
                            "val %N = %L",
                            context.kotlinName(sender.temporary.value, receiveProtocol),
                            context.receive(
                                Commitment::class.asClassName().parameterizedBy(
                                    typeTranslator(typeAnalysis.type(sender))
                                ),
                                events.first().send.host
                            )
                        )
                    }
                }
            }
        }
        return receiveBuilder.build()
    }

    override fun setup(protocol: Protocol): Iterable<PropertySpec> = listOf()
}
