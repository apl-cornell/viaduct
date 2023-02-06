package io.github.apl_cornell.viaduct.circuitcodegeneration

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.joinToCode
import io.github.apl_cornell.viaduct.syntax.Protocol
import io.github.apl_cornell.viaduct.syntax.circuit.ArrayTypeNode
import io.github.apl_cornell.viaduct.syntax.circuit.CircuitDeclarationNode
import io.github.apl_cornell.viaduct.syntax.circuit.CircuitLetNode
import io.github.apl_cornell.viaduct.syntax.circuit.CircuitStatementNode
import io.github.apl_cornell.viaduct.syntax.circuit.ExpressionNode
import io.github.apl_cornell.viaduct.syntax.circuit.LiteralNode
import io.github.apl_cornell.viaduct.syntax.circuit.LookupNode
import io.github.apl_cornell.viaduct.syntax.circuit.OperatorApplicationNode
import io.github.apl_cornell.viaduct.syntax.circuit.OperatorNode
import io.github.apl_cornell.viaduct.syntax.circuit.ReduceNode
import io.github.apl_cornell.viaduct.syntax.circuit.ReferenceNode
import io.github.apl_cornell.viaduct.syntax.types.ValueType

abstract class AbstractCodeGenerator(val context: CodeGeneratorContext) : CodeGenerator {
    override fun paramType(protocol: Protocol, sourceType: ValueType): TypeName = typeTranslator(sourceType)

    override fun storageType(protocol: Protocol, sourceType: ValueType): TypeName = typeTranslator(sourceType)

    fun paramType(protocol: Protocol, sourceType: ArrayTypeNode): TypeName =
        kotlinType(sourceType.shape, paramType(protocol, sourceType.elementType.value))

    fun storageType(protocol: Protocol, sourceType: ArrayTypeNode): TypeName =
        kotlinType(sourceType.shape, storageType(protocol, sourceType.elementType.value))

    override fun circuitBody(protocol: Protocol, circuitDeclaration: CircuitDeclarationNode): CodeBlock {
        val builder = CodeBlock.builder()
        for (stmt in circuitDeclaration.body) {
            generate(protocol, builder, stmt)
        }
        circuitDeclaration.body.returnStatement.values.forEachIndexed { index, value ->
            builder.addStatement(
                "%N.set(%L)",
                context.kotlinName(circuitDeclaration.outputs[index].name.value),
                indexExpression(value, context)
            )
        }
        return builder.build()
    }

    private fun generate(protocol: Protocol, builder: CodeBlock.Builder, stmt: CircuitStatementNode) {
        when (stmt) {
            is CircuitLetNode -> {
                val rhsBuilder = CodeBlock.builder()
                for (indexParameter in stmt.indices) {
                    rhsBuilder.add(
                        "%T(%L){ %N -> ",
                        Array::class,
                        indexExpression(indexParameter.bound, context),
                        context.kotlinName(indexParameter.name.value)
                    )
                }
                rhsBuilder.add("%L", exp(protocol, stmt.value))
                repeat(stmt.indices.size) { builder.add(" }") }
                builder.addStatement("val %N = %L", context.kotlinName(stmt.name.value), rhsBuilder.build())
            }
        }
    }

    open fun exp(protocol: Protocol, expr: ExpressionNode): CodeBlock = when (expr) {
        is LiteralNode -> {
            CodeBlock.of("%L", expr.value)
        }
        is ReferenceNode -> {
            CodeBlock.of("%N", context.kotlinName(expr.name.value))
        }
        is LookupNode -> {
            if (expr.indices.isEmpty()) {
                CodeBlock.of("%N", context.kotlinName(expr.variable.value))
            } else {
                CodeBlock.of(
                    "%N%L",
                    context.kotlinName(expr.variable.value),
                    expr.indices.map { CodeBlock.of("[%L]", exp(protocol, it)) }.joinToCode(separator = "")
                )
            }
        }
        is ReduceNode -> reduce(protocol, expr)
        is OperatorApplicationNode -> CodeBlock.of(
            "(%L)",
            operatorApplication(protocol, expr.operator, expr.arguments.map { exp(protocol, it) })
        )
    }

    open fun operatorApplication(protocol: Protocol, op: OperatorNode, arguments: List<CodeBlock>): CodeBlock =
        throw UnsupportedOperatorException(protocol, op)

    private fun reduce(protocol: Protocol, r: ReduceNode): CodeBlock {
        val acc = context.newTemporary("acc")
        val element = context.newTemporary("element")
        return CodeBlock.of(
            "(0 until %L).%M { %N -> %L }.%M(%L) { %N, %N -> %L }",
            exp(protocol, r.indices.bound),
            MemberName("kotlin.collections", "map"),
            context.kotlinName(r.indices.name.value),
            exp(protocol, r.body),
            MemberName("kotlin.collections", "fold"),
            exp(protocol, r.defaultValue),
            acc,
            element,
            operatorApplication(
                protocol,
                r.operator,
                listOf(CodeBlock.of(acc), CodeBlock.of(element))
            )
        )
    }

    override fun setup(protocol: Protocol): Iterable<PropertySpec> = listOf()
}
