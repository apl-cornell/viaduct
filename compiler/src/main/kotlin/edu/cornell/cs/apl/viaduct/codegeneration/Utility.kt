package edu.cornell.cs.apl.viaduct.codegeneration

import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.U_BYTE_ARRAY
import edu.cornell.cs.apl.viaduct.analysis.TypeAnalysis
import edu.cornell.cs.apl.viaduct.selection.CommunicationEvent
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.types.BooleanType
import edu.cornell.cs.apl.viaduct.syntax.types.ByteVecType
import edu.cornell.cs.apl.viaduct.syntax.types.IntegerType
import edu.cornell.cs.apl.viaduct.syntax.types.StringType
import edu.cornell.cs.apl.viaduct.syntax.types.ValueType
import edu.cornell.cs.apl.viaduct.syntax.values.Value
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

fun receiveReplicated(
    sender: LetNode,
    events: Set<CommunicationEvent>,
    context: CodeGeneratorContext,
    typeAnalysis: TypeAnalysis
): CodeBlock {

    val eventSet = events
    val receiveExpression = CodeBlock.builder()
    val it = eventSet.iterator()

    if (eventSet.size > 1) {
        receiveExpression.beginControlFlow(
            "%L.also",
            context.receive(typeTranslator(typeAnalysis.type(sender)), it.next().send.host)
        )
    } else {
        receiveExpression.add(
            "%L",
            context.receive(typeTranslator(typeAnalysis.type(sender)), it.next().send.host)
        )
        return receiveExpression.build()
    }

// check to make sure that you got the same data from all hosts
    while (it.hasNext()) {
        val currentEvent = it.next()
        receiveExpression.add(
            "%N(%N, %L)",
            "assertEquals",
            "it",
            eventSet.first().send.host,
            context.receive(typeTranslator(typeAnalysis.type(sender)), currentEvent.send.host),
            currentEvent.send.host
        )
    }

    receiveExpression.endControlFlow()

    return receiveExpression.build()
}
