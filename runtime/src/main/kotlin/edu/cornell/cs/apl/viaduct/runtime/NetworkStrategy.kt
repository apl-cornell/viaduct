package edu.cornell.cs.apl.viaduct.runtime

import edu.cornell.cs.apl.viaduct.syntax.Host
import java.net.InetSocketAddress
import kotlin.reflect.KType
import kotlin.reflect.typeOf

interface NetworkStrategy {
    /** Receives a value of type [type] from [sender]. */
    fun <T> receive(type: KType, sender: Host): T

    /** Sends [value] of type [type] to [receiver]. */
    fun <T> send(type: KType, value: T, receiver: Host)

    /** Returns the network address of [host]. */
    fun url(host: Host): InetSocketAddress
}

@OptIn(ExperimentalStdlibApi::class)
inline fun <reified T> NetworkStrategy.receive(sender: Host): T =
    receive(typeOf<T>(), sender)

@OptIn(ExperimentalStdlibApi::class)
inline fun <reified T> NetworkStrategy.send(value: T, receiver: Host) =
    send(typeOf<T>(), value, receiver)
