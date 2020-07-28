package edu.cornell.cs.apl.viaduct.backend

import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.Protocol

/* Provides helper classes for building C++ ASTs during backend compilation. */
abstract class CppBuilder {
    private fun protocolMangledName(protocol: Protocol): String {
        val processName = StringBuilder()
        processName.append(protocol.protocolName)
        processName.append("_")
        for (host: Host in protocol.hosts) {
            processName.append("_")
            processName.append(host.name)
        }
        return processName.toString()
    }

    fun protocolProjectionMangledName(protocol: Protocol, host: Host): String {
        return "${protocolMangledName(protocol)}__at__${host.name}"
    }

    protected val cppIntType = CppTypeName("int")
    protected val cppVoidType = CppTypeName("void")

    protected val runtimeIdent = "runtime"
    protected val assertFunction = "assert"

    // operation helpers
    fun methodCallStmt(receiver: CppExpression, method: CppIdentifier, vararg arguments: CppExpression) =
        CppCallStmt(
            CppMethodCall(
                receiver,
                method,
                *arguments
            )
        )

    fun methodCallExpr(receiver: CppExpression, method: CppIdentifier, vararg arguments: CppExpression) =
        CppCallExpr(
            CppMethodCall(
                receiver,
                method,
                *arguments
            )
        )

    fun read(variable: String) =
        CppReferenceRead(
            CppVariable(
                variable
            )
        )

    fun readArray(variable: CppIdentifier, index: CppExpression) =
        CppReferenceRead(
            CppArrayIndex(
                CppVariable(variable),
                index
            )
        )

    // command helpers

    fun declare(variable: CppIdentifier, type: CppType, initVal: CppExpression): CppStatement =
        CppVariableDeclAndAssignment(type, variable, initVal)

    fun declareArray(variable: CppIdentifier, elementType: CppType, length: CppExpression) =
        CppArrayDecl(elementType, variable, length)

    fun update(variable: CppIdentifier, rhs: CppExpression) =
        CppAssignment(
            CppVariable(
                variable
            ), rhs
        )

    fun updateArray(array: CppIdentifier, index: CppExpression, rhs: CppExpression) =
        CppAssignment(
            CppArrayIndex(
                CppVariable(array),
                index
            ), rhs
        )

    fun send(recvProtocol: Protocol, recvHost: Host, message: CppExpression) =
        methodCallStmt(
            read(runtimeIdent),
            "send",
            read(protocolProjectionMangledName(recvProtocol, recvHost)),
            message
        )

    fun receive(variable: CppIdentifier, type: CppType, senderProtocol: Protocol, senderHost: Host) =
        CppVariableDeclAndAssignment(
            type,
            variable,
            methodCallExpr(
                read(runtimeIdent),
                "receive",
                read(protocolProjectionMangledName(senderProtocol, senderHost))
            )
        )

    fun input(variable: CppIdentifier, type: CppType) =
        CppVariableDeclAndAssignment(
            type,
            variable,
            methodCallExpr(read(runtimeIdent), "input")
        )

    fun output(value: CppExpression) =
        methodCallStmt(read(runtimeIdent), "output", value)

    fun loop(body: CppBlock) =
        CppWhileLoop(CppTrue, body)

    fun loopBreak() = CppBreak

    fun delete(variable: CppIdentifier) =
        CppDelete(variable)

    fun deleteArray(variable: CppIdentifier) =
        CppDelete(variable, isArray = true)

    fun assert(expr: CppExpression) =
        CppCallStmt(
            CppFunctionCall(
                assertFunction,
                expr
            )
        )
}
