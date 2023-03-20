package io.github.aplcornell.viaduct.circuitcodegeneration

import com.squareup.kotlinpoet.ARRAY
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.U_BYTE_ARRAY
import io.github.aplcornell.viaduct.group
import io.github.aplcornell.viaduct.runtime.EquivocationException
import io.github.aplcornell.viaduct.syntax.Arguments
import io.github.aplcornell.viaduct.syntax.Host
import io.github.aplcornell.viaduct.syntax.circuit.IndexExpressionNode
import io.github.aplcornell.viaduct.syntax.circuit.IndexParameterNode
import io.github.aplcornell.viaduct.syntax.circuit.LiteralNode
import io.github.aplcornell.viaduct.syntax.circuit.ReferenceNode
import io.github.aplcornell.viaduct.syntax.types.BooleanType
import io.github.aplcornell.viaduct.syntax.types.ByteVecType
import io.github.aplcornell.viaduct.syntax.types.IntegerType
import io.github.aplcornell.viaduct.syntax.types.StringType
import io.github.aplcornell.viaduct.syntax.types.ValueType
import io.github.aplcornell.viaduct.syntax.values.Value
import kotlin.reflect.KClass

typealias Shape = List<IndexExpressionNode>

/** Top level package name for the runtime module. */
// TODO: is there a better way of doing this?
val runtimePackage = "${group.replace("-", "")}.runtime"

/** Returns code for finding an available TCP port. */
val findAvailableTcpPort: CodeBlock =
    CodeBlock.of("%M()", MemberName(runtimePackage, "findAvailableTcpPort"))

/** Returns the [KClass] object for values of this type. */
val ValueType.valueClass: KClass<out Value>
    get() = this.defaultValue::class

fun typeTranslator(viaductType: ValueType): TypeName =
    when (viaductType) {
        ByteVecType -> U_BYTE_ARRAY
        BooleanType -> BOOLEAN
        IntegerType -> INT
        StringType -> STRING
        else -> throw IllegalArgumentException("Cannot convert ${viaductType.toDocument().print()} to Kotlin type.")
    }

fun kotlinType(shape: Shape, elementType: TypeName): TypeName =
    if (shape.isEmpty()) {
        elementType
    } else {
        ARRAY.parameterizedBy(kotlinType(shape.drop(1), elementType))
    }

fun indexExpression(expression: IndexExpressionNode, context: CodeGeneratorContext): CodeBlock = when (expression) {
    is LiteralNode -> {
        CodeBlock.of("%L", expression.value)
    }

    is ReferenceNode -> {
        CodeBlock.of("%N", context.kotlinName(expression.name.value))
    }
}

fun CodeBlock.lookup(indices: List<CodeBlock>): CodeBlock {
    val builder = CodeBlock.builder()
    builder.add(this)
    indices.forEach { builder.add("[%L]", it) }
    return builder.build()
}

/**
 * Generates code that constructs a new array with bounds and indexing variables in the initializer determined by [this].
 *
 * @param init returns the value of an element.
 */
fun Arguments<IndexParameterNode>.new(
    context: CodeGeneratorContext,
    init: CodeBlock,
): CodeBlock {
    val builder = CodeBlock.builder()
    for (indexParameter in this) {
        builder.beginControlFlow(
            "%T(%L){ %N -> ",
            Array::class,
            indexExpression(indexParameter.bound, context),
            context.kotlinName(indexParameter.name.value),
        )
    }
    builder.add(init)
    repeat(this.size) { builder.endControlFlow() }
    return builder.build()
}

/**
 * Generates code that constructs a new array with [this] shape.
 *
 * @param init returns the value of an element given its indices.
 */
fun Shape.new(
    context: CodeGeneratorContext,
    init: (indices: List<CodeBlock>) -> CodeBlock,
): CodeBlock {
    val builder = CodeBlock.builder()
    val indexVariables: List<CodeBlock> = this.map { size ->
        val indexVar = CodeBlock.of("%N", context.newTemporary("i"))
        builder.beginControlFlow(
            "%T(%L) { %L ->",
            Array::class,
            indexExpression(size, context),
            indexVar,
        )
        indexVar
    }
    // Don't add a newline if we did not create any blocks.
    if (indexVariables.isEmpty()) {
        builder.add(init(indexVariables))
    } else {
        builder.add("%L\n", init(indexVariables))
    }
    repeat(this.size) { builder.endControlFlow() }
    return builder.build()
}

/**
 * Generates code to do [action] for each element in [this] array.
 *
 * The action is given the index and value of the processed element.
 */
fun CodeBlock.forEachIndexed(
    shape: Shape,
    context: CodeGeneratorContext,
    action: (indices: List<CodeBlock>, value: CodeBlock) -> CodeBlock,
): CodeBlock {
    val builder = CodeBlock.builder()
    val indexVariables: List<CodeBlock> = shape.map { size ->
        val indexVar = CodeBlock.of("%N", context.newTemporary("i"))
        builder.beginControlFlow("for (%L in 0 until %L)", indexVar, indexExpression(size, context))
        indexVar
    }
    builder.addStatement("%L", action(indexVariables, this.lookup(indexVariables)))
    repeat(shape.size) { builder.endControlFlow() }
    return builder.build()
}

/** Code for the replicated value being received from [senders], along with associated equivocation checks. */
fun receiveReplicated(type: TypeName, senders: List<Host>, context: CodeGeneratorContext) =
    receiveExpected(context.receive(type, senders.first()), senders.first(), type, senders.drop(1), context)

/** Code for receiving values from [senders] expected to match [expectedValue]. */
fun receiveExpected(
    expectedValue: CodeBlock,
    expectedValueProvider: Host,
    type: TypeName,
    senders: List<Host>,
    context: CodeGeneratorContext,
): CodeBlock {
    if (senders.isEmpty()) return expectedValue

    val builder = CodeBlock.builder()
    builder.beginControlFlow("%L.also", expectedValue)
    for (host in senders) {
        builder.add(
            "%T.assertEquals(it, %L, %L, %L)\n",
            EquivocationException::class,
            context.codeOf(expectedValueProvider),
            context.receive(type, host),
            context.codeOf(host),
        )
    }
    builder.endControlFlow()
    return builder.build()
}