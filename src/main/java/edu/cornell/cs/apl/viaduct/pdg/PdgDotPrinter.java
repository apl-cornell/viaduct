package edu.cornell.cs.apl.viaduct.pdg;

import static guru.nidi.graphviz.model.Factory.mutGraph;
import static guru.nidi.graphviz.model.Factory.mutNode;

import edu.cornell.cs.apl.viaduct.AstNode;
import edu.cornell.cs.apl.viaduct.AstPrinter;
import edu.cornell.cs.apl.viaduct.protocol.Protocol;
import edu.cornell.cs.apl.viaduct.protocol.ProtocolCostEstimator;
import guru.nidi.graphviz.attribute.Arrow;
import guru.nidi.graphviz.attribute.Color;
import guru.nidi.graphviz.attribute.Label;
import guru.nidi.graphviz.attribute.Shape;
import guru.nidi.graphviz.attribute.Style;
import guru.nidi.graphviz.model.Link;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.model.MutableNode;

import java.util.List;
import java.util.Map;

/** prints a PDG into a DOT graph. */
public class PdgDotPrinter {
  /** build DOT graph. */
  private static <T extends AstNode> MutableGraph pdgDotGraph(
      ProgramDependencyGraph<T> pdg,
      Map<PdgNode<T>, Protocol<T>> protocolMap,
      ProtocolCostEstimator<T> costEstimator,
      AstPrinter<T> printer,
      GraphData dataFormat) {

    MutableGraph g = mutGraph().setDirected(true);
    List<PdgNode<T>> nodes = pdg.getOrderedNodes();

    for (PdgNode<T> node : nodes) {
      String nodeId = node.getId();
      String data = "";
      switch (dataFormat) {
        case LABEL:
          data = node.getLabel().toString();
          break;

        case PROTOCOL:
          Protocol<T> nodeProto = protocolMap.get(node);
          if (nodeProto != null && costEstimator != null) {
            data =
              nodeProto.toString()
              + "\n"
              + costEstimator.estimateNodeCost(node, protocolMap, pdg);

          } else if (nodeProto != null) {
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
        label = String.format("%s\\n%s", printer.print(node.getAstNode()), data);
      } else if (node instanceof PdgComputeNode<?>) {
        shape = Shape.EGG;
        label = String.format("%s\\n%s", printer.print(node.getAstNode()), data);
      } else {
        shape = Shape.DIAMOND;
        label = String.format("%s\\n%s", node.getId(), data);
      }

      MutableNode grNode = mutNode(nodeId).add("label", label).add(shape);

      g.add(grNode);
      for (PdgInfoEdge<T> infoEdge : node.getOutInfoEdges()) {
        PdgNode<T> outNode = infoEdge.getTarget();
        String strOutNode = outNode.getId();
        Style style;

        // draw edge as a read channel
        if (infoEdge.isReadChannelEdge()) {
          style = Style.DOTTED;

        /*
        } else if (infoEdge.isPcFlowEdge()) {
          style = Style.DASHED;
        */

        } else {
          style = Style.SOLID;
        }

        if (!infoEdge.isPcFlowEdge()) {
          Link link = Link.to(mutNode(strOutNode)).add(style).add(Color.BLUE);

          String infoEdgeLabel = infoEdge.getLabel(printer);
          if (infoEdgeLabel != null) {
            link.add(Label.of(infoEdgeLabel));
          }

          if (infoEdge.isWriteEdge()) {
            link.add(Arrow.BOX);
          }

          grNode.addLink(link);
        }
      }
    }
    return g;
  }

  public static <T extends AstNode> MutableGraph pdgDotGraphWithLabels(
      ProgramDependencyGraph<T> pdg, AstPrinter<T> printer) {
    return pdgDotGraph(pdg, null, null, printer, GraphData.LABEL);
  }

  public static <T extends AstNode> MutableGraph pdgDotGraphWithProtocols(
      ProgramDependencyGraph<T> pdg,
      Map<PdgNode<T>, Protocol<T>> protoMap,
      ProtocolCostEstimator<T> costEstimator,
      AstPrinter<T> printer) {
    return pdgDotGraph(pdg, protoMap, costEstimator, printer, GraphData.PROTOCOL);
  }

  private enum GraphData {
    LABEL,
    PROTOCOL
  }
}
