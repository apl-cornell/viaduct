package io.github.apl_cornell.viaduct.circuitcodegeneration

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
import io.github.apl_cornell.viaduct.runtime.Out
import io.github.apl_cornell.viaduct.runtime.ViaductGeneratedProgram
import io.github.apl_cornell.viaduct.runtime.ViaductRuntime
import io.github.apl_cornell.viaduct.syntax.Host
import io.github.apl_cornell.viaduct.syntax.circuit.CircuitDeclarationNode
import io.github.apl_cornell.viaduct.syntax.circuit.ProgramNode
import io.github.apl_cornell.viaduct.syntax.circuit.Variable
import io.github.apl_cornell.viaduct.util.FreshNameGenerator
import java.util.LinkedList
import java.util.Queue
import javax.annotation.processing.Generated

private class BackendCodeGenerator(
    val program: ProgramNode,
    val host: Host,
    codeGenerator: (context: CodeGeneratorContext) -> CodeGenerator,
//    val protocolComposer: ProtocolComposer,
    val hostDeclarations: TypeSpec
) {
    //    private val typeAnalysis = TypeAnalysis.get(program)
//    private val nameAnalysis = NameAnalysis.get(program)
//    private val protocolAnalysis = ProtocolAnalysis(program, protocolComposer)
    private val context = Context()
    private val codeGenerator = codeGenerator(context)

    fun generateClass(): TypeSpec {
        val classBuilder = TypeSpec.classBuilder(
            host.name.replaceFirstChar { it.uppercase() }
        ).primaryConstructor(
            FunSpec.constructorBuilder()
                .addParameter("runtime", ViaductRuntime::class)
                .build()
        ).addProperty(
            PropertySpec.builder("runtime", ViaductRuntime::class)
                .initializer("runtime")
                .addModifiers(KModifier.PRIVATE)
                .build()
        )
        // TODO Replace with a call to ProtocolAnalysis
        val participatingProtocols = program.circuits.map { it.protocol.value }
        for (protocol in participatingProtocols) {
            for (property in codeGenerator.setup(protocol)) {
                classBuilder.addProperty(property)
            }
        }
        for (circuit in program.circuits) {
            classBuilder.addFunction(generate(host, circuit)).build()
        }
        return classBuilder.build()
    }

    /** Generates code for [host]'s role in the circuit [circuitDeclaration]. */
    fun generate(host: Host, circuitDeclaration: CircuitDeclarationNode): FunSpec {
        val protocol = circuitDeclaration.protocol.value
        val builder = FunSpec.builder(circuitDeclaration.name.value.name)
        builder.addModifiers(KModifier.PRIVATE)

        for (bound in circuitDeclaration.bounds) {
            builder.addParameter(context.kotlinName(bound.name.value), INT)
        }
        for (param in circuitDeclaration.inputs) {
            val paramName = context.kotlinName(param.name.value)
            val paramType = codeGenerator.kotlinType(protocol, param.type.elementType.value)
            builder.addParameter(paramName, paramType)
        }
        for (param in circuitDeclaration.outputs) {
            val paramName = context.kotlinName(param.name.value)
            val baseType = param.type.elementType.value
            val paramType = if (param.type.shape.isEmpty()) Out::class.asClassName().parameterizedBy(
                codeGenerator.kotlinType(protocol, baseType)
            ) else
                codeGenerator.kotlinType(protocol, baseType)
            builder.addParameter(paramName, paramType)
        }

        builder.addCode(codeGenerator.circuitBody(protocol, host, circuitDeclaration))
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

//        override val protocolComposer: ProtocolComposer
//            get() = this@BackendCodeGenerator.protocolComposer

        override fun kotlinName(sourceName: Variable): String =
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
                .build()
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
//    protocolComposer: ProtocolComposer
): FileSpec {
    val fileBuilder = FileSpec.builder(packageName, fileName)

    // Mark generated code as automatically generated.
    fileBuilder.addAnnotation(
        AnnotationSpec.builder(Generated::class)
            .addMember("%S", BackendCodeGenerator::class.qualifiedName!!)
            .build()
    )

    // Suppress warnings expected in generated code.
    fileBuilder.addAnnotation(
        AnnotationSpec.builder(Suppress::class)
            .addMember("%S", "RedundantVisibilityModifier")
            .addMember("%S", "UNUSED_PARAMETER")
            .addMember("%S", "UNUSED_VARIABLE")
            .build()
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
                    hostDeclarations.propertySpecs.map { CodeBlock.of("%N.%N", hostDeclarations, it) }.joinToCode()
                )
            )
            .addModifiers(KModifier.OVERRIDE)
            .build()
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
