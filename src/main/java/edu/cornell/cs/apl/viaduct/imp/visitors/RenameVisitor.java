package edu.cornell.cs.apl.viaduct.imp.visitors;

import edu.cornell.cs.apl.viaduct.imp.ast.ExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReadNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;
import java.util.HashMap;
import java.util.Map;

public class RenameVisitor extends ReplaceVisitor {
  public RenameVisitor(Map<Variable, Variable> varMap) {
    super(buildReplaceMap(varMap), new HashMap<>());
  }

  private static Map<ExpressionNode, ExpressionNode> buildReplaceMap(
      Map<Variable, Variable> varMap) {
    Map<ExpressionNode, ExpressionNode> replacements = new HashMap<>();
    for (Map.Entry<Variable, Variable> kv : varMap.entrySet()) {
      replacements.put(
          ReadNode.builder().setReference(kv.getKey()).build(),
          ReadNode.builder().setReference(kv.getValue()).build());
    }

    return replacements;
  }
}
