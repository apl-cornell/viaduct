package edu.cornell.cs.apl.viaduct.codegeneration

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import edu.cornell.cs.apl.viaduct.backends.cleartext.Plaintext
import edu.cornell.cs.apl.viaduct.errors.CodeGenerationError
import edu.cornell.cs.apl.viaduct.runtime.EquivocationException
import edu.cornell.cs.apl.viaduct.runtime.commitment.Commitment
import edu.cornell.cs.apl.viaduct.runtime.commitment.Committed
import edu.cornell.cs.apl.viaduct.selection.CommunicationEvent
import edu.cornell.cs.apl.viaduct.selection.ProtocolCommunication
import edu.cornell.cs.apl.viaduct.syntax.BinaryOperator
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.ProtocolProjection
import edu.cornell.cs.apl.viaduct.syntax.UnaryOperator
import edu.cornell.cs.apl.viaduct.syntax.datatypes.Get
import edu.cornell.cs.apl.viaduct.syntax.datatypes.Modify
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AtomicExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
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
import edu.cornell.cs.apl.viaduct.syntax.operators.Maximum
import edu.cornell.cs.apl.viaduct.syntax.operators.Minimum
import edu.cornell.cs.apl.viaduct.syntax.types.ImmutableCellType
import edu.cornell.cs.apl.viaduct.syntax.types.MutableCellType
import edu.cornell.cs.apl.viaduct.syntax.types.VectorType

class PlainTextCodeGenerator(context: CodeGeneratorContext) :
    AbstractCodeGenerator(context) {
    override fun exp(protocol: Protocol, expr: ExpressionNode): CodeBlock =
        when (expr) {
            is LiteralNode -> CodeBlock.of("%L", expr.value)

            is ReadNode ->
                CodeBlock.of(
                    "%N",
                    context.kotlinName(
                        expr.temporary.value,
                        protocol
                    )
                )

            is OperatorApplicationNode -> {
                when (expr.operator) {
                    Minimum ->
                        CodeBlock.of(
                            "%M(%L, %L)",
                            MemberName("kotlin.math", "min"),
                            exp(protocol, expr.arguments[0]),
                            exp(protocol, expr.arguments[1])
                        )
                    Maximum ->
                        CodeBlock.of(
                            "%M(%L, %L)",
                            MemberName("kotlin.math", "max"),
                            exp(protocol, expr.arguments[0]),
                            exp(protocol, expr.arguments[1])
                        )
                    is UnaryOperator ->
                        CodeBlock.of(
                            "%L%L",
                            expr.operator.toString(),
                            exp(protocol, expr.arguments[0])
                        )
                    is BinaryOperator ->
                        CodeBlock.of(
                            "%L %L %L",
                            exp(protocol, expr.arguments[0]),
                            expr.operator,
                            exp(protocol, expr.arguments[1])
                        )
                    else -> throw CodeGenerationError("unknown operator", expr)
                }
            }

            is QueryNode ->
                when (context.typeAnalysis.type(context.nameAnalysis.declaration(expr))) {
                    is VectorType -> {
                        when (expr.query.value) {
                            is Get -> CodeBlock.of(
                                "%N[%L]",
                                context.kotlinName(expr.variable.value),
                                exp(protocol, expr.arguments.first())
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

            is DowngradeNode -> exp(protocol, expr.expression)

            is InputNode ->
                CodeBlock.of(
                    "(runtime.input(%T) as %T).value",
                    expr.type.value::class,
                    expr.type.value.valueClass
                )
        }

    override fun let(protocol: Protocol, stmt: LetNode): CodeBlock =
        CodeBlock.of(
            "val %N = %L",
            context.kotlinName(stmt.temporary.value, protocol),
            exp(protocol, stmt.value)
        )

    override fun declaration(protocol: Protocol, stmt: DeclarationNode): CodeBlock =
        declarationHelper(
            context.kotlinName(stmt.name.value),
            stmt.className,
            stmt.arguments,
            value(stmt.typeArguments[0].value.defaultValue),
            protocol
        )

    override fun update(protocol: Protocol, stmt: UpdateNode): CodeBlock =
        when (context.typeAnalysis.type(context.nameAnalysis.declaration(stmt))) {
            is VectorType ->
                when (stmt.update.value) {
                    is edu.cornell.cs.apl.viaduct.syntax.datatypes.Set ->
                        CodeBlock.of(
                            "%N[%L] = %L",
                            context.kotlinName(stmt.variable.value),
                            exp(protocol, stmt.arguments[0]),
                            exp(protocol, stmt.arguments[1])
                        )

                    is Modify ->
                        CodeBlock.of(
                            "%N[%L] %L %L",
                            context.kotlinName(stmt.variable.value),
                            exp(protocol, stmt.arguments[0]),
                            stmt.update.value.name,
                            exp(protocol, stmt.arguments[1])
                        )

                    else -> throw CodeGenerationError("unknown update", stmt)
                }

            is MutableCellType ->
                when (stmt.update.value) {
                    is edu.cornell.cs.apl.viaduct.syntax.datatypes.Set ->
                        CodeBlock.of(
                            "%N = %L",
                            context.kotlinName(stmt.variable.value),
                            exp(protocol, stmt.arguments[0])
                        )

                    is Modify ->
                        CodeBlock.of(
                            "%N %L %L",
                            context.kotlinName(stmt.variable.value),
                            stmt.update.value.name,
                            exp(protocol, stmt.arguments[0])
                        )

                    else -> throw CodeGenerationError("unknown update", stmt)
                }

            else -> throw CodeGenerationError("unknown object to update", stmt)
        }

    /*override fun outParameterInitialization(
        protocol: Protocol,
        stmt: OutParameterInitializationNode
    ): CodeBlock =
        when (val initializer = stmt.initializer) {
            is OutParameterConstructorInitializerNode -> {
                val outTmpString = context.newTemporary("outTmp")
                CodeBlock.builder()
                    .add(
                        // declare object
                        declarationHelper(
                            outTmpString,
                            initializer.className,
                            initializer.arguments,
                            initializer.typeArguments[0].value
                        )
                    )
                    .add(
                        // fill box with constructed object
                        CodeBlock.of(
                            "%N.set(%L)",
                            context.kotlinName(stmt.name.value),
                            outTmpString
                        )
                    )
                    .build()
            }
            // fill box named [stmt.name.value.name] with [initializer.expression]
            is OutParameterExpressionInitializerNode ->
                CodeBlock.of(
                    "%N.set(%L)",
                    context.kotlinName(stmt.name.value),
                    exp(initializer.expression)
                )
        }*/

    override fun output(protocol: Protocol, stmt: OutputNode): CodeBlock =
        CodeBlock.of(
            "runtime.output(%T(%L))",
            context.typeAnalysis.type(stmt.message).valueClass,
            exp(protocol, stmt.message)
        )

    override fun guard(protocol: Protocol, expr: AtomicExpressionNode): CodeBlock = exp(protocol, expr)

    override fun send(
        sendingHost: Host,
        sender: LetNode,
        sendProtocol: Protocol,
        receiveProtocol: Protocol,
        events: ProtocolCommunication
    ): CodeBlock {
        val sendBuilder = CodeBlock.builder()
        if (sendProtocol != receiveProtocol) {
            val relevantEvents: Set<CommunicationEvent> =
                events.getProjectionSends(ProtocolProjection(sendProtocol, sendingHost))
            for (event in relevantEvents) {
                if (sendingHost != event.recv.host) {
                    if (sender.value is InputNode)
                        sendBuilder.addStatement(
                            "%L",
                            context.send(
                                CodeBlock.of("%L", context.kotlinName(sender.temporary.value, sendProtocol)),
                                event.recv.host
                            )
                        )
                    else
                        sendBuilder.addStatement("%L", context.send(exp(sendProtocol, sender.value), event.recv.host))
                }
            }
        }
        return sendBuilder.build()
    }

    override fun receive(
        receivingHost: Host,
        sender: LetNode,
        sendProtocol: Protocol,
        receiveProtocol: Protocol,
        events: ProtocolCommunication
    ): CodeBlock {
        val receiveBuilder = CodeBlock.builder()
        val clearTextTemp = context.newTemporary("clearTextTemp")
        val clearTextCommittedTemp = context.newTemporary("clearTextCommittedTemp")
        if (sendProtocol != receiveProtocol) {
            val projection = ProtocolProjection(receiveProtocol, receivingHost)
            val cleartextInputs = events.getProjectionReceives(
                projection,
                Plaintext.INPUT
            )

            val cleartextCommitmentInputs =
                events.getProjectionReceives(projection, Plaintext.CLEARTEXT_COMMITMENT_INPUT)

            var hashCommitmentInputs =
                events.getProjectionReceives(projection, Plaintext.HASH_COMMITMENT_INPUT)

            when {
                cleartextInputs.isNotEmpty() && cleartextCommitmentInputs.isEmpty() &&
                    hashCommitmentInputs.isEmpty() -> {

                    receiveBuilder.add(
                        "val %L = %L",
                        clearTextTemp,
                        receiveReplicated(
                            sender,
                            sendProtocol,
                            cleartextInputs,
                            context
                        )
                    )

                    // calculate set of hosts with whom [receivingHost] needs to check for equivocation
                    var hostsToCheckWith: List<Host> =
                        events
                            .filter { event ->
                                // remove events where receiving host is not receiving plaintext data
                                event.recv.id == Plaintext.INPUT &&

                                    // remove events where a host is sending data to themselves
                                    event.send.host != event.recv.host &&

                                    // remove events where [receivingHost] is the sender of the data
                                    event.send.host != receivingHost
                            }
                            // of events matching above criteria, get set of data receivers
                            .map { event -> event.recv.host }
                            // remove [receivingHost] from the set of hosts with whom [receivingHost] needs to
                            // check for equivocation
                            .filter { host -> host != receivingHost }
                            .sorted()

                    for (host in hostsToCheckWith)
                        receiveBuilder.addStatement("%L", context.send(CodeBlock.of(clearTextTemp), host))

                    val receiveTmp = context.newTemporary("receiveTmp")

                    // start equivocation check by receiving from host in [hostsToCheckWith]
                    if (hostsToCheckWith.isNotEmpty()) {
                        receiveBuilder.addStatement(
                            "var %N = %L",
                            receiveTmp,
                            context.receive(
                                typeTranslator(context.typeAnalysis.type(sender)),
                                hostsToCheckWith.first()
                            )
                        )
                        receiveBuilder.beginControlFlow(
                            "if (%N != %N)",
                            receiveTmp,
                            clearTextTemp
                        )
                        receiveBuilder.addStatement(
                            "throw %T(%S)",
                            EquivocationException::class.asClassName(),
                            "equivocation error between hosts: " + receivingHost.toDocument().print() + ", " +
                                hostsToCheckWith.first().toDocument().print()
                        )
                        receiveBuilder.endControlFlow()
                        hostsToCheckWith = hostsToCheckWith.minusElement(hostsToCheckWith.first())
                    }

                    // potentially receive from the rest of the hosts in [hostsToCheckWith]
                    for (host in hostsToCheckWith) {
                        receiveBuilder.addStatement(
                            "%N = %L",
                            receiveTmp,
                            context.receive(
                                typeTranslator(context.typeAnalysis.type(sender)),
                                host
                            )
                        )
                        receiveBuilder.beginControlFlow(
                            "if (%N != %N)",
                            receiveTmp,
                            clearTextTemp
                        )
                        receiveBuilder.addStatement(
                            "throw %T(%S)",
                            EquivocationException::class.asClassName(),
                            "equivocation error between hosts: " + receivingHost.toDocument().print() + ", " +
                                host.toDocument().print()
                        )
                        receiveBuilder.endControlFlow()
                    }
                    receiveBuilder.addStatement(
                        "val %N = %N",
                        context.kotlinName(sender.temporary.value, receiveProtocol),
                        clearTextTemp
                    )

                    return receiveBuilder.build()
                }

                // commitment opening
                cleartextInputs.isEmpty() && cleartextCommitmentInputs.isNotEmpty() &&
                    hashCommitmentInputs.isNotEmpty() -> {
                    if (cleartextCommitmentInputs.size != 1) {
                        throw CodeGenerationError("Commitment open: open multiple commitments at once")
                    }
                    val cleartextSendEvent = cleartextCommitmentInputs.first()

                    val receiveBlock = CodeBlock.builder()

                    if (cleartextSendEvent.send.host == context.host) {
                        receiveBlock.add(
                            "%L",
                            context.kotlinName(sender.temporary.value, sendProtocol)
                        )
                    } else {
                        receiveBlock.add(
                            "%L",
                            context.receive(
                                Committed::class.asTypeName().parameterizedBy(
                                    typeTranslator(context.typeAnalysis.type(sender))
                                ),
                                cleartextSendEvent.send.host
                            )
                        )
                    }

                    receiveBuilder.addStatement(
                        "val %N = %L",
                        clearTextCommittedTemp,
                        receiveBlock.build()
                    )

                    val firstHashReceiveBlock = CodeBlock.builder()

                    if (hashCommitmentInputs.first().send.host == context.host) {
                        firstHashReceiveBlock.add(
                            "%L",
                            context.kotlinName(sender.temporary.value, sendProtocol)
                        )
                    } else {
                        firstHashReceiveBlock.add(
                            "%L",
                            context.receive(
                                Commitment::class.asTypeName().parameterizedBy(
                                    typeTranslator(context.typeAnalysis.type(sender))
                                ),
                                hashCommitmentInputs.first().send.host
                            )
                        )
                    }

                    receiveBuilder.addStatement(
                        "var %N = %L.open(%N)",
                        context.kotlinName(sender.temporary.value, receiveProtocol),
                        firstHashReceiveBlock.build(),
                        clearTextCommittedTemp
                    )

                    hashCommitmentInputs = hashCommitmentInputs.minusElement(hashCommitmentInputs.first())

                    // compare commitment creator's value with all hash replica holder's value
                    for (hashCommitmentInput in hashCommitmentInputs) {

                        val hashReceiveBlock = CodeBlock.builder()

                        if (hashCommitmentInput.send.host == context.host) {
                            hashReceiveBlock.add(
                                "%L",
                                context.kotlinName(sender.temporary.value, sendProtocol)
                            )
                        } else {
                            hashReceiveBlock.add(
                                "%L",
                                context.receive(
                                    Commitment::class.asTypeName().parameterizedBy(
                                        typeTranslator(context.typeAnalysis.type(sender))
                                    ),
                                    hashCommitmentInput.send.host
                                )
                            )
                        }

                        // validate commitment with all hash holders
                        receiveBuilder.addStatement(
                            "%L.open(%N)",
                            hashReceiveBlock.build(),
                            clearTextCommittedTemp
                        )
                    }
                }

                else ->
                    throw
                    CodeGenerationError("Plaintext: received both commitment opening and cleartext value")
            }
        }
        return receiveBuilder.build()
    }
}
