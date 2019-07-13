package edu.cornell.cs.apl.viaduct.imp.visitors;

import edu.cornell.cs.apl.viaduct.imp.ast.ArrayDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.AssignNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BlockNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Host;
import edu.cornell.cs.apl.viaduct.imp.ast.LetBindingNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ProcessName;
import edu.cornell.cs.apl.viaduct.imp.ast.ReceiveNode;
import edu.cornell.cs.apl.viaduct.imp.ast.SendNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StmtNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;
import edu.cornell.cs.apl.viaduct.imp.ast.VariableDeclarationNode;
import edu.cornell.cs.apl.viaduct.util.SymbolTable;
import java.util.LinkedList;
import java.util.Queue;

/** remove self communication. */
public class SelfCommunicationVisitor extends IdentityVisitor {
  private Host selfHost;
  private Queue<ExpressionNode> sentExprs;
  private SymbolTable<Variable, Boolean> declaredVars;

  /** run visitor. */
  public StmtNode run(Host host, StmtNode program) {
    this.selfHost = host;
    this.sentExprs = new LinkedList<>();
    this.declaredVars = new SymbolTable<>();
    return program.accept(this);
  }

  @Override
  public StmtNode visit(VariableDeclarationNode varDeclNode) {
    this.declaredVars.add(varDeclNode.getVariable(), true);
    return super.visit(varDeclNode);
  }

  @Override
  public StmtNode visit(ArrayDeclarationNode arrayDeclNode) {
    this.declaredVars.add(arrayDeclNode.getVariable(), true);
    return super.visit(arrayDeclNode);
  }

  @Override
  public StmtNode visit(SendNode sendNode) {
    if (sendNode.getRecipient().equals(ProcessName.create(this.selfHost))) {
      this.sentExprs.add(sendNode.getSentExpression());
      return BlockNode.create();

    } else {
      return super.visit(sendNode);
    }
  }

  @Override
  public StmtNode visit(ReceiveNode recvNode) {
    if (recvNode.getSender().equals(ProcessName.create(this.selfHost))) {
      ExpressionNode recvExpr = this.sentExprs.remove();
      Variable recvVar = recvNode.getVariable();
      if (this.declaredVars.contains(recvVar)) {
        return AssignNode.create(recvVar, recvExpr);

      } else {
        return LetBindingNode.create(recvVar, recvExpr);
      }
    } else {
      return super.visit(recvNode);
    }
  }

  @Override
  public StmtNode visit(BlockNode blockNode) {
    this.declaredVars.push();
    StmtNode newBlock = super.visit(blockNode);
    this.declaredVars.pop();
    return newBlock;
  }
}
