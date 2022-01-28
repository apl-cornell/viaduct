package edu.cornell.cs.apl.viaduct.runtime

import edu.cornell.cs.apl.viaduct.syntax.Host
import java.net.InetSocketAddress
import kotlin.reflect.KType
import kotlin.reflect.typeOf

interface ViaductRuntime : IOStrategy {
    fun <T> receive(type: KType, sender: Host): T

    fun <T> send(type: KType, value: T, receiver: Host)

    fun url(host: Host): InetSocketAddress
}

@OptIn(ExperimentalStdlibApi::class)
inline fun <reified T> ViaductRuntime.receive(sender: Host): T =
    receive(typeOf<T>(), sender)

@OptIn(ExperimentalStdlibApi::class)
inline fun <reified T> ViaductRuntime.send(value: T, receiver: Host) =
    send(typeOf<T>(), value, receiver)
