package edu.cornell.cs.apl.viaduct.codegeneration

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.MAP
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.asClassName
import edu.cornell.cs.apl.viaduct.analysis.NameAnalysis
import edu.cornell.cs.apl.viaduct.analysis.ProtocolAnalysis
import edu.cornell.cs.apl.viaduct.errors.CodeGenerationError
import edu.cornell.cs.apl.viaduct.errors.ViaductInterpreterError
import edu.cornell.cs.apl.viaduct.selection.ProtocolCommunication
import edu.cornell.cs.apl.viaduct.selection.SimpleProtocolComposer
import edu.cornell.cs.apl.viaduct.syntax.FunctionName
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AssertionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BlockNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BreakNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.FunctionCallNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.IfNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InfiniteLoopNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LiteralNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReadNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.SimpleStatementNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.StatementNode
import mu.KotlinLogging

private val logger = KotlinLogging.logger("Code Generation")

// TODO - resolve package imports
class BackendCodeGenerator(
    private val program: ProgramNode,

    // TODO - change this map to <Host, HostAddress> once imports are resolved
    private val customConnectionInfo: Map<Host, String>,

    // TODO - uncomment this once imports are resolved
    // private val strategy: Strategy,
    private val codeGenerators: List<CodeGenerator>,
    private val fileName: String
) {
    private val codeGeneratorMap: Map<Protocol, CodeGenerator>
    private val nameAnalysis = NameAnalysis.get(program)
    private val protocolAnalysis = ProtocolAnalysis(program, SimpleProtocolComposer)
    private val connectionMap: Map<Host, String>
    private val runtimeClassName = "edu.cornell.cs.apl.viaduct.backend.ViaductRuntime"::class.asClassName()
    private val protocolAnalysisClassName = ProtocolAnalysis::class.asClassName()
    private val hostClassName = Host::class.asClassName()
    private val hostAddressClassName = "edu.cornell.cs.apl.viaduct.backend.HostAddress"::class.asClassName()
    private val strategyClassName = "edu.cornell.cs.apl.viaduct.backend.IO.Strategy"::class.asClassName()

    private val ProtocolBackendClassName = "edu.cornell.cs.apl.viaduct.backend"::class.asClassName()
    private val PlaintextProtocolInterpreterClassName = "edu.cornell.cs.apl.viaduct.backend.PlaintextProtocolInterpreter"::class.asClassName()
    private val ABYProtocolInterpreterClassName = "edu.cornell.cs.apl.viaduct.backend.ABYProtocolInterpreter"::class.asClassName()
    private val CommitmentProtocolInterpreterFactoryClassName = "edu.cornell.cs.apl.viaduct.backend.commitment"::class.asClassName()
    private val ZKPProtocolInterpreterFactoryClassName = "edu.cornell.cs.apl.viaduct.backend.zkp"::class.asClassName()


    //TODO - figure out right way do do this once imports are working
    private val protocolBackends = setOf(
        PlaintextProtocolInterpreterClassName,
        ABYProtocolInterpreterClassName,
        CommitmentProtocolInterpreterFactoryClassName,
        ZKPProtocolInterpreterFactoryClassName
    )

    init {
        val initGeneratorMap: MutableMap<Protocol, CodeGenerator> = mutableMapOf()
        val currentProtocols: MutableSet<Protocol> = mutableSetOf()
        for (codeGenerator in codeGenerators) {
            for (protocol in codeGenerator.availableProtocols) {
                if (currentProtocols.contains(protocol)) {
                    throw CodeGenerationError("Code Generation: multiple code generators for protocol ${protocol.asDocument.print()}")
                } else {
                    currentProtocols.add(protocol)
                    initGeneratorMap[protocol] = codeGenerator
                }
            }
        }

//        val hostParticipatingProtocols = protocolAnalysis.participatingProtocols(program)
//
//        for (protocol in hostParticipatingProtocols) {
//            if (!currentProtocols.contains(protocol)) {
//                throw CodeGenerationError("Code Generation: No backend for ${protocol.protocolName}")
//            }
//        }

        codeGeneratorMap = initGeneratorMap

        var portNum = DEFAULT_PORT

        //TODO - change this map to <Host, HostAddress> once imports are resolved
        val initConnectionMap: Map<Host, String> =
            // custom connection info must provide info for all hosts or none
            if (customConnectionInfo.isEmpty()) {
                program.hostDeclarations
                    .map { hostDecl ->

                        // TODO change to: val addr = HostAddress(DEFAULT_ADDRESS, portNum) once imports are resolved
                        val addr = DEFAULT_ADDRESS + portNum.toString()
                        portNum++
                        Pair(hostDecl.name.value, addr)
                    }.toMap()
            } else {
                val allHostsDefined = program.hostDeclarations.all { hostDecl ->
                    customConnectionInfo.containsKey(hostDecl.name.value)
                }
                if (!allHostsDefined) {
                    throw ViaductInterpreterError("Some hosts do not have connection information.")
                }
                customConnectionInfo
            }
        connectionMap = initConnectionMap
    }

    companion object {
        const val DEFAULT_PORT = 5000
        const val DEFAULT_ADDRESS = "127.0.0.1"
    }

    fun generate(): String {
//        val mainBody = program.main.body

        // create a main file builder, main function builder
        val fileBuilder = FileSpec.builder("src", this.fileName)
        val mainFunctionBuilder = FunSpec.builder("main")
        mainFunctionBuilder.addParameter("args", String::class)

        // TODO - consider generating user input validation code

        // create protocol analysis class to feed to the runtime
        mainFunctionBuilder.addStatement(
            "val protocolAnalysis : %T = %T()",
            protocolAnalysisClassName,
            protocolAnalysisClassName
        )

        // TODO - figure out right way do do this once imports are working
        // create a set of protocol backends to feed to the runtime
        mainFunctionBuilder.addStatement(
            "val backendInterpreters : %T = setOf<%T>",
            LIST.parameterizedBy(ProtocolBackendClassName::class.asClassName()),
            LIST.parameterizedBy(ProtocolBackendClassName::class.asClassName()),
        )

//        for (backend in protocolBackends) {
//            mainFunctionBuilder.addStatement(
//                "backendInterpreters.add(%T)",
//                backend::class.asClassName()
//            )
//        }

        // declare host connection map
        mainFunctionBuilder.addStatement(
            "val hostConnectionInfo : %T = mapOf<%T>()",
            //TODO - change this map to <Host, HostAddress> once imports are resolved
            MAP.parameterizedBy(Host::class.asClassName(), String::class.asClassName()),
            MAP.parameterizedBy(Host::class.asClassName(), String::class.asClassName())
        )

        for (hostConnectionInfo in this.connectionMap) {
            mainFunctionBuilder.addStatement(
                "hostConnectionInfo[%L] = %L",
                hostConnectionInfo.key.name,
                hostConnectionInfo.value
            )
        }

        // TODO - declare strategy, is this done on a per host level?

        // create switch statement in main method so program can be run on any host
        mainFunctionBuilder.beginControlFlow("when(args[0])")

        for (host: Host in this.program.hosts) {

            // for each host, create a function that they call to run the program
            val hostFunName = host.name + '_' + this.fileName
            val hostFunctionBuilder = FunSpec.builder(hostFunName).addModifiers(KModifier.PRIVATE)

            // pass arguments to [host]'s function so they can construct a runtime object
            hostFunctionBuilder.addParameter(
                "protocolAnalysis",
                protocolAnalysisClassName
            )

            //TODO - change this map to <Host, HostAddress> once imports are resolved
            hostFunctionBuilder.addParameter(
                "hostConnectionInfo",
                MAP.parameterizedBy(Host::class.asClassName(), String::class.asClassName())
            )

            hostFunctionBuilder.addParameter(
                "backendInterpreters",
                LIST.parameterizedBy(ProtocolBackendClassName)
            )

            hostFunctionBuilder.addParameter(
                "strategy",
                strategyClassName
            )

            // create an object for representing [host]
            hostFunctionBuilder.addStatement(
                "val host : %T = %T(%L)",
                hostClassName,
                hostClassName,
                host.name
            )

            // create instance of the runtime for host [host]
            hostFunctionBuilder.addStatement(
                "val runtime : %T = ViaductRuntime(%L, %L, %L, %L, %L) ",
                runtimeClassName,
                "host",
                // TODO - figure out how to access a program node in generated code
                "protocolAnalysis",
                "hostConnectionInfo",
                "backendInterpreters",
                "strategy"
            )

            // generate code for [host]'s role in [this.program]
//            generate(hostFunctionBuilder, nameAnalysis.enclosingFunctionName(mainBody), mainBody, host)
            fileBuilder.addFunction(hostFunctionBuilder.build())

            // update switch statement in main method to have an option for [host]
            mainFunctionBuilder.beginControlFlow("is %S ->", host.name)

            // TODO - figure out how to access a program node in generated code
            mainFunctionBuilder.addStatement(
                "%L(%L, %L, %L, %L)",
                hostFunName,
                "protocolAnalysis",
                "hostConnectionInfo",
                "backendInterpreters",
                "strategy"
            )
            mainFunctionBuilder.endControlFlow()
        }

        mainFunctionBuilder.endControlFlow()
        fileBuilder.addFunction(mainFunctionBuilder.build())

        return fileBuilder.build().toString()
    }

    fun generate(
        hostFunctionBuilder: FunSpec.Builder,
        function: FunctionName,
        stmt: StatementNode,
        host: Host
    ) {
        when (stmt) {
            is LetNode -> {
                val protocol = protocolAnalysis.primaryProtocol(stmt)
                var reader: SimpleStatementNode? = null
                var readerProtocol: Protocol? = null
                var events: ProtocolCommunication? = null

                // there should only be a single reader, if any
                val readers = nameAnalysis.readers(stmt).filterIsInstance<SimpleStatementNode>()
                if (readers.isNotEmpty()) {
                    reader = readers.first()
                    readerProtocol = protocolAnalysis.primaryProtocol(reader)
                    events = protocolAnalysis.relevantCommunicationEvents(stmt, reader)
                }

                if (protocolAnalysis.participatingHosts(stmt).contains(host)) {

                    // generate code for the statement, if [this.host] participating
                    val protocolCodeGenerator = codeGeneratorMap[protocol]
                        ?: throw CodeGenerationError("no code generator for protocol ${protocol.asDocument.print()}")
                    hostFunctionBuilder.addStatement(protocolCodeGenerator.SimpleStatement(protocol, stmt).toString())

                    // generate code for sending data
                    if (readers.isNotEmpty()) {
                        hostFunctionBuilder.addStatement(protocolCodeGenerator.Send(stmt, protocol, reader!!, readerProtocol!!, events!!).toString())
                    }
                }

                // generate code for receiving data
                if (readers.isNotEmpty()) {
                    if (protocolAnalysis.participatingHosts(reader!!).contains(host)) {
                        val protocolCodeGenerator = codeGeneratorMap[readerProtocol]
                            ?: throw CodeGenerationError("no code generator for protocol ${protocol.asDocument.print()}")
                        hostFunctionBuilder.addStatement(protocolCodeGenerator.Recieve(stmt, protocol, reader, readerProtocol!!, events!!).toString())
                    }
                }
            }

            is SimpleStatementNode -> {
                if (protocolAnalysis.participatingHosts(stmt).contains(host)) {
                    val protocol = protocolAnalysis.primaryProtocol(stmt)
                    val protocolCodeGenerator = codeGeneratorMap[protocol]
                        ?: throw CodeGenerationError("no code generator for protocol ${protocol.asDocument.print()}")
                    hostFunctionBuilder.addStatement(protocolCodeGenerator.SimpleStatement(protocol, stmt).toString())
                }
            }

            // TODO
            is FunctionCallNode -> throw CodeGenerationError("TODO")

            is IfNode -> {
                if (protocolAnalysis.participatingHosts(stmt).contains(host)) {

                    // TODO - do I need to check if guardValue is a boolean? -interpreter does this but I feel like
                    // semantic analysis would accomplish this
                    var guardValue =
                        when (val guard = stmt.guard) {
                            is LiteralNode -> guard.value.asDocument.print()

                            is ReadNode -> {
                                val guardProtocol = protocolAnalysis.primaryProtocol(guard)
                                val protocolCodeGenerator = codeGeneratorMap[guardProtocol]
                                    ?: throw CodeGenerationError("no code generator for protocol ${guardProtocol.asDocument.print()}")
                                protocolCodeGenerator.Guard(guardProtocol, guard).toString()
                            }
                        }

                    hostFunctionBuilder.beginControlFlow("if ($guardValue)")
                    generate(hostFunctionBuilder, function, stmt.thenBranch, host)
                    hostFunctionBuilder.endControlFlow()
                    hostFunctionBuilder.beginControlFlow("else")
                    generate(hostFunctionBuilder, function, stmt.elseBranch, host)
                    hostFunctionBuilder.endControlFlow()
                }
            }

            is InfiniteLoopNode ->
                if (protocolAnalysis.participatingHosts(stmt).contains(host)) {
                    hostFunctionBuilder.beginControlFlow("while (true)")
                    generate(hostFunctionBuilder, function, stmt.body, host)
                    hostFunctionBuilder.endControlFlow()
                }

            is BreakNode -> {
                if (protocolAnalysis.participatingHosts(stmt).contains(host)) {
                    hostFunctionBuilder.addStatement("break")
                }
            }

            is BlockNode -> {
                for (child: StatementNode in stmt) {
                    generate(hostFunctionBuilder, function, child, host)
                }
            }

            is AssertionNode -> throw CodeGenerationError("TODO")
        }
    }
}
