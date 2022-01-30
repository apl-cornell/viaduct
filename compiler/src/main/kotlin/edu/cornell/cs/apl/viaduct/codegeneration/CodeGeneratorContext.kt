package edu.cornell.cs.apl.viaduct.codegeneration

import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.TypeName
import edu.cornell.cs.apl.viaduct.selection.ProtocolComposer
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.ObjectVariable
import edu.cornell.cs.apl.viaduct.syntax.Protocol
import edu.cornell.cs.apl.viaduct.syntax.Temporary
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode

interface CodeGeneratorContext {
    val program: ProgramNode
    val host: Host
    val protocolComposer: ProtocolComposer

    // returns a kotlin name for a temporary used in the source program
    fun kotlinName(sourceName: Temporary, protocol: Protocol): String

    fun kotlinName(sourceName: ObjectVariable): String

    /** Returns a fresh kotlin name based on [baseName]. */
    fun newTemporary(baseName: String): String

    /** Returns code that will evaluate to [host]. */
    fun codeOf(host: Host): CodeBlock

    /** Returns code that will receive a value of type [type] from [sender]. */
    fun receive(type: TypeName, sender: Host): CodeBlock

    /** Returns code that will send [value] to [receiver]. */
    fun send(value: CodeBlock, receiver: Host): CodeBlock

    /** Returns code that will evaluate to the address of [host]. */
    fun url(host: Host): CodeBlock
}
