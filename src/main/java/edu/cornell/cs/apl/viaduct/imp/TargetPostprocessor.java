package edu.cornell.cs.apl.viaduct.imp;

import edu.cornell.cs.apl.viaduct.imp.ast.Host;
import edu.cornell.cs.apl.viaduct.imp.ast.StatementNode;
import edu.cornell.cs.apl.viaduct.imp.visitors.EraseSecurityVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.LetInlineVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.SelfCommunicationVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.UnelaborationVisitor;

/**
 * Postprocess protocol instantiation.
 *
 * <p>Remove downgrades, self communication, and security labels on variables.
 */
public class TargetPostprocessor {
  private static final SelfCommunicationVisitor selfComm;
  private static final EraseSecurityVisitor eraseSecurity;
  private static final LetInlineVisitor letInline;
  private static final UnelaborationVisitor unelaborator;

  static {
    selfComm = new SelfCommunicationVisitor();
    eraseSecurity = new EraseSecurityVisitor();
    letInline = new LetInlineVisitor();
    unelaborator = new UnelaborationVisitor();
  }

  /** set host of current program, then postprocess. */
  public static StatementNode postprocess(Host h, StatementNode program) {
    StatementNode processedProgram = selfComm.run(h, program);
    processedProgram = eraseSecurity.run(processedProgram);
    processedProgram = letInline.run(processedProgram);
    processedProgram = unelaborator.run(processedProgram);
    return processedProgram;
  }
}
