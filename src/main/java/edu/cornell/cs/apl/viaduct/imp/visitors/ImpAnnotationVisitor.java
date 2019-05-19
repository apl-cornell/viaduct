package edu.cornell.cs.apl.viaduct.imp.visitors;

import edu.cornell.cs.apl.viaduct.imp.ImpAnnotation;
import edu.cornell.cs.apl.viaduct.imp.ImpAnnotationProcessor;
import edu.cornell.cs.apl.viaduct.imp.ImpAnnotationProcessors;
import edu.cornell.cs.apl.viaduct.imp.ast.AndNode;
import edu.cornell.cs.apl.viaduct.imp.ast.AnnotationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.AssignNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BinaryExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BlockNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BooleanLiteralNode;
import edu.cornell.cs.apl.viaduct.imp.ast.DowngradeNode;
import edu.cornell.cs.apl.viaduct.imp.ast.EqualNode;
import edu.cornell.cs.apl.viaduct.imp.ast.IfNode;
import edu.cornell.cs.apl.viaduct.imp.ast.IntegerLiteralNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LeqNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LessThanNode;
import edu.cornell.cs.apl.viaduct.imp.ast.NotNode;
import edu.cornell.cs.apl.viaduct.imp.ast.OrNode;
import edu.cornell.cs.apl.viaduct.imp.ast.PlusNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReadNode;
import edu.cornell.cs.apl.viaduct.imp.ast.RecvNode;
import edu.cornell.cs.apl.viaduct.imp.ast.SendNode;
import edu.cornell.cs.apl.viaduct.imp.ast.SkipNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StmtNode;
import edu.cornell.cs.apl.viaduct.imp.ast.VarDeclNode;

public class ImpAnnotationVisitor implements AstVisitor<Void> {
  ImpAnnotationProcessor processor;

  public ImpAnnotationVisitor() {
    processor = ImpAnnotationProcessors.getProcessorMap();
  }

  public ImpAnnotationVisitor(ImpAnnotationProcessor annotProc) {
    processor = annotProc;
  }

  private Void visitBinary(BinaryExpressionNode binNode) {
    binNode.getLhs().accept(this);
    binNode.getRhs().accept(this);
    return null;
  }

  public Void visit(ReadNode readNode) {
    return null;
  }

  public Void visit(IntegerLiteralNode integerLiteralNode) {
    return null;
  }

  public Void visit(PlusNode plusNode) {
    return null;
  }

  public Void visit(BooleanLiteralNode booleanLiteralNode) {
    return null;
  }

  public Void visit(OrNode orNode) {
    return visitBinary(orNode);
  }

  public Void visit(AndNode andNode) {
    return visitBinary(andNode);
  }

  public Void visit(LessThanNode lessThanNode) {
    return visitBinary(lessThanNode);
  }

  public Void visit(EqualNode equalNode) {
    return visitBinary(equalNode);
  }

  public Void visit(LeqNode leqNode) {
    return visitBinary(leqNode);
  }

  public Void visit(NotNode notNode) {
    notNode.getExpression().accept(this);
    return null;
  }

  public Void visit(DowngradeNode downgradeNode) {
    downgradeNode.getExpression().accept(this);
    return null;
  }

  public Void visit(SkipNode skipNode) {
    return null;
  }

  public Void visit(VarDeclNode varDecl) {
    return null;
  }

  public Void visit(AssignNode assignNode) {
    assignNode.getRhs().accept(this);
    return null;
  }

  /** visit conditional node. */
  public Void visit(IfNode ifNode) {
    ifNode.getGuard().accept(this);
    ifNode.getThenBranch().accept(this);
    ifNode.getElseBranch().accept(this);
    return null;
  }

  /** visit block node. */
  public Void visit(BlockNode blockNode) {
    for (StmtNode stmt : blockNode.getStatements()) {
      stmt.accept(this);
    }

    return null;
  }

  public Void visit(SendNode sendNode) {
    sendNode.getSentExpr().accept(this);
    return null;
  }

  public Void visit(RecvNode recvNode) {
    return null;
  }

  /** process annotation. */
  public Void visit(AnnotationNode annotNode) {
    ImpAnnotation annotation = processor.processAnnotation(annotNode);
    annotNode.setAnnotation(annotation);
    return null;
  }
}
