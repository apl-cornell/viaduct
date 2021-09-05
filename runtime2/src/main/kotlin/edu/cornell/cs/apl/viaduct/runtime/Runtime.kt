package edu.cornell.cs.apl.viaduct.runtime

import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.types.ValueType
import edu.cornell.cs.apl.viaduct.syntax.values.Value
import kotlin.reflect.KType
import kotlin.reflect.typeOf

interface Runtime {
    suspend fun input(type: ValueType): Value

    suspend fun output(value: Value)

    suspend fun <T> send(type: KType, value: T, receiver: Host)

    suspend fun <T> receive(type: KType, sender: Host): T
}

@ExperimentalStdlibApi
suspend inline fun <reified T> Runtime.send(value: T, receiver: Host) =
    send(typeOf<T>(), value, receiver)

@ExperimentalStdlibApi
suspend inline fun <reified T> Runtime.receive(sender: Host): T =
    receive(typeOf<T>(), sender)
