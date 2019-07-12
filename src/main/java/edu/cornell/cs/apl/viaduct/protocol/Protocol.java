package edu.cornell.cs.apl.viaduct.protocol;

import edu.cornell.cs.apl.viaduct.AstNode;
import edu.cornell.cs.apl.viaduct.Binding;
import edu.cornell.cs.apl.viaduct.imp.ast.Host;
import edu.cornell.cs.apl.viaduct.pdg.PdgNode;

import java.util.List;
import java.util.Set;

/** A cryptographic protocol for instantiating a PDG node. */
public interface Protocol<T extends AstNode> {
  Set<Host> getHosts();

  void initialize(PdgNode<T> node, ProtocolInstantiationInfo<T> info);

  void instantiate(PdgNode<T> node, ProtocolInstantiationInfo<T> info);

  Binding<T> readFrom(PdgNode<T> node, Host readHost,
      Binding<T> readLabel, List<T> args, ProtocolInstantiationInfo<T> info);

  void writeTo(PdgNode<T> node, Host writeHost, List<T> args, ProtocolInstantiationInfo<T> info);
}
