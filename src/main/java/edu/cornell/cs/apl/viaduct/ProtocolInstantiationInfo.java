package edu.cornell.cs.apl.viaduct;

import edu.cornell.cs.apl.viaduct.imp.ast.Variable;
import edu.cornell.cs.apl.viaduct.imp.builders.StmtBuilder;

import java.util.Map;

/** helper class for protocol instantiation. */
public class ProtocolInstantiationInfo<T extends AstNode> {
  final ProcessConfigBuilder pconfig;
  final Map<PdgNode<T>,Protocol<T>> protocolMap;

  /** store config builder and protocol map. */
  public ProtocolInstantiationInfo(
      ProcessConfigBuilder pc, Map<PdgNode<T>,Protocol<T>> pm) {

    this.pconfig = pc;
    this.protocolMap = pm;
  }

  public Protocol<T> getProtocol(PdgNode<T> node) {
    return this.protocolMap.get(node);
  }

  public StmtBuilder getBuilder(Host h) {
    return this.pconfig.getBuilder(h);
  }

  public Variable getFreshVar(String base) {
    return this.pconfig.getFreshVar(base);
  }

  public Variable getFreshVar(Binding<T> base) {
    return getFreshVar(base.getBinding());
  }

  public String getFreshName(String base) {
    return this.pconfig.getFreshName(base);
  }

  public String getFreshName(Binding<T> base) {
    return getFreshName(base.getBinding());
  }
}
