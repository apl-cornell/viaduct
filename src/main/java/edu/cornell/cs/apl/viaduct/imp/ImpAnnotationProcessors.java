package edu.cornell.cs.apl.viaduct.imp;

import edu.cornell.cs.apl.viaduct.Host;
import edu.cornell.cs.apl.viaduct.imp.ast.AnnotationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpValue;
import edu.cornell.cs.apl.viaduct.imp.ast.IntegerLiteralNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StmtNode;
import edu.cornell.cs.apl.viaduct.imp.parser.Parser;
import java.io.StringReader;

public class ImpAnnotationProcessors {
  private static ImpAnnotationMapProcessor processorMap = new ImpAnnotationMapProcessor();

  static {
    processorMap.registerProcessor("process", new ProcessAnnotationProcessor());
    processorMap.registerProcessor("interp", new InterpAnnotationProcessor());
    processorMap.registerProcessor("input", new InputAnnotationProcessor());
  }

  public static ImpAnnotationProcessor getProcessorMap() {
    return processorMap;
  }

  public static class ProcessAnnotationProcessor implements ImpAnnotationProcessor {

    @Override
    public ImpAnnotation processAnnotation(AnnotationNode annotNode) {
      Host annotStr = new Host(annotNode.getAnnotationString());
      return new ImpAnnotations.ProcessAnnotation(annotStr);
    }
  }

  public static class InterpAnnotationProcessor implements ImpAnnotationProcessor {
    /** parse an annotation into a statement. */
    @Override
    public ImpAnnotation processAnnotation(AnnotationNode annot) {
      try {
        StringReader reader = new StringReader(annot.getAnnotationString());
        StmtNode program = Parser.parse(reader);
        return new ImpAnnotations.InterpAnnotation(program);
      } catch (Exception e) {
        return null;
      }
    }
  }

  public static class InputAnnotationProcessor implements ImpAnnotationProcessor {
    /** parse an annotation into a statement. */
    @Override
    public ImpAnnotation processAnnotation(AnnotationNode annot) {
      try {
        int i = Integer.parseInt(annot.getAnnotationString());
        ImpValue value = new IntegerLiteralNode(i);
        return new ImpAnnotations.InputAnnotation(value);

      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }
}
