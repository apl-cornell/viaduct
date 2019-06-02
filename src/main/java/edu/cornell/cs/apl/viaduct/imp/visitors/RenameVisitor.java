package edu.cornell.cs.apl.viaduct.imp.visitors;

import edu.cornell.cs.apl.viaduct.imp.ast.ExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReadNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;
import java.util.Map;

public class RenameVisitor extends IdentityVisitor {
  private final Map<Variable, Variable> renameMap;

  public RenameVisitor(Map<Variable, Variable> rm) {
    this.renameMap = rm;
  }

  @Override
  public ExpressionNode visit(ReadNode readNode) {
    Variable var = readNode.getVariable();
    if (this.renameMap.containsKey(var)) {
      return new ReadNode(this.renameMap.get(var));

    } else {
      return new ReadNode(readNode.getVariable());
    }
  }
}
