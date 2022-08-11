package io.github.apl_cornell.viaduct.circuitcodegeneration

import com.squareup.kotlinpoet.ARRAY
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import io.github.apl_cornell.viaduct.syntax.Arguments
import io.github.apl_cornell.viaduct.syntax.Host
import io.github.apl_cornell.viaduct.syntax.Protocol
import io.github.apl_cornell.viaduct.syntax.circuit.ArrayType
import io.github.apl_cornell.viaduct.syntax.circuit.CircuitDeclarationNode
import io.github.apl_cornell.viaduct.syntax.circuit.CircuitStatementNode
import io.github.apl_cornell.viaduct.syntax.circuit.ExpressionNode
import io.github.apl_cornell.viaduct.syntax.circuit.LetNode
import io.github.apl_cornell.viaduct.syntax.circuit.LiteralNode
import io.github.apl_cornell.viaduct.syntax.circuit.LookupNode
import io.github.apl_cornell.viaduct.syntax.circuit.OperatorApplicationNode
import io.github.apl_cornell.viaduct.syntax.circuit.ReduceNode
import io.github.apl_cornell.viaduct.syntax.circuit.ReferenceNode
import io.github.apl_cornell.viaduct.syntax.circuit.Variable
import io.github.apl_cornell.viaduct.syntax.circuit.VariableNode
import io.github.apl_cornell.viaduct.syntax.types.ValueType
import io.github.apl_cornell.viaduct.syntax.values.Value

abstract class AbstractCodeGenerator(val context: CodeGeneratorContext) : CodeGenerator {
//    private val nameAnalysis = NameAnalysis.get(context.program)
//    private val typeAnalysis = TypeAnalysis.get(context.program)

    override fun kotlinType(protocol: Protocol, sourceType: ValueType): TypeName = typeTranslator(sourceType)

    override fun kotlinType(protocol: Protocol, sourceType: ArrayType): TypeName =
        if (sourceType.shape.isEmpty()) kotlinType(protocol, sourceType.elementType.value)
        else ARRAY.parameterizedBy(
            kotlinType(
                protocol,
                ArrayType(
                    sourceType.elementType,
                    Arguments(sourceType.shape.subList(1, sourceType.shape.size), sourceType.shape.sourceLocation)
                )
            )
        )

    override fun circuitBody(protocol: Protocol, host: Host, circuitDeclaration: CircuitDeclarationNode): CodeBlock {
        val builder = CodeBlock.builder()
        for (stmt in circuitDeclaration.body) {
            generate(protocol, builder, host, stmt)
        }
        for (ret in circuitDeclaration.body.returnStatement.values.withIndex()) {
            val outParam = circuitDeclaration.outputs[ret.index]
            val returnName = context.kotlinName(outParam.name.value)
            if (outParam.type.value.shape.isEmpty()) builder.addStatement(
                "%N.set(%L)", returnName, exp(protocol, ret.value)
            )
            else builder.addStatement("%N = %L", returnName, exp(protocol, ret.value))
        }
        return builder.build()
    }

//    override fun send(
//        sender: LetNode, sendProtocol: Protocol, receiveProtocol: Protocol, events: ProtocolCommunication
//    ): CodeBlock = CodeBlock.of("send (temporary fix)") // TODO remove this and change communication node structure
//
//    override fun receive(
//        sender: LetNode, sendProtocol: Protocol, receiveProtocol: Protocol, events: ProtocolCommunication
//    ): CodeBlock = CodeBlock.of("receive (temporary fix)")

    private fun generate(protocol: Protocol, builder: CodeBlock.Builder, host: Host, stmt: CircuitStatementNode) {
        print("Ignore $host") // TODO remove this, just used to make the compiler stop complaining
        when (stmt) {
            is LetNode -> {
                val lhs: CodeBlock
                if (stmt.indices.isEmpty()) {
                    lhs = CodeBlock.of("val %N", context.kotlinName(stmt.name.value))
                } else {
                    val name = context.kotlinName(stmt.name.value)
                    // Declare and initialize target array
                    val arrayDecl = CodeBlock.builder()
                    arrayDecl.add("val %N = ", name)
                    for (i in 0 until stmt.indices.size) {
                        arrayDecl.add("%T(%L){ ", Array::class, exp(protocol, stmt.indices[i].bound))
                        if (i == stmt.indices.size - 1) {
                            arrayDecl.add("%L", 0) // TODO Use type analysis to put ValueType's default value here
                        }
                    }
                    for (i in 0 until stmt.indices.size) {
                        arrayDecl.add(" }")
                    }
                    builder.addStatement(arrayDecl.build().toString())
                    // Generate left-hand side of array let
                    val lhsBuilder = CodeBlock.builder()
                    lhsBuilder.add("%N", name)
                    for (i in 0 until stmt.indices.size) {
                        val ind = context.kotlinName(stmt.indices[i].name.value)
                        lhsBuilder.add("[$ind]")
                    }
                    lhs = lhsBuilder.build()
                }

                for (i in 0 until stmt.indices.size) {
                    val name = context.kotlinName(stmt.indices[i].name.value)
                    val bound = exp(protocol, stmt.indices[i].bound)
                    builder.beginControlFlow("for ($name in 0 until $bound)")
                }
                builder.addStatement("%L = %L", lhs, exp(protocol, stmt.value))
                for (i in 0 until stmt.indices.size) {
                    builder.endControlFlow()
                }
            }
        }
    }

    open fun exp(protocol: Protocol, expr: ExpressionNode): CodeBlock = when (expr) {
        is LiteralNode -> {
            value(expr.value)
        }
        is ReferenceNode -> {
            CodeBlock.of("%N", context.kotlinName(expr.name.value))
        }
        is LookupNode -> {
            if (expr.indices.isEmpty()) {
                CodeBlock.of("%N", context.kotlinName(expr.variable.value))
            } else {
                val builder = CodeBlock.builder()
                builder.add("%N", context.kotlinName(expr.variable.value))
                for (i in 0 until expr.indices.size) {
                    builder.add("[%L]", exp(protocol, expr.indices[i]))
                }
                builder.build()
            }
        }
        is ReduceNode -> reduce(protocol, expr)
        is OperatorApplicationNode -> throw UnsupportedOperatorException(protocol, expr)
    }

    private fun reduce(protocol: Protocol, r: ReduceNode): CodeBlock {
        val loc = r.sourceLocation
        val acc = Variable("acc")
        val element = Variable("element")
        val accRef = ReferenceNode(VariableNode(acc, loc), loc)
        val elemRef = ReferenceNode(VariableNode(element, loc), loc)
        return CodeBlock.of(
            "(0 until %L).%M { %N -> %L }.%M(%L) { %N, %N -> %L }",
            exp(protocol, r.indices.bound),
            MemberName("kotlin.collections", "map"),
            context.kotlinName(r.indices.name.value),
            exp(protocol, r.body),
            MemberName("kotlin.collections", "fold"),
            exp(protocol, r.defaultValue),
            context.kotlinName(acc),
            context.kotlinName(element),
            exp(
                protocol,
                OperatorApplicationNode(
                    r.operator.operator, Arguments(listOf(accRef, elemRef), loc), loc
                )
            )
        )
    }

    fun value(value: Value): CodeBlock = CodeBlock.of("%L", value)

    override fun setup(protocol: Protocol): Iterable<PropertySpec> = listOf()
}
