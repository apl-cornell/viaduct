package edu.cornell.cs.apl.viaduct.imp.visitors;

import edu.cornell.cs.apl.viaduct.imp.ImpAnnotation;
import edu.cornell.cs.apl.viaduct.imp.ImpAnnotations;
import edu.cornell.cs.apl.viaduct.imp.ast.AndNode;
import edu.cornell.cs.apl.viaduct.imp.ast.AnnotationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.AssignNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BlockNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BooleanLiteralNode;
import edu.cornell.cs.apl.viaduct.imp.ast.DowngradeNode;
import edu.cornell.cs.apl.viaduct.imp.ast.EqualNode;
import edu.cornell.cs.apl.viaduct.imp.ast.IfNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpValue;
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
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;

import java.util.HashMap;
import java.util.Map;


/** interpret an IMP program. */
public class InterpVisitor
    implements ExprVisitor<ImpValue>, StmtVisitor<Void> {

  Map<Variable,ImpValue> store;

  public InterpVisitor() {}

  /** interpret program. */
  public Map<Variable,ImpValue> interpret(StmtNode stmt) {
    this.store = new HashMap<>();
    stmt.accept(this);
    return this.store;
  }

  public ImpValue visit(ReadNode readNode) {
    return this.store.get(readNode.getVariable());
  }

  public ImpValue visit(IntegerLiteralNode integerLiteralNode) {
    return integerLiteralNode;
  }

  /** interpret plus node. */
  public ImpValue visit(PlusNode plusNode) {
    IntegerLiteralNode lval = (IntegerLiteralNode)plusNode.getLhs().accept(this);
    IntegerLiteralNode rval = (IntegerLiteralNode)plusNode.getRhs().accept(this);
    return new IntegerLiteralNode(lval.getValue() + rval.getValue());
  }

  public ImpValue visit(BooleanLiteralNode booleanLiteralNode) {
    return booleanLiteralNode;
  }

  /** interpret or node. */
  public ImpValue visit(OrNode orNode) {
    BooleanLiteralNode lval = (BooleanLiteralNode)orNode.getLhs().accept(this);
    BooleanLiteralNode rval = (BooleanLiteralNode)orNode.getRhs().accept(this);
    return new BooleanLiteralNode(lval.getValue() || rval.getValue());
  }

  /** interpret and node. */
  public ImpValue visit(AndNode andNode) {
    BooleanLiteralNode lval = (BooleanLiteralNode)andNode.getLhs().accept(this);
    BooleanLiteralNode rval = (BooleanLiteralNode)andNode.getRhs().accept(this);
    return new BooleanLiteralNode(lval.getValue() && rval.getValue());
  }

  /** interpret lt node. */
  public ImpValue visit(LessThanNode ltNode) {
    IntegerLiteralNode lval = (IntegerLiteralNode)ltNode.getLhs().accept(this);
    IntegerLiteralNode rval = (IntegerLiteralNode)ltNode.getRhs().accept(this);
    return new BooleanLiteralNode(lval.getValue() < rval.getValue());
  }

  /** interpret equals node. */
  public ImpValue visit(EqualNode eqNode) {
    IntegerLiteralNode lval = (IntegerLiteralNode)eqNode.getLhs().accept(this);
    IntegerLiteralNode rval = (IntegerLiteralNode)eqNode.getRhs().accept(this);
    return new BooleanLiteralNode(lval.getValue() == rval.getValue());
  }

  /** interpret leq node. */
  public ImpValue visit(LeqNode leqNode) {
    IntegerLiteralNode lval = (IntegerLiteralNode)leqNode.getLhs().accept(this);
    IntegerLiteralNode rval = (IntegerLiteralNode)leqNode.getRhs().accept(this);
    return new BooleanLiteralNode(lval.getValue() <= rval.getValue());
  }

  public ImpValue visit(NotNode notNode) {
    BooleanLiteralNode val = (BooleanLiteralNode)notNode.getExpression().accept(this);
    return new BooleanLiteralNode(!val.getValue());
  }

  public ImpValue visit(DowngradeNode downgradeNode) {
    return downgradeNode.getExpression().accept(this);
  }

  public Void visit(SkipNode skipNode) {
    return null;
  }

  public Void visit(VarDeclNode varDeclNode) {
    this.store.put(varDeclNode.getVariable(), null);
    return null;
  }

  /** interpret assignment node. */
  public Void visit(AssignNode assignNode) {
    ImpValue rhsVal = assignNode.getRhs().accept(this);
    this.store.put(assignNode.getVariable(), rhsVal);
    return null;
  }

  /** interpret block node. */
  public Void visit(BlockNode blockNode) {
    for (StmtNode stmt : blockNode.getStatements()) {
      stmt.accept(this);
    }
    return null;
  }

  /** interpret conditional node. */
  public Void visit(IfNode ifNode) {
    BooleanLiteralNode guardVal = (BooleanLiteralNode)ifNode.getGuard().accept(this);
    if (guardVal.getValue()) {
      ifNode.getThenBranch().accept(this);
    } else {
      ifNode.getElseBranch().accept(this);
    }
    return null;
  }

  public Void visit(SendNode sendNode) {
    return null;
  }

  public Void visit(RecvNode recvNode) {
    return null;
  }

  /** interpret a statement in an InterpAnnotation. */
  public Void visit(AnnotationNode annotNode) {
    ImpAnnotation annot = annotNode.getAnnotation();
    if (annot != null) {
      if (annot instanceof ImpAnnotations.InterpAnnotation) {
        ImpAnnotations.InterpAnnotation interpAnnot =
            (ImpAnnotations.InterpAnnotation)annot;
        interpAnnot.getProgram().accept(this);
      }
    }

    return null;
  }
}
