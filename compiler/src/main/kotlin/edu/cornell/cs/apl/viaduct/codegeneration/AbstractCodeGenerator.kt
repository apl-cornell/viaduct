package edu.cornell.cs.apl.viaduct.codegeneration

import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.U_BYTE_ARRAY
import edu.cornell.cs.apl.viaduct.errors.CodeGenerationError
import edu.cornell.cs.apl.viaduct.errors.IllegalInternalCommunicationError
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
    override fun simpleStatement(protocol: Protocol, stmt: SimpleStatementNode): CodeBlock {
        return when (stmt) {
            is LetNode -> let(protocol, stmt)

            is DeclarationNode -> declaration(protocol, stmt)

            is UpdateNode -> update(protocol, stmt)

            is OutParameterInitializationNode -> outParameterInitialization(stmt)

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
        initFun: CodeBlock
    ): CodeBlock {
        return when (className.value) {
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

            // When you are a commitment creator, create a zero Committed, and send to bob
            // Array(size){val com = Committed(0) runtime.send(com.commitment()); com}
            // As a commitment receiver in an Array(size){runtime.recv(alice)}
            Vector -> {
                CodeBlock.of(
                    "val %N = Array(%L){ %L }",
                    name,
                    exp(arguments.first()),
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
