package edu.cornell.cs.apl.viaduct.imp.transformers;

import edu.cornell.cs.apl.viaduct.imp.ast.AssertNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BlockNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.IfNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ProgramNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StatementNode;
import edu.cornell.cs.apl.viaduct.imp.visitors.IdentityProgramVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;

import java.util.LinkedList;
import java.util.List;

/** Remove communication and assert statements. */
public class ImpPdgBuilderPreprocessor {
  public static ProgramNode run(ProgramNode program) {
    return new ProcessProgramVisitor().run(program);
  }

  private static class ProcessProgramVisitor extends IdentityProgramVisitor {
    private final StmtVisitor<StatementNode> statementVisitor = new ProcessStmtVisitor();

    @Override
    protected StmtVisitor<StatementNode> getStatementVisitor() {
      return statementVisitor;
    }

    protected class ProcessStmtVisitor extends IdentityStmtVisitor {
      @Override
      protected StatementNode leave(
          IfNode node,
          IdentityStmtVisitor visitor,
          ExpressionNode guard,
          StatementNode thenBranch,
          StatementNode elseBranch)
      {
        return node.toBuilder()
            .setGuard(guard)
            .setThenBranch((BlockNode) thenBranch)
            .setElseBranch((BlockNode) elseBranch)
            .setLoopGuard(node.isLoopGuard())
            .build();
      }

      @Override
      protected StatementNode leave(
          BlockNode node, IdentityStmtVisitor visitor, Iterable<StatementNode> statements) {
        final List<StatementNode> filteredStatements = new LinkedList<>();
        for (StatementNode statement : statements) {
          if (!(statement instanceof AssertNode)) {
            filteredStatements.add(statement);
          }
        }
        return node.toBuilder().setStatements(filteredStatements).build();
      }
    }
  }
}
