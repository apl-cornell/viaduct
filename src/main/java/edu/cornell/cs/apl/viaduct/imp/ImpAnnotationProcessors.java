package edu.cornell.cs.apl.viaduct.imp;

import edu.cornell.cs.apl.viaduct.imp.ast.AnnotationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpValue;
import edu.cornell.cs.apl.viaduct.imp.ast.IntegerLiteralNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StmtNode;
import edu.cornell.cs.apl.viaduct.imp.parser.ImpLexer;
import edu.cornell.cs.apl.viaduct.imp.parser.ImpParser;
import java.io.StringReader;
import java_cup.runtime.ComplexSymbolFactory;
import java_cup.runtime.Scanner;

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
      String annotStr = annotNode.getAnnotationString();
      return new ImpAnnotations.ProcessAnnotation(annotStr);
    }
  }

  public static class InterpAnnotationProcessor implements ImpAnnotationProcessor {
    /** parse an annotation into a statement. */
    @Override
    public ImpAnnotation processAnnotation(AnnotationNode annot) {
      try {
        StringReader reader = new StringReader(annot.getAnnotationString());
        ComplexSymbolFactory symbolFactory = new ComplexSymbolFactory();
        Scanner progLexer = new ImpLexer(reader, symbolFactory);
        ImpParser progParser = new ImpParser(progLexer, symbolFactory);
        StmtNode program = (StmtNode) (progParser.parse().value);
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
        Integer i = Integer.valueOf(annot.getAnnotationString());
        ImpValue value = new IntegerLiteralNode(i);
        return new ImpAnnotations.InputAnnotation(value);

      } catch (Exception e) {
        return null;
      }
    }
  }
}
