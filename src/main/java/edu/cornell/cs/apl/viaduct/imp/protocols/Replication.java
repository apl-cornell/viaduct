package edu.cornell.cs.apl.viaduct.imp.protocols;

import edu.cornell.cs.apl.viaduct.Binding;
import edu.cornell.cs.apl.viaduct.PdgComputeNode;
import edu.cornell.cs.apl.viaduct.PdgControlNode;
import edu.cornell.cs.apl.viaduct.PdgNode;
import edu.cornell.cs.apl.viaduct.PdgStorageNode;
import edu.cornell.cs.apl.viaduct.Protocol;
import edu.cornell.cs.apl.viaduct.ProtocolInstantiationInfo;
import edu.cornell.cs.apl.viaduct.imp.ast.ExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Host;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ProcessName;
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;
import edu.cornell.cs.apl.viaduct.imp.builders.ExpressionBuilder;
import edu.cornell.cs.apl.viaduct.imp.builders.StmtBuilder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** replication protocol. */
public class Replication extends Cleartext implements Protocol<ImpAstNode> {
  private ReplicaSets replicas;
  private Map<Host, Variable> outVarMap;

  public Replication(Set<Host> real, Set<Host> hash) {
    this.replicas = new ReplicaSets(real, hash);
    this.outVarMap = new HashMap<>();
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

  @Override
  public Set<Host> readFrom(
      PdgNode<ImpAstNode> node, Host readHost, ProtocolInstantiationInfo<ImpAstNode> info) {

    // should not be read from until it has been instantiated
    assert this.outVarMap.size() == getNumReplicas();

    ExpressionBuilder e = new ExpressionBuilder();
    Set<Host> hosts = new HashSet<>();

    if (this.replicas.realReplicas.contains(readHost)) {
      StmtBuilder builder = info.getBuilder(readHost);
      builder.send(new ProcessName(readHost), e.var(this.outVarMap.get(readHost)));
      hosts.add(readHost);

    } else {
      for (Host realHost : this.replicas.realReplicas) {
        StmtBuilder builder = info.getBuilder(realHost);
        builder.send(new ProcessName(readHost), e.var(this.outVarMap.get(realHost)));
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
      Map<Host, Binding<ImpAstNode>> hostBindings,
      Host host,
      ProtocolInstantiationInfo<ImpAstNode> info) {

    // TODO: fix this
    Host h = (Host) hostBindings.keySet().toArray()[0];
    return hostBindings.get(h);
  }

  @Override
  public void writeTo(
      PdgNode<ImpAstNode> node,
      Host writeHost,
      ImpAstNode val,
      ProtocolInstantiationInfo<ImpAstNode> info) {

    if (node.isStorageNode()) {
      // node must have been instantiated before being written to
      assert this.outVarMap.size() == getNumReplicas();

      StmtBuilder writerBuilder = info.getBuilder(writeHost);

      if (this.replicas.realReplicas.contains(writeHost)) {
        writerBuilder.assign(this.outVarMap.get(writeHost), (ExpressionNode) val);

      } else {
        for (Host realHost : this.replicas.realReplicas) {
          StmtBuilder builder = info.getBuilder(realHost);

          writerBuilder.send(new ProcessName(realHost), (ExpressionNode) val);
          builder.recv(new ProcessName(writeHost), this.outVarMap.get(realHost));
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
            instantiateStorageNode(realHost, (PdgStorageNode<ImpAstNode>) node, info);
        this.outVarMap.put(realHost, hostStorageVar);
      }

      for (Host hashHost : this.replicas.hashReplicas) {
        Variable hostStorageVar =
            instantiateStorageNode(hashHost, (PdgStorageNode<ImpAstNode>) node, info);
        this.outVarMap.put(hashHost, hostStorageVar);
      }

    } else if (node.isComputeNode()) {
      for (Host realHost : this.replicas.realReplicas) {
        Variable hostOutVar =
            instantiateComputeNode(realHost, (PdgComputeNode<ImpAstNode>) node, info);
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
      instantiateControlNode(getHosts(), (PdgControlNode<ImpAstNode>) node, info);
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

  @Override
  public Set<Host> getHosts() {
    Set<Host> hosts = new HashSet<>();
    hosts.addAll(this.replicas.realReplicas);
    hosts.addAll(this.replicas.hashReplicas);
    return hosts;
  }

  private static class ReplicaSets {
    private Set<Host> realReplicas;
    private Set<Host> hashReplicas;

    private ReplicaSets(Set<Host> real, Set<Host> hash) {
      this.realReplicas = real;
      this.hashReplicas = hash;
    }
  }
}
