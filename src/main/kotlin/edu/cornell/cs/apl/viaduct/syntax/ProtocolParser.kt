package edu.cornell.cs.apl.viaduct.syntax

import edu.cornell.cs.apl.viaduct.protocols.GenericProtocol

class ProtocolParser {
    val factoryMap: MutableMap<String, ProtocolFactory> = mutableMapOf()

    fun registerProtocolFactory(factory: ProtocolFactory) {
        factoryMap[factory.protocolName] = factory
    }

    fun parseProtocol(protocol: Protocol): Protocol {
        return when (protocol) {
            is GenericProtocol -> {
                val actualProtocolName: String = protocol.actualProtocolName

                factoryMap[actualProtocolName]?.let { factory ->
                    factory.buildProtocol(protocol.participants)
                } ?: throw Exception("cannot parse unknown protocol $actualProtocolName")
            }

            else -> protocol
        }
    }
}
