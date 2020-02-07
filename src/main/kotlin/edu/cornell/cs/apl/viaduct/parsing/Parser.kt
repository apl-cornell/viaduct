package edu.cornell.cs.apl.viaduct.parsing

import edu.cornell.cs.apl.viaduct.syntax.surface.ProgramNode
import java_cup.runtime.ComplexSymbolFactory

/** Parses the string and returns the AST. */
fun String.parse(): ProgramNode {
    return SourceFile.from("<string>", this).parse()
}

/** Parses the source file and returns the AST. */
fun SourceFile.parse(): ProgramNode {
    val symbolFactory = ComplexSymbolFactory()
    val scanner = ImpLexer(this, symbolFactory)
    val parser = ImpParser(scanner, symbolFactory)
    return parser.parse().value as ProgramNode
}
