package edu.cornell.cs.apl.viaduct.backend

import edu.cornell.cs.apl.viaduct.analysis.ProtocolAnalysis
import edu.cornell.cs.apl.viaduct.errors.ViaductInterpreterError
import edu.cornell.cs.apl.viaduct.protocols.Synchronization
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import edu.cornell.cs.apl.viaduct.syntax.values.BooleanValue
import edu.cornell.cs.apl.viaduct.syntax.values.ByteVecValue
import edu.cornell.cs.apl.viaduct.syntax.values.IntegerValue
import edu.cornell.cs.apl.viaduct.syntax.values.UnitValue
import edu.cornell.cs.apl.viaduct.syntax.values.Value
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import java.io.InputStream
import java.io.OutputStream
import java.net.ConnectException
import java.net.ServerSocket
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Scanner
import java.util.concurrent.Executors

private var logger = KotlinLogging.logger("Runtime")

typealias ProcessId = Int
typealias HostId = Int

sealed class ViaductMessage

sealed class CommunicationMessage : ViaductMessage()

data class SendMessage(
    val sender: ProcessId,
    val receiver: ProcessId,
    val message: Value
) : CommunicationMessage()

data class ReceiveMessage(
    val sender: ProcessId,
    val receiver: ProcessId
) : CommunicationMessage()

object ShutdownMessage : ViaductMessage()

private abstract class ViaductThread(
    val msgQueue: Channel<ViaductMessage>
) {
    abstract suspend fun processCommunicationMessage(msg: CommunicationMessage)

    suspend fun run() {
        loop@ while (true) {
            when (val msg: ViaductMessage = msgQueue.receive()) {
                is CommunicationMessage -> processCommunicationMessage(msg)

                is ShutdownMessage -> {
                    logger.info { "shutting down $this" }
                    break@loop
                }
            }
        }
    }
}

private class ViaductReceiverThread(
    val host: Host,
    val socket: Socket,
    val runtime: ViaductRuntime,
    msgQueue: Channel<ViaductMessage>
) : ViaductThread(msgQueue) {
    override fun toString(): String {
        return "receiver thread for host ${host.name}"
    }

    override suspend fun processCommunicationMessage(msg: CommunicationMessage) {
        when (msg) {
            is ReceiveMessage -> {
                val result: Triple<Value, Process, Process> =
                    withContext(Dispatchers.IO) {
                        val socketInput: InputStream = socket.getInputStream()
                        val senderId: Int = socketInput.read()
                        val receiverId: Int = socketInput.read()
                        val valType: Int = socketInput.read()

                        val sender: Process = runtime.getProcessById(senderId).process
                        val receiver: Process = runtime.getProcessById(receiverId).process
                        val value: Value =
                            when (valType) {
                                // BooleanValue
                                0 -> BooleanValue(socketInput.read() != 0)

                                // IntegerValue
                                1 -> {
                                    val b = socketInput.readNBytes(4)
                                    IntegerValue (ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).int)
                                }

                                // ByteVecValue
                                2 -> {
                                    val len_buf = socketInput.readNBytes(4)
                                    val len = ByteBuffer.wrap(len_buf).order(ByteOrder.LITTLE_ENDIAN).int
                                    val i = socketInput.readNBytes(len).toList()
                                    ByteVecValue(i)
                                }

                                // UnitValue
                                3 -> UnitValue

                                else -> throw ViaductInterpreterError("parsed invalid value type $valType")
                            }

                        Triple(value, sender, receiver)
                    }

                logger.info {
                    "received remote message ${result.first} from ${result.second.asDocument.print()} to ${result.third.asDocument.print()}"
                }

                runtime.send(result.first, result.second, result.third)
            }

            else -> throw ViaductInterpreterError("receiver coroutine cannot send")
        }
    }
}

private class ViaductSenderThread(
    val host: Host,
    val socket: Socket,
    val runtime: ViaductRuntime,
    msgQueue: Channel<ViaductMessage>
) : ViaductThread(msgQueue) {
    override fun toString(): String {
        return "sender thread for host ${host.name}"
    }

    override suspend fun processCommunicationMessage(msg: CommunicationMessage) {
        when (msg) {
            is SendMessage -> {
                withContext(Dispatchers.IO) {
                    val socketOutput: OutputStream = socket.getOutputStream()
                    socketOutput.write(msg.sender)
                    socketOutput.write(msg.receiver)
                    when (msg.message) {
                        is BooleanValue -> {
                            socketOutput.write(0)
                            socketOutput.write(if (msg.message.value) 1 else 0)
                        }

                        is IntegerValue -> {
                            socketOutput.write(1)
                            val b = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(msg.message.value)
                            socketOutput.write(b.array())
                        }

                        is ByteVecValue -> {
                            socketOutput.write(2)
                            val bytes = msg.message.value.toByteArray()
                            val len = bytes.size
                            val len_buf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(len)
                            socketOutput.write(len_buf.array())
                            socketOutput.write(bytes)
                        }

                        is UnitValue -> {
                            socketOutput.write(3)
                        }
                    }
                }
            }

            else -> throw ViaductInterpreterError("sender coroutine cannot receive")
        }
    }
}

typealias IpAddress = String
typealias Port = Int

data class HostAddress(val ipAddress: IpAddress, val port: Port)

data class HostInfo(
    val host: Host,
    val id: HostId,
    val address: HostAddress,
    val recvChannel: Channel<ViaductMessage>,
    val sendChannel: Channel<ViaductMessage>
)

data class ProcessInfo(
    val process: ProtocolProjection,
    val id: ProcessId
) {
    val host: Host
        get() = process.host

    val protocol: Protocol
        get() = process.protocol
}

typealias Process = ProtocolProjection
typealias ProcessInterpreter = suspend (ViaductRuntime) -> ProtocolInterpreter

class ViaductRuntime(
    private val program: ProgramNode,
    private val protocolAnalysis: ProtocolAnalysis,
    private val hostConnectionInfo: Map<Host, HostAddress>,
    private val processBodyMap: Map<Process, ProcessInterpreter>,
    val host: Host
) {
    private val syncProtocol = Synchronization(program.hosts.map { it.name.value }.toSet())
    private val processInfoMap: Map<Process, ProcessInfo>
    private val hostInfoMap: Map<Host, HostInfo>

    private val channelMap: Map<Process, Map<Process, Channel<Value>>>

    private val stdinScanner: Scanner = Scanner(System.`in`)

    companion object {
        // output buffer for channels
        private const val CHANNEL_CAPACITY: Int = 100

        // try to connect for 10 seconds, at 100ms intervals
        const val CONNECTION_NUM_RETRY: Int = 100
        const val CONNECTION_RETRY_DELAY: Long = 100
    }

    init {
        // we need a deterministic algorithm to assign identifiers (ints) to
        // all hosts and processes that interpreters in all hosts will agree on

        // create identifiers for processes (protocol projections)
        val processList: List<Process> =
            protocolAnalysis
                .participatingProtocols(program)
                .flatMap { protocol ->
                    protocol.hosts.map { host ->
                        ProtocolProjection(protocol, host)
                    }
                }.plus(
                    syncProtocol.hosts.map { host ->
                        ProtocolProjection(syncProtocol, host)
                    }
                ).sorted()

        val tempProcessInfoMap: MutableMap<Process, ProcessInfo> = mutableMapOf()
        var j = 1
        for (projection: ProtocolProjection in processList) {
            tempProcessInfoMap[projection] = ProcessInfo(projection, j)
            j++
        }

        processInfoMap = tempProcessInfoMap

        // initialize channel map
        val processPairList: List<Pair<Process, Process>> =
            processInfoMap.values
                .filter { pinfo -> pinfo.protocol !is Synchronization }
                .flatMap { pinfo ->
                    processInfoMap.values
                        .filter { pinfo2 -> pinfo2.id != pinfo.id }
                        .map { pinfo2 -> Pair(pinfo.process, pinfo2.process) }
                }
                .toList()

        val tempChannelMap: MutableMap<Process, MutableMap<Process, Channel<Value>>> = mutableMapOf()
        for (pair: Pair<Process, Process> in processPairList) {
            if (!tempChannelMap.containsKey(pair.first)) {
                tempChannelMap[pair.first] = mutableMapOf()
            }

            if (!tempChannelMap[pair.first]!!.containsKey(pair.second)) {
                tempChannelMap[pair.first]!![pair.second] = Channel(CHANNEL_CAPACITY)
            }
        }

        // add synchronization channels
        for (host in syncProtocol.hosts) {
            val syncProcess = ProtocolProjection(syncProtocol, host)
            tempChannelMap[syncProcess] = mutableMapOf()
            tempChannelMap[syncProcess]!!.putAll(
                syncProtocol.hosts
                    .filter { host2 -> host2 != host }
                    .map { host2 ->
                        ProtocolProjection(syncProtocol, host2) to Channel(CHANNEL_CAPACITY)
                    }
            )
        }

        channelMap = tempChannelMap

        // create identifiers for hosts
        val hostList: List<Host> =
            program.hosts.map { node -> node.name.value }.sorted()

        val tempHostInfoMap = mutableMapOf<Host, HostInfo>()
        var i = 1
        for (host: Host in hostList) {
            tempHostInfoMap[host] =
                HostInfo(host, i, hostConnectionInfo[host]!!, Channel(CHANNEL_CAPACITY), Channel(CHANNEL_CAPACITY))
            i++
        }
        hostInfoMap = tempHostInfoMap
    }

    fun getHostById(id: HostId): HostInfo =
        hostInfoMap.values.first { hinfo -> hinfo.id == id }

    fun getProcessById(id: ProcessId): ProcessInfo {
        val lst = processInfoMap.values.filter { pinfo -> pinfo.id == id }
        if (lst.isNotEmpty()) {
            return lst.first()
        } else {
            throw ViaductInterpreterError("unknown process id: $id")
        }
    }

    suspend fun send(value: Value, sender: Process, receiver: Process) {
        if (receiver.host == host) { // local communication
            channelMap[sender]!![receiver]!!.send(value)
        } else { // remote communication

            val msg = SendMessage(processInfoMap[sender]!!.id, processInfoMap[receiver]!!.id, value)
            hostInfoMap[receiver.host]!!.sendChannel.send(msg)

            logger.info {
                "sent remote message $value from ${sender.asDocument.print()} to ${receiver.asDocument.print()}"
            }
        }
    }

    suspend fun receive(sender: Process, receiver: Process): Value {
        if (sender.host != host) { // remote communication
            val msg = ReceiveMessage(processInfoMap[sender]!!.id, processInfoMap[receiver]!!.id)
            hostInfoMap[sender.host]!!.recvChannel.send(msg)
        }

        return channelMap[sender]!![receiver]!!.receive()
    }

    suspend fun input(): Value {
        return withContext(Dispatchers.IO) {
            // TODO: support booleans as well
            IntegerValue(stdinScanner.nextInt())
        }
    }

    suspend fun output(value: Value) {
        logger.info { "output $value" }
        withContext(Dispatchers.IO) { println(value) }
    }

    // protocol for connections: for hosts i and j where i < j, j connects to i
    private fun createRemoteConnections(): Map<Host, Socket> {
        val connectionMap: MutableMap<Host, Socket> = mutableMapOf()
        val hinfo: HostInfo = hostInfoMap[host]!!

        // connect to hosts with lower ID
        for (hinfo2: HostInfo in hostInfoMap.values) {
            if (hinfo2.id < hinfo.id) { // this host connects to that host
                var retries = 0
                var connected = false
                while (retries < CONNECTION_NUM_RETRY) {
                    try {
                        val clientSocket = Socket(hinfo2.address.ipAddress, hinfo2.address.port)
                        clientSocket.getOutputStream().write(hinfo.id)
                        connectionMap[hinfo2.host] = clientSocket
                        connected = true
                        logger.info {
                            "connected to host ${hinfo2.host.name} " +
                                "at ${hinfo2.address.ipAddress}:${hinfo2.address.port}"
                        }

                        break
                    } catch (e: ConnectException) {
                        logger.info {
                            "failed to connect to host ${hinfo2.host.name} " +
                                "at ${hinfo2.address.ipAddress}:${hinfo2.address.port}, retrying"
                        }
                        retries++
                        Thread.sleep(CONNECTION_RETRY_DELAY)
                    }
                }

                if (!connected) {
                    throw ViaductInterpreterError("host ${hinfo.host} failed to connect to ${hinfo2.host}")
                }
            }
        }

        // accept connections from hosts with higher ID
        val incomingConnections: MutableSet<HostId> =
            hostInfoMap.values
                .filter { hinfo2 -> hinfo2.id > hinfo.id }
                .map { hinfo2 -> hinfo2.id }
                .toMutableSet()

        val serverSocket = ServerSocket(hinfo.address.port)
        while (incomingConnections.isNotEmpty()) {
            val clientSocket = serverSocket.accept()
            val clientHostId = clientSocket.getInputStream().read()
            val clientHost = getHostById(clientHostId)
            connectionMap[clientHost.host] = clientSocket
            incomingConnections.remove(clientHostId)

            logger.info { "accepted connection from host ${clientHost.host.name}" }
        }

        serverSocket.close()

        return connectionMap
    }

    fun start() {
        // check if all processes have registered bodies
        assert(
            processBodyMap.keys.containsAll(
                processInfoMap.keys.filter { k -> k.host == host && k.protocol !is Synchronization }
            )
        )

        val connectionMap: Map<Host, Socket> = createRemoteConnections()

        runBlocking {
            for (kv: Map.Entry<Host, HostInfo> in hostInfoMap) {
                if (host != kv.key) {
                    launch(Executors.newSingleThreadExecutor().asCoroutineDispatcher()) {
                        logger.info { "launching receiver thread for host ${kv.key.name}" }

                        ViaductReceiverThread(
                            kv.key,
                            connectionMap[kv.key]!!,
                            this@ViaductRuntime,
                            hostInfoMap[kv.key]!!.recvChannel
                        ).run()
                    }

                    launch(Executors.newSingleThreadExecutor().asCoroutineDispatcher()) {
                        logger.info { "launching sender thread for host ${kv.key.name}" }

                        ViaductSenderThread(
                            kv.key,
                            connectionMap[kv.key]!!,
                            this@ViaductRuntime,
                            hostInfoMap[kv.key]!!.sendChannel
                        ).run()
                    }
                }
            }

            // run interpreter
            val job: Job = launch {
                val processInterpreters =
                    processBodyMap.map { kv ->
                        logger.info { "created interpreter for protocol ${kv.key.protocol.asDocument.print()}" }
                        kv.key.protocol to kv.value(this@ViaductRuntime)
                    }.toMap()

                val interpreter =
                    BackendInterpreter(
                        program,
                        protocolAnalysis,
                        host,
                        processInterpreters,
                        ViaductProcessRuntime(
                            this@ViaductRuntime,
                            ProtocolProjection(syncProtocol, host)
                        )
                    )
                interpreter.run()
            }

            job.invokeOnCompletion {
                launch {
                    for (kv: Map.Entry<Host, HostInfo> in hostInfoMap) {
                        if (host != kv.key) {
                            kv.value.recvChannel.send(ShutdownMessage)
                            kv.value.sendChannel.send(ShutdownMessage)
                        }
                    }
                }
            }
        }

        for (kv in connectionMap) {
            logger.info { "closing connection to host ${kv.key.name}" }
            kv.value.close()
        }
    }
}

class ViaductProcessRuntime(
    private val runtime: ViaductRuntime,
    val projection: ProtocolProjection
) {
    suspend fun send(value: Value, receiver: ProtocolProjection) {
        runtime.send(value, projection, receiver)
    }

    suspend fun receive(sender: ProtocolProjection): Value {
        return runtime.receive(sender, projection)
    }

    suspend fun input(): Value {
        return runtime.input()
    }

    suspend fun output(value: Value) {
        runtime.output(value)
    }
}
