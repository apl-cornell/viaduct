package io.github.aplcornell.viaduct.codegeneration

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.SET
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.joinToCode
import io.github.aplcornell.viaduct.analysis.NameAnalysis
import io.github.aplcornell.viaduct.analysis.ProtocolAnalysis
import io.github.aplcornell.viaduct.analysis.TypeAnalysis
import io.github.aplcornell.viaduct.analysis.mainFunction
import io.github.aplcornell.viaduct.runtime.Out
import io.github.aplcornell.viaduct.runtime.ViaductGeneratedProgram
import io.github.aplcornell.viaduct.runtime.ViaductRuntime
import io.github.aplcornell.viaduct.selection.ProtocolComposer
import io.github.aplcornell.viaduct.syntax.Host
import io.github.aplcornell.viaduct.syntax.ObjectVariable
import io.github.aplcornell.viaduct.syntax.Protocol
import io.github.aplcornell.viaduct.syntax.Temporary
import io.github.aplcornell.viaduct.syntax.intermediate.AssertionNode
import io.github.aplcornell.viaduct.syntax.intermediate.BlockNode
import io.github.aplcornell.viaduct.syntax.intermediate.BreakNode
import io.github.aplcornell.viaduct.syntax.intermediate.DeclarationNode
import io.github.aplcornell.viaduct.syntax.intermediate.ExpressionArgumentNode
import io.github.aplcornell.viaduct.syntax.intermediate.FunctionArgumentNode
import io.github.aplcornell.viaduct.syntax.intermediate.FunctionCallNode
import io.github.aplcornell.viaduct.syntax.intermediate.FunctionDeclarationNode
import io.github.aplcornell.viaduct.syntax.intermediate.IfNode
import io.github.aplcornell.viaduct.syntax.intermediate.InfiniteLoopNode
import io.github.aplcornell.viaduct.syntax.intermediate.LetNode
import io.github.aplcornell.viaduct.syntax.intermediate.LiteralNode
import io.github.aplcornell.viaduct.syntax.intermediate.ObjectDeclarationArgumentNode
import io.github.aplcornell.viaduct.syntax.intermediate.ObjectReferenceArgumentNode
import io.github.aplcornell.viaduct.syntax.intermediate.OutParameterArgumentNode
import io.github.aplcornell.viaduct.syntax.intermediate.OutParameterConstructorInitializerNode
import io.github.aplcornell.viaduct.syntax.intermediate.OutParameterExpressionInitializerNode
import io.github.aplcornell.viaduct.syntax.intermediate.OutParameterInitializationNode
import io.github.aplcornell.viaduct.syntax.intermediate.OutputNode
import io.github.aplcornell.viaduct.syntax.intermediate.ProgramNode
import io.github.aplcornell.viaduct.syntax.intermediate.ReadNode
import io.github.aplcornell.viaduct.syntax.intermediate.SimpleStatementNode
import io.github.aplcornell.viaduct.syntax.intermediate.StatementNode
import io.github.aplcornell.viaduct.syntax.intermediate.UpdateNode
import io.github.aplcornell.viaduct.util.FreshNameGenerator
import java.util.LinkedList
import java.util.Queue
import javax.annotation.processing.Generated

private class BackendCodeGenerator(
    val program: ProgramNode,
    val host: Host,
    codeGenerator: (context: CodeGeneratorContext) -> CodeGenerator,
    val protocolComposer: ProtocolComposer,
    val hostDeclarations: TypeSpec,
) {
    private val typeAnalysis = TypeAnalysis.get(program)
    private val nameAnalysis = NameAnalysis.get(program)
    private val protocolAnalysis = ProtocolAnalysis(program, protocolComposer)
    private val context = Context()
    private val codeGenerator = codeGenerator(context)
    private val outBoxNames: MutableMap<ObjectVariable, String> = mutableMapOf()

    // Returns the kotlin name of the box of an out argument used in the source program
    private fun outBoxName(outVariable: ObjectVariable): String =
        outBoxNames.getOrPut(outVariable) { context.newTemporary(context.kotlinName(outVariable) + "_box") }

    fun generateClass(): TypeSpec {
        val classBuilder = TypeSpec.classBuilder(
            host.name.replaceFirstChar { it.uppercase() },
        ).primaryConstructor(
            FunSpec.constructorBuilder()
                .addParameter("runtime", ViaductRuntime::class)
                .build(),
        ).addProperty(
            PropertySpec.builder("runtime", ViaductRuntime::class)
                .initializer("runtime")
                .addModifiers(KModifier.PRIVATE)
                .build(),
        )

        for (protocol in protocolAnalysis.participatingProtocols(program)) {
            for (property in codeGenerator.setup(protocol)) {
                classBuilder.addProperty(property)
            }
        }

        for (function in program.functions) {
            classBuilder.addFunction(generate(function)).build()
        }
        return classBuilder.build()
    }

    /** Generates code for [host]'s role in the function [functionDeclaration]. */
    private fun generate(functionDeclaration: FunctionDeclarationNode): FunSpec {
        val hostFunctionBuilder = FunSpec.builder(functionDeclaration.name.value.name)
        hostFunctionBuilder.addModifiers(if (functionDeclaration.name.value == mainFunction) KModifier.PUBLIC else KModifier.PRIVATE)

        for (
        param in functionDeclaration.parameters.filter { param ->
            protocolAnalysis.primaryProtocol(param).hosts.contains(
                host,
            )
        }
        ) {
            val paramName = context.kotlinName(param.name.value)
            val paramType = codeGenerator.kotlinType(protocolAnalysis.primaryProtocol(param), typeAnalysis.type(param))
            if (param.isInParameter) {
                hostFunctionBuilder.addParameter(paramName, paramType)
            } else {
                hostFunctionBuilder.addParameter(
                    outBoxName(param.name.value),
                    Out::class.asClassName().parameterizedBy(paramType),
                )
                hostFunctionBuilder.addStatement("val %N: %T", paramName, paramType)
            }
        }
        generate(hostFunctionBuilder, functionDeclaration.body, host)
        return hostFunctionBuilder.build()
    }

    private fun generate(hostFunctionBuilder: FunSpec.Builder, stmt: StatementNode, host: Host) {
        when (stmt) {
            is LetNode -> {
                val protocol = protocolAnalysis.primaryProtocol(stmt)

                val readers: MutableMap<Protocol, SimpleStatementNode> = mutableMapOf()
                for (reader in nameAnalysis.readers(stmt).filterIsInstance<SimpleStatementNode>()) {
                    readers[protocolAnalysis.primaryProtocol(reader)] = reader
                }

                if (protocolAnalysis.participatingHosts(stmt).contains(host)) {
                    hostFunctionBuilder.addComment(stmt.toDocument().print())
                    // generate code for the statement, if [host] participating
                    hostFunctionBuilder.addStatement("%L", simpleStatement(protocol, stmt))

                    // generate code for sending data
                    for (reader in readers) {
                        hostFunctionBuilder.addCode(
                            "%L",
                            codeGenerator.send(
                                stmt,
                                protocol,
                                reader.key,
                                protocolAnalysis.relevantCommunicationEvents(stmt, reader.value),
                            ),
                        )
                    }
                }

                // generate code for receiving data
                for (reader in readers) {
                    if (protocolAnalysis.participatingHosts(reader.value).contains(host)) {
                        hostFunctionBuilder.addCode(
                            codeGenerator.receive(
                                stmt,
                                protocol,
                                reader.key,
                                protocolAnalysis.relevantCommunicationEvents(stmt, reader.value),
                            ),
                        )
                    }
                }
            }

            is SimpleStatementNode -> {
                if (protocolAnalysis.participatingHosts(stmt).contains(host)) {
                    hostFunctionBuilder.addComment(stmt.toDocument().print())
                    val protocol = protocolAnalysis.primaryProtocol(stmt)
                    hostFunctionBuilder.addStatement("%L", simpleStatement(protocol, stmt))
                }
            }

            is FunctionCallNode -> {
                val arguments =
                    stmt.arguments.filter { arg -> protocolAnalysis.primaryProtocol(arg).hosts.contains(host) }
                val outObjectDeclarations = arguments.filterIsInstance<ObjectDeclarationArgumentNode>()

                // Declare boxed variables
                val newNames = outObjectDeclarations.associateWith { outDeclaration ->
                    val newName = context.newTemporary(context.kotlinName(outDeclaration.name.value) + "_boxed")
                    hostFunctionBuilder.addStatement(
                        "val %L = %T()",
                        newName,
                        Out::class.asClassName().parameterizedBy(
                            codeGenerator.kotlinType(
                                protocolAnalysis.primaryProtocol(outDeclaration),
                                typeAnalysis.type(outDeclaration),
                            ),
                        ),
                    )
                    newName
                }

                // Call function
                hostFunctionBuilder.addStatement(
                    "%N(%L)",
                    stmt.name.value.name,
                    arguments.map { arg ->
                        if (arg is ObjectDeclarationArgumentNode) {
                            CodeBlock.of("%N", newNames[arg])
                        } else {
                            argument(protocolAnalysis.primaryProtocol(arg), arg)
                        }
                    }.joinToCode(),
                )

                // Unpack boxed values
                for (outDeclaration in outObjectDeclarations) {
                    hostFunctionBuilder.addStatement(
                        "val %N = %N.get()",
                        context.kotlinName(outDeclaration.name.value),
                        newNames[outDeclaration]!!,
                    )
                }

                // Set local variable after out parameter initialized by function call
                arguments.filterIsInstance<OutParameterArgumentNode>().forEach {
                    hostFunctionBuilder.addStatement(
                        "%N = %N.get()",
                        context.kotlinName(it.parameter.value),
                        outBoxName(it.parameter.value),
                    )
                }
            }

            is IfNode -> {
                if (protocolAnalysis.participatingHosts(stmt).contains(host)) {
                    val guardValue: CodeBlock =
                        when (val guard = stmt.guard) {
                            is LiteralNode -> {
                                CodeBlock.of("%L", guard.value)
                            }
                            is ReadNode -> {
                                val guardProtocol = protocolAnalysis.primaryProtocol(guard)
                                codeGenerator.guard(guardProtocol, guard)
                            }
                        }

                    hostFunctionBuilder.beginControlFlow("if (%L)", guardValue)
                    generate(hostFunctionBuilder, stmt.thenBranch, host)
                    hostFunctionBuilder.nextControlFlow("else")
                    generate(hostFunctionBuilder, stmt.elseBranch, host)
                    hostFunctionBuilder.endControlFlow()
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

            is AssertionNode -> TODO("Assertions not yet implemented.")
        }
    }

    private fun argument(protocol: Protocol, argument: FunctionArgumentNode): CodeBlock {
        return when (argument) {
            // Input arguments
            is ObjectReferenceArgumentNode -> {
                CodeBlock.of("%N", context.kotlinName(argument.variable.value))
            }
            is ExpressionArgumentNode -> {
                codeGenerator.exp(protocol, argument.expression)
            }
            // Output arguments
            is ObjectDeclarationArgumentNode -> {
                throw UnsupportedOperatorException(protocol, argument)
            }
            is OutParameterArgumentNode -> { // Out box already in scope
                CodeBlock.of("%N", outBoxName(argument.parameter.value))
            }
        }
    }

    private fun simpleStatement(protocol: Protocol, stmt: SimpleStatementNode): CodeBlock =
        when (stmt) {
            is LetNode -> CodeBlock.of(
                "val %N = %L",
                context.kotlinName(stmt.name.value, protocol),
                codeGenerator.exp(protocol, stmt.value),
            )

            is DeclarationNode -> CodeBlock.of(
                "val %N = %L",
                context.kotlinName(stmt.name.value),
                codeGenerator.constructorCall(protocol, stmt.objectType, stmt.arguments),
            )

            is UpdateNode -> codeGenerator.update(protocol, stmt)

            is OutParameterInitializationNode -> outParameterInitialization(protocol, stmt)

            is OutputNode -> CodeBlock.of(
                "runtime.output(%T(%L))",
                typeAnalysis.type(stmt.message).valueClass,
                codeGenerator.exp(protocol, stmt.message),
            )
        }

    private fun outParameterInitialization(
        protocol: Protocol,
        stmt: OutParameterInitializationNode,
    ): CodeBlock {
        val rhs = when (val init = stmt.initializer) {
            is OutParameterConstructorInitializerNode -> codeGenerator.constructorCall(
                protocol,
                init.objectType,
                init.arguments,
            )
            is OutParameterExpressionInitializerNode -> codeGenerator.exp(protocol, init.expression)
        }
        val parameterName = context.kotlinName(stmt.name.value)

        return CodeBlock.of(
            "%N = %L; %N.set(%N)",
            parameterName,
            rhs,
            outBoxName(stmt.name.value),
            parameterName,
        )
    }

    private inner class Context : CodeGeneratorContext {
        private var tempMap: MutableMap<Pair<Temporary, Protocol>, String> = mutableMapOf()
        private var varMap: MutableMap<ObjectVariable, String> = mutableMapOf()

        private var selfSends: Queue<String> = LinkedList()

        private val receiveMember = MemberName(ViaductRuntime::class.java.packageName, "receive")
        private val sendMember = MemberName(ViaductRuntime::class.java.packageName, "send")

        private val freshNameGenerator: FreshNameGenerator = FreshNameGenerator().apply {
            this.getFreshName("runtime")
        }

        override val program: ProgramNode
            get() = this@BackendCodeGenerator.program

        override val host: Host
            get() = this@BackendCodeGenerator.host

        override val protocolComposer: ProtocolComposer
            get() = this@BackendCodeGenerator.protocolComposer

        override fun kotlinName(sourceName: Temporary, protocol: Protocol): String =
            tempMap.getOrPut(Pair(sourceName, protocol)) { freshNameGenerator.getFreshName(sourceName.name.drop(1)) }

        override fun kotlinName(sourceName: ObjectVariable): String =
            varMap.getOrPut(sourceName) { freshNameGenerator.getFreshName(sourceName.name) }

        override fun newTemporary(baseName: String): String =
            freshNameGenerator.getFreshName(baseName)

        override fun codeOf(host: Host) =
            hostDeclarations.reference(host)

        override fun receive(type: TypeName, sender: Host): CodeBlock =
            if (sender == context.host) {
                CodeBlock.of("%L", selfSends.remove())
            } else {
                CodeBlock.of("%N.%M<%T>(%L)", "runtime", receiveMember, type, codeOf(sender))
            }

        override fun send(value: CodeBlock, receiver: Host): CodeBlock =
            if (receiver == context.host) {
                val sendTemp = newTemporary("sendTemp")
                selfSends.add(sendTemp)
                CodeBlock.of("val %N = %L", sendTemp, value)
            } else {
                CodeBlock.of("%N.%M(%L, %L)", "runtime", sendMember, value, codeOf(receiver))
            }

        override fun url(host: Host): CodeBlock =
            CodeBlock.of("%N.url(%L)", "runtime", codeOf(host))
    }
}

/** Creates [Host] instances for each host for efficiency. */
private fun hostDeclarations(program: ProgramNode): TypeSpec {
    val hosts = TypeSpec.objectBuilder("Hosts").addModifiers(KModifier.PRIVATE)

    // Create a declaration for each host.
    program.hosts.forEach { host ->
        hosts.addProperty(
            PropertySpec.builder(host.name, Host::class)
                .initializer(CodeBlock.of("%T(%S)", Host::class, host.name))
                .build(),
        )
    }

    return hosts.build()
}

/** Returns a reference to the declaration of [host]. */
private fun TypeSpec.reference(host: Host): CodeBlock =
    CodeBlock.of("%N.%N", this, host.name)

fun ProgramNode.compileToKotlin(
    fileName: String,
    packageName: String,
    codeGenerator: (context: CodeGeneratorContext) -> CodeGenerator,
    protocolComposer: ProtocolComposer,
): FileSpec {
    val fileBuilder = FileSpec.builder(packageName, fileName)

    // Mark generated code as automatically generated.
    fileBuilder.addAnnotation(
        AnnotationSpec.builder(Generated::class)
            .addMember("%S", BackendCodeGenerator::class.qualifiedName!!)
            .build(),
    )

    // Suppress warnings expected in generated code.
    fileBuilder.addAnnotation(
        AnnotationSpec.builder(Suppress::class)
            .addMember("%S", "RedundantVisibilityModifier")
            .addMember("%S", "UNUSED_PARAMETER")
            .addMember("%S", "UNUSED_VARIABLE")
            .build(),
    )

    // Create an object wrapping all generated code.
    val objectBuilder = TypeSpec.objectBuilder(fileName).addSuperinterface(ViaductGeneratedProgram::class)

    // Add host declarations to wrapper object.
    val hostDeclarations = hostDeclarations(this)
    objectBuilder.addType(hostDeclarations)

    // Expose set of all hosts.
    objectBuilder.addProperty(
        PropertySpec.builder(ViaductGeneratedProgram::hosts.name, SET.parameterizedBy(Host::class.asClassName()))
            .initializer(
                CodeBlock.of(
                    "%M(%L)",
                    MemberName("kotlin.collections", "setOf"),
                    hostDeclarations.propertySpecs.map { CodeBlock.of("%N.%N", hostDeclarations, it) }.joinToCode(),
                ),
            )
            .addModifiers(KModifier.OVERRIDE)
            .build(),
    )

    // Generate code for each host.
    val hostSpecs = hosts.map { host ->
        BackendCodeGenerator(this, host, codeGenerator, protocolComposer, hostDeclarations).generateClass()
    }
    objectBuilder.addTypes(hostSpecs)

    // Add a main function that handles dispatch.
    val main = with(FunSpec.builder("main")) {
        addModifiers(KModifier.OVERRIDE)
        addParameter("host", Host::class)
        addParameter("runtime", ViaductRuntime::class)

        // Dispatch to correct class based on host.
        beginControlFlow("when (host)")
        this@compileToKotlin.hosts.zip(hostSpecs) { host, spec ->
            addStatement("%L -> %N(%L).main()", hostDeclarations.reference(host), spec, "runtime")
        }
        endControlFlow()
    }
    objectBuilder.addFunction(main.build())

    fileBuilder.addType(objectBuilder.build())
    return fileBuilder.build()
}
