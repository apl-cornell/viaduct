package edu.cornell.cs.apl.viaduct;

import java.util.Set;

/** prints a PDG into a DOT graph. */
public class PdgDotPrinter {
  /** print a PDG into a DOT graph. */
  public static <T extends AstNode> String printPdgDotGraph(ProgramDependencyGraph<T> pdg) {
    StringBuffer out = new StringBuffer("digraph {\n");

    Set<PdgNode<T>> nodes = pdg.getNodes();
    for (PdgNode<T> node : nodes) {
      String strNode = node.getLineNumber().toString();

      if (node instanceof PdgStorageNode<?>) {
        out.append("  \"" + strNode
            + "\" [shape=box;label=\"" + node.getAstNode().toString() + "\"];\n");
      } else if (node instanceof PdgControlNode<?>) {
        out.append("  \"" + strNode
            + "\" [shape=diamond;label=\"CONTROL\"];\n");
      } else if (node instanceof PdgComputeNode<?>) {
        out.append("  \"" + strNode
            + "\" [shape=oval;label=\"" + strNode + "\\n"
            + node.getAstNode().toString() + "\"];\n");
      }

      for (PdgNode<T> outNode : node.getOutNodes()) {
        String strOutNode = outNode.getLineNumber().toString();
        out.append("  \"" + strNode + "\" -> \"" + strOutNode + "\";\n");
      }
    }

    out.append("}\n");
    return out.toString();
  }
}
