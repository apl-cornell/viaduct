package edu.cornell.cs.apl.viaduct.backend

import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.values.IntegerValue
import edu.cornell.cs.apl.viaduct.syntax.values.Value

class ViaductRuntime(host: Host) {
    fun send(value: Value, sender: ProtocolProjection, receiver: ProtocolProjection) {
    }

    fun receive(sender: ProtocolProjection, receiver: ProtocolProjection): Value {
        return IntegerValue(0)
    }

    fun input(): Value {
        return IntegerValue(0)
    }

    fun output(value: Value) {
    }
}

class ViaductProcessRuntime(
    val runtime: ViaductRuntime,
    private val projection: ProtocolProjection
) {
    fun send(value: Value, receiver: ProtocolProjection) {
        runtime.send(value, projection, receiver)
    }

    fun receive(sender: ProtocolProjection): Value {
        return runtime.receive(sender, projection)
    }

    fun input(): Value {
        return runtime.input()
    }

    fun output(value: Value) {
        runtime.output(value)
    }
}
