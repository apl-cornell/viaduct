package edu.cornell.cs.apl.viaduct.imp;

import edu.cornell.cs.apl.viaduct.imp.ast.AnnotationNode;

public interface ImpAnnotationProcessor {
  ImpAnnotation processAnnotation(AnnotationNode annotNode);
}
