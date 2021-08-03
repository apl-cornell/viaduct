package edu.cornell.cs.apl.viaduct.codegeneration

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import edu.cornell.cs.apl.viaduct.analysis.NameAnalysis
import edu.cornell.cs.apl.viaduct.analysis.ProtocolAnalysis
import edu.cornell.cs.apl.viaduct.analysis.main
import edu.cornell.cs.apl.viaduct.errors.CodeGenerationError
import edu.cornell.cs.apl.viaduct.errors.ViaductInterpreterError
import edu.cornell.cs.apl.viaduct.selection.ProtocolCommunication
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
import kotlin.system.measureTimeMillis

private val logger = KotlinLogging.logger("Code Generation")

class BackendCodeGenerator(
    private val program: ProgramNode,
    private val protocolAnalysis: ProtocolAnalysis,
    private val codeGenerators: List<CodeGenerator>
) {
    private val codeGeneratorMap: Map<Protocol, CodeGenerator>
    private val nameAnalysis = NameAnalysis.get(program)

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

        val hostParticipatingProtocols =
            protocolAnalysis
                .participatingProtocols(program)

        for (protocol in hostParticipatingProtocols) {
            if (!currentProtocols.contains(protocol)) {
                throw ViaductInterpreterError("Code Generation: No backend for ${protocol.protocolName}")
            }
        }

        codeGeneratorMap = initGeneratorMap
    }

    fun generate(): String {
        val mainBody = program.main.body

        // TODO - come up with way to get source file name
        val fileBuilder = FileSpec.builder("", "TODO")

        logger.info { "starting code generation" }

        val duration = measureTimeMillis {
            for (host: Host in this.program.hosts) {
                val mainBuilder = FunSpec.builder("main_" + host.name)
                generate(mainBuilder, nameAnalysis.enclosingFunctionName(mainBody), mainBody, host)
                fileBuilder.addFunction(mainBuilder.build())
            }
        }

        logger.info { "finished code generation, total running time: ${duration}ms" }

        return fileBuilder.build().toString()
    }

    fun generate(
        mainFunctionBuilder: FunSpec.Builder,
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
                    mainFunctionBuilder.addStatement(protocolCodeGenerator.SimpleStatement(protocol, stmt).toString())

                    // generate code for sending data
                    if (readers.isNotEmpty()) {
                        mainFunctionBuilder.addStatement(protocolCodeGenerator.Send(stmt, protocol, reader!!, readerProtocol!!, events!!).toString())
                    }
                }

                // generate code for receiving data
                if (readers.isNotEmpty()) {
                    if (protocolAnalysis.participatingHosts(reader!!).contains(host)) {
                        val protocolCodeGenerator = codeGeneratorMap[readerProtocol]
                            ?: throw CodeGenerationError("no code generator for protocol ${protocol.asDocument.print()}")
                        mainFunctionBuilder.addStatement(protocolCodeGenerator.Recieve(stmt, protocol, reader, readerProtocol!!, events!!).toString())
                    }
                }
            }

            is SimpleStatementNode -> {
                if (protocolAnalysis.participatingHosts(stmt).contains(host)) {
                    val protocol = protocolAnalysis.primaryProtocol(stmt)
                    val protocolCodeGenerator = codeGeneratorMap[protocol]
                        ?: throw CodeGenerationError("no code generator for protocol ${protocol.asDocument.print()}")
                    mainFunctionBuilder.addStatement(protocolCodeGenerator.SimpleStatement(protocol, stmt).toString())
                }
            }

            //TODO
            is FunctionCallNode -> throw ViaductInterpreterError("TODO")

            is IfNode -> {
                if (protocolAnalysis.participatingHosts(stmt).contains(host)) {

                    //TODO - do I need to check if guardValue is a boolean? -interpreter does this but I feel like
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

                    //TODO - this needs verification, potential refactoring
                    mainFunctionBuilder.beginControlFlow("if ($guardValue)")
                    generate(mainFunctionBuilder, function, stmt.thenBranch, host)
                    mainFunctionBuilder.endControlFlow()
                    mainFunctionBuilder.beginControlFlow("else")
                    generate(mainFunctionBuilder, function, stmt.elseBranch, host)
                    mainFunctionBuilder.endControlFlow()
                }
            }

            is InfiniteLoopNode ->
                if (protocolAnalysis.participatingHosts(stmt).contains(host)) {
                    mainFunctionBuilder.beginControlFlow("while (true)")
                    generate(mainFunctionBuilder, function, stmt.body, host)
                    mainFunctionBuilder.endControlFlow()
                }

            is BreakNode -> {
                if (protocolAnalysis.participatingHosts(stmt).contains(host)) {
                    mainFunctionBuilder.addStatement("break")
                }
            }

            is BlockNode -> {
                for (child: StatementNode in stmt) {
                    generate(mainFunctionBuilder, function, child, host)
                }
            }

            is AssertionNode -> throw ViaductInterpreterError("TODO")
        }
    }
}
