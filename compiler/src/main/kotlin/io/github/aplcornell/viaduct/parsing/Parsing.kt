package io.github.apl_cornell.viaduct.parsing

import io.github.apl_cornell.viaduct.security.LabelExpression
import io.github.apl_cornell.viaduct.syntax.Protocol
import io.github.apl_cornell.viaduct.syntax.ProtocolName
import io.github.apl_cornell.viaduct.syntax.surface.ProgramNode
import java_cup.runtime.ComplexSymbolFactory

/** Parses [this] string and returns the AST. */
fun String.parse(
    path: String = "<string>",
    protocolParsers: Map<ProtocolName, ProtocolParser<Protocol>> = defaultProtocolParsers
): ProgramNode {
    return SourceFile.from(path, this).parse(protocolParsers)
}

/** Parses [this] source file and returns the AST. */
fun SourceFile.parse(
    protocolParsers: Map<ProtocolName, ProtocolParser<Protocol>> = defaultProtocolParsers
): ProgramNode {
    val symbolFactory = ComplexSymbolFactory()
    val scanner = Lexer(this, symbolFactory)
    val parser = Parser(scanner, symbolFactory)
    parser.protocolParsers = protocolParsers
    return parser.parseProgram()
}

/** Parses [this] string as a security label. */
fun String.parseLabel(path: String = "<string>"): LabelExpression {
    return SourceFile.from(path, this).parseLabel()
}

/** Parses [this] source file as a security label. */
fun SourceFile.parseLabel(): LabelExpression {
    val symbolFactory = ComplexSymbolFactory()
    val scanner = Lexer(this, symbolFactory)
    val parser = Parser(scanner, symbolFactory)
    return parser.parseLabel()
}
