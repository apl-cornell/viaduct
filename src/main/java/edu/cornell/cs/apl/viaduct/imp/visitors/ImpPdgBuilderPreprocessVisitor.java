package edu.cornell.cs.apl.viaduct.imp.visitors;

import edu.cornell.cs.apl.viaduct.imp.ast.AssertNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BlockNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReceiveNode;
import edu.cornell.cs.apl.viaduct.imp.ast.SendNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StatementNode;

/**
 * preprocess AST before generating PDG. this does the following: - remove communication and asserts
 * in program - elaborate derived forms - convert to A-normal form
 */
public class ImpPdgBuilderPreprocessVisitor extends FormatBlockVisitor {
  /** run the visitor. */
  @Override
  public StatementNode run(StatementNode program) {
    StatementNode preprocessedProgram = program.accept(this);

    ElaborationVisitor elaborator = new ElaborationVisitor();
    preprocessedProgram = elaborator.run(preprocessedProgram);

    LetInlineVisitor letInliner = new LetInlineVisitor();
    preprocessedProgram = letInliner.run(preprocessedProgram);

    AnfVisitor anfRewriter = new AnfVisitor();
    preprocessedProgram = anfRewriter.run(preprocessedProgram);

    return preprocessedProgram;
  }

  @Override
  public StatementNode visit(SendNode sendNode) {
    return BlockNode.create();
  }

  @Override
  public StatementNode visit(ReceiveNode recvNode) {
    return BlockNode.create();
  }

  @Override
  public StatementNode visit(AssertNode assertNode) {
    return BlockNode.create();
  }
}
