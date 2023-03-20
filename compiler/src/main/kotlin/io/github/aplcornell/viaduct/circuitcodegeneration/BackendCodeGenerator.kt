package io.github.aplcornell.viaduct.circuitcodegeneration

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.SET
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.joinToCode
import io.github.aplcornell.viaduct.circuitanalysis.NameAnalysis
import io.github.aplcornell.viaduct.circuitanalysis.protocols
import io.github.aplcornell.viaduct.runtime.Out
import io.github.aplcornell.viaduct.runtime.ViaductGeneratedProgram
import io.github.aplcornell.viaduct.runtime.ViaductRuntime
import io.github.aplcornell.viaduct.syntax.Host
import io.github.aplcornell.viaduct.syntax.circuit.CircuitCallNode
import io.github.aplcornell.viaduct.syntax.circuit.CircuitDeclarationNode
import io.github.aplcornell.viaduct.syntax.circuit.FunctionDeclarationNode
import io.github.aplcornell.viaduct.syntax.circuit.InputNode
import io.github.aplcornell.viaduct.syntax.circuit.LetNode
import io.github.aplcornell.viaduct.syntax.circuit.OutputNode
import io.github.aplcornell.viaduct.syntax.circuit.ProgramNode
import io.github.aplcornell.viaduct.syntax.circuit.Variable
import io.github.aplcornell.viaduct.syntax.circuit.VariableBindingNode
import io.github.aplcornell.viaduct.syntax.types.ValueType
import io.github.aplcornell.viaduct.util.FreshNameGenerator
import java.util.LinkedList
import java.util.Queue
import javax.annotation.processing.Generated

private class BackendCodeGenerator(
    val program: ProgramNode,
    val host: Host,
    codeGenerator: (context: CodeGeneratorContext) -> CodeGenerator,
    val hostDeclarations: TypeSpec,
) {
    private val nameAnalysis = NameAnalysis.get(program)

    private val context = Context()
    private val codeGenerator = codeGenerator(context)

    fun generateClass(): TypeSpec {
        val classBuilder = TypeSpec.classBuilder(host.name.replaceFirstChar { it.uppercase() }).primaryConstructor(
            FunSpec.constructorBuilder().addParameter("runtime", ViaductRuntime::class).build(),
        ).addProperty(
            PropertySpec.builder("runtime", ViaductRuntime::class).initializer("runtime")
                .addModifiers(KModifier.PRIVATE).build(),
        )
        for (protocol in program.protocols()) {
            for (property in codeGenerator.setup(protocol)) {
                classBuilder.addProperty(property)
            }
        }
        for (declaration in program.declarations) {
            when (declaration) {
                is CircuitDeclarationNode -> classBuilder.addFunction(generate(declaration)).build()
                is FunctionDeclarationNode -> classBuilder.addFunction(generate(declaration)).build()
                else -> {} // Do nothing for hosts
            }
        }
        return classBuilder.build()
    }

    /** Generates code for the function [functionDeclaration]. Currently only supports main. */
    fun generate(functionDeclaration: FunctionDeclarationNode): FunSpec {
        val builder = FunSpec.builder(functionDeclaration.name.value.name)
        for (stmt in functionDeclaration.body.statements) {
            when (stmt) {
                is LetNode -> {
                    when (val command = stmt.command) {
                        is CircuitCallNode -> {
                            val circuitDecl: CircuitDeclarationNode = nameAnalysis.declaration(command)
                            val circuitHosts = circuitDecl.protocol.value.hosts

                            val importingHosts = command.inputs.map {
                                (nameAnalysis.declaration(it) as VariableBindingNode).protocol.value.hosts
                            }.flatten().toSet() - circuitHosts // hosts who import to but do not run the circuit
                            val exportingHosts = stmt.bindings.map {
                                it.protocol.value.hosts
                            }.flatten().toSet() // hosts who export from the circuit (and may also run the circuit)

                            val (importCode, inputs) = codeGenerator.import(
                                circuitDecl.protocol.value,
                                command.inputs.zip(circuitDecl.inputs).map { (arg, inParam) ->
                                    Argument(
                                        indexExpression(arg, context),
                                        inParam.type,
                                        (nameAnalysis.declaration(arg) as VariableBindingNode).protocol.value,
                                        arg.sourceLocation,
                                    )
                                },
                            )
                            val outTmps = circuitDecl.outputs.map {
                                val tmp = context.newTemporary(it.name.value.name)
                                CodeBlock.of("%N", tmp)
                            }
                            val (exportCode, outputs) = codeGenerator.export(
                                circuitDecl.protocol.value,
                                stmt.bindings.mapIndexed { index, binding ->
                                    Argument(
                                        outTmps[index],
                                        circuitDecl.outputs[index].type,
                                        binding.protocol.value,
                                        binding.sourceLocation,
                                    )
                                },
                            )

                            when (context.host) {
                                in importingHosts -> builder.addCode(importCode)
                                in circuitHosts -> {
                                    circuitDecl.sizes.zip(command.bounds) { sizeParam, sizeArg ->
                                        builder.addStatement(
                                            "val %N = %L",
                                            context.kotlinName(sizeParam.name.value),
                                            indexExpression(sizeArg, context),
                                        )
                                    }
                                    builder.addCode(importCode)
                                    val outNames = circuitDecl.outputs.associateWith { outParam ->
                                        val outName =
                                            context.newTemporary(context.kotlinName(outParam.name.value) + "_boxed")
                                        builder.addStatement(
                                            "val %L = %T()",
                                            outName,
                                            Out::class.asClassName().parameterizedBy(
                                                kotlinType(
                                                    outParam.type.shape,
                                                    codeGenerator.paramType(
                                                        circuitDecl.protocol.value,
                                                        outParam.type.elementType.value,
                                                    ),
                                                ),
                                            ),
                                        )
                                        outName
                                    }
                                    builder.addStatement(
                                        "%N(%L)",
                                        command.name.value.name,
                                        (
                                            (command.bounds).map { indexExpression(it, context) } + inputs +
                                                circuitDecl.outputs.map { CodeBlock.of("%N", outNames[it]) }
                                            ).joinToCode(),
                                    )
                                    circuitDecl.outputs.forEachIndexed { index, param ->
                                        builder.addStatement("val %L = %N.get()", outTmps[index], outNames[param]!!)
                                    }
                                    builder.addCode(exportCode)
                                }
                                in exportingHosts -> builder.addCode(exportCode)
                            }
                            if (context.host in exportingHosts) {
                                stmt.bindings.zip(outputs).forEach { (binding, output) ->
                                    builder.addStatement("val %N = %L", context.kotlinName(binding.name.value), output)
                                }
                            }
                        }

                        is InputNode -> {
                            if (command.host.value != context.host) continue
                            val name = context.kotlinName(stmt.bindings[0].name.value)
                            val shape = command.type.shape
                            builder.addStatement(
                                "val %N = %L",
                                name,
                                shape.new(context) { context.input(command.type.elementType.value) },
                            )
                        }

                        is OutputNode -> {
                            if (command.host.value != context.host) continue
                            val shape = command.type.shape
                            builder.addCode(
                                indexExpression(command.message, context).forEachIndexed(shape, context) { _, value ->
                                    context.output(value, command.type.elementType.value)
                                },
                            )
                        }
                    }
                }

                else -> throw UnsupportedOperationException("Incorrect statement type in function body")
            }
        }
        return builder.build()
    }

    /** Generates code for the circuit [circuitDeclaration]. */
    fun generate(circuitDeclaration: CircuitDeclarationNode): FunSpec {
        val protocol = circuitDeclaration.protocol.value
        val builder = FunSpec.builder(circuitDeclaration.name.value.name)
        builder.addModifiers(KModifier.PRIVATE)

        for (bound in circuitDeclaration.sizes) {
            builder.addParameter(context.kotlinName(bound.name.value), INT)
        }
        for (param in circuitDeclaration.inputs) {
            builder.addParameter(
                context.kotlinName(param.name.value),
                kotlinType(param.type.shape, codeGenerator.paramType(protocol, param.type.elementType.value)),
            )
        }
        val outParams = circuitDeclaration.outputs.map { param ->
            val paramName = context.newTemporary(param.name.value.name + "Box")
            builder.addParameter(
                paramName,
                Out::class.asClassName().parameterizedBy(
                    kotlinType(param.type.shape, codeGenerator.paramType(protocol, param.type.elementType.value)),
                ),
            )
            CodeBlock.of(paramName)
        }
        builder.addCode(codeGenerator.circuitBody(protocol, circuitDeclaration, outParams))
        return builder.build()
    }

    private inner class Context : CodeGeneratorContext {
        private var varMap: MutableMap<Variable, String> = mutableMapOf()
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

        override fun kotlinName(sourceName: Variable): String =
            varMap.getOrPut(sourceName) { freshNameGenerator.getFreshName(sourceName.name) }

        override fun newTemporary(baseName: String): String =
            freshNameGenerator.getFreshName(baseName)

        override fun codeOf(host: Host) =
            hostDeclarations.reference(host)

        fun input(type: ValueType): CodeBlock =
            CodeBlock.of(
                "(%N.input(%T) as %T).value",
                "runtime",
                type::class,
                type.valueClass,
            )

        fun output(value: CodeBlock, type: ValueType): CodeBlock =
            CodeBlock.of(
                "%N.output(%T(%L))",
                "runtime",
                type.valueClass,
                value,
            )

        override fun receive(type: TypeName, sender: Host): CodeBlock =
            if (sender == context.host) {
                try {
                    CodeBlock.of("%L", selfSends.remove())
                } catch (e: NoSuchElementException) {
                    println("Failing on: " + context.host.name)
                    throw e
                }
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

        override fun url(host: Host): CodeBlock = CodeBlock.of("%N.url(%L)", "runtime", codeOf(host))
    }
}

/** Creates [Host] instances for each host for efficiency. */
private fun hostDeclarations(program: ProgramNode): TypeSpec {
    val hosts = TypeSpec.objectBuilder("Hosts").addModifiers(KModifier.PRIVATE)

    // Create a declaration for each host.
    program.hosts.forEach { host ->
        hosts.addProperty(
            PropertySpec.builder(host.name, Host::class).initializer(CodeBlock.of("%T(%S)", Host::class, host.name))
                .build(),
        )
    }

    return hosts.build()
}

/** Returns a reference to the declaration of [host]. */
private fun TypeSpec.reference(host: Host): CodeBlock = CodeBlock.of("%N.%N", this, host.name)

fun ProgramNode.compileToKotlin(
    fileName: String,
    packageName: String,
    codeGenerator: (context: CodeGeneratorContext) -> CodeGenerator,
//    protocolComposer: ProtocolComposer
): FileSpec {
    val fileBuilder = FileSpec.builder(packageName, fileName)

    // Mark generated code as automatically generated.
    fileBuilder.addAnnotation(
        AnnotationSpec.builder(Generated::class).addMember("%S", BackendCodeGenerator::class.qualifiedName!!)
            .build(),
    )

    // Suppress warnings expected in generated code.
    fileBuilder.addAnnotation(
        AnnotationSpec.builder(Suppress::class).addMember("%S", "RedundantVisibilityModifier")
            .addMember("%S", "UNUSED_PARAMETER").addMember("%S", "UNUSED_VARIABLE").build(),
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
            ).addModifiers(KModifier.OVERRIDE).build(),
    )

    // Generate code for each host.
    val hostSpecs = hosts.map { host ->
        BackendCodeGenerator(this, host, codeGenerator, hostDeclarations).generateClass()
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
