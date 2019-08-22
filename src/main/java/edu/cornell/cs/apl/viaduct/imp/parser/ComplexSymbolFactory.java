package edu.cornell.cs.apl.viaduct.imp.parser;

import java_cup.runtime.Symbol;
import java_cup.runtime.SymbolFactory;

/** Generates {@link ComplexSymbol}s. */
class ComplexSymbolFactory implements SymbolFactory {
  private final SourceFile sourceFile;

  ComplexSymbolFactory(SourceFile sourceFile) {
    this.sourceFile = sourceFile;
  }

  ComplexSymbol newSymbol(String name, int id, int leftOffset, int rightOffset, Object value) {
    final SourcePosition start = SourcePosition.create(sourceFile, leftOffset);
    final SourcePosition end = SourcePosition.create(sourceFile, rightOffset);
    return new ComplexSymbol(name, id, SourceRange.create(start, end), value);
  }

  @Override
  public ComplexSymbol newSymbol(String name, int id, Symbol left, Symbol right, Object value) {
    return newSymbol(name, id, left.left, right.right, value);
  }

  @Override
  public ComplexSymbol newSymbol(String name, int id, Symbol left, Symbol right) {
    return newSymbol(name, id, left, right, null);
  }

  @Override
  public ComplexSymbol newSymbol(String name, int id, Symbol left, Object value) {
    final int offset = left.right;
    return newSymbol(name, id, offset, offset, value);
  }

  @Override
  public ComplexSymbol newSymbol(String name, int id, Object value) {
    return new ComplexSymbol(name, id, value);
  }

  @Override
  public ComplexSymbol newSymbol(String name, int id) {
    return newSymbol(name, id, null);
  }

  @Override
  public ComplexSymbol startSymbol(String name, int id, int state) {
    return new ComplexSymbol(name, id, state);
  }
}
