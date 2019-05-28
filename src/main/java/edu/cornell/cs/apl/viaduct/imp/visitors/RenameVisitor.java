package edu.cornell.cs.apl.viaduct.imp.visitors;

import edu.cornell.cs.apl.viaduct.imp.ast.ExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReadNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;

public class RenameVisitor extends CloneVisitor {
  Variable oldVar;
  Variable newVar;

  public RenameVisitor(Variable ov, Variable nv) {
    this.oldVar = ov;
    this.newVar = nv;
  }

  @Override
  public ExpressionNode visit(ReadNode readNode) {
    if (readNode.getVariable().equals(this.oldVar)) {
      return new ReadNode(this.newVar);

    } else {
      return new ReadNode(readNode.getVariable());
    }
  }
}
