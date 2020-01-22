package edu.cornell.cs.apl.viaduct.selection

import edu.cornell.cs.apl.viaduct.syntax.ArrayRead
import edu.cornell.cs.apl.viaduct.syntax.DefaultMethod
import edu.cornell.cs.apl.viaduct.syntax.TemporaryArgument
import edu.cornell.cs.apl.viaduct.syntax.Variable
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DowngradeNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.MethodRegistry
import edu.cornell.cs.apl.viaduct.syntax.intermediate.QueryNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReadNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.StatementNode
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.toPersistentMap

data class ProtocolSelectionMetadata(
    val isArrayIndex: Boolean,
    val isLoopGuard: Boolean,
    val isConditionalGuard: Boolean,
    val isInLoopBody: Boolean,
    val isConstructorArgument: Boolean,
    val isDowngradeExpression: Boolean
) {
    constructor() :
        this(
            false, false, false, false,
            false, false
        )
}

typealias MetadataMap = PersistentMap<Variable, ProtocolSelectionMetadata>

/** build metadata describing syntactic information about data and computations,
 *  to be used by protocol selection. */
class ProtocolSelectionMetadataBuilder<M : DefaultMethod>
private constructor(val methodRegistry: MethodRegistry<M>) {
    companion object {
        fun <M : DefaultMethod> run(
            methodRegistry: MethodRegistry<M>,
            stmt: StatementNode
        ): MetadataMap {
            val builder = ProtocolSelectionMetadataBuilder(methodRegistry)
            builder.buildStmtMetadata(stmt)
            return builder.metadataMap.toPersistentMap()
        }
    }

    private val metadataMap = mutableMapOf<Variable, ProtocolSelectionMetadata>()

    private fun updateMetadataMap(
        v: Variable,
        updateFunc: (ProtocolSelectionMetadata) -> ProtocolSelectionMetadata
    ) {
        val newMetadata = updateFunc(metadataMap[v] ?: ProtocolSelectionMetadata())
        metadataMap[v] = newMetadata
    }

    fun buildStmtMetadata(stmt: StatementNode) {
        when (stmt) {
            is LetNode -> {
                when (val letExpr = stmt.value) {
                    is QueryNode -> {
                        when (val method = methodRegistry.interpretMethodCall(letExpr)) {
                            is ArrayRead -> {
                                when (val arg = method.index) {
                                    is TemporaryArgument -> {
                                        updateMetadataMap(arg.temporary)
                                            { m -> m.copy(isArrayIndex = true) }
                                    }
                                }
                            }
                        }
                    }

                    is DowngradeNode -> {
                        when (val expr = stmt.value) {
                            is ReadNode -> {
                                updateMetadataMap(expr.temporary)
                                    { m -> m.copy(isDowngradeExpression = true) }
                            }
                        }
                    }
                }
            }

            is DeclarationNode -> {
                for (arg in stmt.arguments) {
                    when (arg) {
                        is ReadNode -> {
                            updateMetadataMap(arg.temporary)
                                { m -> m.copy(isConstructorArgument = true) }
                        }
                    }
                }
            }
        }
    }
}

