package edu.cornell.cs.apl.viaduct.parsing

import edu.cornell.cs.apl.viaduct.security.Label
import edu.cornell.cs.apl.viaduct.syntax.surface.ProgramNode
import java_cup.runtime.ComplexSymbolFactory

/** Parses [this] string and returns the AST. */
fun String.parse(path: String = "<string>"): ProgramNode {
    return SourceFile.from(path, this).parse()
}

/** Parses [this] source file and returns the AST. */
fun SourceFile.parse(): ProgramNode {
    val symbolFactory = ComplexSymbolFactory()
    val scanner = Lexer(this, symbolFactory)
    val parser = Parser(scanner, symbolFactory)
    return parser.parseProgram()
}

/** Parses [this] string as a security label. */
fun String.parseLabel(path: String = "<string>"): Label {
    return SourceFile.from(path, this).parseLabel()
}

/** Parses [this] source file as a security label. */
fun SourceFile.parseLabel(): Label {
    val symbolFactory = ComplexSymbolFactory()
    val scanner = Lexer(this, symbolFactory)
    val parser = Parser(scanner, symbolFactory)
    return parser.parseLabel()
}
