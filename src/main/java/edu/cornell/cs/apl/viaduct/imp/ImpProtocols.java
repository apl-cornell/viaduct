package edu.cornell.cs.apl.viaduct.imp;

import edu.cornell.cs.apl.viaduct.Host;
import edu.cornell.cs.apl.viaduct.PdgNode;
import edu.cornell.cs.apl.viaduct.Protocol;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ImpProtocols {
  static class Single implements Protocol<ImpAstNode> {
    private static final Single rep = new Single();

    private Host host;

    private Single() {
      this.host = Host.getDefault();
    }

    private Single(Host h) {
      this.host = h;
    }

    public static Single getRepresentative() {
      return rep;
    }

    public Set<Protocol<ImpAstNode>> createInstances(
          Set<Host> hostConfig,
          Map<PdgNode<ImpAstNode>,Protocol<ImpAstNode>> protocolMap,
          PdgNode<ImpAstNode> node) {

      HashSet<Protocol<ImpAstNode>> instances = new HashSet<>();
      for (Host h : hostConfig) {
        instances.add(new Single(h));
      }
      return instances;
    }

    @Override
    public boolean equals(Object o) {
      if (o == null) { return false; }

      if (o instanceof Single) {
        Single osingle = (Single)o;
        return this.host.equals(osingle.host);

      } else {
        return false;
      }
    }

    @Override
    public int hashCode() {
      return this.host.hashCode();
    }

    @Override
    public String toString() {
      return String.format("Single(%s)", this.host.toString());
    }
  }

  static class Replication implements Protocol<ImpAstNode> {
    private static final Replication rep = new Replication();
    private Set<Host> realReplicas;
    private Set<Host> hashReplicas;

    private Replication() {
      this.realReplicas = new HashSet<Host>();
      this.hashReplicas = new HashSet<Host>();
    }

    private Replication(Set<Host> real, Set<Host> hash) {
      this.realReplicas = real;
      this.hashReplicas = hash;
    }

    public static Replication getRepresentative() {
      return rep;
    }

    public Set<Protocol<ImpAstNode>> createInstances(
          Set<Host> hostConfig,
          Map<PdgNode<ImpAstNode>,Protocol<ImpAstNode>> protocolMap,
          PdgNode<ImpAstNode> node) {

      return new HashSet<Protocol<ImpAstNode>>();
    }

    @Override
    public boolean equals(Object o) {
      if (o == null) { return false; }

      if (o instanceof Replication) {
        Replication oreplication = (Replication)o;
        boolean realEq = this.realReplicas.equals(oreplication.realReplicas);
        boolean hashEq = this.hashReplicas.equals(oreplication.hashReplicas);
        return realEq && hashEq;

      } else {
        return false;
      }
    }

    @Override
    public int hashCode() {
      return Objects.hash(this.realReplicas, this.hashReplicas);
    }

    @Override
    public String toString() {
      HashSet<String> realStrs = new HashSet<>();
      for (Host real : this.realReplicas) {
        realStrs.add(real.toString());
      }

      HashSet<String> hashStrs = new HashSet<>();
      for (Host hash : this.hashReplicas) {
        hashStrs.add(hash.toString());
      }

      String realList = String.join(",", realStrs);
      String hashList = String.join(",", hashStrs);
      return String.format("Replication({%s},{%s})", realList, hashList);
    }
  }

  static class MPC implements Protocol<ImpAstNode> {
    private static final MPC rep = new MPC();
    private Set<Host> parties;

    private MPC() {
      this.parties = new HashSet<Host>();
    }

    private MPC(Set<Host> ps) {
      this.parties = ps;
    }

    public static MPC getRepresentative() {
      return rep;
    }

    public Set<Protocol<ImpAstNode>> createInstances(
          Set<Host> hostConfig,
          Map<PdgNode<ImpAstNode>,Protocol<ImpAstNode>> protocolMap,
          PdgNode<ImpAstNode> node) {
      return new HashSet<Protocol<ImpAstNode>>();
    }

    @Override
    public boolean equals(Object o) {
      if (o == null) { return false; }

      if (o instanceof MPC) {
        MPC ompc = (MPC)o;
        return this.parties.equals(ompc.parties);

      } else {
        return false;
      }
    }

    @Override
    public int hashCode() {
      return this.parties.hashCode();
    }

    @Override
    public String toString() {
      HashSet<String> strs = new HashSet<>();
      for (Host party : this.parties) {
        strs.add(party.toString());
      }

      String strList = String.join(",", strs);
      return String.format("MPC({%s})", strList);
    }
  }

  static class ZK implements Protocol<ImpAstNode> {
    private static final ZK rep = new ZK();
    private Host prover;
    private Host verifier;

    private ZK() {
      this.prover = Host.getDefault();
      this.verifier = Host.getDefault();
    }

    private ZK(Host p, Host v) {
      this.prover = p;
      this.verifier = v;
    }

    public static ZK getRepresentative() {
      return rep;
    }

    public Set<Protocol<ImpAstNode>> createInstances(
          Set<Host> hostConfig,
          Map<PdgNode<ImpAstNode>,Protocol<ImpAstNode>> protocolMap,
          PdgNode<ImpAstNode> node) {
      return new HashSet<Protocol<ImpAstNode>>();
    }

    @Override
    public boolean equals(Object o) {
      if (o == null) { return false; }

      if (o instanceof ZK) {
        ZK ozk = (ZK)o;
        boolean peq = this.prover.equals(ozk.prover);
        boolean veq = this.prover.equals(ozk.prover);
        return peq && veq;
      } else {
        return false;
      }
    }

    @Override
    public int hashCode() {
      return Objects.hash(this.prover, this.verifier);
    }

    @Override
    public String toString() {
      String pname = this.prover.toString();
      String vname = this.verifier.toString();
      return String.format("ZK(%s,%s)", pname, vname);
    }
  }
}
