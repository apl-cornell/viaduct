package edu.cornell.cs.apl.viaduct.parsing

import edu.cornell.cs.apl.viaduct.syntax.surface.ProgramNode
import java_cup.runtime.ComplexSymbolFactory

/** Parses this string and returns the AST. */
fun String.parse(): ProgramNode {
    return SourceFile.from("<string>", this).parse()
}

/** Parses this source file and returns the AST. */
fun SourceFile.parse(): ProgramNode {
    val symbolFactory = ComplexSymbolFactory()
    val scanner = Lexer(this, symbolFactory)
    val parser = Parser(scanner, symbolFactory)
    return (parser.parse().value as ProgramNode)
}
