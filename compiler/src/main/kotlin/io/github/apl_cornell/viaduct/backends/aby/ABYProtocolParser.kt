package io.github.apl_cornell.viaduct.backends.aby

import io.github.apl_cornell.viaduct.parsing.ProtocolArguments
import io.github.apl_cornell.viaduct.parsing.ProtocolParser
import io.github.apl_cornell.viaduct.syntax.values.HostValue

/** Parser for the [ABY] protocol. */
object ArithABYProtocolParser : ProtocolParser<ArithABY> {
    override fun parse(arguments: ProtocolArguments): ArithABY {
        val server = arguments.get<HostValue>("server")
        val client = arguments.get<HostValue>("client")
        return ArithABY(server.value, client.value)
    }
}

object BoolABYProtocolParser : ProtocolParser<BoolABY> {
    override fun parse(arguments: ProtocolArguments): BoolABY {
        val server = arguments.get<HostValue>("server")
        val client = arguments.get<HostValue>("client")
        return BoolABY(server.value, client.value)
    }
}

object YaoABYProtocolParser : ProtocolParser<YaoABY> {
    override fun parse(arguments: ProtocolArguments): YaoABY {
        val server = arguments.get<HostValue>("server")
        val client = arguments.get<HostValue>("client")
        return YaoABY(server.value, client.value)
    }
}