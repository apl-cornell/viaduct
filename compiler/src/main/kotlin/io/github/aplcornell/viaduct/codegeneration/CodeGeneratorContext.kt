package io.github.aplcornell.viaduct.codegeneration

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.TypeName
import io.github.aplcornell.viaduct.selection.ProtocolComposer
import io.github.aplcornell.viaduct.syntax.Host
import io.github.aplcornell.viaduct.syntax.ObjectVariable
import io.github.aplcornell.viaduct.syntax.Protocol
import io.github.aplcornell.viaduct.syntax.Temporary
import io.github.aplcornell.viaduct.syntax.intermediate.ProgramNode

interface CodeGeneratorContext {
    val program: ProgramNode
    val host: Host
    val protocolComposer: ProtocolComposer

    // returns a kotlin name for a temporary used in the source program
    fun kotlinName(
        sourceName: Temporary,
        protocol: Protocol,
    ): String

    fun kotlinName(sourceName: ObjectVariable): String

    /** Returns a fresh kotlin name based on [baseName]. */
    fun newTemporary(baseName: String): String

    /** Returns code that will evaluate to [host]. */
    fun codeOf(host: Host): CodeBlock

    /** Returns code that will receive a value of type [type] from [sender]. */
    fun receive(
        type: TypeName,
        sender: Host,
    ): CodeBlock

    /** Returns code that will send [value] to [receiver]. */
    fun send(
        value: CodeBlock,
        receiver: Host,
    ): CodeBlock

    /** Returns code that will evaluate to the address of [host]. */
    fun url(host: Host): CodeBlock
}
