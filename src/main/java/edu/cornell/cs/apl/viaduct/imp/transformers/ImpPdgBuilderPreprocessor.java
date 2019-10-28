package edu.cornell.cs.apl.viaduct.imp.transformers;

import edu.cornell.cs.apl.viaduct.imp.ast.AssertNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BlockNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ProgramNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReceiveNode;
import edu.cornell.cs.apl.viaduct.imp.ast.SendNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StatementNode;
import edu.cornell.cs.apl.viaduct.imp.visitors.IdentityProgramVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;

import java.util.LinkedList;
import java.util.List;

/** Remove communication and assert statements. */
// TODO: PDG builder really needs to be able to handle communication.
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
          BlockNode node, IdentityStmtVisitor visitor, Iterable<StatementNode> statements) {
        final List<StatementNode> filteredStatements = new LinkedList<>();
        for (StatementNode statement : statements) {
          if (!(statement instanceof SendNode
              || statement instanceof ReceiveNode
              || statement instanceof AssertNode)) {
            filteredStatements.add(statement);
          }
        }
        return node.toBuilder().setStatements(filteredStatements).build();
      }
    }
  }
}
