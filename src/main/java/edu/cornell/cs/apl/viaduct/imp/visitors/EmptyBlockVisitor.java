package edu.cornell.cs.apl.viaduct.imp.visitors;

import edu.cornell.cs.apl.viaduct.imp.ast.BlockNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StatementNode;
import java.util.ArrayList;
import java.util.List;

/** A visitor that removes empty blocks. */
public class EmptyBlockVisitor extends IdentityProgramVisitor {
  private final StmtVisitor<StatementNode> statementVisitor = new EmptyBlockStmtVisitor();

  @Override
  protected StmtVisitor<StatementNode> getStatementVisitor() {
    return statementVisitor;
  }

  protected class EmptyBlockStmtVisitor extends IdentityStmtVisitor {
    @Override
    protected StatementNode leave(
        BlockNode node, IdentityStmtVisitor visitor, Iterable<StatementNode> statements) {

      List<StatementNode> blockStmts = new ArrayList<>();
      for (StatementNode statement : statements) {
        boolean add = true;
        if (statement instanceof BlockNode) {
          BlockNode childBlock = (BlockNode) statement;
          if (childBlock.getStatements().size() == 0) {
            add = false;
          }
        }

        if (add) {
          blockStmts.add(statement);
        }
      }
      return node.toBuilder().setStatements(blockStmts).build();
    }
  }
}
