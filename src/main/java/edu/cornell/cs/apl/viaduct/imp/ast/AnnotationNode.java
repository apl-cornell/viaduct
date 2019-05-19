package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.imp.ImpAnnotation;
import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;

/** annotation node. */
public class AnnotationNode extends StmtNode {
  String annotationStr;
  ImpAnnotation annotation;

  public AnnotationNode(String annotStr) {
    this.annotationStr = annotStr;
  }

  public String getAnnotationString() {
    return this.annotationStr;
  }

  public void setAnnotation(ImpAnnotation annot) {
    this.annotation = annot;
  }

  public ImpAnnotation getAnnotation() {
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
