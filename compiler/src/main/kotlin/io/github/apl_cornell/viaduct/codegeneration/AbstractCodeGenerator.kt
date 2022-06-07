package io.github.apl_cornell.viaduct.codegeneration

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.PropertySpec
import io.github.apl_cornell.viaduct.analysis.NameAnalysis
import io.github.apl_cornell.viaduct.analysis.TypeAnalysis
import io.github.apl_cornell.viaduct.syntax.Protocol
import io.github.apl_cornell.viaduct.syntax.datatypes.Get
import io.github.apl_cornell.viaduct.syntax.datatypes.ImmutableCell
import io.github.apl_cornell.viaduct.syntax.datatypes.MutableCell
import io.github.apl_cornell.viaduct.syntax.datatypes.Vector
import io.github.apl_cornell.viaduct.syntax.intermediate.AtomicExpressionNode
import io.github.apl_cornell.viaduct.syntax.intermediate.DeclarationNode
import io.github.apl_cornell.viaduct.syntax.intermediate.DowngradeNode
import io.github.apl_cornell.viaduct.syntax.intermediate.ExpressionNode
import io.github.apl_cornell.viaduct.syntax.intermediate.LetNode
import io.github.apl_cornell.viaduct.syntax.intermediate.LiteralNode
import io.github.apl_cornell.viaduct.syntax.intermediate.OutParameterInitializationNode
import io.github.apl_cornell.viaduct.syntax.intermediate.OutputNode
import io.github.apl_cornell.viaduct.syntax.intermediate.QueryNode
import io.github.apl_cornell.viaduct.syntax.intermediate.ReadNode
import io.github.apl_cornell.viaduct.syntax.intermediate.SimpleStatementNode
import io.github.apl_cornell.viaduct.syntax.intermediate.UpdateNode
import io.github.apl_cornell.viaduct.syntax.types.ImmutableCellType
import io.github.apl_cornell.viaduct.syntax.types.MutableCellType
import io.github.apl_cornell.viaduct.syntax.types.VectorType
import io.github.apl_cornell.viaduct.syntax.values.Value

abstract class AbstractCodeGenerator(val context: CodeGeneratorContext) : CodeGenerator {
    private val nameAnalysis = NameAnalysis.get(context.program)
    private val typeAnalysis = TypeAnalysis.get(context.program)

    override fun guard(protocol: Protocol, expr: AtomicExpressionNode): CodeBlock =
        throw UnsupportedOperatorException(protocol, expr)

    final override fun simpleStatement(protocol: Protocol, stmt: SimpleStatementNode): CodeBlock {
        return when (stmt) {
            is LetNode -> let(protocol, stmt)

            is DeclarationNode -> declaration(protocol, stmt)

            is UpdateNode -> update(protocol, stmt)

            is OutParameterInitializationNode -> outParameterInitialization()

            is OutputNode -> output(protocol, stmt)
        }
    }

    fun value(value: Value): CodeBlock =
        CodeBlock.of("%L", value)

    open fun exp(protocol: Protocol, expr: ExpressionNode): CodeBlock =
        when (expr) {
            is ReadNode ->
                CodeBlock.of("%N", context.kotlinName(expr.temporary.value, protocol))

            is QueryNode ->
                when (typeAnalysis.type(nameAnalysis.declaration(expr))) {
                    is ImmutableCellType ->
                        when (expr.query.value) {
                            is Get -> CodeBlock.of("%N", context.kotlinName(expr.variable.value))
                            else -> throw UnsupportedOperatorException(protocol, expr)
                        }

                    is MutableCellType ->
                        when (expr.query.value) {
                            is Get -> CodeBlock.of("%N", context.kotlinName(expr.variable.value))
                            else -> throw UnsupportedOperatorException(protocol, expr)
                        }

                    is VectorType ->
                        when (expr.query.value) {
                            is Get ->
                                CodeBlock.of(
                                    "%N[%L]",
                                    context.kotlinName(expr.variable.value),
                                    cleartextExp(protocol, expr.arguments.first())
                                )
                            else -> throw UnsupportedOperatorException(protocol, expr)
                        }
                    else -> throw UnsupportedOperatorException(protocol, expr)
                }

            is DowngradeNode ->
                exp(protocol, expr.expression)

            else -> throw UnsupportedOperatorException(protocol, expr)
        }

    fun cleartextExp(protocol: Protocol, expr: AtomicExpressionNode): CodeBlock =
        when (expr) {
            is LiteralNode ->
                value(expr.value)
            is ReadNode ->
                CodeBlock.of("%N", context.kotlinName(expr.temporary.value, protocol))
        }

    open fun let(protocol: Protocol, stmt: LetNode): CodeBlock =
        CodeBlock.of(
            "val %N = %L",
            context.kotlinName(stmt.name.value, protocol),
            exp(protocol, stmt.value)
        )

    fun declaration(protocol: Protocol, stmt: DeclarationNode): CodeBlock =
        when (stmt.objectType.className.value) {
            ImmutableCell -> CodeBlock.of(
                "val %N = %L",
                context.kotlinName(stmt.name.value),
                exp(protocol, stmt.arguments.first())
            )

            // TODO - change this (difference between viaduct, kotlin semantics)
            MutableCell -> CodeBlock.of(
                "var %N = %L",
                context.kotlinName(stmt.name.value),
                exp(protocol, stmt.arguments.first())
            )

            Vector -> {
                CodeBlock.of(
                    "val %N = %T(%L){ %L }",
                    context.kotlinName(stmt.name.value),
                    Array::class,
                    cleartextExp(protocol, stmt.arguments.first()),
                    exp(
                        protocol,
                        LiteralNode(
                            stmt.objectType.typeArguments[0].value.defaultValue,
                            stmt.objectType.typeArguments[0].sourceLocation
                        )
                    )
                )
            }

            else -> throw UnsupportedOperatorException(protocol, stmt)
        }

    open fun update(protocol: Protocol, stmt: UpdateNode): CodeBlock =
        when (typeAnalysis.type(nameAnalysis.declaration(stmt))) {
            is MutableCellType ->
                when (stmt.update.value) {
                    is io.github.apl_cornell.viaduct.syntax.datatypes.Set ->
                        CodeBlock.of(
                            "%N = %L",
                            context.kotlinName(stmt.variable.value),
                            exp(protocol, stmt.arguments[0])
                        )

                    else -> throw UnsupportedOperatorException(protocol, stmt)
                }

            is VectorType ->
                when (stmt.update.value) {
                    is io.github.apl_cornell.viaduct.syntax.datatypes.Set ->
                        CodeBlock.of(
                            "%N[%L] = %L",
                            context.kotlinName(stmt.variable.value),
                            cleartextExp(protocol, stmt.arguments[0]),
                            exp(protocol, stmt.arguments[1])
                        )

                    else -> throw UnsupportedOperatorException(protocol, stmt)
                }

            else -> throw UnsupportedOperatorException(protocol, stmt)
        }

    private fun outParameterInitialization(
/*        protocol: Protocol,
        stmt: OutParameterInitializationNode*/
    ): CodeBlock =
        TODO()
/*        when (val initializer = stmt.initializer) {
            is OutParameterConstructorInitializerNode -> {
                val outTmpString = context.newTemporary("outTmp")
                CodeBlock.builder()
                    .add(
                        // declare object
                        declarationHelper(
                            outTmpString,
                            initializer.className,
                            initializer.arguments,
                            value(initializer.typeArguments[0].value.defaultValue),
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
                    exp(protocol, initializer.expression)
                )
        }*/

    open fun output(protocol: Protocol, stmt: OutputNode): CodeBlock =
        throw UnsupportedOperatorException(protocol, stmt)

    override fun setup(protocol: Protocol): Iterable<PropertySpec> =
        listOf()
}
