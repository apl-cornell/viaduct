package edu.cornell.cs.apl.viaduct.imp.transformers;

import edu.cornell.cs.apl.viaduct.imp.ast.ProgramNode;
import edu.cornell.cs.apl.viaduct.imp.visitors.EmptyBlockVisitor;

/** Post-process the result of protocol instantiation. */
public class TargetPostprocessor {
  /** Remove downgrades, self communication, and security labels on variables. */
  public static ProgramNode run(final ProgramNode program) {
    final SelfCommunicationVisitor selfComm = new SelfCommunicationVisitor();
    final EraseSecurityVisitor eraseSecurity = new EraseSecurityVisitor();
    final LetInlineVisitor letInline = new LetInlineVisitor();
    final EmptyBlockVisitor emptyBlockRemover = new EmptyBlockVisitor();

    // ProgramNode processedProgram = program;
    ProgramNode processedProgram = selfComm.run(program);
    processedProgram = eraseSecurity.run(processedProgram);
    processedProgram = letInline.run(processedProgram);
    processedProgram = emptyBlockRemover.run(processedProgram);
    processedProgram = ReverseElaborator.run(processedProgram);
    return processedProgram;
  }
}
