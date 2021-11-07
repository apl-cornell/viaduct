package edu.cornell.cs.apl.viaduct.codegeneration

import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.U_BYTE_ARRAY
import edu.cornell.cs.apl.viaduct.errors.CodeGenerationError
import edu.cornell.cs.apl.viaduct.errors.IllegalInternalCommunicationError
import edu.cornell.cs.apl.viaduct.errors.RuntimeError
import edu.cornell.cs.apl.viaduct.selection.CommunicationEvent
import edu.cornell.cs.apl.viaduct.syntax.Arguments
import edu.cornell.cs.apl.viaduct.syntax.ClassNameNode
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.datatypes.ImmutableCell
import edu.cornell.cs.apl.viaduct.syntax.datatypes.MutableCell
import edu.cornell.cs.apl.viaduct.syntax.datatypes.Vector
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AtomicExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutParameterConstructorInitializerNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutParameterExpressionInitializerNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutParameterInitializationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.SendNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.SimpleStatementNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.UpdateNode
import edu.cornell.cs.apl.viaduct.syntax.types.BooleanType
import edu.cornell.cs.apl.viaduct.syntax.types.ByteVecType
import edu.cornell.cs.apl.viaduct.syntax.types.IntegerType
import edu.cornell.cs.apl.viaduct.syntax.types.StringType
import edu.cornell.cs.apl.viaduct.syntax.types.ValueType
import edu.cornell.cs.apl.viaduct.syntax.values.Value

abstract class AbstractCodeGenerator(val context: CodeGeneratorContext) : CodeGenerator {
    val runtimeErrorClass = RuntimeError::class
    override fun simpleStatement(protocol: Protocol, stmt: SimpleStatementNode): CodeBlock {
        return when (stmt) {
            is LetNode -> let(protocol, stmt)

            is DeclarationNode -> declaration(protocol, stmt)

            is UpdateNode -> update(protocol, stmt)

            is OutParameterInitializationNode -> outParameterInitialization(stmt, protocol)

            is OutputNode -> output(protocol, stmt)

            is SendNode -> throw IllegalInternalCommunicationError(stmt)
        }
    }

    fun exp(value: Value): CodeBlock =
        CodeBlock.of(
            "%L",
            value
        )

    abstract fun exp(expr: ExpressionNode, protocol: Protocol): CodeBlock

    abstract fun let(protocol: Protocol, stmt: LetNode): CodeBlock

    abstract fun declaration(protocol: Protocol, stmt: DeclarationNode): CodeBlock

    abstract fun update(protocol: Protocol, stmt: UpdateNode): CodeBlock

    fun typeTranslator(viaductType: ValueType): TypeName =
        when (viaductType) {
            ByteVecType -> U_BYTE_ARRAY
            BooleanType -> BOOLEAN
            IntegerType -> INT
            StringType -> STRING
            else -> throw CodeGenerationError("unknown send and receive type")
        }

    fun declarationHelper(
        name: String,
        className: ClassNameNode,
        arguments: Arguments<AtomicExpressionNode>,
        initFun: CodeBlock,
        protocol: Protocol
    ): CodeBlock {
        return when (className.value) {
            ImmutableCell -> CodeBlock.of(
                "val %N = %L",
                name,
                exp(arguments.first(), protocol)
            )

            // TODO - change this (difference between viaduct, kotlin semantics)
            MutableCell -> CodeBlock.of(
                "var %N = %L",
                name,
                exp(arguments.first(), protocol)
            )

            Vector -> {
                CodeBlock.of(
                    "val %N = Array(%L){ %L }",
                    name,
                    exp(arguments.first(), protocol),
                    initFun
                )
            }

            else -> TODO("throw error")
        }
    }

    fun outParameterInitialization(
        stmt: OutParameterInitializationNode,
        protocol: Protocol
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
                            exp(initializer.typeArguments[0].value.defaultValue),
                            protocol
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
                    exp(initializer.expression, protocol)
                )
        }

    abstract fun output(protocol: Protocol, stmt: OutputNode): CodeBlock

    fun receiveHelper(
        sender: LetNode,
        sendProtocol: Protocol,
        events: Set<CommunicationEvent>,
        context: CodeGeneratorContext,
        clearTextTemp: String,
        errorMessage: String
    ): CodeBlock {
        var eventSet = events
        var receiveBuilder = CodeBlock.builder()

        if (eventSet.first().send.host == context.host) {
            receiveBuilder.addStatement(
                "val %N = %N",
                clearTextTemp,
                context.kotlinName(sender.temporary.value, sendProtocol)
            )
        } else {
            receiveBuilder.addStatement(
                "val %N = %L",
                clearTextTemp,
                context.receive(
                    typeTranslator(context.typeAnalysis.type(sender)),
                    eventSet.first().send.host
                )
            )
        }
        eventSet = eventSet.minusElement(eventSet.first())

        // receive from the rest of the hosts and compare against clearTextValue
        for (event in eventSet) {
            // check to make sure that you got the same data from all hosts
            val receiveVal = CodeBlock.builder()
            if (event.send.host == context.host) {
                receiveVal.add(context.kotlinName(sender.temporary.value, sendProtocol))
            } else {
                receiveVal.add(
                    context.receive(
                        typeTranslator(context.typeAnalysis.type(sender)),
                        event.send.host
                    )
                )
            }
            // check to make sure that you got the same data from all hosts
            receiveBuilder.beginControlFlow(
                "if (%N != %L)",
                clearTextTemp,
                receiveVal
            )
            receiveBuilder.addStatement(
                "throw %T(%S)",
                runtimeErrorClass,
                errorMessage
            )
            receiveBuilder.endControlFlow()
        }
        return receiveBuilder.build()
    }
}

abstract class SingleProtocolCodeGenerator(context: CodeGeneratorContext) : AbstractCodeGenerator(context) {

    abstract fun guard(expr: AtomicExpressionNode): CodeBlock

    override fun guard(protocol: Protocol, expr: AtomicExpressionNode): CodeBlock =
        guard(expr)

    abstract fun let(stmt: LetNode): CodeBlock

    override fun let(protocol: Protocol, stmt: LetNode): CodeBlock =
        let(stmt)

    abstract fun update(stmt: UpdateNode): CodeBlock

    override fun update(protocol: Protocol, stmt: UpdateNode): CodeBlock =
        update(stmt)

    abstract fun output(stmt: OutputNode): CodeBlock

    override fun output(protocol: Protocol, stmt: OutputNode): CodeBlock =
        output(stmt)
}
