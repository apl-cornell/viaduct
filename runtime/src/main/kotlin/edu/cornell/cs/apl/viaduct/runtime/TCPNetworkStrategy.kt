package edu.cornell.cs.apl.viaduct.runtime

import edu.cornell.cs.apl.viaduct.syntax.Host
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.serializer
import mu.KotlinLogging
import java.io.Closeable
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.ConnectException
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KType

private var logger = KotlinLogging.logger("Runtime")

private class HostConnection(private val socket: Socket) : Closeable by socket {
    val input = DataInputStream(socket.getInputStream())
    val output = DataOutputStream(socket.getOutputStream())
}

@OptIn(ExperimentalSerializationApi::class)
@Suppress("UNCHECKED_CAST")
/** Implementation of a pairwise connected network using TCP sockets. */
class TCPNetworkStrategy(
    private val host: Host,
    private val hostAddresses: Map<Host, InetSocketAddress>,
    private val connectionNumRetry: Int = CONNECTION_NUM_RETRY,
    private val connectionRetryDelay: Long = CONNECTION_RETRY_DELAY
) : NetworkStrategy, Closeable {
    companion object {
        // default: try to connect for at most 10 times, at 1000ms intervals
        const val CONNECTION_NUM_RETRY: Int = 10
        const val CONNECTION_RETRY_DELAY: Long = 1000
    }

    private val connectionMap = ConcurrentHashMap<Host, HostConnection>()

    /** Establish socket connections between all pairs of hosts.
     *  The protocol for connecting is that we sorted the hosts
     *  h_1, ..., h_n alphabetically (assuming that no host has the same name),
     *  and then host hi will:
     *  - listen to connections from hosts h_j where i < j
     *  - connect to hosts h_k where k < i */
    private fun createRemoteConnections() {
        val hostAddress = hostAddresses[this.host]!!
        val serverSocket = ServerSocket(hostAddress.port)

        try {
            runBlocking {
                val sortedHosts = hostAddresses.keys.sorted()

                // connect to hosts with lower ID
                val hindex = sortedHosts.indexOf(this@TCPNetworkStrategy.host)

                // hosts to which this host will connect
                val listeningHosts = sortedHosts.subList(0, hindex)

                // hosts that will connect to this host
                val connectingHosts = sortedHosts.subList(hindex + 1, sortedHosts.size)

                // connect to hosts with lower ID
                // spawn a coroutine for each host to connect to
                listeningHosts.map { listeningHost ->
                    launch(Dispatchers.IO) {
                        val listeningHostAddress = hostAddresses[listeningHost]!!
                        var retries = 0
                        var connected = false
                        while (retries < connectionNumRetry && !connected) {
                            try {
                                val clientSocket = Socket(listeningHostAddress.hostString, listeningHostAddress.port)

                                // write this host's ID to the socket to identify this host to the remote host
                                clientSocket.getOutputStream().write(hindex)
                                connectionMap[listeningHost] = HostConnection(clientSocket)
                                connected = true

                                logger.info { "Connected to host ${listeningHost.name} at $listeningHostAddress." }
                            } catch (e: ConnectException) {
                                logger.info { "Failed to connect to host ${listeningHost.name} at $listeningHostAddress; retrying." }
                                retries++
                                delay(connectionRetryDelay)
                            }
                        }

                        if (!connected) {
                            throw HostConnectionException(
                                this@TCPNetworkStrategy.host,
                                listeningHost,
                                listeningHostAddress
                            )
                        }
                    }
                }.joinAll()

                // accept connections from hosts with higher ID
                val incomingConnections = connectingHosts.toMutableSet()

                if (incomingConnections.size > 0) {
                    logger.info { "Listening to incoming connections from: ${connectingHosts.joinToString { it.name }}." }
                }

                while (incomingConnections.isNotEmpty()) {
                    val clientSocket = serverSocket.accept()
                    val clientHostId = clientSocket.getInputStream().read()
                    val clientHost = sortedHosts[clientHostId]
                    connectionMap[clientHost] = HostConnection(clientSocket)
                    incomingConnections.remove(clientHost)

                    logger.info { "Accepted connection from host ${clientHost.name}." }
                }

                serverSocket.close()
            }
        } catch (e: HostConnectionException) { // if this host failed to connect, clean up opened sockets
            for (connection in this@TCPNetworkStrategy.connectionMap.values) {
                connection.close()
            }

            serverSocket.close()

            // propagate exception up
            throw e
        }
    }

    /** Start runtime by opening sockets to other hosts. */
    fun start() {
        createRemoteConnections()
    }

    override fun close() {
        // Closing sockets to other hosts.
        for (kv in connectionMap) {
            logger.info { "Closing connection to host ${kv.key.name}." }
            kv.value.close()
        }
    }

    override fun <T> send(type: KType, value: T, receiver: Host) {
        return connectionMap[receiver]?.output?.let { socketOut ->
            logger.trace { "Sending $value to ${receiver.name}." }

            val bytes = ProtoBuf.encodeToByteArray(ProtoBuf.serializersModule.serializer(type), value)
            val bytesLen = bytes.size
            socketOut.writeInt(bytesLen)
            socketOut.write(bytes)
        } ?: throw HostCommunicationException(this.host, receiver)
    }

    override fun <T> receive(type: KType, sender: Host): T {
        connectionMap[sender]?.input?.let { socketIn ->
            val bytesLen = socketIn.readInt()
            val bytes = socketIn.readNBytes(bytesLen)
            val serializer = ProtoBuf.serializersModule.serializer(type) as KSerializer<T>
            val value = ProtoBuf.decodeFromByteArray(serializer, bytes)

            logger.trace { "Received $value from ${sender.name}." }

            return value
        } ?: throw HostCommunicationException(this.host, sender)
    }

    override fun url(host: Host): InetSocketAddress {
        return hostAddresses[host] ?: throw UnknownHostException(host)
    }
}
