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
import edu.cornell.cs.apl.viaduct.runtime.Runtime
import edu.cornell.cs.apl.viaduct.selection.ProtocolCommunication
import edu.cornell.cs.apl.viaduct.selection.SimpleProtocolComposer
import edu.cornell.cs.apl.viaduct.syntax.FunctionName
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
    private val fileName: String
) {
    private val codeGeneratorMap: Map<Protocol, CodeGenerator>
    private val nameAnalysis = NameAnalysis.get(program)
    private val protocolAnalysis = ProtocolAnalysis(program, SimpleProtocolComposer)
    private val hostClassName = Host::class.asClassName()
    private val runtimeClassName = Runtime::class.asClassName()
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

    fun generate(): String {
        val mainBody = program.main.body

        // create a main file builder, main function builder
        val fileBuilder = FileSpec.builder("src", this.fileName)
        val mainFunctionBuilder = FunSpec.builder("main").addModifiers(KModifier.SUSPEND)
        mainFunctionBuilder.addParameter("host", hostClassName)
        mainFunctionBuilder.addParameter("runtime", runtimeClassName)

        // create switch statement in main method so program can be run on any host
        mainFunctionBuilder.beginControlFlow("when(host)")

        // add a global host object for each host
        for (host: Host in this.program.hosts) {
            fileBuilder.addProperty(
                PropertySpec.builder(host.name, Host::class)
                    .initializer(
                        CodeBlock.of(
                            "Host(%S)",
                            host.name
                        )
                    )
                    .build()
            )
        }

        for (host: Host in this.program.hosts) {

            // for each host, create a function that they call to run the program
            val hostFunName = host.name
            val hostFunctionBuilder = FunSpec.builder(hostFunName).addModifiers(KModifier.PRIVATE, KModifier.SUSPEND)

            // pass runtime object to [host]'s function
            hostFunctionBuilder.addParameter("runtime", runtimeClassName)

            // generate code for [host]'s role in [this.program]
            generate(hostFunctionBuilder, nameAnalysis.enclosingFunctionName(mainBody), mainBody, host)
            fileBuilder.addFunction(hostFunctionBuilder.build())

            // update switch statement in main method to have an option for [host]
            mainFunctionBuilder.beginControlFlow("%N ->", host.name)

            mainFunctionBuilder.addStatement(
                "%N(%N)",
                hostFunName,
                "runtime"
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

                    // generate code for the statement, if [host] participating
                    val protocolCodeGenerator = codeGeneratorMap[protocol]
                        ?: throw CodeGenerationError("no code generator for protocol ${protocol.asDocument.print()}")
                    hostFunctionBuilder.addStatement(protocolCodeGenerator.simpleStatement(protocol, stmt).toString())

                    // generate code for sending data
                    if (readers.isNotEmpty()) {
                        hostFunctionBuilder.addStatement(protocolCodeGenerator.send(host, stmt, protocol, readerProtocol!!, events!!).toString())
                    }
                }

                // generate code for receiving data
                if (readers.isNotEmpty()) {
                    if (protocolAnalysis.participatingHosts(reader!!).contains(host)) {
                        val protocolCodeGenerator = codeGeneratorMap[readerProtocol]
                            ?: throw CodeGenerationError("no code generator for protocol ${protocol.asDocument.print()}")
                        hostFunctionBuilder.addStatement(protocolCodeGenerator.receive(host, stmt, protocol, readerProtocol!!, events!!).toString())
                    }
                }
            }

            is SimpleStatementNode -> {
                if (protocolAnalysis.participatingHosts(stmt).contains(host)) {
                    val protocol = protocolAnalysis.primaryProtocol(stmt)
                    val protocolCodeGenerator = codeGeneratorMap[protocol]
                        ?: throw CodeGenerationError("no code generator for protocol ${protocol.asDocument.print()}")
                    hostFunctionBuilder.addStatement(protocolCodeGenerator.simpleStatement(protocol, stmt).toString())
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
                        outObjectDeclarations[i].name.value.name,
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
                        outObjectDeclarations[i].name.value.name,
                        outObjectDeclarations[i].name.value.name
                    )
                }
            }

            is IfNode -> {
                if (protocolAnalysis.participatingHosts(stmt).contains(host)) {

                    // TODO - do I need to check if guardValue is a boolean? - interpreter does this but I feel like
                    // semantic analysis would accomplish this
                    val guardValue =
                        when (val guard = stmt.guard) {
                            is LiteralNode -> guard.value.asDocument.print()

                            is ReadNode -> {
                                val guardProtocol = protocolAnalysis.primaryProtocol(guard)
                                val protocolCodeGenerator = codeGeneratorMap[guardProtocol]
                                    ?: throw CodeGenerationError("no code generator for protocol ${guardProtocol.asDocument.print()}")
                                protocolCodeGenerator.guard(guardProtocol, guard).toString()
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

    private class Context(override val program: ProgramNode) : CodeGeneratorContext {
        private var tempMap: MutableMap<Temporary, String> = mutableMapOf()
        private var varMap: MutableMap<ObjectVariable, String> = mutableMapOf()
        private var baseNames = setOf<String>()

        private val receiveMember = MemberName(Runtime::class.java.packageName, "receive")
        private val sendMember = MemberName(Runtime::class.java.packageName, "send")

        init {
            val initNames: MutableSet<String> = program.hosts.map { host -> host.toString() }.toSet().toMutableSet()
            initNames += "runtime"
            baseNames = initNames
        }

        val freshNameGenerator: FreshNameGenerator = FreshNameGenerator(baseNames)

        override fun kotlinName(sourceName: Temporary, protocol: Protocol): String =
            tempMap.getOrPut(sourceName) { freshNameGenerator.getFreshName((sourceName.name + protocol.name).filter { it.isLetterOrDigit() }) }

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
