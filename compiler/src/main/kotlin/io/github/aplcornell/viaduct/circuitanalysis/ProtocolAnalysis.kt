package io.github.aplcornell.viaduct.circuitanalysis

import io.github.aplcornell.viaduct.syntax.Protocol
import io.github.aplcornell.viaduct.syntax.circuit.CircuitDeclarationNode
import io.github.aplcornell.viaduct.syntax.circuit.Node
import io.github.aplcornell.viaduct.syntax.circuit.VariableBindingNode

fun Node.protocols(): Iterable<Protocol> {
    val protocols = mutableSetOf<Protocol>()

    fun visit(node: Node) {
        when (node) {
            is CircuitDeclarationNode -> protocols.add(node.protocol.value)
            is VariableBindingNode -> protocols.add(node.protocol.value)
            else -> {}
        }
        node.children.forEach(::visit)
    }
    visit(this)
    return protocols.sorted()
}
