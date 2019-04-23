package edu.cornell.cs.apl.viaduct;

/** a protocol for instantiating a PDG node. */
public interface Protocol<T extends AstNode> {
  boolean canInstantiate(PdgNode<T> node);

  Protocol<T> createInstance(PdgNode<T> node);
}
