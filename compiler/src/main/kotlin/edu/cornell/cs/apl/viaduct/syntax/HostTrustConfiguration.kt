package edu.cornell.cs.apl.viaduct.syntax

import edu.cornell.cs.apl.viaduct.security.LabelExpression
import edu.cornell.cs.apl.viaduct.syntax.intermediate.HostDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.toPersistentMap

/** A map that associates each host with its authority label. */
class HostTrustConfiguration
private constructor(
    authority: PersistentMap<Host, LabelExpression>
) : Map<Host, LabelExpression> by authority, (Host) -> LabelExpression {
    constructor(program: ProgramNode) : this(
        program.filterIsInstance<HostDeclarationNode>()
            .associate { Pair(it.name.value, it.authority.value) }.toPersistentMap()
    )

    override fun invoke(host: Host): LabelExpression = getValue(host)
}
