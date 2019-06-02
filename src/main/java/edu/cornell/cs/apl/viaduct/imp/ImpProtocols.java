package edu.cornell.cs.apl.viaduct.imp;

import edu.cornell.cs.apl.viaduct.Binding;
import edu.cornell.cs.apl.viaduct.Host;
import edu.cornell.cs.apl.viaduct.PdgComputeNode;
import edu.cornell.cs.apl.viaduct.PdgControlNode;
import edu.cornell.cs.apl.viaduct.PdgEdge;
import edu.cornell.cs.apl.viaduct.PdgInfoEdge;
import edu.cornell.cs.apl.viaduct.PdgNode;
import edu.cornell.cs.apl.viaduct.PdgReadEdge;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ImpProtocols {
  static class Cleartext {
    protected Variable instantiateStorageNode(
        Host host, PdgStorageNode<ImpAstNode> node, ProtocolInstantiationInfo<ImpAstNode> info) {

      // declare new variable
      DeclarationNode varDecl = (DeclarationNode) node.getAstNode();
      // Variable newVar = info.getFreshVar(varDecl.getVariable().getName());
      Variable newVar = varDecl.getVariable();

      StmtBuilder builder = info.getBuilder(host);
      builder.varDecl(newVar, varDecl.getLabel());

      return newVar;
    }

    protected Binding<ImpAstNode> performRead(
        PdgNode<ImpAstNode> node, Binding<ImpAstNode> readLabel, Host host,
        ProtocolInstantiationInfo<ImpAstNode> info) {

      StmtBuilder builder = info.getBuilder(host);
      Protocol<ImpAstNode> readNodeProto = info.getProtocol(node);
      Set<Host> readHosts = readNodeProto.readFrom(node, host, info);

      Map<Host,Binding<ImpAstNode>> hostBindings = new HashMap<>();
      for (Host readHost : readHosts) {
        Variable readVar = info.getFreshVar(readLabel);
        builder.recv(readHost, readVar);
        hostBindings.put(readHost, readVar);
      }

      Binding<ImpAstNode> binding =
          readNodeProto.readPostprocess(hostBindings, host, info);

      return binding;
    }

    protected Variable instantiateComputeNode(
        Host host, PdgComputeNode<ImpAstNode> node, ProtocolInstantiationInfo<ImpAstNode> info) {

      StmtBuilder builder = info.getBuilder(host);
      // read from inputs
      Map<Variable, Variable> renameMap = new HashMap<>();
      for (PdgReadEdge<ImpAstNode> readEdge : node.getReadEdges()) {
        Variable readVar =
            (Variable)performRead(readEdge.getSource(), readEdge.getLabel(), host, info);
        renameMap.put((Variable)readEdge.getLabel(), readVar);
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
          outProto.writeTo(outNode, host, e.var(outVar), info);
        }
      }

      return outVar;
    }

    void instantiateControlNode(Set<Host> hosts, PdgControlNode<ImpAstNode> node,
        ProtocolInstantiationInfo<ImpAstNode> info) {

      // conditional node should only have one read input (result of the guard)
      List<PdgReadEdge<ImpAstNode>> infoEdges = new ArrayList<>(node.getReadEdges());
      assert infoEdges.size() == 1;

      // create conditional in all nodes that have a read channel from the control node
      Set<Host> controlStructureHosts = new HashSet<>();
      controlStructureHosts.addAll(hosts);
      for (PdgInfoEdge<ImpAstNode> infoEdge : node.getOutInfoEdges()) {
        controlStructureHosts.addAll(info.getProtocol(infoEdge.getTarget()).getHosts());
      }

      info.pushControlContext(controlStructureHosts);

      ExpressionBuilder e = new ExpressionBuilder();
      PdgReadEdge<ImpAstNode> guardEdge = infoEdges.get(0);
      PdgNode<ImpAstNode> guardNode = guardEdge.getSource();
      Binding<ImpAstNode> guardLabel = guardEdge.getLabel();
      for (Host controlStructureHost : controlStructureHosts) {
        StmtBuilder controlStructureBuilder = info.getBuilder(controlStructureHost);
        Binding<ImpAstNode> guardBinding =
            performRead(guardNode, guardLabel, controlStructureHost, info);
        controlStructureBuilder.pushIf(e.var((Variable)guardBinding));
      }
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

    public Set<Host> getHosts() {
      Set<Host> hosts = new HashSet<>();
      hosts.add(this.host);
      return hosts;
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
    public Set<Host> readFrom(PdgNode<ImpAstNode> node, Host readHost,
        ProtocolInstantiationInfo<ImpAstNode> info) {

      // this should not be read from until it has been instantiated!
      assert this.outVar != null;

      ExpressionBuilder e = new ExpressionBuilder();
      StmtBuilder builder = info.getBuilder(this.host);
      builder.send(readHost, e.var(this.outVar));

      Set<Host> hosts = new HashSet<>();
      hosts.add(this.host);
      return hosts;
    }

    @Override
    public Binding<ImpAstNode> readPostprocess(
        Map<Host,Binding<ImpAstNode>> hostBindings, Host host,
        ProtocolInstantiationInfo<ImpAstNode> info) {

      // because this is the Single protocol, there should only
      // have been one host that the node read from
      assert hostBindings.size() == 1;
      return hostBindings.get(this.host);
    }

    @Override
    public void writeTo(PdgNode<ImpAstNode> node, Host writeHost, ImpAstNode val,
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
        instantiateControlNode(getHosts(), (PdgControlNode<ImpAstNode>)node, info);
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
    private Map<Host,Variable> outVarMap;

    private Replication(Set<Host> real, Set<Host> hash) {
      this.replicas = new ReplicaSets(real, hash);
      this.outVarMap = new HashMap<>();
    }

    private Replication() {
      this(new HashSet<Host>(), new HashSet<Host>());
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

    public int getNumReplicas() {
      return this.replicas.realReplicas.size() + this.replicas.hashReplicas.size();
    }

    public Set<Host> getHosts() {
      Set<Host> hosts = new HashSet<>();
      hosts.addAll(this.replicas.realReplicas);
      hosts.addAll(this.replicas.hashReplicas);
      return hosts;
    }

    @Override
    public Set<Protocol<ImpAstNode>> createInstances(
        Set<Host> hostConfig,
        Map<PdgNode<ImpAstNode>, Protocol<ImpAstNode>> protocolMap,
        PdgNode<ImpAstNode> node) {

      Set<Protocol<ImpAstNode>> instances = new HashSet<>();
      if (hostConfig.size() >= 2
          && (node.isStorageNode() || node.isEndorseNode() || !node.isDowngradeNode())) {

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

        /*
        Set<Host> host1Set = new HashSet<Host>();
        host1Set.add(hostPair[0]);
        Set<Host> host2Set = new HashSet<Host>();
        host2Set.add(hostPair[1]);
        */
        Set<Host> host12Set = new HashSet<Host>();
        host12Set.add(hostPair[0]);
        host12Set.add(hostPair[1]);

        Set<ReplicaSets> possibleInstances = new HashSet<>();
        possibleInstances.add(new ReplicaSets(host12Set, new HashSet<Host>()));
        // possibleInstances.add(new ReplicaSets(host1Set, host2Set));
        // possibleInstances.add(new ReplicaSets(host2Set, host1Set));

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
    public Set<Host> readFrom(PdgNode<ImpAstNode> node, Host readHost,
        ProtocolInstantiationInfo<ImpAstNode> info) {

      // should not be read from until it has been instantiated
      assert this.outVarMap.size() == getNumReplicas();

      ExpressionBuilder e = new ExpressionBuilder();
      Set<Host> hosts = new HashSet<>();

      if (this.replicas.realReplicas.contains(readHost)) {
        StmtBuilder builder = info.getBuilder(readHost);
        builder.send(readHost, e.var(this.outVarMap.get(readHost)));
        hosts.add(readHost);

      } else {
        for (Host realHost : this.replicas.realReplicas) {
          StmtBuilder builder = info.getBuilder(realHost);
          builder.send(readHost, e.var(this.outVarMap.get(realHost)));
          hosts.add(realHost);
        }
      }

      /*
      for (Host hashHost : this.replicas.hashReplicas) {
        StmtBuilder builder = info.getBuilder(hashHost);
        builder.send(readHost, e.var(this.outVarMap.get(hashHost)));
        hosts.add(hashHost);
      }
      */

      return hosts;
    }

    @Override
    public Binding<ImpAstNode> readPostprocess(
        Map<Host,Binding<ImpAstNode>> hostBindings, Host host,
        ProtocolInstantiationInfo<ImpAstNode> info) {

      // TODO: fix this
      Host h = (Host)hostBindings.keySet().toArray()[0];
      return hostBindings.get(h);
    }

    @Override
    public void writeTo(PdgNode<ImpAstNode> node, Host writeHost, ImpAstNode val,
        ProtocolInstantiationInfo<ImpAstNode> info) {

      if (node.isStorageNode()) {
        // node must have been instantiated before being written to
        assert this.outVarMap.size() == getNumReplicas();

        StmtBuilder writerBuilder = info.getBuilder(writeHost);

        if (this.replicas.realReplicas.contains(writeHost)) {
          writerBuilder.assign(this.outVarMap.get(writeHost), (ExpressionNode)val);

        } else {
          for (Host realHost : this.replicas.realReplicas) {
            StmtBuilder builder = info.getBuilder(realHost);

            writerBuilder.send(realHost, (ExpressionNode)val);
            builder.recv(writeHost, this.outVarMap.get(realHost));
          }
        }

        /*
        for (Host hashHost : this.replicas.hashReplicas) {
          StmtBuilder builder = info.getBuilder(hashHost);

          writerBuilder.send(hashHost, (ExpressionNode)val);
          builder.recv(writeHost, this.outVarMap.get(hashHost));
        }
        */
      }
    }

    @Override
    public void instantiate(PdgNode<ImpAstNode> node, ProtocolInstantiationInfo<ImpAstNode> info) {
      if (node.isStorageNode()) {
        for (Host realHost : this.replicas.realReplicas) {
          Variable hostStorageVar =
              instantiateStorageNode(realHost, (PdgStorageNode<ImpAstNode>)node, info);
          this.outVarMap.put(realHost, hostStorageVar);
        }

        for (Host hashHost : this.replicas.hashReplicas) {
          Variable hostStorageVar =
              instantiateStorageNode(hashHost, (PdgStorageNode<ImpAstNode>)node, info);
          this.outVarMap.put(hashHost, hostStorageVar);
        }

      } else if (node.isComputeNode()) {
        for (Host realHost : this.replicas.realReplicas) {
          Variable hostOutVar =
              instantiateComputeNode(realHost, (PdgComputeNode<ImpAstNode>)node, info);
          this.outVarMap.put(realHost, hostOutVar);
        }

        /*
        for (Host hashHost : this.replicas.hashReplicas) {
          Variable hostOutVar =
              instantiateComputeNode(hashHost, (PdgComputeNode<ImpAstNode>)node, info);
          this.outVarMap.put(hashHost, hostOutVar);
        }
        */

      } else if (node.isControlNode()) {
        instantiateControlNode(getHosts(), (PdgControlNode<ImpAstNode>)node, info);
      }
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

  static class MPC extends Cleartext implements Protocol<ImpAstNode> {
    private static final MPC rep = new MPC();
    private Set<Host> parties;
    private Host synthesizedHost;
    private Variable outVar;

    private MPC() {
      this.parties = new HashSet<Host>();
    }

    private MPC(Set<Host> ps) {
      this.parties = ps;
    }

    public static MPC getRepresentative() {
      return rep;
    }

    public Set<Host> getHosts() {
      Set<Host> hosts = new HashSet<>();
      hosts.addAll(this.parties);
      return hosts;
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
    public Set<Host> readFrom(PdgNode<ImpAstNode> node, Host readHost,
        ProtocolInstantiationInfo<ImpAstNode> info) {

      // this should not be read from until it has been instantiated!
      assert this.outVar != null;

      ExpressionBuilder e = new ExpressionBuilder();
      StmtBuilder builder = info.getBuilder(this.synthesizedHost);
      builder.send(readHost, e.var(this.outVar));

      Set<Host> hosts = new HashSet<>();
      hosts.add(this.synthesizedHost);
      return hosts;
    }

    @Override
    public Binding<ImpAstNode> readPostprocess(
        Map<Host,Binding<ImpAstNode>> hostBindings, Host host,
        ProtocolInstantiationInfo<ImpAstNode> info) {

      // because this is the Single protocol, there should only
      // have been one host that the node read from
      assert hostBindings.size() == 1;
      return hostBindings.get(this.synthesizedHost);
    }

    @Override
    public void writeTo(PdgNode<ImpAstNode> node, Host h, ImpAstNode val,
        ProtocolInstantiationInfo<ImpAstNode> info) {

      // MPC is only for computations, so it cannot be written to!
      // do nothing here.
    }

    @Override
    public void instantiate(PdgNode<ImpAstNode> node, ProtocolInstantiationInfo<ImpAstNode> info) {
      this.synthesizedHost = new Host(info.getFreshName(toString()));
      info.createProcess(this.synthesizedHost);
      this.outVar =
          instantiateComputeNode(this.synthesizedHost, (PdgComputeNode<ImpAstNode>)node, info);
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

    public Set<Host> getHosts() {
      Set<Host> hosts = new HashSet<>();
      hosts.add(this.prover);
      hosts.add(this.verifier);
      return hosts;
    }

    @Override
    public Set<Protocol<ImpAstNode>> createInstances(
        Set<Host> hostConfig,
        Map<PdgNode<ImpAstNode>, Protocol<ImpAstNode>> protocolMap,
        PdgNode<ImpAstNode> node) {

      if (hostConfig.size() < 2) {
        return new HashSet<>();
      }

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
    public Set<Host> readFrom(PdgNode<ImpAstNode> node, Host h,
        ProtocolInstantiationInfo<ImpAstNode> info) {

      // TODO: finish
      return new HashSet<>();
    }

    @Override
    public Binding<ImpAstNode> readPostprocess(
        Map<Host,Binding<ImpAstNode>> hostBindings, Host host,
        ProtocolInstantiationInfo<ImpAstNode> info) {

      // TODO: finish
      return null;
    }

    @Override
    public void writeTo(PdgNode<ImpAstNode> node, Host h, ImpAstNode val,
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
