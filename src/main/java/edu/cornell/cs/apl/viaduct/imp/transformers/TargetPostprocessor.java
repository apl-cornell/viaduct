package edu.cornell.cs.apl.viaduct.imp.transformers;

import edu.cornell.cs.apl.viaduct.imp.ast.ProgramNode;

/** Post-process the result of protocol instantiation. */
public class TargetPostprocessor {
  /** Remove downgrades, self communication, and security labels on variables. */
  public static ProgramNode run(final ProgramNode program) {
    final SelfCommunicationVisitor selfComm = new SelfCommunicationVisitor();
    final EraseSecurityVisitor eraseSecurity = new EraseSecurityVisitor();
    final LetInlineVisitor letInline = new LetInlineVisitor();

    ProgramNode processedProgram = selfComm.run(program);
    processedProgram = eraseSecurity.run(processedProgram);
    processedProgram = letInline.run(processedProgram);
    processedProgram = ReverseElaborator.run(processedProgram);
    return processedProgram;
  }
}
