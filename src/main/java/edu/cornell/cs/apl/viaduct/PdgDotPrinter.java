package edu.cornell.cs.apl.viaduct;

import static guru.nidi.graphviz.model.Factory.mutGraph;
import static guru.nidi.graphviz.model.Factory.mutNode;

import edu.cornell.cs.apl.viaduct.ProgramDependencyGraph.ControlLabel;
import guru.nidi.graphviz.attribute.Arrow;
import guru.nidi.graphviz.attribute.Color;
import guru.nidi.graphviz.attribute.Label;
import guru.nidi.graphviz.attribute.Shape;
import guru.nidi.graphviz.attribute.Style;
import guru.nidi.graphviz.model.Link;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.model.MutableNode;
import java.util.Map;
import java.util.Set;

/** prints a PDG into a DOT graph. */
public class PdgDotPrinter {
  /** build DOT graph. */
  private static <T extends AstNode> MutableGraph pdgDotGraph(
      ProgramDependencyGraph<T> pdg,
      Map<PdgNode<T>, Protocol<T>> protocolMap,
      GraphData dataFormat) {

    MutableGraph g = mutGraph().setDirected(true);
    Set<PdgNode<T>> nodes = pdg.getNodes();

    for (PdgNode<T> node : nodes) {
      String nodeId = node.getId();
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

      Shape shape;
      String label = "";
      if (node instanceof PdgStorageNode<?>) {
        shape = Shape.RECTANGLE;
        label = String.format("%s\\n%s", node.getAstNode(), data);
      } else if (node instanceof PdgComputeNode<?>) {
        shape = Shape.EGG;
        label = String.format("%s\\n%s", node.getAstNode(), data);
      } else {
        shape = Shape.DIAMOND;
        label = String.format("%s\\n%s", "CONDITIONAL", data);
      }

      MutableNode grNode = mutNode(nodeId).add("label", label).add(shape);

      g.add(grNode);
      for (PdgInfoEdge<T> infoEdge : node.getOutInfoEdges()) {
        PdgNode<T> outNode = infoEdge.getTarget();
        String strOutNode = outNode.getId();
        Style style;

        // draw edge as a read channel
        if (infoEdge.isFlowEdge()) {
          style = Style.DOTTED;
        } else {
          style = Style.SOLID;
        }

        Link link = Link.to(mutNode(strOutNode)).add(style).add(Color.BLUE);

        Binding<T> infoEdgeLabel = infoEdge.getLabel();
        if (infoEdgeLabel != null) {
          link.add(Label.of(infoEdgeLabel.getBinding()));
        }

        if (infoEdge.isWriteEdge()) {
          link.add(Arrow.BOX);
        }

        grNode.addLink(link);
      }

      for (PdgControlEdge<T> ctrlEdge : node.getOutControlEdges()) {
        PdgNode<T> outNode = ctrlEdge.getTarget();
        String strOutNode = outNode.getId();

        if (ctrlEdge.getLabel().equals(ControlLabel.SEQ)) {
          grNode.addLink(Link.to(mutNode(strOutNode)).add(Color.RED));

        } else {
          grNode.addLink(
              Link.to(mutNode(strOutNode))
                  .add(Label.of(ctrlEdge.getLabel().toString()))
                  .add(Color.RED));
        }
      }
    }
    return g;
  }

  public static <T extends AstNode> MutableGraph pdgDotGraphWithLabels(
      ProgramDependencyGraph<T> pdg) {
    return pdgDotGraph(pdg, null, GraphData.LABEL);
  }

  public static <T extends AstNode> MutableGraph pdgDotGraphWithProtocols(
      ProgramDependencyGraph<T> pdg, Map<PdgNode<T>, Protocol<T>> protoMap) {
    return pdgDotGraph(pdg, protoMap, GraphData.PROTOCOL);
  }

  private enum GraphData {
    LABEL,
    PROTOCOL
  }
}
