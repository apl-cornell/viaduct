package edu.cornell.cs.apl.viaduct;

import java.util.Map;
import java.util.Set;

/** prints a PDG into a DOT graph. */
public class PdgDotPrinter {
  private static enum GraphData { LABEL, PROTOCOL }

  /** print a PDG into a DOT graph. */
  protected static <T extends AstNode> String pdgDotGraph(
      ProgramDependencyGraph<T> pdg,
      Map<PdgNode<T>,Protocol<T>> protocolMap,
      GraphData dataFormat) {

    StringBuffer out = new StringBuffer("digraph {\n");

    Set<PdgNode<T>> nodes = pdg.getNodes();
    for (PdgNode<T> node : nodes) {
      String lineNumStr = node.getLineNumber().toString();
      String data = "";
      switch (dataFormat) {
        case LABEL:
          if (node.isDowngradeNode()) {
            String inLabelStr = node.getInLabel().toString();
            String outLabelStr = node.getOutLabel().toString();
            data = inLabelStr + " / " + outLabelStr;
          } else {
            data = node.getOutLabel().toString();
          }
          break;

        case PROTOCOL:
          Protocol<T> nodeProto = protocolMap.get(node);
          if (nodeProto != null) {
            data = nodeProto.toString();
          } else {
            data = "NO PROTOCOL";
          }
          break;

        default:
      }

      if (node instanceof PdgStorageNode<?>) {
        out.append(
            String.format("  \"%s\" [shape=box;label=\"%s\\n%s\"];%n",
                lineNumStr, node.getAstNode(), data));

      } else if (node instanceof PdgControlNode<?>) {
        out.append(
            String.format("  \"%s\" [shape=diamond;label=\"%s\\n%s\"];%n",
                lineNumStr, "CONDITIONAL", data));

      } else if (node instanceof PdgComputeNode<?>) {
        out.append(
            String.format("  \"%s\" [shape=oval;label=\"%s\\n%s\"];%n",
                lineNumStr, node.getAstNode(), data));
      }

      for (PdgNode<T> outNode : node.getOutNodes()) {
        String strOutNode = outNode.getLineNumber().toString();

        // draw edge as a read channel
        if (node.isControlNode() && outNode.isStorageNode())  {
          out.append("  \"" + lineNumStr + "\" -> \"" + strOutNode + "\"[style=dotted];\n");

        } else {
          out.append("  \"" + lineNumStr + "\" -> \"" + strOutNode + "\";\n");
        }
      }
    }

    out.append("}\n");
    return out.toString();
  }

  public static <T extends AstNode> String pdgDotGraphWithLabels(
      ProgramDependencyGraph<T> pdg)
  {
    return pdgDotGraph(pdg, null, GraphData.LABEL);
  }

  public static <T extends AstNode> String pdgDotGraphWithProtocols(
      ProgramDependencyGraph<T> pdg, Map<PdgNode<T>,Protocol<T>> protoMap)
  {
    return pdgDotGraph(pdg, protoMap, GraphData.PROTOCOL);
  }
}
