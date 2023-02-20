package io.github.aplcornell.viaduct.circuitcodegeneration

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.joinToCode
import io.github.aplcornell.viaduct.syntax.Protocol
import io.github.aplcornell.viaduct.syntax.circuit.ArrayTypeNode
import io.github.aplcornell.viaduct.syntax.circuit.CircuitDeclarationNode
import io.github.aplcornell.viaduct.syntax.circuit.CircuitLetNode
import io.github.aplcornell.viaduct.syntax.circuit.CircuitStatementNode
import io.github.aplcornell.viaduct.syntax.circuit.ExpressionNode
import io.github.aplcornell.viaduct.syntax.circuit.LiteralNode
import io.github.aplcornell.viaduct.syntax.circuit.LookupNode
import io.github.aplcornell.viaduct.syntax.circuit.OperatorApplicationNode
import io.github.aplcornell.viaduct.syntax.circuit.OperatorNode
import io.github.aplcornell.viaduct.syntax.circuit.ReduceNode
import io.github.aplcornell.viaduct.syntax.circuit.ReferenceNode
import io.github.aplcornell.viaduct.syntax.types.ValueType

abstract class AbstractCodeGenerator(val context: CodeGeneratorContext) : CodeGenerator {
    override fun paramType(protocol: Protocol, sourceType: ValueType): TypeName = typeTranslator(sourceType)

    override fun storageType(protocol: Protocol, sourceType: ValueType): TypeName = typeTranslator(sourceType)

    fun paramType(protocol: Protocol, sourceType: ArrayTypeNode): TypeName =
        kotlinType(sourceType.shape, paramType(protocol, sourceType.elementType.value))

    fun storageType(protocol: Protocol, sourceType: ArrayTypeNode): TypeName =
        kotlinType(sourceType.shape, storageType(protocol, sourceType.elementType.value))

    override fun circuitBody(
        protocol: Protocol,
        circuitDeclaration: CircuitDeclarationNode,
        outParams: List<CodeBlock>,
    ): CodeBlock {
        val builder = CodeBlock.builder()
        for (stmt in circuitDeclaration.body) {
            generate(protocol, builder, stmt)
        }
        circuitDeclaration.body.returnStatement.values.forEachIndexed { index, value ->
            builder.addStatement(
                "%L.set(%L)",
                outParams[index],
                indexExpression(value, context),
            )
        }
        return builder.build()
    }

    private fun generate(protocol: Protocol, builder: CodeBlock.Builder, stmt: CircuitStatementNode) {
        when (stmt) {
            is CircuitLetNode -> {
                val rhsBuilder = CodeBlock.builder()
                for (indexParameter in stmt.indices) {
                    rhsBuilder.beginControlFlow(
                        "%T(%L){ %N -> ",
                        Array::class,
                        indexExpression(indexParameter.bound, context),
                        context.kotlinName(indexParameter.name.value),
                    )
                }
                rhsBuilder.add("%L", exp(protocol, stmt.value))
                repeat(stmt.indices.size) { rhsBuilder.endControlFlow() }
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
            CodeBlock.of(
                "%N%L",
                context.kotlinName(expr.variable.value),
                expr.indices.map { CodeBlock.of("[%L]", indexExpression(it, context)) }.joinToCode(separator = ""),
            )
        }

        is ReduceNode -> reduce(protocol, expr)
        is OperatorApplicationNode -> CodeBlock.of(
            "(%L)",
            operatorApplication(protocol, expr.operator, expr.arguments.map { exp(protocol, it) }),
        )
    }

    open fun operatorApplication(protocol: Protocol, op: OperatorNode, arguments: List<CodeBlock>): CodeBlock =
        throw UnsupportedOperatorException(protocol, op)

    private fun reduce(protocol: Protocol, r: ReduceNode): CodeBlock {
        val builder = CodeBlock.builder()
        builder.beginControlFlow(
            "if (%L <= 0) %L else",
            indexExpression(r.indices.bound, context),
            exp(protocol, r.defaultValue),
        )
        val acc = context.newTemporary("acc")
        val element = context.newTemporary("element")
        builder.beginControlFlow(
            "%T(%L) { %L ->",
            Array::class,
            indexExpression(r.indices.bound, context),
            context.kotlinName(r.indices.name.value),
        )
        builder.add(exp(protocol, r.body))
        builder.endControlFlow()
        builder.beginControlFlow(
            ".%M { %N, %N ->",
            MemberName("kotlin.collections", "reduce"),
            acc,
            element,
        )
        builder.add(
            operatorApplication(
                protocol,
                r.operator,
                listOf(CodeBlock.of(acc), CodeBlock.of(element)),
            ),
        )
        builder.endControlFlow()
        builder.endControlFlow()
        return builder.build()
    }

    override fun setup(protocol: Protocol): Iterable<PropertySpec> = listOf()
}
