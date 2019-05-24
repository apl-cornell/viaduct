package edu.cornell.cs.apl.viaduct.imp;

import edu.cornell.cs.apl.viaduct.Host;
import edu.cornell.cs.apl.viaduct.PdgComputeNode;
import edu.cornell.cs.apl.viaduct.PdgEdge;
import edu.cornell.cs.apl.viaduct.PdgInfoEdge;
import edu.cornell.cs.apl.viaduct.PdgNode;
import edu.cornell.cs.apl.viaduct.PdgStorageNode;
import edu.cornell.cs.apl.viaduct.PdgWriteEdge;
import edu.cornell.cs.apl.viaduct.Protocol;
import edu.cornell.cs.apl.viaduct.ProtocolInstantiationInfo;
import edu.cornell.cs.apl.viaduct.imp.ast.AssignNode;
import edu.cornell.cs.apl.viaduct.imp.ast.DeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;
import edu.cornell.cs.apl.viaduct.imp.builders.ExpressionBuilder;
import edu.cornell.cs.apl.viaduct.imp.builders.StmtBuilder;
import edu.cornell.cs.apl.viaduct.imp.visitors.RenameVisitor;
import edu.cornell.cs.apl.viaduct.security.Label;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ImpProtocols {
  static class Cleartext {
    protected Variable instantiateStorageNode(
        Host host, PdgStorageNode<ImpAstNode> node, ProtocolInstantiationInfo<ImpAstNode> info) {

      // declare new variable
      DeclarationNode varDecl = (DeclarationNode) node.getAstNode();
      Variable newVar = info.getFreshVar(varDecl.getVariable().getName());

      StmtBuilder builder = info.getBuilder(host);
      builder.varDecl(newVar, varDecl.getLabel());

      return newVar;
    }

    protected Variable instantiateComputeNode(
        Host host, PdgComputeNode<ImpAstNode> node, ProtocolInstantiationInfo<ImpAstNode> info) {

      StmtBuilder builder = info.getBuilder(host);
      // read from inputs
      Map<Variable, Variable> renameMap = new HashMap<>();
      for (PdgInfoEdge<ImpAstNode> inEdge : node.getInInfoEdges()) {
        if (!inEdge.getSource().isControlNode()) {
          PdgNode<ImpAstNode> inNode = inEdge.getSource();
          Protocol<ImpAstNode> inNodeProto = info.getProtocol(inNode);
          Set<Host> readHosts = inNodeProto.readFrom(inNode, host, node, info);
          for (Host readHost : readHosts) {
            String edgeLabel = inEdge.getLabel();
            Variable readVar = info.getFreshVar(edgeLabel);
            builder.recv(readHost, readVar);
            renameMap.put(new Variable(edgeLabel), readVar);
          }

          // inNodeProto.readPostprocess(this.host, node, protocolMap, pconfig);
        }
      }

      // perform computation
      Variable outVar = info.getFreshVar(node.getId());
      ImpAstNode astNode = node.getAstNode();
      RenameVisitor renamer = new RenameVisitor(renameMap);
      if (astNode instanceof AssignNode) {
        AssignNode assignNode = (AssignNode) astNode;
        ExpressionNode computation = assignNode.getRhs().accept(renamer);
        builder.assign(outVar, computation);

      } else if (astNode instanceof ExpressionNode) {
        ExpressionNode computation = ((ExpressionNode) astNode).accept(renamer);
        builder.assign(outVar, computation);
      }

      // write to storage nodes
      ExpressionBuilder e = new ExpressionBuilder();
      for (PdgEdge<ImpAstNode> outEdge : node.getOutInfoEdges()) {
        // only write to variables, since if computations read from
        // the output of this node then it will call readFrom() anyway
        if (outEdge instanceof PdgWriteEdge<?>) {
          PdgStorageNode<ImpAstNode> outNode = (PdgStorageNode<ImpAstNode>) outEdge.getTarget();
          Protocol<ImpAstNode> outProto = info.getProtocol(outNode);
          outProto.writeTo(outNode, host, node, e.var(outVar), info);
        }
      }

      return outVar;
    }
  }

  static class Single extends Cleartext implements Protocol<ImpAstNode> {
    private static final Single rep = new Single();

    private Host host;
    // private Variable storageVar;
    private Variable outVar;

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

    @Override
    public Set<Protocol<ImpAstNode>> createInstances(
        Set<Host> hostConfig,
        Map<PdgNode<ImpAstNode>, Protocol<ImpAstNode>> protocolMap,
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
    public Set<Host> readFrom(
        PdgNode<ImpAstNode> node,
        Host readHost,
        PdgNode<ImpAstNode> reader,
        ProtocolInstantiationInfo<ImpAstNode> info) {

      // this should not be read from unti it has been instantiated!
      assert this.outVar != null;

      ExpressionBuilder e = new ExpressionBuilder();
      StmtBuilder builder = info.getBuilder(this.host);
      builder.send(readHost, e.var(this.outVar));

      Set<Host> readHosts = new HashSet<>();
      readHosts.add(this.host);
      return readHosts;
    }

    @Override
    public void writeTo(
        PdgNode<ImpAstNode> node,
        Host writeHost,
        PdgNode<ImpAstNode> writer,
        ImpAstNode val,
        ProtocolInstantiationInfo<ImpAstNode> info) {

      // can only write to storage nodes
      if (node.isStorageNode()) {
        // node must have been instantiated before being written to
        assert this.outVar != null;

        StmtBuilder builder = info.getBuilder(this.host);
        StmtBuilder writerBuilder = info.getBuilder(writeHost);

        writerBuilder.send(this.host, (ExpressionNode) val);
        builder.recv(writeHost, this.outVar);
      }
    }

    @Override
    public void instantiate(PdgNode<ImpAstNode> node, ProtocolInstantiationInfo<ImpAstNode> info) {

      if (node.isStorageNode()) {
        this.outVar = instantiateStorageNode(this.host, (PdgStorageNode<ImpAstNode>) node, info);

      } else if (node.isComputeNode()) {
        this.outVar = instantiateComputeNode(this.host, (PdgComputeNode<ImpAstNode>) node, info);

      } else if (node.isControlNode()) {
        // read guard

      }
    }

    @Override
    public boolean equals(Object o) {
      if (o == null) {
        return false;
      }

      if (o instanceof Single) {
        Single osingle = (Single) o;
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

  static class Replication extends Cleartext implements Protocol<ImpAstNode> {
    private static final Replication rep = new Replication();
    private ReplicaSets replicas;
    // private Map<Host,Variable> storageVarMap;
    // private Map<Host,Variable> outVarMap;

    private Replication() {
      this.replicas = new ReplicaSets(new HashSet<Host>(), new HashSet<Host>());
      // this.storageVarMap = new HashMap<>();
      // this.outVarMap = new HashMap<>();
    }

    private Replication(Set<Host> real, Set<Host> hash) {
      this.replicas = new ReplicaSets(real, hash);
      // this.storageVarMap = new HashMap<>();
      // this.outVarMap = new HashMap<>();
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

    @Override
    public Set<Protocol<ImpAstNode>> createInstances(
        Set<Host> hostConfig,
        Map<PdgNode<ImpAstNode>, Protocol<ImpAstNode>> protocolMap,
        PdgNode<ImpAstNode> node) {

      Set<Protocol<ImpAstNode>> instances = new HashSet<>();
      if (node.isStorageNode() || node.isEndorseNode() || !node.isDowngradeNode()) {
        // assume for now that there are only two hosts
        // generalize this later
        Host[] hostPair = new Host[2];
        int i = 0;
        for (Host host : hostConfig) {
          hostPair[i] = host;
          i++;
          if (i == hostPair.length) {
            break;
          }
        }

        Set<Host> host1Set = new HashSet<Host>();
        host1Set.add(hostPair[0]);
        Set<Host> host2Set = new HashSet<Host>();
        host2Set.add(hostPair[1]);
        Set<Host> host12Set = new HashSet<Host>();
        host12Set.add(hostPair[0]);
        host12Set.add(hostPair[1]);

        Set<ReplicaSets> possibleInstances = new HashSet<>();
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
              && !(node.isControlNode() && possibleInstance.hashReplicas.size() > 0)
              // has to be more than 1 replica to actually be replicated
              && possibleInstance.realReplicas.size() + possibleInstance.hashReplicas.size() > 1) {

            instances.add(
                new Replication(possibleInstance.realReplicas, possibleInstance.hashReplicas));
          }
        }
      }

      return instances;
    }

    @Override
    public Set<Host> readFrom(
        PdgNode<ImpAstNode> node,
        Host h,
        PdgNode<ImpAstNode> reader,
        ProtocolInstantiationInfo<ImpAstNode> info) {

      // TODO: finish
      return new HashSet<>();
    }

    @Override
    public void writeTo(
        PdgNode<ImpAstNode> node,
        Host h,
        PdgNode<ImpAstNode> writer,
        ImpAstNode val,
        ProtocolInstantiationInfo<ImpAstNode> info) {

      // TODO: finish
    }

    @Override
    public void instantiate(PdgNode<ImpAstNode> node, ProtocolInstantiationInfo<ImpAstNode> info) {

      /*
      if (node.isStorageNode()) {
        for (Host realHost : this.replicas.realReplicas) {
          Variable hostStorageVar =
              instantiateStorageNode(realHost, (PdgStorageNode<ImpAstNode>)node, info);
          this.storageVarMap.put(realHost, hostStorageVar);
        }

        for (Host hashHost : this.replicas.hashReplicas) {
          Variable hostStorageVar =
              instantiateStorageNode(hashHost, (PdgStorageNode<ImpAstNode>)node, info);
          this.storageVarMap.put(hashHost, hostStorageVar);
        }

      } else if (node.isComputeNode()) {
        for (Host realHost : this.replicas.realReplicas) {
          Variable hostOutVar =
              instantiateComputeNode(realHost, (PdgComputeNode<ImpAstNode>)node, info);
          this.outVarMap.put(realHost, hostOutVar);
        }

        for (Host hashHost : this.replicas.realReplicas) {
          Variable hostOutVar =
              instantiateComputeNode(hashHost, (PdgComputeNode<ImpAstNode>)node, info);
          this.outVarMap.put(hashHost, hostOutVar);
        }

      } else if (node.isControlNode()) {
        // read guard

      }
      */
    }

    @Override
    public boolean equals(Object o) {
      if (o == null) {
        return false;
      }

      if (o instanceof Replication) {
        Replication oreplication = (Replication) o;
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

    @Override
    public Set<Protocol<ImpAstNode>> createInstances(
        Set<Host> hostConfig,
        Map<PdgNode<ImpAstNode>, Protocol<ImpAstNode>> protocolMap,
        PdgNode<ImpAstNode> node) {

      Set<Protocol<ImpAstNode>> instances = new HashSet<>();

      Set<PdgNode<ImpAstNode>> inNodes = new HashSet<>();
      for (PdgInfoEdge<ImpAstNode> edge : node.getInInfoEdges()) {
        PdgNode<ImpAstNode> source = edge.getSource();
        if (!source.isControlNode()) {
          inNodes.add(source);
        }
      }
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
    public Set<Host> readFrom(
        PdgNode<ImpAstNode> node,
        Host h,
        PdgNode<ImpAstNode> reader,
        ProtocolInstantiationInfo<ImpAstNode> info) {

      // TODO: finish
      return new HashSet<>();
    }

    @Override
    public void writeTo(
        PdgNode<ImpAstNode> node,
        Host h,
        PdgNode<ImpAstNode> writer,
        ImpAstNode val,
        ProtocolInstantiationInfo<ImpAstNode> info) {

      // TODO: finish
    }

    @Override
    public void instantiate(PdgNode<ImpAstNode> node, ProtocolInstantiationInfo<ImpAstNode> info) {

      // TODO: finish
    }

    @Override
    public boolean equals(Object o) {
      if (o == null) {
        return false;
      }

      if (o instanceof MPC) {
        MPC ompc = (MPC) o;
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

    @Override
    public Set<Protocol<ImpAstNode>> createInstances(
        Set<Host> hostConfig,
        Map<PdgNode<ImpAstNode>, Protocol<ImpAstNode>> protocolMap,
        PdgNode<ImpAstNode> node) {

      // assume for now that there are only two hosts
      // generalize this later
      Host[] hostPair = new Host[2];
      hostConfig.toArray(hostPair);
      int i = 0;
      for (Host host : hostConfig) {
        hostPair[i] = host;
        i++;
        if (i == hostPair.length) {
          break;
        }
      }

      Set<PdgNode<ImpAstNode>> inNodes = new HashSet<>();
      for (PdgInfoEdge<ImpAstNode> edge : node.getInInfoEdges()) {
        PdgNode<ImpAstNode> source = edge.getSource();
        if (!source.isControlNode()) {
          inNodes.add(source);
        }
      }
      Host hostA = hostPair[0];
      Host hostB = hostPair[1];
      Label nInLabel = node.getInLabel();
      Label nOutLabel = node.getOutLabel();
      Label aLabel = hostA.getLabel();
      Label bLabel = hostB.getLabel();

      Set<Protocol<ImpAstNode>> instances = new HashSet<>();

      // prover: A, verifier: B
      if (inNodes.size() == 1 && node.isDeclassifyNode()) {
        PdgNode<ImpAstNode> inNode = (PdgNode<ImpAstNode>) inNodes.toArray()[0];
        Protocol<ImpAstNode> inProto = protocolMap.get(inNode);
        if (inProto instanceof Replication) {
          Replication inReplProto = (Replication) inProto;

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
    public Set<Host> readFrom(
        PdgNode<ImpAstNode> node,
        Host h,
        PdgNode<ImpAstNode> reader,
        ProtocolInstantiationInfo<ImpAstNode> info) {

      // TODO: finish
      return new HashSet<>();
    }

    @Override
    public void writeTo(
        PdgNode<ImpAstNode> node,
        Host h,
        PdgNode<ImpAstNode> writer,
        ImpAstNode val,
        ProtocolInstantiationInfo<ImpAstNode> info) {

      // TODO: finish
    }

    @Override
    public void instantiate(PdgNode<ImpAstNode> node, ProtocolInstantiationInfo<ImpAstNode> info) {

      // TODO: finish
    }

    @Override
    public boolean equals(Object o) {
      if (o == null) {
        return false;
      }

      if (o instanceof ZK) {
        ZK ozk = (ZK) o;
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
