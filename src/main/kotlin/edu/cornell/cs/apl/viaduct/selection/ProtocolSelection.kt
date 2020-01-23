package edu.cornell.cs.apl.viaduct.selection

import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.Variable
import edu.cornell.cs.apl.viaduct.syntax.intermediate.StatementNode
import kotlinx.collections.immutable.PersistentMap

typealias ProtocolMap = PersistentMap<Variable, Protocol>

interface ProtocolSelection {
    fun selectProtocols(context: ProtocolSelectionContext, stmt: StatementNode): ProtocolMap?
}
