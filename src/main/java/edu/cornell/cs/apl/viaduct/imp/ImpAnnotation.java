package edu.cornell.cs.apl.viaduct.imp;

import edu.cornell.cs.apl.viaduct.imp.ast.AnnotationNode;

public abstract class ImpAnnotation {
  public abstract String getKeyword();

  protected abstract String argsToString();

  public AnnotationNode toAnnotationNode() {
    return new AnnotationNode(toAnnotationString());
  }

  public String toAnnotationString() {
    return String.format("%s %s", getKeyword(), argsToString());
  }

  @Override
  public String toString() {
    return String.format("@%s %s", getKeyword(), argsToString());
  }
}
