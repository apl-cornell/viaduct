package edu.cornell.cs.apl.viaduct.imp.transformers;

import com.google.common.collect.Iterables;
import edu.cornell.cs.apl.viaduct.imp.ast.BlockNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BreakNode;
import edu.cornell.cs.apl.viaduct.imp.ast.IfNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LoopNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ProgramNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StatementNode;
import edu.cornell.cs.apl.viaduct.imp.ast.WhileNode;
import edu.cornell.cs.apl.viaduct.imp.visitors.IdentityProgramVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;

/** Turn unguarded loops into while and for loops. */
public class ReverseElaborator {
  public static ProgramNode run(ProgramNode program) {
    return new ReverseProgramVisitor().run(program);
  }

  private static class ReverseProgramVisitor extends IdentityProgramVisitor {
    private final StmtVisitor<StatementNode> statementVisitor = new ReverseStmtVisitor();

    @Override
    protected StmtVisitor<StatementNode> getStatementVisitor() {
      return statementVisitor;
    }

    protected class ReverseStmtVisitor extends IdentityStmtVisitor {
      @Override
      protected StatementNode leave(
          LoopNode node, IdentityStmtVisitor visitor, StatementNode bodyStatement) {
        final BlockNode body = (BlockNode) bodyStatement;

        // Check that the body consists of a single if statement
        if (body.getStatements().size() != 1
            || !(Iterables.getOnlyElement(body) instanceof IfNode)) {
          // Not a loop we can reverse
          return super.leave(node, visitor, bodyStatement);
        }

        final IfNode ifNode = (IfNode) Iterables.getOnlyElement(body);

        // Check that the else branch is a break statement
        if (ifNode.getElseBranch().getStatements().size() != 1) {
          return super.leave(node, visitor, bodyStatement);
        }
        final StatementNode elseBranch = Iterables.getOnlyElement(ifNode.getElseBranch());
        if (!(elseBranch instanceof BreakNode) || ((BreakNode) elseBranch).getJumpLabel() != null) {
          return super.leave(node, visitor, bodyStatement);
        }

        // We can write this loop as a while loop
        return WhileNode.builder()
            .setGuard(ifNode.getGuard())
            .setBody(ifNode.getThenBranch())
            .setSourceLocation(node)
            .build();
      }
    }
  }
}
