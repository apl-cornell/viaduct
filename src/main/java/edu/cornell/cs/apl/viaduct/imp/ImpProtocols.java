package edu.cornell.cs.apl.viaduct.imp;

import edu.cornell.cs.apl.viaduct.Host;
import edu.cornell.cs.apl.viaduct.Label;
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

    public Host getHost() {
      return this.host;
    }

    public Set<Protocol<ImpAstNode>> createInstances(
          Set<Host> hostConfig,
          Map<PdgNode<ImpAstNode>,Protocol<ImpAstNode>> protocolMap,
          PdgNode<ImpAstNode> node) {

      HashSet<Protocol<ImpAstNode>> instances = new HashSet<>();
      if (node.isStorageNode() || node.isEndorseNode() || !node.isDowngradeNode()) {
        for (Host h : hostConfig) {
          Label hLabel = h.getLabel();
          Label nInLabel = node.getInLabel();

          if (nInLabel.confidentiality().flowsTo(hLabel.confidentiality())
              && hLabel.integrity().flowsTo(nInLabel.integrity())) {
            instances.add(new Single(h));
          }
        }
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

  static class ReplicaSets {
    public Set<Host> realReplicas;
    public Set<Host> hashReplicas;

    public ReplicaSets(Set<Host> real, Set<Host> hash) {
      this.realReplicas = real;
      this.hashReplicas = hash;
    }
  }

  static class Replication implements Protocol<ImpAstNode> {
    private static final Replication rep = new Replication();
    private ReplicaSets replicas;

    private Replication() {
      this.replicas = new ReplicaSets(new HashSet<Host>(), new HashSet<Host>());
    }

    private Replication(Set<Host> real, Set<Host> hash) {
      this.replicas = new ReplicaSets(real, hash);
    }

    public static Replication getRepresentative() {
      return rep;
    }

    public Set<Host> getRealReplicas() {
      return this.replicas.realReplicas;
    }

    public Set<Host> getHashReplicas() {
      return this.replicas.hashReplicas;
    }

    public Set<Protocol<ImpAstNode>> createInstances(
          Set<Host> hostConfig,
          Map<PdgNode<ImpAstNode>,Protocol<ImpAstNode>> protocolMap,
          PdgNode<ImpAstNode> node) {

      Set<Protocol<ImpAstNode>> instances = new HashSet<>();
      if (node.isStorageNode() || node.isEndorseNode() || !node.isDowngradeNode()) {
        // assume for now that there are only two hosts
        // generalize this later
        Host[] hostPair = new Host[2];
        hostConfig.toArray(hostPair);

        Set<ReplicaSets> possibleInstances = new HashSet<>();
        Set<Host> host1Set = new HashSet<Host>();
        host1Set.add(hostPair[0]);
        Set<Host> host2Set = new HashSet<Host>();
        host2Set.add(hostPair[1]);
        Set<Host> host12Set = new HashSet<Host>();
        host12Set.add(hostPair[0]);
        host12Set.add(hostPair[1]);
        possibleInstances.add(new ReplicaSets(host12Set, new HashSet<Host>()));
        possibleInstances.add(new ReplicaSets(host1Set, host2Set));
        possibleInstances.add(new ReplicaSets(host2Set, host1Set));

        Label nInLabel = node.getInLabel();

        for (ReplicaSets possibleInstance : possibleInstances) {
          Label rLabel = Label.top();
          Label rhLabel = Label.top();

          for (Host real : possibleInstance.realReplicas) {
            rLabel = rLabel.meet(real.getLabel());
            rhLabel = rhLabel.meet(real.getLabel());
          }
          for (Host hash : possibleInstance.hashReplicas) {
            rhLabel = rhLabel.meet(hash.getLabel());
          }

          if (nInLabel.confidentiality().flowsTo(rLabel.confidentiality())
              && rhLabel.integrity().flowsTo(nInLabel.integrity())
              // control nodes can't be hash replicated!
              && !(node.isControlNode() && possibleInstance.hashReplicas.size() > 0)) {
            instances.add(new Replication(
                possibleInstance.realReplicas,
                possibleInstance.hashReplicas));
          }
        }
      }

      return instances;
    }

    @Override
    public boolean equals(Object o) {
      if (o == null) { return false; }

      if (o instanceof Replication) {
        Replication oreplication = (Replication)o;
        boolean realEq = this.replicas.realReplicas.equals(oreplication.replicas.realReplicas);
        boolean hashEq = this.replicas.hashReplicas.equals(oreplication.replicas.hashReplicas);
        return realEq && hashEq;

      } else {
        return false;
      }
    }

    @Override
    public int hashCode() {
      return Objects.hash(this.replicas.realReplicas, this.replicas.hashReplicas);
    }

    @Override
    public String toString() {
      HashSet<String> realStrs = new HashSet<>();
      for (Host real : this.replicas.realReplicas) {
        realStrs.add(real.toString());
      }

      HashSet<String> hashStrs = new HashSet<>();
      for (Host hash : this.replicas.hashReplicas) {
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

      Set<Protocol<ImpAstNode>> instances = new HashSet<>();

      Set<PdgNode<ImpAstNode>> inNodes = node.getInNodes();
      Label nOutLabel = node.getOutLabel();

      boolean noInputFlow = true;
      for (PdgNode<ImpAstNode> inNode : inNodes) {
        if (inNode.getOutLabel().confidentiality().flowsTo(nOutLabel.confidentiality())) {
          noInputFlow = false;
          break;
        }
      }

      if (node.isDeclassifyNode() && noInputFlow) {
        Label hsLabel = Label.top();
        for (Host h : hostConfig) {
          hsLabel = hsLabel.meet(h.getLabel());
        }

        if (nOutLabel.confidentiality().flowsTo(hsLabel.confidentiality())) {
          instances.add(new MPC(hostConfig));
        }
      }

      return instances;
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

      // assume for now that there are only two hosts
      // generalize this later
      Host[] hostPair = new Host[2];
      hostConfig.toArray(hostPair);
      Set<PdgNode<ImpAstNode>> inNodes = node.getInNodes();
      Host hostA = hostPair[0];
      Host hostB = hostPair[1];
      Label nInLabel = node.getInLabel();
      Label nOutLabel = node.getOutLabel();
      Label aLabel = hostA.getLabel();
      Label bLabel = hostB.getLabel();

      Set<Protocol<ImpAstNode>> instances = new HashSet<>();

      // prover: A, verifier: B
      if (inNodes.size() == 1 && node.isDeclassifyNode()) {
        PdgNode<ImpAstNode> inNode = (PdgNode<ImpAstNode>)inNodes.toArray()[0];
        Protocol<ImpAstNode> inProto = protocolMap.get(inNode);
        if (inProto instanceof Replication) {
          Replication inReplProto = (Replication)inProto;

          if (nInLabel.confidentiality().flowsTo(aLabel.confidentiality())
              && !nInLabel.confidentiality().flowsTo(bLabel.confidentiality())
              && nOutLabel.confidentiality().flowsTo(bLabel.confidentiality())
              // && bLabel.integrity().flowsTo(nOutLabel.integrity())
              && inReplProto.getRealReplicas().contains(hostA)
              && inReplProto.getHashReplicas().contains(hostB)) {

            instances.add(new ZK(hostA, hostB));
          }

          if (nInLabel.confidentiality().flowsTo(bLabel.confidentiality())
              && !nInLabel.confidentiality().flowsTo(aLabel.confidentiality())
              && nOutLabel.confidentiality().flowsTo(aLabel.confidentiality())
              // && aLabel.integrity().flowsTo(nOutLabel.integrity())
              && inReplProto.getRealReplicas().contains(hostB)
              && inReplProto.getHashReplicas().contains(hostA)) {

            instances.add(new ZK(hostB, hostA));
          }
        }
      }

      return instances;
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
