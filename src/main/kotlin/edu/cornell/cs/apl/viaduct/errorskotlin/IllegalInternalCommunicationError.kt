package edu.cornell.cs.apl.viaduct.errorskotlin

import edu.cornell.cs.apl.prettyprinting.Document
import edu.cornell.cs.apl.prettyprinting.times
import edu.cornell.cs.apl.viaduct.protocols.Ideal
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ExternalCommunicationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InternalCommunicationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProcessDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.surface.keyword

/**
 * Thrown when a [Protocol] (usually an [Ideal] protocol) contains an [InternalCommunicationNode]
 * when it is not allowed to do so.
 * These protocols are only allowed to communicate with hosts, not other protocols, so they should
 * be using [ExternalCommunicationNode]s.
 *
 * @param process Protocol containing the illegal statement.
 * @param illegalStatement The illegal communication statement.
 */
class IllegalInternalCommunicationError(
    private val process: ProcessDeclarationNode,
    private val illegalStatement: InternalCommunicationNode
) : CompilationError() {
    override val category: String
        get() = "Illegal Internal Communication"

    override val source: String
        get() = illegalStatement.sourceLocation.sourcePath

    override val description: Document
        get() =
            Document("Protocol") * process.protocol *
                Document("is not allowed to communicate with other protocols:")
                    .withSource(illegalStatement.sourceLocation)

    override val hint: Document?
        get() =
            Document("Use") * keyword("input") * "and" * keyword("output") *
                "if you want to communicate with hosts."
}
