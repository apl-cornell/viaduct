package edu.cornell.cs.apl.viaduct.imp.protocols;

import edu.cornell.cs.apl.viaduct.Binding;
import edu.cornell.cs.apl.viaduct.imp.ast.Host;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;
import edu.cornell.cs.apl.viaduct.pdg.PdgComputeNode;
import edu.cornell.cs.apl.viaduct.pdg.PdgNode;
import edu.cornell.cs.apl.viaduct.pdg.PdgStorageNode;
import edu.cornell.cs.apl.viaduct.protocol.Protocol;
import edu.cornell.cs.apl.viaduct.protocol.ProtocolInstantiationException;
import edu.cornell.cs.apl.viaduct.protocol.ProtocolInstantiationInfo;

import java.util.List;

public abstract class AbstractSingle extends Cleartext implements Protocol<ImpAstNode> {
  private Variable outVar;

  protected abstract Host getActualHost();

  @Override
  public void initialize(PdgNode<ImpAstNode> node, ProtocolInstantiationInfo<ImpAstNode> info) {
    return;
  }

  @Override
  public void instantiate(PdgNode<ImpAstNode> node, ProtocolInstantiationInfo<ImpAstNode> info) {
    Host host = getActualHost();
    if (node.isStorageNode()) {
      this.outVar = instantiateStorageNode(host, (PdgStorageNode<ImpAstNode>) node, info);

    } else if (node.isComputeNode()) {
      this.outVar = instantiateComputeNode(host, (PdgComputeNode<ImpAstNode>) node, info);

    } else {
      throw new ProtocolInstantiationException("control nodes must have Control protocol");
    }
  }

  @Override
  public Binding<ImpAstNode> readFrom(
      PdgNode<ImpAstNode> node,
      Host readHost,
      Binding<ImpAstNode> readLabel,
      List<ImpAstNode> args,
      ProtocolInstantiationInfo<ImpAstNode> info) {

    // this should not be read from until it has been instantiated!
    assert this.outVar != null;
    return performRead(node, readHost, readLabel, getActualHost(), this.outVar, args, info);
  }

  @Override
  public void writeTo(
      PdgNode<ImpAstNode> node,
      Host writeHost,
      List<ImpAstNode> args,
      ProtocolInstantiationInfo<ImpAstNode> info) {

    // can only write to storage nodes
    if (node.isStorageNode()) {
      // node must have been instantiated before being written to
      assert this.outVar != null;
      performWrite(node, writeHost, getActualHost(), this.outVar, args, info);

    } else {
      throw new ProtocolInstantiationException(
          "attempted to write to a non storage node");
    }
  }
}
