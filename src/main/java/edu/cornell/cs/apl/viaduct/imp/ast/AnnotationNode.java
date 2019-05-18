package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;

/** annotation node. */
public class AnnotationNode extends StmtNode {
  String annotation;

  public AnnotationNode(String annot) {
    this.annotation = annot;
  }

  public String getAnnotation() {
    return this.annotation;
  }

  public <R> R accept(StmtVisitor<R> v) {
    return v.visit(this);
  }

  @Override
  public boolean isAnnotation() {
    return true;
  }

  @Override
  public String toString() {
    return "@" + annotation;
  }
}
