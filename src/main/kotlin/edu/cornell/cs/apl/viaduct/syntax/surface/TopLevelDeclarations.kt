package edu.cornell.cs.apl.viaduct.syntax.surface

import edu.cornell.cs.apl.viaduct.syntax.HostNode
import edu.cornell.cs.apl.viaduct.syntax.LabelNode
import edu.cornell.cs.apl.viaduct.syntax.ProtocolNode
import edu.cornell.cs.apl.viaduct.syntax.SourceLocation

/** A declaration at the top level of a file. */
sealed class TopLevelDeclarationNode : Node()

/**
 * Declaration of a participant and their authority.
 *
 * @param name Host name.
 * @param authority Label specifying the trust placed in this host.
 */
class HostDeclarationNode(
    val name: HostNode,
    val authority: LabelNode,
    override val sourceLocation: SourceLocation
) : TopLevelDeclarationNode()

/**
 * A process declaration associating a protocol with the code that process should run.
 *
 * @param protocol Name of the process.
 * @param body Code that will be executed by this process.
 */
class ProcessDeclarationNode(
    val protocol: ProtocolNode,
    val body: BlockNode,
    override val sourceLocation: SourceLocation
) : TopLevelDeclarationNode()
