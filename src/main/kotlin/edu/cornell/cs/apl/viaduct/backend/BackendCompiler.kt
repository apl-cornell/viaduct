package edu.cornell.cs.apl.viaduct.backend

import edu.cornell.cs.apl.viaduct.analysis.NameAnalysis
import edu.cornell.cs.apl.viaduct.analysis.TypeAnalysis
import edu.cornell.cs.apl.viaduct.cli.print
import edu.cornell.cs.apl.viaduct.protocols.HostInterface
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.intermediate.HostDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProcessDeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.TopLevelDeclarationNode
import java.io.File

class BackendCompiler(
    val nameAnalysis: NameAnalysis,
    val typeAnalysis: TypeAnalysis
) : CppBuilder() {
    val backendMap: MutableMap<String, CppBackend> = mutableMapOf()

    private val runtimeType = CppReferenceType(CppTypeName("ViaductRuntime"))
    private val runtimeProcessType = CppReferenceType(CppTypeName("ViaductProcessRuntime"))
    private val runtimeRegisterProcessMethod = "registerProcess"
    private val runtimeGetPartyMethod = "getParty"

    private val startFunction = "start"

    private val startParameters: MutableList<CppFormalDecl> =
        mutableListOf(CppFormalDecl(runtimeType, runtimeIdent))

    private fun processFunctionName(protocol: Protocol, host: Host): String =
        "func_${protocolProjectionMangledName(protocol, host)}"

    private fun processObjectName(protocol: Protocol, host: Host): String =
        "proc_${protocolProjectionMangledName(protocol, host)}"

    private fun runtimeRegisterProcess(processId: CppIdentifier, hostId: Int, processObj: CppIdentifier) =
        methodCallStmt(
            read(runtimeIdent),
            runtimeRegisterProcessMethod,
            read(processId),
            CppIntLiteral(hostId),
            read(processObj)
        )

    private fun runtimeGetParty() =
        methodCallExpr(read(runtimeIdent), runtimeGetPartyMethod)

    fun registerBackend(backend: CppBackend) {
        for (protocolName: String in backend.supportedProtocols) {
            if (backendMap.containsKey(protocolName)) {
                throw Error("backend compilation: multiple backends registered for $protocolName")
            } else {
                backendMap[protocolName] = backend
                startParameters.addAll(backend.extraStartArguments)
            }
        }
    }

    fun compile(splitProgram: ProgramNode, output: File?) {
        val hostIdMap: MutableMap<Host, Int> = mutableMapOf()
        var curHostId = 0

        for (decl: TopLevelDeclarationNode in splitProgram) {
            if (decl is HostDeclarationNode) {
                hostIdMap[decl.name.value] = curHostId
                curHostId++
            }
        }

        val processIdDeclarations: MutableList<CppDefineMacro> = mutableListOf()
        val functionDeclarations: MutableList<CppFunctionDecl> = mutableListOf()
        val processDeclarations: MutableList<CppStatement> = mutableListOf()
        val processRegistrations: MutableList<CppStatement> = mutableListOf()

        var procId = 0
        for (decl: TopLevelDeclarationNode in splitProgram) {
            if (decl is ProcessDeclarationNode) {
                val protocol: Protocol = decl.protocol.value
                if (protocol !is HostInterface) {
                    for (host: Host in protocol.hosts) {
                        val hostId: Int =
                            hostIdMap[host] ?: throw Error("backend compilation: no id for host ${host.name}")

                        backendMap[protocol.protocolName]?.let { backend: CppBackend ->
                            val protocolId: CppIdentifier = protocolProjectionMangledName(protocol, host)
                            val functionName: CppIdentifier = processFunctionName(protocol, host)
                            val processName: CppIdentifier = processObjectName(protocol, host)

                            val hostProcess: CppBlock = backend.compile(decl.body, protocol, host)

                            val cppFormalParams: MutableList<CppFormalDecl> =
                                mutableListOf(CppFormalDecl(runtimeProcessType, runtimeIdent))
                            cppFormalParams.addAll(backend.extraFunctionArguments(protocol))

                            processIdDeclarations.add(
                                CppDefineMacro(
                                    protocolId,
                                    CppIntLiteral(procId)
                                )
                            )

                            functionDeclarations.add(
                                CppFunctionDecl(
                                    type = cppVoidType,
                                    name = functionName,
                                    arguments = cppFormalParams,
                                    body = hostProcess
                                )
                            )

                            processDeclarations.addAll(
                                backend.buildProcessObject(protocol, processName, functionName)
                            )

                            processRegistrations.add(
                                runtimeRegisterProcess(
                                    processId = processName,
                                    hostId = hostId,
                                    processObj = processName
                                )
                            )
                        } ?: throw Error("backend compilation: no backend registered for ${protocol.protocolName}")

                        procId++
                    }
                }
            }
        }

        val startStmts: MutableList<CppStatement> = mutableListOf()
        startStmts.addAll(processDeclarations)
        startStmts.addAll(processRegistrations)

        val startFunction =
            CppFunctionDecl(
                type = cppVoidType,
                name = startFunction,
                arguments = startParameters,
                body = CppBlock(startStmts)
            )

        val topLevelDecls: MutableList<CppTopLevelDeclaration> = mutableListOf()

        // host id defines
        for (hostIdPair: MutableMap.MutableEntry<Host, Int> in hostIdMap) {
            topLevelDecls.add(
                CppDefineMacro(name = hostIdPair.key.name, value = CppIntLiteral(hostIdPair.value))
            )
        }

        // process id defines
        topLevelDecls.addAll(processIdDeclarations)
        topLevelDecls.addAll(functionDeclarations)
        topLevelDecls.add(startFunction)

        val cppProgram = CppProgram(topLevelDecls)
        output.print(cppProgram)
    }
}
