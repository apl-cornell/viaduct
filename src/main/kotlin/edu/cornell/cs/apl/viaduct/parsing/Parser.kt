package edu.cornell.cs.apl.viaduct.parsing

import edu.cornell.cs.apl.viaduct.syntax.surface.ProgramNode

/** Parses the string and returns the AST. */
fun String.parse(): ProgramNode {
    return SourceFile.from("<string>", this).parse()
}

/** Parses the source file and returns the AST. */
fun SourceFile.parse(): ProgramNode {
    return Parser.parse(this)
}
