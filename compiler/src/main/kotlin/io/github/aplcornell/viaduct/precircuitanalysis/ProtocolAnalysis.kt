package io.github.aplcornell.viaduct.precircuitanalysis

import io.github.aplcornell.viaduct.syntax.Protocol
import io.github.aplcornell.viaduct.syntax.precircuit.CommandLetNode
import io.github.aplcornell.viaduct.syntax.precircuit.ComputeLetNode
import io.github.aplcornell.viaduct.syntax.precircuit.Node

fun Node.protocols(): Iterable<Protocol> {
    val protocols = mutableSetOf<Protocol>()

    fun visit(node: Node) {
        when (node) {
            is ComputeLetNode -> protocols.add(node.protocol.value)
            is CommandLetNode -> protocols.add(node.protocol.value)
            else -> {}
        }
        node.children.forEach(::visit)
    }
    visit(this)
    return protocols.sorted()
}
