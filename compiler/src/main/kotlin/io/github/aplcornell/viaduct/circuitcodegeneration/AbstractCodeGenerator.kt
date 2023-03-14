package io.github.aplcornell.viaduct.circuitcodegeneration

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import io.github.aplcornell.viaduct.syntax.Arguments
import io.github.aplcornell.viaduct.syntax.Protocol
import io.github.aplcornell.viaduct.syntax.circuit.ArrayTypeNode
import io.github.aplcornell.viaduct.syntax.circuit.CircuitDeclarationNode
import io.github.aplcornell.viaduct.syntax.circuit.CircuitLetNode
import io.github.aplcornell.viaduct.syntax.circuit.CircuitStatementNode
import io.github.aplcornell.viaduct.syntax.circuit.ExpressionNode
import io.github.aplcornell.viaduct.syntax.circuit.IndexExpressionNode
import io.github.aplcornell.viaduct.syntax.circuit.LookupNode
import io.github.aplcornell.viaduct.syntax.circuit.OperatorApplicationNode
import io.github.aplcornell.viaduct.syntax.circuit.OperatorNode
import io.github.aplcornell.viaduct.syntax.circuit.ReduceNode
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
                builder.addStatement(
                    "val %N = %L",
                    context.kotlinName(stmt.name.value),
                    stmt.indices.new(context, exp(protocol, stmt.value)),
                )
            }
        }
    }

    open fun exp(protocol: Protocol, expr: ExpressionNode): CodeBlock = when (expr) {
        is IndexExpressionNode -> indexExpression(expr, context)

        is LookupNode -> {
            CodeBlock.of("%N", context.kotlinName(expr.variable.value))
                .lookup(expr.indices.map { indexExpression(it, context) })
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
        builder.add(Arguments(listOf(r.indices), r.indices.bound.sourceLocation).new(context, exp(protocol, r.body)))
        val left = context.newTemporary("left")
        val right = context.newTemporary("right")
        builder.beginControlFlow(
            ".%M { %N, %N ->",
            MemberName("kotlin.collections", "reduceOrNull"),
            left,
            right,
        )
        builder.add(
            operatorApplication(
                protocol,
                r.operator,
                listOf(CodeBlock.of(left), CodeBlock.of(right)),
            ),
        )
        builder.endControlFlow()
        builder.add("?: %L", exp(protocol, r.defaultValue))
        return builder.build()
    }

    override fun setup(protocol: Protocol): Iterable<PropertySpec> = listOf()
}
