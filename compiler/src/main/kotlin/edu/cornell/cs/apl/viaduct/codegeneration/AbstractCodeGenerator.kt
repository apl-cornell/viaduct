package edu.cornell.cs.apl.viaduct.codegeneration

import com.squareup.kotlinpoet.ARRAY
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import edu.cornell.cs.apl.viaduct.analysis.NameAnalysis
import edu.cornell.cs.apl.viaduct.analysis.TypeAnalysis
import edu.cornell.cs.apl.viaduct.syntax.Arguments
import edu.cornell.cs.apl.viaduct.syntax.ObjectTypeNode
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.datatypes.Get
import edu.cornell.cs.apl.viaduct.syntax.datatypes.ImmutableCell
import edu.cornell.cs.apl.viaduct.syntax.datatypes.MutableCell
import edu.cornell.cs.apl.viaduct.syntax.datatypes.Vector
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AtomicExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DowngradeNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ExpressionArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.FunctionArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LiteralNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ObjectDeclarationArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ObjectReferenceArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutParameterArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutParameterConstructorInitializerNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutParameterExpressionInitializerNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutParameterInitializationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.QueryNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReadNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.SimpleStatementNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.UpdateNode
import edu.cornell.cs.apl.viaduct.syntax.types.ImmutableCellType
import edu.cornell.cs.apl.viaduct.syntax.types.MutableCellType
import edu.cornell.cs.apl.viaduct.syntax.types.ObjectType
import edu.cornell.cs.apl.viaduct.syntax.types.ValueType
import edu.cornell.cs.apl.viaduct.syntax.types.VectorType
import edu.cornell.cs.apl.viaduct.syntax.values.Value

abstract class AbstractCodeGenerator(val context: CodeGeneratorContext) : CodeGenerator {
    private val nameAnalysis = NameAnalysis.get(context.program)
    private val typeAnalysis = TypeAnalysis.get(context.program)

    override fun kotlinType(protocol: Protocol, sourceType: ValueType): TypeName = typeTranslator(sourceType)

    override fun kotlinType(protocol: Protocol, sourceType: ObjectType): TypeName {
        return when (sourceType) {
            is ImmutableCellType -> {
                kotlinType(protocol, sourceType.elementType)
            }
            is MutableCellType -> {
                kotlinType(protocol, sourceType.elementType)
            }
            is VectorType -> {
                ARRAY.parameterizedBy(kotlinType(protocol, sourceType.elementType))
            }
            else -> {
                throw IllegalArgumentException(
                    "Cannot convert ${
                        sourceType.toDocument().print()
                    } to Kotlin type."
                )
            }
        }
    }

    override fun guard(protocol: Protocol, expr: AtomicExpressionNode): CodeBlock =
        throw UnsupportedOperatorException(protocol, expr)

    final override fun simpleStatement(protocol: Protocol, stmt: SimpleStatementNode): CodeBlock {
        return when (stmt) {
            is LetNode -> let(protocol, stmt)

            is DeclarationNode -> declaration(protocol, stmt)

            is UpdateNode -> update(protocol, stmt)

            is OutParameterInitializationNode -> outParameterInitialization(protocol, stmt)

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

    fun declaration(protocol: Protocol, stmt: DeclarationNode): CodeBlock {
        val rhs =
            constructorCall(protocol, stmt.objectType, stmt.arguments)
        return when (stmt.objectType.className.value) {
            ImmutableCell -> CodeBlock.of(
                "val %N = %L",
                context.kotlinName(stmt.name.value),
                rhs
            )

            // TODO - change this (difference between viaduct, kotlin semantics)
            MutableCell -> CodeBlock.of(
                "var %N = %L",
                context.kotlinName(stmt.name.value),
                rhs
            )

            Vector -> CodeBlock.of(
                "val %N = %L",
                context.kotlinName(stmt.name.value),
                rhs
            )
            else -> throw UnsupportedOperatorException(protocol, stmt)
        }
    }

    /** Produces the right-hand side of a declaration. */
    private fun constructorCall(
        protocol: Protocol,
        objectType: ObjectTypeNode,
        arguments: Arguments<AtomicExpressionNode>
    ): CodeBlock =
        when (objectType.className.value) {
            ImmutableCell, MutableCell -> exp(
                protocol, arguments.first()
            )
            Vector -> CodeBlock.of(
                "%T(%L){ %L }",
                Array::class,
                cleartextExp(protocol, arguments.first()),
                exp(
                    protocol,
                    LiteralNode(
                        objectType.typeArguments[0].value.defaultValue,
                        objectType.typeArguments[0].sourceLocation
                    )
                )
            )
            else -> throw IllegalArgumentException("Protocol ${protocol.name} does not support object ${objectType.toDocument().print()}")
        }

    open fun update(protocol: Protocol, stmt: UpdateNode): CodeBlock =
        when (typeAnalysis.type(nameAnalysis.declaration(stmt))) {
            is MutableCellType ->
                when (stmt.update.value) {
                    is edu.cornell.cs.apl.viaduct.syntax.datatypes.Set ->
                        CodeBlock.of(
                            "%N = %L",
                            context.kotlinName(stmt.variable.value),
                            exp(protocol, stmt.arguments[0])
                        )

                    else -> throw UnsupportedOperatorException(protocol, stmt)
                }

            is VectorType ->
                when (stmt.update.value) {
                    is edu.cornell.cs.apl.viaduct.syntax.datatypes.Set ->
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

    override fun argument(protocol: Protocol, argument: FunctionArgumentNode): CodeBlock {
        return when (argument) {
            // Input arguments
            is ObjectReferenceArgumentNode -> {
                CodeBlock.of("%N", context.kotlinName(argument.variable.value))
            }
            is ExpressionArgumentNode -> {
                exp(protocol, argument.expression)
            }
            // Output arguments
            is ObjectDeclarationArgumentNode -> {
                throw UnsupportedOperatorException(protocol, argument)
            }
            is OutParameterArgumentNode -> {  // Out box already in scope
                CodeBlock.of("%N", context.outBoxName(context.kotlinName(argument.parameter.value)))
            }
        }
    }

    private fun outParameterInitialization(
        protocol: Protocol,
        stmt: OutParameterInitializationNode
    ): CodeBlock {
        val rhs = when (val init = stmt.initializer) {
            is OutParameterConstructorInitializerNode -> constructorCall(
                protocol,
                init.objectType,
                init.arguments
            )
            is OutParameterExpressionInitializerNode -> exp(protocol, init.expression)
        }
        val parameterName = context.kotlinName(stmt.name.value)

        return CodeBlock.builder().add(
            "%N = %L \n",
            parameterName,
            rhs
        ).add(
            "%N.set(%N)",
            context.outBoxName(parameterName),
            parameterName
        ).build()
    }

    open fun output(protocol: Protocol, stmt: OutputNode): CodeBlock =
        throw UnsupportedOperatorException(protocol, stmt)

    override fun setup(protocol: Protocol): Iterable<PropertySpec> =
        listOf()
}
