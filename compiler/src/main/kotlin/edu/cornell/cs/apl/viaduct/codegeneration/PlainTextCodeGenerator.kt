package edu.cornell.cs.apl.viaduct.codegeneration

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.asClassName
import edu.cornell.cs.apl.prettyprinting.joined
import edu.cornell.cs.apl.viaduct.analysis.NameAnalysis
import edu.cornell.cs.apl.viaduct.analysis.TypeAnalysis
import edu.cornell.cs.apl.viaduct.errors.CodeGenerationError
import edu.cornell.cs.apl.viaduct.errors.RuntimeError
import edu.cornell.cs.apl.viaduct.protocols.Plaintext
import edu.cornell.cs.apl.viaduct.selection.CommunicationEvent
import edu.cornell.cs.apl.viaduct.selection.ProtocolCommunication
import edu.cornell.cs.apl.viaduct.syntax.Arguments
import edu.cornell.cs.apl.viaduct.syntax.ClassNameNode
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.ProtocolProjection
import edu.cornell.cs.apl.viaduct.syntax.datatypes.Get
import edu.cornell.cs.apl.viaduct.syntax.datatypes.ImmutableCell
import edu.cornell.cs.apl.viaduct.syntax.datatypes.Modify
import edu.cornell.cs.apl.viaduct.syntax.datatypes.MutableCell
import edu.cornell.cs.apl.viaduct.syntax.datatypes.Vector
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AtomicExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DowngradeNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LiteralNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OperatorApplicationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutParameterConstructorInitializerNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutParameterExpressionInitializerNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutParameterInitializationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.QueryNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReadNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReceiveNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.UpdateNode
import edu.cornell.cs.apl.viaduct.syntax.types.ImmutableCellType
import edu.cornell.cs.apl.viaduct.syntax.types.MutableCellType
import edu.cornell.cs.apl.viaduct.syntax.types.VectorType

class PlainTextCodeGenerator(
    program: ProgramNode,
    typeAnalysis: TypeAnalysis,
    nameAnalysis: NameAnalysis,
    override val availableProtocols: Set<Protocol>
) : AbstractCodeGenerator(program) {
    val runtimeErrorClassName = RuntimeError::class.asClassName()
    val typeAnalysis = typeAnalysis
    val nameAnalysis = nameAnalysis

    private fun exp(expr: ExpressionNode): Any =
        when (expr) {
            is LiteralNode -> expr.value

            is ReadNode -> expr.temporary.value.name

            is OperatorApplicationNode -> expr.operator.asDocument(expr.arguments).print()

            is QueryNode ->
                when (this.typeAnalysis.type(nameAnalysis.declaration(expr))) {
                    is VectorType -> {
                        when (expr.query.value) {
                            is Get -> expr.variable.value.name + "[" + expr.arguments.first().toString() + "]"
                            else -> throw CodeGenerationError("unknown vector query", expr)
                        }
                    }

                    is ImmutableCellType -> {
                        when (expr.query.value) {
                            is Get -> expr.variable.value.name
                            else -> throw CodeGenerationError("unknown query", expr)
                        }
                    }

                    is MutableCellType -> {
                        when (expr.query.value) {
                            is Get -> expr.variable.value.name
                            else -> throw CodeGenerationError("unknown query", expr)
                        }
                    }

                    else -> throw CodeGenerationError("unknown AST object", expr)
                }

            is DowngradeNode -> exp(expr)

            // TODO - figure out better way to do this
            is InputNode -> {
                val viaductTypeClass = expr.type.value::class.asClassName()
                val viaductValueClass = expr.type.value.defaultValue::class.asClassName()
                "(runtime.input($viaductTypeClass) as $viaductValueClass).value"
            }

            is ReceiveNode -> TODO()
        }

    override fun Let(protocol: Protocol, stmt: LetNode): CodeBlock =
        CodeBlock.of(
            "val %L = %L",
            stmt.temporary.value.name,
            exp(stmt.value)
        )

    fun DeclarationHelper(
        name: String,
        className: ClassNameNode,
        arguments: Arguments<AtomicExpressionNode>
    ): CodeBlock =
        when (className.value) {
            ImmutableCell -> CodeBlock.of(
                "val %L = %L",
                name,
                arguments.joined().print()
            )

            // TODO - change this (difference between viaduct, kotlin semantics)
            MutableCell -> CodeBlock.of(
                "var %L = %L",
                name,
                arguments.joined().print()
            )

            Vector -> CodeBlock.of(
                "var %L = Array(%L)",
                name,
                arguments.joined().print()
            )

            else -> CodeBlock.of(
                "var %L = %T(%L)",
                name,
                className.value.name::class.asClassName(),
                arguments.joined().print()
            )
        }

    override fun Declaration(protocol: Protocol, stmt: DeclarationNode): CodeBlock =
        DeclarationHelper(stmt.name.value.name, stmt.className, stmt.arguments)

    override fun Update(protocol: Protocol, stmt: UpdateNode): CodeBlock =
        when (this.typeAnalysis.type(nameAnalysis.declaration(stmt))) {
            is VectorType ->
                when (stmt.update.value) {
                    is edu.cornell.cs.apl.viaduct.syntax.datatypes.Set ->
                        CodeBlock.of(
                            "%L[%L] = %L",
                            stmt.variable.value.name,
                            exp(stmt.arguments[0]),
                            exp(stmt.arguments[1])
                        )

                    is Modify ->
                        CodeBlock.of(
                            "%L[%L] %L %L",
                            stmt.variable.value.name,
                            exp(stmt.arguments[0]),
                            stmt.update.value.name,
                            exp(stmt.arguments[1])
                        )

                    else -> throw CodeGenerationError("unknown update", stmt)
                }

            is MutableCellType ->
                when (stmt.update.value) {
                    is edu.cornell.cs.apl.viaduct.syntax.datatypes.Set ->
                        CodeBlock.of(
                            "%L = %L",
                            stmt.variable.value.name,
                            exp(stmt.arguments[0])
                        )

                    is Modify ->
                        CodeBlock.of(
                            "%L %L %L",
                            stmt.variable.value.name,
                            stmt.update.value.name,
                            exp(stmt.arguments[0])
                        )

                    else -> throw CodeGenerationError("unknown update", stmt)
                }

            else -> throw CodeGenerationError("unknown object to update", stmt)
        }

    override fun OutParameterInitialization(
        protocol: Protocol,
        stmt: OutParameterInitializationNode
    ):
        CodeBlock =
        when (val initializer = stmt.initializer) {
            is OutParameterConstructorInitializerNode ->
                CodeBlock.builder()
                    .add(
                        // declare object
                        DeclarationHelper(
                            "outTemp",
                            initializer.className,
                            initializer.arguments
                        )
                    )
                    .add(
                        // fill box named [stmt.name.value.name] with constructed object
                        CodeBlock.of(
                            "%L.set(%L)",
                            stmt.name.value.name,
                            "outTemp"
                        )
                    )
                    .build()

            // fill box named [stmt.name.value.name] with [initializer.expression]
            is OutParameterExpressionInitializerNode ->
                CodeBlock.of("%L.set(%L)",
                    stmt.name.value.name,
                    exp(initializer.expression)
                )
        }

    override fun Output(protocol: Protocol, stmt: OutputNode): CodeBlock =
        CodeBlock.of(
            "runtime.output(%L)",
            exp(stmt.message)
        )

    override fun Guard(protocol: Protocol, expr: AtomicExpressionNode): CodeBlock =
        CodeBlock.of(exp(expr) as String)

    override fun Send(
        sendingHost: Host,
        sender: LetNode,
        sendProtocol: Protocol,
        recvProtocol: Protocol,
        events: ProtocolCommunication
    ): CodeBlock {
        val sendBuilder = CodeBlock.builder()
        if (sendProtocol != recvProtocol) {
            val relevantEvents: Set<CommunicationEvent> =
                events.getProjectionSends(ProtocolProjection(sendProtocol, sendingHost))
            for (event in relevantEvents) {
                sendBuilder.addStatement(
                    "runtime.send(%L, %L)",
                    sender.temporary.value.name,
                    event.recv.host.name
                )
            }
        }
        return sendBuilder.build()
    }

    // note - this implementation does not support commitments
    override fun Recieve(
        receivingHost: Host,
        sender: LetNode,
        sendProtocol: Protocol,
        recvProtocol: Protocol,
        events: ProtocolCommunication
    ): CodeBlock {
        val receiveBuilder = CodeBlock.builder()
        if (sendProtocol != recvProtocol) {
            val projection = ProtocolProjection(recvProtocol, receivingHost)
            var cleartextInputs = events.getProjectionReceives(projection, Plaintext.INPUT)

            // initialize cleartext receive value by receiving from first host
            receiveBuilder.addStatement(
                "val clearTextValue = runtime.receive(%L)",
                cleartextInputs.first().send.host.name
            )
            cleartextInputs = cleartextInputs.minusElement(cleartextInputs.first())

            // receive from the rest of the hosts and compare against clearTextValue
            for (event in cleartextInputs) {

                // check to make sure that you got the same data from all hosts
                receiveBuilder.beginControlFlow(
                    "if(clearTextValue != runtime.receive(%L))",
                    event.send.host.name
                )
                receiveBuilder.addStatement(
                    "throw %T(%S)",
                    runtimeErrorClassName,
                    "Plaintext : received different values"
                )
                receiveBuilder.endControlFlow()
            }

            // check if value you received is null
            receiveBuilder.beginControlFlow("if(clearTextValue == null)")
            receiveBuilder.addStatement(
                "throw %T(%S)",
                runtimeErrorClassName,
                "Plaintext : received null value"
            )
            receiveBuilder.endControlFlow()

            // calculate set of hosts with whom [receivingHost] needs to check for equivocation
            val hostsToCheckWith: Set<Host> =
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
                    .toSet()

            for (host in hostsToCheckWith) {
                receiveBuilder.addStatement(
                    "runtime.send(clearTextValue, %L)",
                    host.name
                )
            }

            for (host in hostsToCheckWith) {
                receiveBuilder.addStatement(
                    "var recvValue = runtime.receive(%L)",
                    host.name
                )
                receiveBuilder.beginControlFlow("if(recvValue != clearTextValue)")
                receiveBuilder.addStatement(
                    "throw %T(%S)",
                    runtimeErrorClassName,
                    "equivocation·error·between·hosts:·" + receivingHost.asDocument.print() + ",·" + host.asDocument.print()
                )
                receiveBuilder.endControlFlow()
            }
            receiveBuilder.addStatement(
                "var %L = clearTextValue",
                sender.temporary.value.name
            )
        }
        return receiveBuilder.build()
    }
}
