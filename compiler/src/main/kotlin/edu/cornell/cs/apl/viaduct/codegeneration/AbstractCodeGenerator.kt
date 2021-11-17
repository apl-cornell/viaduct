package edu.cornell.cs.apl.viaduct.codegeneration

import com.squareup.kotlinpoet.CodeBlock
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.datatypes.ImmutableCell
import edu.cornell.cs.apl.viaduct.syntax.datatypes.MutableCell
import edu.cornell.cs.apl.viaduct.syntax.datatypes.Vector
import edu.cornell.cs.apl.viaduct.syntax.intermediate.AtomicExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.DeclarationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ExpressionNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LetNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.LiteralNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutParameterInitializationNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.OutputNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ReadNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.SimpleStatementNode
import edu.cornell.cs.apl.viaduct.syntax.intermediate.UpdateNode
import edu.cornell.cs.apl.viaduct.syntax.values.Value

abstract class AbstractCodeGenerator(val context: CodeGeneratorContext) : CodeGenerator {
    override fun simpleStatement(protocol: Protocol, stmt: SimpleStatementNode): CodeBlock {
        return when (stmt) {
            is LetNode -> let(protocol, stmt)

            is DeclarationNode -> declaration(protocol, stmt)

            is UpdateNode -> update(protocol, stmt)

            is OutParameterInitializationNode -> outParameterInitialization()

            is OutputNode -> output(protocol, stmt)
        }
    }

    fun value(value: Value): CodeBlock =
        CodeBlock.of(
            "%L",
            value
        )

    abstract fun exp(protocol: Protocol, expr: ExpressionNode): CodeBlock

    fun cleartextExp(protocol: Protocol, expr: AtomicExpressionNode): CodeBlock =
        when (expr) {
            is LiteralNode -> CodeBlock.of("%L", expr.value)
            is ReadNode -> CodeBlock.of(
                "%N",
                context.kotlinName(
                    expr.temporary.value,
                    protocol
                )
            )
        }

    abstract fun let(protocol: Protocol, stmt: LetNode): CodeBlock

    fun declaration(protocol: Protocol, stmt: DeclarationNode): CodeBlock {
        return when (stmt.className.value) {
            ImmutableCell -> CodeBlock.of(
                "val %N = %L",
                context.kotlinName(stmt.name.value),
                exp(protocol, stmt.arguments.first())
            )

            // TODO - change this (difference between viaduct, kotlin semantics)
            MutableCell -> CodeBlock.of(
                "var %N = %L",
                context.kotlinName(stmt.name.value),
                exp(protocol, stmt.arguments.first())
            )

            Vector -> {
                CodeBlock.of(
                    "val %N = Array(%L){ %L }",
                    context.kotlinName(stmt.name.value),
                    cleartextExp(protocol, stmt.arguments.first()),
                    exp(
                        protocol,
                        LiteralNode(
                            stmt.typeArguments[0].value.defaultValue,
                            stmt.sourceLocation
                        )
                    )
                )
            }

            else -> TODO("throw error")
        }
    }

    abstract fun update(protocol: Protocol, stmt: UpdateNode): CodeBlock

    private fun outParameterInitialization(
/*        protocol: Protocol,
        stmt: OutParameterInitializationNode*/
    ): CodeBlock =
        TODO()
/*        when (val initializer = stmt.initializer) {
            is OutParameterConstructorInitializerNode -> {
                val outTmpString = context.newTemporary("outTmp")
                CodeBlock.builder()
                    .add(
                        // declare object
                        declarationHelper(
                            outTmpString,
                            initializer.className,
                            initializer.arguments,
                            value(initializer.typeArguments[0].value.defaultValue),
                            protocol
                        )
                    )
                    .add(
                        // fill box with constructed object
                        CodeBlock.of(
                            "%N.set(%L)",
                            context.kotlinName(stmt.name.value),
                            outTmpString
                        )
                    )
                    .build()
            }
            // fill box named [stmt.name.value.name] with [initializer.expression]
            is OutParameterExpressionInitializerNode ->
                CodeBlock.of(
                    "%N.set(%L)",
                    context.kotlinName(stmt.name.value),
                    exp(protocol, initializer.expression)
                )
        }*/

    abstract fun output(protocol: Protocol, stmt: OutputNode): CodeBlock
}

abstract class SingleProtocolCodeGenerator(context: CodeGeneratorContext) : AbstractCodeGenerator(context) {

    abstract fun guard(expr: AtomicExpressionNode): CodeBlock

    override fun guard(protocol: Protocol, expr: AtomicExpressionNode): CodeBlock =
        guard(expr)

    abstract fun let(stmt: LetNode): CodeBlock

    override fun let(protocol: Protocol, stmt: LetNode): CodeBlock =
        let(stmt)

    abstract fun update(stmt: UpdateNode): CodeBlock

    override fun update(protocol: Protocol, stmt: UpdateNode): CodeBlock =
        update(stmt)

    abstract fun output(stmt: OutputNode): CodeBlock

    override fun output(protocol: Protocol, stmt: OutputNode): CodeBlock =
        output(stmt)
}
