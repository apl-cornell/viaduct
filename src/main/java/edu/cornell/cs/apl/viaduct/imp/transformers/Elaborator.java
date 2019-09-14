package edu.cornell.cs.apl.viaduct.imp.transformers;

import edu.cornell.cs.apl.viaduct.imp.ast.BlockNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BreakNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ForNode;
import edu.cornell.cs.apl.viaduct.imp.ast.IfNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LoopNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ProgramNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StatementNode;
import edu.cornell.cs.apl.viaduct.imp.ast.WhileNode;
import edu.cornell.cs.apl.viaduct.imp.visitors.IdentityProgramVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;

/** Elaborate derived AST nodes into internal ones. */
public class Elaborator {
  public static ProgramNode run(ProgramNode program) {
    return new ElaborationProgramVisitor().run(program);
  }

  // TODO: we shouldn't need this
  public static StatementNode run(StatementNode statement) {
    return new ElaborationProgramVisitor().run(statement);
  }

  private static class ElaborationProgramVisitor extends IdentityProgramVisitor {
    private final StmtVisitor<StatementNode> statementVisitor = new ElaborateStmtVisitor();

    @Override
    protected StmtVisitor<StatementNode> getStatementVisitor() {
      return statementVisitor;
    }

    protected class ElaborateStmtVisitor extends IdentityStmtVisitor {
      /** Rewrite while loops into loop-until-break loops. */
      @Override
      protected StatementNode leave(
          WhileNode node, IdentityStmtVisitor visitor, ExpressionNode guard, StatementNode body) {

        /*
         * while (guard) { body... }
         *   gets translated to
         * loop {
         *   if (guard) {
         *     body...
         *   } else {
         *     break;
         *   }
         * }
         */
        final BreakNode breakNode = BreakNode.builder().setSourceLocation(node).build();
        final IfNode resultGuard =
            IfNode.builder()
                .setGuard(guard)
                .setThenBranch((BlockNode) body)
                .setElseBranch(BlockNode.builder().add(breakNode).setSourceLocation(node).build())
                .setSourceLocation(node)
                .build();
        return LoopNode.builder()
            .setBody(BlockNode.builder().add(resultGuard).build())
            .setSourceLocation(node)
            .build();
      }

      @Override
      protected StatementNode leave(
          ForNode node,
          IdentityStmtVisitor visitor,
          StatementNode initialize,
          ExpressionNode guard,
          StatementNode update,
          StatementNode body) {

        /*
         * for (init; guard; update) { body... }
         *   gets translated to
         * {
         *   init;
         *   loop {
         *     if (guard) {
         *       body...
         *       update;
         *     } else {
         *       break;
         *     }
         *   }
         * }
         */

        final BlockNode.Builder result = BlockNode.builder().setSourceLocation(node);

        result.add(initialize);

        final BreakNode breakNode = BreakNode.builder().setSourceLocation(node).build();
        final BlockNode resultBody = ((BlockNode) body).toBuilder().add(update).build();
        final IfNode resultLoopBody =
            IfNode.builder()
                .setGuard(guard)
                .setThenBranch(resultBody)
                .setElseBranch(BlockNode.builder().add(breakNode).setSourceLocation(node).build())
                .setSourceLocation(node)
                .build();

        result.add(
            LoopNode.builder()
                .setBody(BlockNode.builder().add(resultLoopBody).setSourceLocation(node).build())
                .setSourceLocation(node)
                .build());

        return result.build();
      }
    }
  }
}
