package io.github.aplcornell.viaduct.codegeneration

import com.squareup.kotlinpoet.ARRAY
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
import io.github.aplcornell.viaduct.analysis.NameAnalysis
import io.github.aplcornell.viaduct.analysis.TypeAnalysis
import io.github.aplcornell.viaduct.runtime.Boxed
import io.github.aplcornell.viaduct.syntax.Arguments
import io.github.aplcornell.viaduct.syntax.ObjectTypeNode
import io.github.aplcornell.viaduct.syntax.Protocol
import io.github.aplcornell.viaduct.syntax.datatypes.Get
import io.github.aplcornell.viaduct.syntax.datatypes.ImmutableCell
import io.github.aplcornell.viaduct.syntax.datatypes.MutableCell
import io.github.aplcornell.viaduct.syntax.datatypes.Vector
import io.github.aplcornell.viaduct.syntax.intermediate.AtomicExpressionNode
import io.github.aplcornell.viaduct.syntax.intermediate.DowngradeNode
import io.github.aplcornell.viaduct.syntax.intermediate.ExpressionNode
import io.github.aplcornell.viaduct.syntax.intermediate.LiteralNode
import io.github.aplcornell.viaduct.syntax.intermediate.QueryNode
import io.github.aplcornell.viaduct.syntax.intermediate.ReadNode
import io.github.aplcornell.viaduct.syntax.intermediate.UpdateNode
import io.github.aplcornell.viaduct.syntax.types.ImmutableCellType
import io.github.aplcornell.viaduct.syntax.types.MutableCellType
import io.github.aplcornell.viaduct.syntax.types.ObjectType
import io.github.aplcornell.viaduct.syntax.types.ValueType
import io.github.aplcornell.viaduct.syntax.types.VectorType
import io.github.aplcornell.viaduct.syntax.values.Value

abstract class AbstractCodeGenerator(val context: CodeGeneratorContext) : CodeGenerator {
    private val nameAnalysis = context.program.analyses.get<NameAnalysis>()
    private val typeAnalysis = context.program.analyses.get<TypeAnalysis>()

    override fun kotlinType(protocol: Protocol, sourceType: ValueType): TypeName = typeTranslator(sourceType)

    override fun kotlinType(protocol: Protocol, sourceType: ObjectType): TypeName {
        return when (sourceType) {
            is ImmutableCellType -> {
                kotlinType(protocol, sourceType.elementType)
            }

            is MutableCellType -> {
                (Boxed::class).asTypeName().parameterizedBy(kotlinType(protocol, sourceType.elementType))
            }

            is VectorType -> {
                ARRAY.parameterizedBy(kotlinType(protocol, sourceType.elementType))
            }

            else -> {
                throw IllegalArgumentException(
                    "Cannot convert ${
                        sourceType.toDocument().print()
                    } to Kotlin type.",
                )
            }
        }
    }

    override fun guard(protocol: Protocol, expr: AtomicExpressionNode): CodeBlock =
        throw UnsupportedOperatorException(protocol, expr)

    fun value(value: Value): CodeBlock =
        CodeBlock.of("%L", value)

    fun cleartextExp(protocol: Protocol, expr: AtomicExpressionNode): CodeBlock =
        when (expr) {
            is LiteralNode ->
                value(expr.value)

            is ReadNode ->
                CodeBlock.of("%N", context.kotlinName(expr.temporary.value, protocol))
        }

    override fun exp(protocol: Protocol, expr: ExpressionNode): CodeBlock =
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
                            is Get -> CodeBlock.of("%N.get()", context.kotlinName(expr.variable.value))
                            else -> throw UnsupportedOperatorException(protocol, expr)
                        }

                    is VectorType ->
                        when (expr.query.value) {
                            is Get ->
                                CodeBlock.of(
                                    "%N[%L]",
                                    context.kotlinName(expr.variable.value),
                                    cleartextExp(protocol, expr.arguments.first()),
                                )

                            else -> throw UnsupportedOperatorException(protocol, expr)
                        }

                    else -> throw UnsupportedOperatorException(protocol, expr)
                }

            is DowngradeNode ->
                exp(protocol, expr.expression)

            else -> throw UnsupportedOperatorException(protocol, expr)
        }

    override fun constructorCall(
        protocol: Protocol,
        objectType: ObjectTypeNode,
        arguments: Arguments<AtomicExpressionNode>,
    ): CodeBlock =
        when (objectType.className.value) {
            ImmutableCell -> exp(
                protocol,
                arguments.first(),
            )

            MutableCell -> CodeBlock.of(
                "%T(%L)",
                Boxed::class,
                exp(protocol, arguments.first()),
            )

            Vector -> CodeBlock.of(
                "%T(%L){ %L }",
                Array::class,
                cleartextExp(protocol, arguments.first()),
                exp(
                    protocol,
                    LiteralNode(
                        objectType.typeArguments[0].value.defaultValue,
                        objectType.typeArguments[0].sourceLocation,
                    ),
                ),
            )

            else -> throw IllegalArgumentException(
                "Protocol ${protocol.name} does not support object ${
                    objectType.toDocument().print()
                }",
            )
        }

    override fun update(protocol: Protocol, stmt: UpdateNode): CodeBlock =
        when (typeAnalysis.type(nameAnalysis.declaration(stmt))) {
            is MutableCellType ->
                when (stmt.update.value) {
                    is io.github.aplcornell.viaduct.syntax.datatypes.Set ->
                        CodeBlock.of(
                            "%N.set(%L)",
                            context.kotlinName(stmt.variable.value),
                            exp(protocol, stmt.arguments[0]),
                        )

                    else -> throw UnsupportedOperatorException(protocol, stmt)
                }

            is VectorType ->
                when (stmt.update.value) {
                    is io.github.aplcornell.viaduct.syntax.datatypes.Set ->
                        CodeBlock.of(
                            "%N[%L] = %L",
                            context.kotlinName(stmt.variable.value),
                            cleartextExp(protocol, stmt.arguments[0]),
                            exp(protocol, stmt.arguments[1]),
                        )

                    else -> throw UnsupportedOperatorException(protocol, stmt)
                }

            else -> throw UnsupportedOperatorException(protocol, stmt)
        }

    override fun setup(protocol: Protocol): Iterable<PropertySpec> =
        listOf()
}
