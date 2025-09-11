package io.github.aplcornell.viaduct.circuitcodegeneration

import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.LIST
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

/** Lists the size of each dimension of a multidimensional array. */
typealias Shape = List<IndexExpressionNode>

/** Kotlin class used throughout the generated code to represent arrays. */
private val arrayType: ClassName = LIST

/** Top level package name for the `runtime` module. */
val runtimePackage =
    // TODO: is there a better way of computing this?
    "${group.replace("-", "")}.runtime"

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

fun kotlinType(
    shape: Shape,
    elementType: TypeName,
): TypeName =
    if (shape.isEmpty()) {
        elementType
    } else {
        arrayType.parameterizedBy(kotlinType(shape.drop(1), elementType))
    }

/** Translates [expression] to Kotlin code. */
fun indexExpression(
    expression: IndexExpressionNode,
    context: CodeGeneratorContext,
): CodeBlock =
    when (expression) {
        is LiteralNode -> {
            CodeBlock.of("%L", expression.value)
        }

        is ReferenceNode -> {
            CodeBlock.of("%N", context.kotlinName(expression.name.value))
        }
    }

/** Generates code for array lookup into [this] at [indices]. */
fun CodeBlock.lookup(indices: List<CodeBlock>): CodeBlock {
    val builder = CodeBlock.builder()
    builder.add(this)
    indices.forEach { builder.add("[%L]", it) }
    return builder.build()
}

/**
 * Generates code that constructs a new array with bounds and indexing variables
 * in the initializer determined by [this].
 *
 * @param init returns the value of an element.
 */
fun Arguments<IndexParameterNode>.new(
    context: CodeGeneratorContext,
    init: CodeBlock,
): CodeBlock {
    val shape = this.map { it.bound }
    val indices = this.map { CodeBlock.of("%N", context.kotlinName(it.name.value)) }
    return shape.new(context, indices, init)
}

/**
 * Generates code that constructs a new array with [this] shape.
 * Generates fresh names for indices.
 *
 * @param init returns the value of an element given its indices.
 */
fun Shape.new(
    context: CodeGeneratorContext,
    init: (indices: List<CodeBlock>) -> CodeBlock,
): CodeBlock {
    val indices = this.map { CodeBlock.of("%N", context.newTemporary("i")) }
    return this.new(context, indices, init(indices))
}

/**
 * Generates code that constructs a new array with [this] shape.
 *
 * @param indices names of index variables for each dimension of the array.
 * @param init gives the value of each element in the array based on [indices].
 */
fun Shape.new(
    context: CodeGeneratorContext,
    indices: List<CodeBlock>,
    init: CodeBlock,
): CodeBlock {
    require(this.size == indices.size)
    val builder = CodeBlock.builder()
    this.zip(indices) { size, index ->
        builder.beginControlFlow(
            "%T(%L) { %L ->",
            arrayType,
            indexExpression(size, context),
            index,
        )
    }
    // Don't add a newline if we did not create any blocks.
    if (this.isEmpty()) {
        builder.add(init)
    } else {
        builder.add("%L\n", init)
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
    val indexVariables: List<CodeBlock> =
        shape.map { size ->
            val indexVar = CodeBlock.of("%N", context.newTemporary("i"))
            builder.beginControlFlow("for (%L in 0 until %L)", indexVar, indexExpression(size, context))
            indexVar
        }
    builder.addStatement("%L", action(indexVariables, this.lookup(indexVariables)))
    repeat(shape.size) { builder.endControlFlow() }
    return builder.build()
}

/** Code for the replicated value being received from [senders], along with associated equivocation checks. */
fun receiveReplicated(
    type: TypeName,
    senders: List<Host>,
    context: CodeGeneratorContext,
) = receiveExpected(context.receive(type, senders.first()), senders.first(), type, senders.drop(1), context)

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
