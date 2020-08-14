package edu.cornell.cs.apl.viaduct.backend

import edu.cornell.cs.apl.viaduct.analysis.TypeAnalysis
import edu.cornell.cs.apl.viaduct.errors.ViaductInterpreterError
import edu.cornell.cs.apl.viaduct.protocols.Commitment
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.Temporary
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AtomicExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BlockNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.SendNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.UpdateNode
import edu.cornell.cs.apl.viaduct.syntax.values.Value
import java.util.Stack
import kotlinx.collections.immutable.PersistentMap

typealias Hash = Int

interface PushPop {
    fun pushContext()
    fun popContext()
    fun getContextMarker(): Int
    fun restoreContextMarker(marker: Int)
}

private class CommitmentSender(
    private val runtime: ViaductProcessRuntime,
    private val receivers: Set<Host>
) : AbstractBackendInterpreter() {

    private val tempStack: Stack<PersistentMap<Temporary, Value>> = Stack()

    private var tempStore: PersistentMap<Temporary, Value>
        get() {
            return tempStack.peek()
        }
        set(value) {
            tempStack.pop()
            tempStack.push(value)
        }

    override fun pushContext() {
        tempStack.push(tempStore)
    }

    override fun popContext() {
        tempStack.pop()
    }

    override fun getContextMarker(): Int {
        return tempStack.size
    }

    override fun restoreContext(marker: Int) {
        while (tempStack.size > marker) {
            tempStack.pop()
        }
    }

    override suspend fun runAtomicExpr(expr: AtomicExpressionNode): Value {
        TODO("runAtomic")
    }

    override suspend fun runDeclaration(stmt: DeclarationNode) {
        TODO("Not yet implemented")
    }

    override suspend fun runLet(stmt: LetNode) {
        TODO("Not yet implemented")
    }

    override suspend fun runOutput(stmt: OutputNode) {
        TODO("Not yet implemented")
    }

    override suspend fun runSend(stmt: SendNode) {
        TODO("Not yet implemented")
    }

    override suspend fun runUpdate(stmt: UpdateNode) {
        TODO("Not yet implemented")
    }
}

private class CommitmentReceiver(
    private val runtime: ViaductProcessRuntime,
    private val sender: Host
) : AbstractBackendInterpreter() {

    private val tempStack: Stack<PersistentMap<Temporary, Hash>> = Stack()

    private var tempStore: PersistentMap<Temporary, Hash>
        get() {
            return tempStack.peek()
        }
        set(value) {
            tempStack.pop()
            tempStack.push(value)
        }

    override fun pushContext() {
        tempStack.push(tempStore)
    }

    override fun popContext() {
        tempStack.pop()
    }

    override fun getContextMarker(): Int {
        return tempStack.size
    }

    override fun restoreContext(marker: Int) {
        while (tempStack.size > marker) {
            tempStack.pop()
        }
    }

    override suspend fun runAtomicExpr(expr: AtomicExpressionNode): Value {
        TODO("runAtomic")
    }

    override suspend fun runDeclaration(stmt: DeclarationNode) {
        TODO("Not yet implemented")
    }

    override suspend fun runLet(stmt: LetNode) {
        TODO("Not yet implemented")
    }

    override suspend fun runOutput(stmt: OutputNode) {
        TODO("Not yet implemented")
    }

    override suspend fun runSend(stmt: SendNode) {
        TODO("Not yet implemented")
    }

    override suspend fun runUpdate(stmt: UpdateNode) {
        TODO("Not yet implemented")
    }
}

class CommitmentBackend(
    val typeAnalysis: TypeAnalysis
) : ProtocolBackend {

    override suspend fun run(runtime: ViaductProcessRuntime, process: BlockNode) {
        when (runtime.projection.protocol) {
            is Commitment ->
                if (runtime.projection.host == runtime.projection.protocol.sender) {
                    CommitmentSender(runtime, runtime.projection.protocol.receivers).run(process)
                } else {
                    CommitmentReceiver(runtime, runtime.projection.protocol.sender).run(process)
                }
            else ->
                throw ViaductInterpreterError("CommitmentBackend: unexpected runtime protocol")
        }
    }
}
