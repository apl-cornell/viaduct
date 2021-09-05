package edu.cornell.cs.apl.viaduct.runtime

import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.types.ValueType
import edu.cornell.cs.apl.viaduct.syntax.values.Value
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf

interface Runtime {
    suspend fun input(type: ValueType): Value

    suspend fun output(value: Value)

    suspend fun receiveBytes(sender: Host): ByteArray

    suspend fun sendBytes(bytes: ByteArray, receiver: Host)
}

@OptIn(ExperimentalSerializationApi::class)
suspend inline fun <reified T> Runtime.receive(sender: Host): T =
    ProtoBuf.decodeFromByteArray(receiveBytes(sender))

@OptIn(ExperimentalSerializationApi::class)
suspend inline fun <reified T> Runtime.send(value: T, receiver: Host) =
    sendBytes(ProtoBuf.encodeToByteArray(value), receiver)
