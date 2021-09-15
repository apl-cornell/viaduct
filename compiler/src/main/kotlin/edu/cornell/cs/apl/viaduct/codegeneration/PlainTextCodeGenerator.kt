package edu.cornell.cs.apl.viaduct.codegeneration

import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.U_BYTE_ARRAY
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
import edu.cornell.cs.apl.viaduct.syntax.types.ByteVecType
import edu.cornell.cs.apl.viaduct.syntax.types.ImmutableCellType
import edu.cornell.cs.apl.viaduct.syntax.types.IntegerType
import edu.cornell.cs.apl.viaduct.syntax.types.MutableCellType
import edu.cornell.cs.apl.viaduct.syntax.types.StringType
import edu.cornell.cs.apl.viaduct.syntax.types.UnitType
import edu.cornell.cs.apl.viaduct.syntax.types.ValueType
import edu.cornell.cs.apl.viaduct.syntax.types.VectorType
import edu.cornell.cs.apl.viaduct.syntax.values.BooleanValue
import edu.cornell.cs.apl.viaduct.syntax.values.IntegerValue
import edu.cornell.cs.apl.viaduct.syntax.values.StringValue
import edu.cornell.cs.apl.viaduct.syntax.values.UnitValue
import edu.cornell.cs.apl.viaduct.syntax.values.Value

class PlainTextCodeGenerator(
    val context: CodeGeneratorContext
) : AbstractCodeGenerator() {
    private val typeAnalysis = TypeAnalysis.get(context.program)
    private val nameAnalysis = NameAnalysis.get(context.program)
    private val protocolAnalysis = ProtocolAnalysis(context.program, SimpleProtocolComposer)
    private val runtimeErrorClass = RuntimeError::class
    private val booleanValueClass = BooleanValue::class
    private val integerValueClass = IntegerValue::class
    private val stringValueClass = StringValue::class
    private val unitValueClass = UnitValue::class
    private val byteVecValueClass = ByteArray::class

    private fun exp(expr: ExpressionNode): CodeBlock =
        when (expr) {
            is LiteralNode -> CodeBlock.of(expr.value.toString())

            is ReadNode ->
                CodeBlock.of(
                    context.kotlinName(
                        expr.temporary.value,
                        protocolAnalysis.primaryProtocol(expr)
                    )
                )

            is OperatorApplicationNode -> {
                if (expr.operator == Minimum) {
                    CodeBlock.of(
                        "%M(%L, %L)",
                        MemberName("kotlin.math", "min"),
                        exp(expr.arguments[0]),
                        exp(expr.arguments[1])
                    )
                } else if (expr.operator == Maximum) {
                    CodeBlock.of(
                        "%M(%L, %L)",
                        MemberName("kotlin.math", "max"),
                        exp(expr.arguments[0]),
                        exp(expr.arguments[1])
                    )
                } else {
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
            }

            is QueryNode ->
                when (this.typeAnalysis.type(nameAnalysis.declaration(expr))) {
                    is VectorType -> {
                        when (expr.query.value) {
                            is Get -> CodeBlock.of(
                                context.kotlinName(expr.variable.value) + "[" +
                                    exp(expr.arguments.first()) + "]"
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

            is DowngradeNode -> exp(expr.expression)

            is InputNode ->
                CodeBlock.of(
                    "(runtime.input(%T) as %T).value",
                    expr.type.value::class,
                    expr.type.value.defaultValue::class
                )

            is ReceiveNode -> TODO()
        }

    override fun let(protocol: Protocol, stmt: LetNode): CodeBlock =
        CodeBlock.of(
            "val %L = %L",
            context.kotlinName(stmt.temporary.value, protocolAnalysis.primaryProtocol(stmt)),
            exp(stmt.value)
        )

    private fun defaultTranslator(value: Value): Any =
        when (value) {
            is IntegerValue -> 0
            is BooleanValue -> false
            is StringValue -> ""
            else -> throw CodeGenerationError("unsupported array initialization")
            // do we need any of the following?
            // is HostSetValue ->
            // is HostValue ->
            // is ByteVecValue -> ByteArray(0)
        }

    private fun declarationHelper(
        name: String,
        className: ClassNameNode,
        arguments: Arguments<AtomicExpressionNode>,
        initType: ValueType
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

            Vector -> {
                CodeBlock.of(
                    "val %N= Array(%L){ %L }",
                    name,
                    exp(arguments.first()),
                    defaultTranslator(initType.defaultValue)
                )
            }

            else -> TODO("throw error")
        }

    override fun declaration(protocol: Protocol, stmt: DeclarationNode): CodeBlock =
        declarationHelper(
            context.kotlinName(stmt.name.value),
            stmt.className,
            stmt.arguments,
            stmt.typeArguments[0].value
        )

    override fun update(protocol: Protocol, stmt: UpdateNode): CodeBlock =
        when (this.typeAnalysis.type(nameAnalysis.declaration(stmt))) {
            is VectorType ->
                when (stmt.update.value) {
                    is edu.cornell.cs.apl.viaduct.syntax.datatypes.Set ->
                        CodeBlock.of(
                            "%N[%L] = %L",
                            context.kotlinName(stmt.variable.value),
                            exp(stmt.arguments[0]),
                            exp(stmt.arguments[1])
                        )

                    is Modify ->
                        CodeBlock.of(
                            "%N[%L] %L %L",
                            context.kotlinName(stmt.variable.value),
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
                            context.kotlinName(stmt.variable.value),
                            exp(stmt.arguments[0])
                        )

                    is Modify ->
                        CodeBlock.of(
                            "%N %L %L",
                            context.kotlinName(stmt.variable.value),
                            stmt.update.value.name,
                            exp(stmt.arguments[0])
                        )

                    else -> throw CodeGenerationError("unknown update", stmt)
                }

            else -> throw CodeGenerationError("unknown object to update", stmt)
        }

    override fun outParameterInitialization(
        protocol: Protocol,
        stmt: OutParameterInitializationNode
    ):
        CodeBlock =
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
        }

    override fun output(protocol: Protocol, stmt: OutputNode): CodeBlock {
        val valueClassNames =
            when (typeAnalysis.type(stmt.message)) {
                is BooleanType -> Pair(booleanValueClass, Boolean::class)
                is IntegerType -> Pair(integerValueClass, Int::class)
                is StringType -> Pair(stringValueClass, String::class)
                is ByteVecType -> Pair(byteVecValueClass, Array<Byte>::class)
                is UnitType -> Pair(unitValueClass, Unit::class)
                else -> throw CodeGenerationError("unknown output value", stmt)
            }

        return CodeBlock.of(
            "runtime.output(%T(%L as %T))",
            valueClassNames.first,
            exp(stmt.message),
            valueClassNames.second
        )
    }

    override fun guard(protocol: Protocol, expr: AtomicExpressionNode): CodeBlock = exp(expr)

    private fun typeTranslator(viaductType: ValueType): TypeName =
        when (viaductType) {
            ByteVecType -> U_BYTE_ARRAY
            BooleanType -> BOOLEAN
            IntegerType -> INT
            StringType -> STRING
            else -> throw CodeGenerationError("unknown send and receive type")
        }

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
                    sendBuilder.addStatement("%L", context.send(exp(sender.value), event.recv.host))
                }
            }
        }
        return sendBuilder.build()
    }

    // note - this implementation does not support commitments
    override fun receive(
        receivingHost: Host,
        sender: LetNode,
        sendProtocol: Protocol,
        receiveProtocol: Protocol,
        events: ProtocolCommunication
    ): CodeBlock {
        val receiveBuilder = CodeBlock.builder()
        if (sendProtocol != receiveProtocol) {
            val projection = ProtocolProjection(receiveProtocol, receivingHost)
            var cleartextInputs = events.getProjectionReceives(projection, Plaintext.INPUT)
            val clearTextTemp = context.newTemporary("clearTextTemp")

            // initialize cleartext receive value by receiving from first host
            if (cleartextInputs.isNotEmpty()) {
                receiveBuilder.addStatement(
                    "val %N = %L",
                    clearTextTemp,
                    context.receive(
                        typeTranslator(typeAnalysis.type(sender)),
                        cleartextInputs.first().send.host
                    )
                )
                cleartextInputs = cleartextInputs.minusElement(cleartextInputs.first())
            }

            // receive from the rest of the hosts and compare against clearTextValue
            for (event in cleartextInputs) {

                // check to make sure that you got the same data from all hosts
                receiveBuilder.beginControlFlow(
                    "if(%N != %L)",
                    clearTextTemp,
                    context.receive(
                        typeTranslator(typeAnalysis.type(sender)),
                        event.send.host
                    )
                )
                receiveBuilder.addStatement(
                    "throw %T(%S)",
                    runtimeErrorClass,
                    "Plaintext : received different values"
                )
                receiveBuilder.endControlFlow()
            }

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

            for (host in hostsToCheckWith)
                receiveBuilder.addStatement("%L", context.send(CodeBlock.of(clearTextTemp), host))

            val receiveTmp = context.newTemporary("receiveTmp")

            // start equivocation check by receiving from host in [hostsToCheckWith]
            if (hostsToCheckWith.isNotEmpty()) {
                receiveBuilder.addStatement(
                    "var %N = %L",
                    receiveTmp,
                    context.receive(
                        typeTranslator(typeAnalysis.type(sender)),
                        hostsToCheckWith.first()
                    )
                )
                receiveBuilder.beginControlFlow(
                    "if(%N != %N)",
                    receiveTmp,
                    clearTextTemp
                )
                receiveBuilder.addStatement(
                    "throw %T(%S)",
                    runtimeErrorClass,
                    "equivocation error between hosts: " + receivingHost.asDocument.print() + ", " +
                        hostsToCheckWith.first().asDocument.print()
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
                        typeTranslator(typeAnalysis.type(sender)),
                        host
                    )
                )
                receiveBuilder.beginControlFlow(
                    "if(%N != %N)",
                    receiveTmp,
                    clearTextTemp
                )
                receiveBuilder.addStatement(
                    "throw %T(%S)",
                    runtimeErrorClass,
                    "equivocation error between hosts: " + receivingHost.asDocument.print() + ", " +
                        host.asDocument.print()
                )
                receiveBuilder.endControlFlow()
            }
            receiveBuilder.addStatement(
                "val %N = %N",
                context.kotlinName(sender.temporary.value, protocolAnalysis.primaryProtocol(sender)),
                clearTextTemp
            )
        }
        return receiveBuilder.build()
    }
}
