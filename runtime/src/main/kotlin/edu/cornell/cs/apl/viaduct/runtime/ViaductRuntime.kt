package edu.cornell.cs.apl.viaduct.runtime

import edu.cornell.cs.apl.viaduct.syntax.Host
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.serializer
import kotlin.reflect.KType

@ExperimentalSerializationApi
@Suppress("UNCHECKED_CAST")
// TODO: fix all this
abstract class ViaductRuntime : Runtime {
    override suspend fun <T> send(type: KType, value: T, receiver: Host) {
        System.out.write(ProtoBuf.encodeToByteArray(ProtoBuf.serializersModule.serializer(type), value))
    }

    override suspend fun <T> receive(type: KType, sender: Host): T {
        val bytes = System.`in`.readBytes()
        return ProtoBuf.decodeFromByteArray(ProtoBuf.serializersModule.serializer(type) as KSerializer<T>, bytes)
    }
}
