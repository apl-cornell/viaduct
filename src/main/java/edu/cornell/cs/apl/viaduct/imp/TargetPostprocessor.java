package edu.cornell.cs.apl.viaduct.imp;

import edu.cornell.cs.apl.viaduct.imp.ast.Host;
import edu.cornell.cs.apl.viaduct.imp.ast.StmtNode;
import edu.cornell.cs.apl.viaduct.imp.dataflow.CopyPropagation;
import edu.cornell.cs.apl.viaduct.imp.visitors.EraseSecurityVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.FormatBlockVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.LetInlineVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.SelfCommunicationVisitor;

/**
 * Postprocess protocol instantiation.
 *
 * <p>Remove downgrades, self communication, and security labels on variables.
 */
public class TargetPostprocessor {
  private static final SelfCommunicationVisitor selfComm;
  private static final EraseSecurityVisitor eraseSecurity;
  private static final CopyPropagation copyProp;
  private static final FormatBlockVisitor formatBlock;
  private static final LetInlineVisitor letInline;

  static {
    selfComm = new SelfCommunicationVisitor();
    eraseSecurity = new EraseSecurityVisitor();
    copyProp = new CopyPropagation();
    formatBlock = new FormatBlockVisitor();
    letInline = new LetInlineVisitor();
  }

  /** set host of current program, then postprocess. */
  public static StmtNode postprocess(Host h, StmtNode program) {
    StmtNode processedProgram = selfComm.run(h, program);
    processedProgram = eraseSecurity.run(processedProgram);
    // processedProgram = copyProp.run(processedProgram);
    processedProgram = formatBlock.run(processedProgram);
    processedProgram = letInline.run(processedProgram);
    return processedProgram;
  }
}
