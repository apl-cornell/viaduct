package edu.cornell.cs.apl.viaduct.parsing

import edu.cornell.cs.apl.viaduct.syntax.surface.ProgramNode

/** Parses this string and returns the AST. */
fun String.parse(): ProgramNode {
    return SourceFile.from("<string>", this).parse()
}

/** Parses this source file and returns the AST. */
fun SourceFile.parse(): ProgramNode {
    return Parser.parse(this)
}
