package edu.cornell.cs.apl.viaduct.imp;

import edu.cornell.cs.apl.viaduct.PdgNode;
import edu.cornell.cs.apl.viaduct.Protocol;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;

public class ImpProtocols {
  static class Single implements Protocol<ImpAstNode> {
    private static Single instance = new Single();

    private Single() {}

    public static Single getInstance() {
      return instance;
    }

    public boolean canInstantiate(PdgNode<ImpAstNode> node) {
      return true;
    }

    public Protocol<ImpAstNode> createInstance(PdgNode<ImpAstNode> node) {
      return new Single();
    }
  }

  static class Replication implements Protocol<ImpAstNode> {
    private static Replication instance = new Replication();

    private Replication() {}

    public static Replication getInstance() {
      return instance;
    }

    public boolean canInstantiate(PdgNode<ImpAstNode> node) {
      return true;
    }

    public Protocol<ImpAstNode> createInstance(PdgNode<ImpAstNode> node) {
      return new Replication();
    }
  }

  static class MPC implements Protocol<ImpAstNode> {
    private static MPC instance = new MPC();

    private MPC() {}

    public static MPC getInstance() {
      return instance;
    }

    public boolean canInstantiate(PdgNode<ImpAstNode> node) {
      return true;
    }

    public Protocol<ImpAstNode> createInstance(PdgNode<ImpAstNode> node) {
      return new MPC();
    }
  }

  static class ZK implements Protocol<ImpAstNode> {
    private static ZK instance = new ZK();

    private ZK() {}

    public static ZK getInstance() {
      return instance;
    }

    public boolean canInstantiate(PdgNode<ImpAstNode> node) {
      return true;
    }

    public Protocol<ImpAstNode> createInstance(PdgNode<ImpAstNode> node) {
      return new ZK();
    }
  }
}
