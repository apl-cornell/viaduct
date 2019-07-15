package edu.cornell.cs.apl.viaduct;

public interface AstPrinter<T extends AstNode> {
  String print(T t);
}
