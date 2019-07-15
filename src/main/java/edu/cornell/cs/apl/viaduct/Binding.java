package edu.cornell.cs.apl.viaduct;

// TODO: I don't know why this is useful apart from complicating Variable...

public interface Binding<T extends AstNode> {
  String getBinding();
}
