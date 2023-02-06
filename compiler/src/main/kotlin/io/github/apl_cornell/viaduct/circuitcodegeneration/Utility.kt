package io.github.apl_cornell.viaduct.circuitcodegeneration

import com.squareup.kotlinpoet.ARRAY
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.U_BYTE_ARRAY
import io.github.apl_cornell.viaduct.runtime.EquivocationException
import io.github.apl_cornell.viaduct.syntax.Host
import io.github.apl_cornell.viaduct.syntax.circuit.IndexExpressionNode
import io.github.apl_cornell.viaduct.syntax.circuit.LiteralNode
import io.github.apl_cornell.viaduct.syntax.circuit.ReferenceNode
import io.github.apl_cornell.viaduct.syntax.types.BooleanType
import io.github.apl_cornell.viaduct.syntax.types.ByteVecType
import io.github.apl_cornell.viaduct.syntax.types.IntegerType
import io.github.apl_cornell.viaduct.syntax.types.StringType
import io.github.apl_cornell.viaduct.syntax.types.ValueType
import io.github.apl_cornell.viaduct.syntax.values.Value
import kotlin.reflect.KClass

typealias Shape = List<IndexExpressionNode>

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
    if (shape.isEmpty()) elementType
    else ARRAY.parameterizedBy(kotlinType(shape.drop(1), elementType))

fun indexExpression(expression: IndexExpressionNode, context: CodeGeneratorContext): CodeBlock = when (expression) {
    is LiteralNode -> {
        CodeBlock.of("%L", expression.value)
    }
    is ReferenceNode -> {
        CodeBlock.of("%N", context.kotlinName(expression.name.value))
    }
}

fun Shape.new(
    init: (indices: List<CodeBlock>) -> CodeBlock,
    context: CodeGeneratorContext
): CodeBlock {
    val declaration = CodeBlock.builder()
    val indices: MutableList<CodeBlock> = mutableListOf()
    for (i in this.indices) {
        declaration.add("%T(%L){ ", Array::class, indexExpression(this[i], context))
        indices.add(CodeBlock.of(context.newTemporary("i")))
    }
    declaration.add(init(indices))
    repeat(this.size) { declaration.add(" }") }
    return declaration.build()
}

/**
 * Generates code to do [action] for each element in [this], where [action] is the code to be executed for each
 * element [value] as accessed by [indices].
 */
fun CodeBlock.forEachIndexed(
    shape: List<IndexExpressionNode>,
    action: (indices: List<CodeBlock>, value: CodeBlock) -> CodeBlock,
    context: CodeGeneratorContext
): CodeBlock {
    val builder = CodeBlock.builder()
    val indexingTmps = shape.map { size ->
        val indexingTmp = context.newTemporary("ind")
        val bound = indexExpression(size, context)
        builder.beginControlFlow("for ($indexingTmp in 0 until $bound)")
        CodeBlock.of(indexingTmp)
    }
    val valueBuilder = CodeBlock.builder()
    valueBuilder.add("%L", this)
    indexingTmps.forEach { valueBuilder.add("[%L]", it) }
    val value = valueBuilder.build()
    builder.addStatement(action(indexingTmps, value).toString())
    repeat(shape.size) { builder.endControlFlow() }
    return builder.build()
}

fun receiveReplicated(type: TypeName, senders: List<Host>, context: CodeGeneratorContext) =
    receiveExpected(context.receive(type, senders.first()), senders.first(), type, senders.drop(1), context)

fun receiveExpected(
    expectedValue: CodeBlock,
    expectedValueProvider: Host,
    type: TypeName,
    senders: List<Host>,
    context: CodeGeneratorContext
): CodeBlock {
    if (senders.isEmpty()) return expectedValue

    val builder = CodeBlock.builder()
    builder.beginControlFlow(
        "%L.also",
        expectedValue
    )
    for (host in senders) {
        builder.addStatement(
            "%T.assertEquals(%L, %L, %L, %L)",
            EquivocationException::class,
            expectedValue,
            context.codeOf(expectedValueProvider),
            context.receive(type, host),
            context.codeOf(host)
        )
    }
    builder.endControlFlow()
    return builder.build()
}
