package edu.cornell.cs.apl.viaduct.imp.visitors;

import edu.cornell.cs.apl.viaduct.imp.ast.ExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReadNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;

import java.util.HashMap;
import java.util.Map;

public class RenameVisitor extends ReplaceVisitor {
  private static Map<ExpressionNode,ExpressionNode> buildReplMap(Map<Variable,Variable> varMap) {
    Map<ExpressionNode,ExpressionNode> replMap = new HashMap<>();
    for (Map.Entry<Variable,Variable> kv : varMap.entrySet()) {
      replMap.put(new ReadNode(kv.getKey()), new ReadNode(kv.getValue()));
    }

    return replMap;
  }

  public RenameVisitor(Map<Variable, Variable> varMap) {
    super(buildReplMap(varMap), new HashMap<>());
  }
}
