package edu.cornell.cs.apl.viaduct.codegeneration

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName
import edu.cornell.cs.apl.prettyprinting.joined
import edu.cornell.cs.apl.viaduct.analysis.NameAnalysis
import edu.cornell.cs.apl.viaduct.analysis.ProtocolAnalysis
import edu.cornell.cs.apl.viaduct.analysis.main
import edu.cornell.cs.apl.viaduct.errors.CodeGenerationError
import edu.cornell.cs.apl.viaduct.runtime.Boxed
import edu.cornell.cs.apl.viaduct.runtime.Runtime
import edu.cornell.cs.apl.viaduct.selection.ProtocolCommunication
import edu.cornell.cs.apl.viaduct.selection.SimpleProtocolComposer
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.ObjectVariable
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.Temporary
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AssertionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BlockNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.BreakNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.FunctionCallNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.IfNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.InfiniteLoopNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LiteralNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ObjectDeclarationArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutParameterArgumentNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReadNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.SimpleStatementNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.StatementNode
import edu.cornell.cs.apl.viaduct.util.FreshNameGenerator

class BackendCodeGenerator(
    private val program: ProgramNode,
    codeGenerators: List<(context: CodeGeneratorContext) -> CodeGenerator>,
    private val fileName: String,
    private val packageName: String
) {
    private val codeGeneratorMap: Map<Protocol, CodeGenerator>
    private val nameAnalysis = NameAnalysis.get(program)
    private val protocolAnalysis = ProtocolAnalysis(program, SimpleProtocolComposer)
    private val context = Context(program)

    init {
        val allProtocols = protocolAnalysis.participatingProtocols(program)
        val initGeneratorMap: MutableMap<Protocol, CodeGenerator> = mutableMapOf()

        // for now, assign all protocols to use plaintext
        for (protocol in allProtocols) {
            initGeneratorMap[protocol] = codeGenerators[0](context)
        }
        codeGeneratorMap = initGeneratorMap

        // TODO - not sure if we need these checks or what this will look like until we have support for multiple protocols

//        val currentProtocols: MutableSet<Protocol> = mutableSetOf()
//
//        for (protocol in allProtocols) {
//            initGeneratorMap[protocol] = codeGenerators[0]
//        }
//
//        for (codeGenerator in codeGenerators) {
//            for (protocol in codeGenerator.availableProtocols) {
//                if (currentProtocols.contains(protocol)) {
//                    throw CodeGenerationError("Code Generation: multiple code generators for protocol ${protocol.asDocument.print()}")
//                } else {
//                    currentProtocols.add(protocol)
//                    initGeneratorMap[protocol] = codeGenerator
//                }
//            }
//        }
//        val hostParticipatingProtocols = protocolAnalysis.participatingProtocols(program)
//
//        for (protocol in hostParticipatingProtocols) {
//            if (!currentProtocols.contains(protocol)) {
//                throw CodeGenerationError("Code Generation: No backend for ${protocol.protocolName}")
//            }
    }

    private fun addHostDeclarations(fileBuilder: FileSpec.Builder) {
        // add a global host object for each host
        for (host: Host in this.program.hosts) {
            fileBuilder.addProperty(
                PropertySpec.builder(host.name, Host::class)
                    .initializer(
                        CodeBlock.of(
                            "%T(%S)",
                            Host::class,
                            host.name
                        )
                    )
                    .build()
            )
        }
    }

    private fun generateHostFunction(
        fileBuilder: FileSpec.Builder,
        host: Host,
        hostFunName: String,
        mainBody: BlockNode
    ) {
        // for each host, create a function that they call to run the program
        val hostFunctionBuilder = FunSpec.builder(hostFunName).addModifiers(KModifier.PRIVATE, KModifier.SUSPEND)

        // pass runtime object to [host]'s function
        hostFunctionBuilder.addParameter("runtime", Runtime::class)

        // generate code for [host]'s role in [this.program]
        generate(hostFunctionBuilder, mainBody, host)
        fileBuilder.addFunction(hostFunctionBuilder.build())
    }

    fun generate(): String {
        val mainBody = program.main.body

        // create a main file builder, main function builder
        val fileBuilder = FileSpec.builder(packageName, this.fileName)

        // add top level declarations to main file
        addHostDeclarations(fileBuilder)

        val mainFunctionBuilder = FunSpec.builder("main").addModifiers(KModifier.SUSPEND)
        mainFunctionBuilder.addParameter("host", Host::class)
        mainFunctionBuilder.addParameter("runtime", Runtime::class)

        // create switch statement in main method so program can be run on any host
        mainFunctionBuilder.beginControlFlow("when (host)")
        for (host in this.program.hosts) {
            val hostFunName = context.freshNameGenerator.getFreshName(host.name)
            mainFunctionBuilder.addStatement(
                "%N -> %N(%N)",
                host.name,
                hostFunName,
                "runtime"
            )

            // generate a function for [host] to run
            generateHostFunction(fileBuilder, host, hostFunName, mainBody)
        }

        mainFunctionBuilder.endControlFlow()
        fileBuilder.addFunction(mainFunctionBuilder.build())

        return fileBuilder.build().toString()
    }

    fun generate(
        hostFunctionBuilder: FunSpec.Builder,
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

                    // generate code for the statement, if [host] participating
                    val protocolCodeGenerator = codeGeneratorMap[protocol]
                        ?: throw CodeGenerationError("no code generator for protocol ${protocol.asDocument.print()}")
                    hostFunctionBuilder.addStatement("%L", protocolCodeGenerator.simpleStatement(protocol, stmt))

                    // generate code for sending data
                    if (readers.isNotEmpty()) {
                        hostFunctionBuilder.addCode("%L", protocolCodeGenerator.send(host, stmt, protocol, readerProtocol!!, events!!))
                    }
                }

                // generate code for receiving data
                if (readers.isNotEmpty()) {
                    if (protocolAnalysis.participatingHosts(reader!!).contains(host)) {
                        val protocolCodeGenerator = codeGeneratorMap[readerProtocol]
                            ?: throw CodeGenerationError("no code generator for protocol ${protocol.asDocument.print()}")
                        hostFunctionBuilder.addCode("%L", protocolCodeGenerator.receive(host, stmt, protocol, readerProtocol!!, events!!))
                    }
                }
            }

            is SimpleStatementNode -> {
                if (protocolAnalysis.participatingHosts(stmt).contains(host)) {
                    val protocol = protocolAnalysis.primaryProtocol(stmt)
                    val protocolCodeGenerator = codeGeneratorMap[protocol]
                        ?: throw CodeGenerationError("no code generator for protocol ${protocol.asDocument.print()}")
                    hostFunctionBuilder.addStatement("%L", protocolCodeGenerator.simpleStatement(protocol, stmt))
                }
            }

            is FunctionCallNode -> {

                // get all ObjectDeclarationArgumentNodes from [stmt]
                val outObjectDeclarations = stmt.arguments.filterIsInstance<ObjectDeclarationArgumentNode>()

                // create a new list of arguments without ObjectDeclarationArgumentNodes
                val newArguments = stmt.arguments.filter { argument -> argument !is ObjectDeclarationArgumentNode }.toMutableList()

                for (i in 0..outObjectDeclarations.size) {

                    // declare boxed variable before function call
                    hostFunctionBuilder.addStatement(
                        "var %L = %T",
                        context.kotlinName(outObjectDeclarations[i].name.value),
                        Boxed::class.asClassName()
                    )

                    // add out parameter for declared object
                    newArguments +=
                        OutParameterArgumentNode(
                            outObjectDeclarations[i].name,
                            outObjectDeclarations[i].sourceLocation
                        )
                }

                // call function
                hostFunctionBuilder.addStatement(
                    "%L(%L)",
                    stmt.name,
                    newArguments.joined().toString()
                )

                // unbox boxes that were created before function call
                for (i in 0..outObjectDeclarations.size) {
                    hostFunctionBuilder.addStatement(
                        "val %L = %L.get()",
                        context.kotlinName(outObjectDeclarations[i].name.value),
                        context.kotlinName(outObjectDeclarations[i].name.value)
                    )
                }
            }

            is IfNode -> {
                if (protocolAnalysis.participatingHosts(stmt).contains(host)) {
                    val guardValue: CodeBlock =
                        when (val guard = stmt.guard) {

                            // TODO() - is there any way that we can make this not go through toString?
                            is LiteralNode -> {
                                CodeBlock.of("%L", guard.value.asDocument.toString())
                            }
                            is ReadNode -> {
                                val guardProtocol = protocolAnalysis.primaryProtocol(guard)
                                val protocolCodeGenerator = codeGeneratorMap[guardProtocol]
                                    ?: throw CodeGenerationError("no code generator for protocol ${guardProtocol.asDocument.print()}")
                                protocolCodeGenerator.guard(guardProtocol, guard)
                            }
                        }

                    hostFunctionBuilder.nextControlFlow("if (%L)", guardValue)
                    generate(hostFunctionBuilder, stmt.thenBranch, host)
                    hostFunctionBuilder.nextControlFlow("else")
                    generate(hostFunctionBuilder, stmt.elseBranch, host)
                }
            }

            is InfiniteLoopNode ->
                if (protocolAnalysis.participatingHosts(stmt).contains(host)) {
                    hostFunctionBuilder.beginControlFlow("while (true)")
                    generate(hostFunctionBuilder, stmt.body, host)
                    hostFunctionBuilder.endControlFlow()
                }

            is BreakNode -> {
                if (protocolAnalysis.participatingHosts(stmt).contains(host)) {
                    hostFunctionBuilder.addStatement("break")
                }
            }

            is BlockNode -> {
                for (child: StatementNode in stmt) {
                    generate(hostFunctionBuilder, child, host)
                }
            }

            is AssertionNode -> throw CodeGenerationError("TODO")
        }
    }

    private class Context(override val program: ProgramNode) : CodeGeneratorContext {
        private var tempMap: MutableMap<Pair<Temporary, Protocol>, String> = mutableMapOf()
        private var varMap: MutableMap<ObjectVariable, String> = mutableMapOf()

        private val receiveMember = MemberName(Runtime::class.java.packageName, "receive")
        private val sendMember = MemberName(Runtime::class.java.packageName, "send")

        val freshNameGenerator: FreshNameGenerator = FreshNameGenerator().apply {
            this.getFreshName("runtime")
            program.hosts.forEach { this.getFreshName(it.name) }
        }

        override fun kotlinName(sourceName: Temporary, protocol: Protocol): String =
            tempMap.getOrPut(Pair(sourceName, protocol)) { freshNameGenerator.getFreshName(sourceName.name.drop(1)) }

        override fun kotlinName(sourceName: ObjectVariable): String =
            varMap.getOrPut(sourceName) { freshNameGenerator.getFreshName(sourceName.name) }

        override fun newTemporary(baseName: String): String =
            freshNameGenerator.getFreshName(baseName)

        // TODO: properly compute host name
        override fun receive(type: TypeName, sender: Host): CodeBlock =
            CodeBlock.of("%N.%M<%T>(%N)", "runtime", receiveMember, type, sender.name)

        // TODO: properly compute host name
        override fun send(value: CodeBlock, receiver: Host): CodeBlock =
            CodeBlock.of("%N.%M(%L, %N)", "runtime", sendMember, value, receiver.name)
    }
}
