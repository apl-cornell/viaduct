package io.github.apl_cornell.viaduct.syntax

import io.github.apl_cornell.viaduct.security.LabelExpression
import io.github.apl_cornell.viaduct.syntax.intermediate.HostDeclarationNode
import io.github.apl_cornell.viaduct.syntax.intermediate.ProgramNode
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
