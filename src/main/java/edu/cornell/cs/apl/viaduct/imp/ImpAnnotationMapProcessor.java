package edu.cornell.cs.apl.viaduct.imp;

import edu.cornell.cs.apl.viaduct.imp.ast.AnnotationNode;

import java.util.HashMap;
import java.util.Map;

/** keep a registry of annotation processors based on keyword. */
public class ImpAnnotationMapProcessor implements ImpAnnotationProcessor {
  Map<String, ImpAnnotationProcessor> processorMap;

  public ImpAnnotationMapProcessor() {
    this.processorMap = new HashMap<>();
  }

  public void registerProcessor(String keyword, ImpAnnotationProcessor processor) {
    this.processorMap.put(keyword, processor);
  }

  /** process annotation node based on keyword. */
  public ImpAnnotation processAnnotation(AnnotationNode annotNode) {
    String annotStr = annotNode.getAnnotationString();
    String[] annotArray = annotStr.split(" ", 2);

    String keyword = null;
    String annotStrTail = "";

    if (annotArray.length == 2) {
      keyword = annotArray[0];
      annotStrTail = annotArray[1];
    } else {
      keyword = annotStr;
    }

    ImpAnnotationProcessor processor = this.processorMap.get(keyword);
    if (processor != null) {
      return processor.processAnnotation(new AnnotationNode(annotStrTail));

    } else {
      return null;
    }
  }
}
