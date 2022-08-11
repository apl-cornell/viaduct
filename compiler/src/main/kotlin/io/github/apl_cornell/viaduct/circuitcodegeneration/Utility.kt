package io.github.apl_cornell.viaduct.circuitcodegeneration

import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.U_BYTE_ARRAY
import io.github.apl_cornell.viaduct.syntax.types.BooleanType
import io.github.apl_cornell.viaduct.syntax.types.ByteVecType
import io.github.apl_cornell.viaduct.syntax.types.IntegerType
import io.github.apl_cornell.viaduct.syntax.types.StringType
import io.github.apl_cornell.viaduct.syntax.types.ValueType
import io.github.apl_cornell.viaduct.syntax.values.Value
import kotlin.reflect.KClass

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

//fun receiveReplicated(
//    sender: LetNode,
//    events: Set<CommunicationEvent>,
//    context: CodeGeneratorContext,
//    typeAnalysis: TypeAnalysis
//): CodeBlock {
//
//    val receiveExpression = CodeBlock.builder()
//    val it = events.iterator()
//
//    if (events.size > 1) {
//        receiveExpression.beginControlFlow(
//            "%L.also",
//            context.receive(typeTranslator(typeAnalysis.type(sender)), it.next().send.host)
//        )
//    } else {
//        receiveExpression.add(
//            "%L",
//            context.receive(typeTranslator(typeAnalysis.type(sender)), it.next().send.host)
//        )
//        return receiveExpression.build()
//    }
//
//// check to make sure that you got the same data from all hosts
//    while (it.hasNext()) {
//        val currentEvent = it.next()
//        receiveExpression.add(
//            "%N(%N, %L)",
//            "assertEquals",
//            "it",
//            events.first().send.host,
//            context.receive(typeTranslator(typeAnalysis.type(sender)), currentEvent.send.host),
//            currentEvent.send.host
//        )
//    }
//
//    receiveExpression.endControlFlow()
//
//    return receiveExpression.build()
//}
