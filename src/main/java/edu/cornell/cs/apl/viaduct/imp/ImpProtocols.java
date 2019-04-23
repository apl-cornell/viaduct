package edu.cornell.cs.apl.viaduct.imp;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import edu.cornell.cs.apl.viaduct.Host;
import edu.cornell.cs.apl.viaduct.PdgNode;
import edu.cornell.cs.apl.viaduct.Protocol;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;

public class ImpProtocols {
  static class Single implements Protocol<ImpAstNode> {
    private static Single rep = new Single();

    private Single() {}

    public static Single getRepresentative() {
      return rep;
    }

    public Set<Protocol<ImpAstNode>> createInstances(
          Set<Host> hostConfig,
          Map<PdgNode<ImpAstNode>,Protocol<ImpAstNode>> protocolMap,
          PdgNode<ImpAstNode> node) {
      return new HashSet<Protocol<ImpAstNode>>();
    }
  }

  static class Replication implements Protocol<ImpAstNode> {
    private static Replication rep = new Replication();

    private Replication() {}

    public static Replication getRepresentative() {
      return rep;
    }

    public Set<Protocol<ImpAstNode>> createInstances(
          Set<Host> hostConfig,
          Map<PdgNode<ImpAstNode>,Protocol<ImpAstNode>> protocolMap,
          PdgNode<ImpAstNode> node) {
      return new HashSet<Protocol<ImpAstNode>>();
    }
  }

  static class MPC implements Protocol<ImpAstNode> {
    private static MPC rep = new MPC();

    private MPC() {}

    public static MPC getRepresentative() {
      return rep;
    }

    public Set<Protocol<ImpAstNode>> createInstances(
          Set<Host> hostConfig,
          Map<PdgNode<ImpAstNode>,Protocol<ImpAstNode>> protocolMap,
          PdgNode<ImpAstNode> node) {
      return new HashSet<Protocol<ImpAstNode>>();
    }
  }

  static class ZK implements Protocol<ImpAstNode> {
    private static ZK rep = new ZK();

    private ZK() {}

    public static ZK getRepresentative() {
      return rep;
    }

    public Set<Protocol<ImpAstNode>> createInstances(
          Set<Host> hostConfig,
          Map<PdgNode<ImpAstNode>,Protocol<ImpAstNode>> protocolMap,
          PdgNode<ImpAstNode> node) {
      return new HashSet<Protocol<ImpAstNode>>();
    }
  }
}
