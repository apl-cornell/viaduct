package io.github.aplcornell.viaduct.syntax.precircuit

import io.github.aplcornell.viaduct.parsing.ProtocolParser
import io.github.aplcornell.viaduct.parsing.SourceFile
import io.github.aplcornell.viaduct.parsing.defaultProtocolParsers
import io.github.aplcornell.viaduct.syntax.Protocol
import io.github.aplcornell.viaduct.syntax.ProtocolName
import java_cup.runtime.ComplexSymbolFactory

/** Parses [this] string and returns the AST. */
fun String.parse(
    path: String = "<string>",
    protocolParsers: Map<ProtocolName, ProtocolParser<Protocol>> = defaultProtocolParsers,
): ProgramNode {
    return SourceFile.from(path, this).parse(protocolParsers)
}

/** Parses [this] source file to IR and returns the IR. */
fun SourceFile.parse(
    protocolParsers: Map<ProtocolName, ProtocolParser<Protocol>> = defaultProtocolParsers,
): ProgramNode {
    val symbolFactory = ComplexSymbolFactory()
    val scanner = PrecircuitLexer(this, symbolFactory)
    val parser = PrecircuitParser(scanner, symbolFactory)
    parser.protocolParsers = protocolParsers
    return parser.parseProgram()
}
