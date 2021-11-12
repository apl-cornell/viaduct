package edu.cornell.cs.apl.viaduct.codegeneration

import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.U_BYTE_ARRAY
import edu.cornell.cs.apl.viaduct.errors.CodeGenerationError
import edu.cornell.cs.apl.viaduct.selection.CommunicationEvent
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.Protocol
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
        else -> throw CodeGenerationError("unknown send and receive type")
    }

fun receiveReplicated(
    sender: LetNode,
    sendProtocol: Protocol,
    events: Set<CommunicationEvent>,
    context: CodeGeneratorContext
): CodeBlock {

    fun receiveDispatcher(event: CommunicationEvent, receiveHost: Host): CodeBlock =
        when (event.send.host == receiveHost) {
            true -> CodeBlock.of("%L", context.kotlinName(sender.temporary.value, sendProtocol))
            false -> CodeBlock.of(
                "%L",
                context.receive(
                    typeTranslator(context.typeAnalysis.type(sender)),
                    event.send.host
                )
            )
        }

    var eventSet = events
    val receiveExpression = CodeBlock.builder()
    val it = eventSet.iterator()

    // TODO() - check if there are future events, if not don't use also
    receiveExpression.beginControlFlow(
        "%L.also",
        receiveDispatcher(it.next(), context.host)
    )

    // check to make sure that you got the same data from all hosts
    while (it.hasNext()) {
        val currentEvent = it.next()
        receiveExpression.add(
            "%N(%N, %L)",
            "assertEquals",
            "it",
            eventSet.first().send.host,
            receiveDispatcher(currentEvent, context.host),
            currentEvent.send.host
        )
    }

    receiveExpression.endControlFlow()

    return receiveExpression.build()
}
