package io.github.apl_cornell.viaduct.circuitbackends.cleartext
//
//import io.github.apl_cornell.viaduct.selection.ProtocolFactory
//import io.github.apl_cornell.viaduct.syntax.Protocol
//import io.github.apl_cornell.viaduct.syntax.circuit.ProgramNode
//import io.github.apl_cornell.viaduct.syntax.circuit.VariableDeclarationNode
//import io.github.apl_cornell.viaduct.util.subsequences
//
//class ReplicationProtocolFactory(val program: ProgramNode) : ProtocolFactory {
//    val protocols: Set<Protocol> =
//        program.hosts.sorted().subsequences().filter { it.size >= 2 }.map { Replication(it.toSet()) }.toSet()
//
////    override fun viableProtocols(node: VariableDeclarationNode): Set<Protocol> = protocols
//}
