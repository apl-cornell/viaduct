package io.github.aplcornell.viaduct.backends.aby

import io.github.aplcornell.viaduct.parsing.ProtocolArguments
import io.github.aplcornell.viaduct.parsing.ProtocolParser
import io.github.aplcornell.viaduct.syntax.values.HostValue

/** Parser for the [ABY] protocol. */
object ArithABYProtocolParser : ProtocolParser<ArithABY> {
    override fun parse(arguments: ProtocolArguments): ArithABY {
        val server = arguments.get<HostValue>("server")
        val client = arguments.get<HostValue>("client")
        checkArguments(arguments)
        return ArithABY(server.value, client.value)
    }
}

object BoolABYProtocolParser : ProtocolParser<BoolABY> {
    override fun parse(arguments: ProtocolArguments): BoolABY {
        val server = arguments.get<HostValue>("server")
        val client = arguments.get<HostValue>("client")
        checkArguments(arguments)
        return BoolABY(server.value, client.value)
    }
}

object YaoABYProtocolParser : ProtocolParser<YaoABY> {
    override fun parse(arguments: ProtocolArguments): YaoABY {
        val server = arguments.get<HostValue>("server")
        val client = arguments.get<HostValue>("client")
        checkArguments(arguments)
        return YaoABY(server.value, client.value)
    }
}

private fun checkArguments(arguments: ProtocolArguments) {
    val server = arguments.get<HostValue>("server")
    arguments.getAndAlso<HostValue>("client") {
        if (server == it) {
            throw IllegalArgumentException("client must be different from server")
        }
    }
}
