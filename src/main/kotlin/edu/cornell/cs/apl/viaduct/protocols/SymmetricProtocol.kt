package edu.cornell.cs.apl.viaduct.protocols

import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.prettyprinting.nested
import edu.cornell.cs.apl.prettyprinting.plus
import edu.cornell.cs.apl.prettyprinting.tupled
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.Name
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import kotlinx.collections.immutable.toPersistentSet

/** A protocol that treats all involved hosts uniformly. */
abstract class SymmetricProtocol(hosts: Set<Host>) : Protocol {
    // Make an immutable copy
    final override val hosts: Set<Host> = hosts.toPersistentSet()

    final override val name: String
        get() {
            val hosts: List<String> = this.hosts.sorted().map(Name::name)
            return protocolName + hosts.joinToString(separator = ", ", prefix = "(", postfix = ")")
        }

    final override val asDocument: Document
        get() {
            val hosts: List<Document> = this.hosts.sorted().map(Host::asDocument)
            return Document(protocolName) + hosts.tupled().nested()
        }
}
