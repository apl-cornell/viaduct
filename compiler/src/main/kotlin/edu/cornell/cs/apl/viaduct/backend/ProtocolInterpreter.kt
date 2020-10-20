package edu.cornell.cs.apl.viaduct.backend

import edu.cornell.cs.apl.viaduct.analysis.ProtocolAnalysis
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AtomicExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.FunctionArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ParameterNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.SimpleStatementNode
import edu.cornell.cs.apl.viaduct.syntax.values.Value
import kotlinx.collections.immutable.PersistentMap

interface ProtocolInterpreter {
    suspend fun runExprAsValue(expr: AtomicExpressionNode): Value

    suspend fun runSimpleStatement(stmt: SimpleStatementNode)

    suspend fun pushContext()

    suspend fun popContext()

    suspend fun pushFunctionContext(arguments: PersistentMap<ParameterNode, FunctionArgumentNode>)

    suspend fun popFunctionContext()

    fun getContextMarker(): Int

    suspend fun restoreContext(marker: Int)
}

interface ProtocolInterpreterFactory {
    fun buildProtocolInterpreter(
        program: ProgramNode,
        protocolAnalysis: ProtocolAnalysis,
        runtime: ViaductProcessRuntime,
        connectionMap: Map<Host, HostAddress>
    ): ProtocolInterpreter
}
