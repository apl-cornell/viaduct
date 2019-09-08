package edu.cornell.cs.apl.viaduct.imp.transformers;

import edu.cornell.cs.apl.viaduct.imp.ast.AssignNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BlockNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ProcessDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ProcessName;
import edu.cornell.cs.apl.viaduct.imp.ast.ReceiveNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReferenceNode;
import edu.cornell.cs.apl.viaduct.imp.ast.SendNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StatementNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;
import edu.cornell.cs.apl.viaduct.imp.visitors.IdentityProgramVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;
import java.util.LinkedList;
import java.util.Queue;

/** Turn self communication into assignment. */
// TODO: this is broken because it moves expressions from where they are sent to where they
//    are received, but expressions can evaluate to different values later.
class SelfCommunicationVisitor extends IdentityProgramVisitor {
  private final StmtVisitor<StatementNode> statementVisitor;
  private final SelfCommunicationDeclarationVisitor declarationVisitor =
      new SelfCommunicationDeclarationVisitor();

  public SelfCommunicationVisitor() {
    this(null);
  }

  private SelfCommunicationVisitor(ProcessName currentProcess) {
    statementVisitor = new SelfCommunicationStmtVisitor(currentProcess);
  }

  @Override
  protected StmtVisitor<StatementNode> getStatementVisitor() {
    return statementVisitor;
  }

  @Override
  protected SelfCommunicationDeclarationVisitor getDeclarationVisitor() {
    return declarationVisitor;
  }

  protected class SelfCommunicationDeclarationVisitor extends IdentityDeclarationVisitor {
    @Override
    protected SelfCommunicationDeclarationVisitor enter(ProcessDeclarationNode node) {
      return new SelfCommunicationVisitor(node.getName()).getDeclarationVisitor();
    }
  }

  protected class SelfCommunicationStmtVisitor extends IdentityStmtVisitor {
    private final ProcessName selfProcess;
    private final Queue<ExpressionNode> selfSentExpressions;

    SelfCommunicationStmtVisitor(ProcessName selfProcess) {
      this(selfProcess, new LinkedList<>());
    }

    private SelfCommunicationStmtVisitor(
        ProcessName selfProcess, Queue<ExpressionNode> selfSentExpressions) {
      this.selfProcess = selfProcess;
      this.selfSentExpressions = selfSentExpressions;
    }

    @Override
    protected StatementNode leave(
        SendNode node, IdentityStmtVisitor visitor, ExpressionNode sentExpression) {
      if (node.getRecipient().equals(selfProcess)) {
        selfSentExpressions.add(sentExpression);
        return BlockNode.empty();
      } else {
        return super.leave(node, visitor, sentExpression);
      }
    }

    @Override
    protected StatementNode leave(
        ReceiveNode node, IdentityStmtVisitor visitor, ReferenceNode lhs) {
      if (!node.getSender().equals(selfProcess)) {
        return super.leave(node, visitor, lhs);
      }

      final Variable var = node.getVariable();
      final ExpressionNode received = selfSentExpressions.remove();
      return AssignNode.builder().setLhs(var).setRhs(received).build();
    }
  }
}
