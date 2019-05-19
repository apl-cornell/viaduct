package edu.cornell.cs.apl.viaduct.imp;

import edu.cornell.cs.apl.viaduct.imp.ast.AnnotationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StmtNode;
import edu.cornell.cs.apl.viaduct.imp.parser.ImpLexer;
import edu.cornell.cs.apl.viaduct.imp.parser.ImpParser;

import java.io.StringReader;

import java_cup.runtime.DefaultSymbolFactory;
import java_cup.runtime.Scanner;
import java_cup.runtime.SymbolFactory;

public class ImpAnnotationProcessors {
  public static class ProcessAnnotationProcessor implements ImpAnnotationProcessor {

    public ImpAnnotation processAnnotation(AnnotationNode annotNode) {
      String annotStr = annotNode.getAnnotationString();
      return new ImpAnnotations.ProcessAnnotation(annotStr);
    }
  }

  public static class InterpAnnotationProcessor implements ImpAnnotationProcessor {
    /** parse an annotation into a statement. */
    public ImpAnnotation processAnnotation(AnnotationNode annot) {
      try {
        StringReader reader = new StringReader(annot.getAnnotationString());
        SymbolFactory symbolFactory = new DefaultSymbolFactory();
        Scanner progLexer = new ImpLexer(reader, symbolFactory);
        ImpParser progParser = new ImpParser(progLexer, symbolFactory);
        StmtNode program = (StmtNode)(progParser.parse().value);
        return new ImpAnnotations.InterpAnnotation(program);

      } catch (Exception e) {
        return null;
      }
    }
  }

  static ImpAnnotationMapProcessor processorMap = new ImpAnnotationMapProcessor();

  static {
    processorMap.registerProcessor("process", new ProcessAnnotationProcessor());
    processorMap.registerProcessor("interp", new InterpAnnotationProcessor());
  }

  public static ImpAnnotationProcessor getProcessorMap() {
    return processorMap;
  }
}
