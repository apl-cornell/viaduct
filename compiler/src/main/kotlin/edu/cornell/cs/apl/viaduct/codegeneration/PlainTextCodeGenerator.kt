package edu.cornell.cs.apl.viaduct.codegeneration

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.asClassName
import edu.cornell.cs.apl.viaduct.analysis.NameAnalysis
import edu.cornell.cs.apl.viaduct.analysis.ProtocolAnalysis
import edu.cornell.cs.apl.viaduct.analysis.TypeAnalysis
import edu.cornell.cs.apl.viaduct.errors.CodeGenerationError
import edu.cornell.cs.apl.viaduct.errors.RuntimeError
import edu.cornell.cs.apl.viaduct.protocols.Plaintext
import edu.cornell.cs.apl.viaduct.selection.CommunicationEvent
import edu.cornell.cs.apl.viaduct.selection.ProtocolCommunication
import edu.cornell.cs.apl.viaduct.selection.SimpleProtocolComposer
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
import edu.cornell.cs.apl.viaduct.syntax.intermediate.QueryNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReadNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReceiveNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.UpdateNode
import edu.cornell.cs.apl.viaduct.syntax.operators.Maximum
import edu.cornell.cs.apl.viaduct.syntax.operators.Minimum
import edu.cornell.cs.apl.viaduct.syntax.types.BooleanType
import edu.cornell.cs.apl.viaduct.syntax.types.ImmutableCellType
import edu.cornell.cs.apl.viaduct.syntax.types.IntegerType
import edu.cornell.cs.apl.viaduct.syntax.types.MutableCellType
import edu.cornell.cs.apl.viaduct.syntax.types.StringType
import edu.cornell.cs.apl.viaduct.syntax.types.UnitType
import edu.cornell.cs.apl.viaduct.syntax.types.VectorType
import edu.cornell.cs.apl.viaduct.syntax.values.BooleanValue
import edu.cornell.cs.apl.viaduct.syntax.values.ByteVecValue
import edu.cornell.cs.apl.viaduct.syntax.values.IntegerValue
import edu.cornell.cs.apl.viaduct.syntax.values.StringValue
import edu.cornell.cs.apl.viaduct.syntax.values.UnitValue

class PlainTextCodeGenerator(
    context: CodeGeneratorContext
) : AbstractCodeGenerator() {
    private val typeAnalysis = TypeAnalysis.get(context.program)
    private val nameAnalysis = NameAnalysis.get(context.program)
    private val protocolAnalysis = ProtocolAnalysis(context.program, SimpleProtocolComposer)
    private val codeGeneratorContext = context
    private val runtimeErrorClassName = RuntimeError::class.asClassName()
    private val booleanValueClassName = BooleanValue::class.asClassName()
    private val integerValueClassName = IntegerValue::class.asClassName()
    private val stringValueClassName = StringValue::class.asClassName()
    private val byteVecValueClassName = ByteVecValue::class.asClassName()
    private val unitValueClassName = UnitValue::class.asClassName()

    private fun exp(expr: ExpressionNode): CodeBlock =
        when (expr) {
            is LiteralNode -> CodeBlock.of(expr.value.toString())

            is ReadNode ->
                CodeBlock.of(codeGeneratorContext.kotlinName(expr.temporary.value, protocolAnalysis.primaryProtocol(expr)))

            is OperatorApplicationNode -> {
                when (expr.operator) {
                    is Minimum -> CodeBlock.of(
                        "%N(%L, %L)",
                        "min",
                        exp(expr.arguments[0]),
                        exp(expr.arguments[1])
                    )
                    is Maximum -> CodeBlock.of(
                        "%N(%L, %L)",
                        "max",
                        exp(expr.arguments[0]),
                        exp(expr.arguments[1])
                    )
                }
                when (expr.arguments.size) {
                    2 -> CodeBlock.of(
                        "%L %L %L",
                        exp(expr.arguments[0]),
                        expr.operator.toString(),
                        exp(expr.arguments[1])
                    )
                    1 -> CodeBlock.of(
                        "%L%L",
                        expr.operator.toString(),
                        exp(expr.arguments[0])
                    )
                    else -> throw CodeGenerationError("unknown operator", expr)
                }
            }

            is QueryNode ->
                when (this.typeAnalysis.type(nameAnalysis.declaration(expr))) {
                    is VectorType -> {
                        when (expr.query.value) {
                            is Get -> CodeBlock.of(codeGeneratorContext.kotlinName(expr.variable.value) + "[" + exp(expr.arguments.first()) + "]")
                            else -> throw CodeGenerationError("unknown vector query", expr)
                        }
                    }

                    is ImmutableCellType -> {
                        when (expr.query.value) {
                            is Get -> CodeBlock.of(codeGeneratorContext.kotlinName(expr.variable.value))
                            else -> throw CodeGenerationError("unknown query", expr)
                        }
                    }

                    is MutableCellType -> {
                        when (expr.query.value) {
                            is Get -> CodeBlock.of(codeGeneratorContext.kotlinName(expr.variable.value))
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
                CodeBlock.of("(runtime.input($viaductTypeClass) as $viaductValueClass).value")
            }

            is ReceiveNode -> TODO()
        }

    override fun Let(protocol: Protocol, stmt: LetNode): CodeBlock =
        CodeBlock.of(
            "val %L = %L",
            codeGeneratorContext.kotlinName(stmt.temporary.value, protocolAnalysis.primaryProtocol(stmt)),
            exp(stmt.value)
        )

    private fun declarationHelper(
        name: String,
        className: ClassNameNode,
        arguments: Arguments<AtomicExpressionNode>
    ): CodeBlock =
        when (className.value) {
            ImmutableCell -> CodeBlock.of(
                "val %N = %L",
                name,
                exp(arguments.first())
            )

            // TODO - change this (difference between viaduct, kotlin semantics)
            MutableCell -> CodeBlock.of(
                "var %N = %L",
                name,
                exp(arguments.first())
            )

            Vector -> CodeBlock.of(
                "val %N= Array(%L)",
                name,
                exp(arguments.first())
            )

            else -> TODO("throw error")
        }

    override fun Declaration(protocol: Protocol, stmt: DeclarationNode): CodeBlock =
        declarationHelper(codeGeneratorContext.kotlinName(stmt.name.value), stmt.className, stmt.arguments)

    override fun Update(protocol: Protocol, stmt: UpdateNode): CodeBlock =
        when (this.typeAnalysis.type(nameAnalysis.declaration(stmt))) {
            is VectorType ->
                when (stmt.update.value) {
                    is edu.cornell.cs.apl.viaduct.syntax.datatypes.Set ->
                        CodeBlock.of(
                            "%N[%L] = %L",
                            codeGeneratorContext.kotlinName(stmt.variable.value),
                            exp(stmt.arguments[0]),
                            exp(stmt.arguments[1])
                        )

                    is Modify ->
                        CodeBlock.of(
                            "%N[%L] %L %L",
                            codeGeneratorContext.kotlinName(stmt.variable.value),
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
                            "%N = %L",
                            codeGeneratorContext.kotlinName(stmt.variable.value),
                            exp(stmt.arguments[0])
                        )

                    is Modify ->
                        CodeBlock.of(
                            "%N %L %L",
                            codeGeneratorContext.kotlinName(stmt.variable.value),
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
            is OutParameterConstructorInitializerNode -> {
                val outTmpString = codeGeneratorContext.newTemporary("outTmp")
                CodeBlock.builder()
                    .add(
                        // declare object
                        declarationHelper(
                            outTmpString,
                            initializer.className,
                            initializer.arguments
                        )
                    )
                    .add(
                        // fill box with constructed object
                        CodeBlock.of(
                            "%N.set(%L)",
                            codeGeneratorContext.kotlinName(stmt.name.value),
                            outTmpString
                        )
                    )
                    .build()
            }
            // fill box named [stmt.name.value.name] with [initializer.expression]
            is OutParameterExpressionInitializerNode ->
                CodeBlock.of("%N.set(%L)",
                    codeGeneratorContext.kotlinName(stmt.name.value),
                    exp(initializer.expression)
                )
        }

    override fun Output(protocol: Protocol, stmt: OutputNode): CodeBlock {
        val valueClassNames =
            when (typeAnalysis.type(stmt.message)) {
                is BooleanType -> Pair(booleanValueClassName, Boolean::class.asClassName())
                is IntegerType -> Pair(integerValueClassName, Int::class.asClassName())
                is StringType -> Pair(stringValueClassName, String::class.asClassName())

                // TODO() - Do I have to convert the list of bytes to an array?
                // is ByteVecType -> Pair(byteVecValueClassName, Array<Byte>::class.asClassName())
                is UnitType -> Pair(unitValueClassName, Unit::class.asClassName())
                else -> throw CodeGenerationError("unknown output value", stmt)
            }

        return CodeBlock.of(
            "runtime.output(%L(%L as %L))",
            valueClassNames.first,
            exp(stmt.message),
            valueClassNames.second
        )
    }

    override fun Guard(protocol: Protocol, expr: AtomicExpressionNode): CodeBlock = exp(expr)

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
                    "runtime.send(%N, %N)",
                    codeGeneratorContext.kotlinName(sender.temporary.value, protocolAnalysis.primaryProtocol(sender)),
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
            val clearTextTemp = codeGeneratorContext.newTemporary("clearTextTemp")

            // initialize cleartext receive value by receiving from first host
            receiveBuilder.addStatement(
                "val %N = runtime.receive(%N)",
                clearTextTemp,
                cleartextInputs.first().send.host.name
            )
            cleartextInputs = cleartextInputs.minusElement(cleartextInputs.first())

            // receive from the rest of the hosts and compare against clearTextValue
            for (event in cleartextInputs) {

                // check to make sure that you got the same data from all hosts
                receiveBuilder.beginControlFlow(
                    "if(%N != runtime.receive(%L))",
                    clearTextTemp,
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
            receiveBuilder.beginControlFlow("if(%N == null)", clearTextTemp)
            receiveBuilder.addStatement(
                "throw %T(%S)",
                runtimeErrorClassName,
                "Plaintext : received null value"
            )
            receiveBuilder.endControlFlow()

            // calculate set of hosts with whom [receivingHost] needs to check for equivocation
            var hostsToCheckWith: Set<Host> =
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
                    "runtime.send(%N, %N)",
                    clearTextTemp,
                    host.name
                )
            }

            val receiveTmp = codeGeneratorContext.newTemporary("receiveTmp")

            // start equivocation check by receiving from host in [hostsToCheckWith]
            if (hostsToCheckWith.isNotEmpty()) {
                receiveBuilder.addStatement(
                    "var %N = runtime.receive(%N)",
                    receiveTmp,
                    hostsToCheckWith.first().name
                )
                receiveBuilder.beginControlFlow(
                    "if(%N != %N)",
                    receiveTmp,
                    clearTextTemp
                )
                receiveBuilder.addStatement(
                    "throw %T(%S)",
                    runtimeErrorClassName,
                    "equivocation·error·between·hosts:·" + receivingHost.asDocument.print() + ",·" + hostsToCheckWith.first().asDocument.print()
                )
                receiveBuilder.endControlFlow()
                hostsToCheckWith = hostsToCheckWith.minusElement(hostsToCheckWith.first())
            }

            // potentially receive from the rest of the hosts in [hostsToCheckWith]
            for (host in hostsToCheckWith) {
                receiveBuilder.addStatement(
                    "%N = runtime.receive(%N)",
                    receiveTmp,
                    host.name
                )
                receiveBuilder.beginControlFlow(
                    "if(%N != %N)",
                    receiveTmp,
                    clearTextTemp
                )
                receiveBuilder.addStatement(
                    "throw %T(%S)",
                    runtimeErrorClassName,
                    "equivocation·error·between·hosts:·" + receivingHost.asDocument.print() + ",·" + host.asDocument.print()
                )
                receiveBuilder.endControlFlow()
            }
            receiveBuilder.addStatement(
                "val %N = %N",
                codeGeneratorContext.kotlinName(sender.temporary.value, protocolAnalysis.primaryProtocol(sender)),
                clearTextTemp
            )
        }
        return receiveBuilder.build()
    }
}
