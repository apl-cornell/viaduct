package edu.cornell.cs.apl.viaduct.imp.visitors;

import edu.cornell.cs.apl.viaduct.imp.ImpAnnotation;
import edu.cornell.cs.apl.viaduct.imp.ImpAnnotationProcessor;
import edu.cornell.cs.apl.viaduct.imp.ImpAnnotationProcessors;
import edu.cornell.cs.apl.viaduct.imp.ast.AnnotationNode;

public class ImpAnnotationVisitor extends VoidVisitor {
  private ImpAnnotationProcessor processor;

  public ImpAnnotationVisitor() {
    this(ImpAnnotationProcessors.getProcessorMap());
  }

  private ImpAnnotationVisitor(ImpAnnotationProcessor annotProc) {
    processor = annotProc;
  }

  /** process annotation. */
  @Override
  public Void visit(AnnotationNode annotNode) {
    ImpAnnotation annotation = processor.processAnnotation(annotNode);
    annotNode.setAnnotation(annotation);
    return null;
  }
}
