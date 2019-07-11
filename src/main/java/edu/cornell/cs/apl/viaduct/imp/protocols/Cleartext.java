package edu.cornell.cs.apl.viaduct.imp.protocols;

import edu.cornell.cs.apl.viaduct.Binding;
import edu.cornell.cs.apl.viaduct.imp.ast.ArrayDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ArrayIndex;
import edu.cornell.cs.apl.viaduct.imp.ast.AssignNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BreakNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Host;
import edu.cornell.cs.apl.viaduct.imp.ast.IfNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LoopNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ProcessName;
import edu.cornell.cs.apl.viaduct.imp.ast.ReadNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StmtNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;
import edu.cornell.cs.apl.viaduct.imp.ast.VariableDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.builders.ExpressionBuilder;
import edu.cornell.cs.apl.viaduct.imp.builders.StmtBuilder;
import edu.cornell.cs.apl.viaduct.imp.visitors.ReferenceVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.RenameVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.ReplaceVisitor;
import edu.cornell.cs.apl.viaduct.pdg.PdgComputeEdge;
import edu.cornell.cs.apl.viaduct.pdg.PdgComputeNode;
import edu.cornell.cs.apl.viaduct.pdg.PdgControlNode;
import edu.cornell.cs.apl.viaduct.pdg.PdgEdge;
import edu.cornell.cs.apl.viaduct.pdg.PdgInfoEdge;
import edu.cornell.cs.apl.viaduct.pdg.PdgNode;
import edu.cornell.cs.apl.viaduct.pdg.PdgQueryEdge;
import edu.cornell.cs.apl.viaduct.pdg.PdgReadEdge;
import edu.cornell.cs.apl.viaduct.pdg.PdgStorageNode;
import edu.cornell.cs.apl.viaduct.pdg.PdgWriteEdge;
import edu.cornell.cs.apl.viaduct.protocol.Protocol;
import edu.cornell.cs.apl.viaduct.protocol.ProtocolInstantiationException;
import edu.cornell.cs.apl.viaduct.protocol.ProtocolInstantiationInfo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class Cleartext {
  protected ExpressionNode getReadValue(
      PdgNode<ImpAstNode> node, List<Variable> readArgs, Variable outVar) {

    if (node.isStorageNode()) {
      StmtNode stmt = (StmtNode)node.getAstNode();
      if (stmt instanceof VariableDeclarationNode) { // variable read
        return new ReadNode(outVar);

      } else if (stmt instanceof ArrayDeclarationNode) { // array access
        Variable idx = readArgs.get(0);
        return new ReadNode(new ArrayIndex(outVar, new ReadNode(idx)));

      } else {
        throw new ProtocolInstantiationException(
            "storage node not associated with var or array declaration");
      }
    } else {
      return new ReadNode(outVar);
    }
  }

  protected Binding<ImpAstNode> performRead(
      PdgNode<ImpAstNode> node,
      Binding<ImpAstNode> readLabel,
      Host host,
      List<ImpAstNode> args,
      ProtocolInstantiationInfo<ImpAstNode> info) {

    StmtBuilder builder = info.getBuilder(host);
    Protocol<ImpAstNode> readNodeProto = info.getProtocol(node);
    Set<Host> readHosts = readNodeProto.readFrom(node, host, args, info);

    Map<Host, Binding<ImpAstNode>> hostBindings = new HashMap<>();
    for (Host readHost : readHosts) {
      ProcessName readHostProc = new ProcessName(readHost);

      Variable readVar = info.getFreshVar(readLabel);
      builder.recv(readHostProc, readVar);
      hostBindings.put(readHost, readVar);
    }

    return readNodeProto.readPostprocess(hostBindings, host, info);
  }

  protected Map<Variable,Variable> performComputeReads(
      Host host, PdgNode<ImpAstNode> node,
      ProtocolInstantiationInfo<ImpAstNode> info)
  {
    // read from other compute nodes
    Map<Variable, Variable> computeRenameMap = new HashMap<>();
    for (PdgReadEdge<ImpAstNode> readEdge : node.getReadEdges()) {
      if (readEdge.isComputeEdge()) {
        PdgComputeEdge<ImpAstNode> computeEdge = (PdgComputeEdge<ImpAstNode>)readEdge;
        Variable edgeBinding = (Variable)computeEdge.getBinding();
        Variable readVar =
            (Variable) performRead(computeEdge.getSource(), edgeBinding,
                host, new ArrayList<>(), info);
        computeRenameMap.put(edgeBinding, readVar);
      }
    }

    return computeRenameMap;
  }

  protected Variable instantiateStorageNode(
      Host host, PdgStorageNode<ImpAstNode> node, ProtocolInstantiationInfo<ImpAstNode> info) {

    // declare new variable
    ImpAstNode astNode = node.getAstNode();
    StmtBuilder builder = info.getBuilder(host);
    Variable newVar = null;

    if (astNode instanceof VariableDeclarationNode) {
      VariableDeclarationNode varDecl = (VariableDeclarationNode)astNode;
      newVar = varDecl.getVariable();
      builder.varDecl(newVar, varDecl.getType(), varDecl.getLabel());

    } else if (astNode instanceof ArrayDeclarationNode) {
      Map<Variable,Variable> computeRenameMap = performComputeReads(host, node, info);
      RenameVisitor computeRenamer = new RenameVisitor(computeRenameMap);
      astNode = computeRenamer.run(astNode);

      ArrayDeclarationNode arrayDecl = (ArrayDeclarationNode)astNode;
      newVar = arrayDecl.getVariable();
      builder.arrayDecl(newVar, arrayDecl.getLength(), arrayDecl.getType(), arrayDecl.getLabel());

    } else {
      throw new ProtocolInstantiationException(
          "storage node not associated with var or array declaration");
    }

    return newVar;
  }


  protected Variable instantiateComputeNode(
      Host host, PdgComputeNode<ImpAstNode> node,
      ProtocolInstantiationInfo<ImpAstNode> info)
  {
    // read from other compute nodes
    Map<Variable,Variable> computeRenameMap = performComputeReads(host, node, info);
    RenameVisitor computeRenamer = new RenameVisitor(computeRenameMap);

    // perform queries (read from storage nodes)
    Map<ExpressionNode, ExpressionNode> queryRenameMap = new HashMap<>();
    for (PdgReadEdge<ImpAstNode> readEdge : node.getReadEdges()) {
      if (readEdge.isQueryEdge()) {
        PdgQueryEdge<ImpAstNode> queryEdge = (PdgQueryEdge<ImpAstNode>)readEdge;

        ReadNode queryRead = (ReadNode)queryEdge.getQuery();
        queryRead = (ReadNode)computeRenamer.run(queryRead);
        List<ImpAstNode> renamedArgs = queryRead.getReference().accept(
            new ReferenceVisitor<List<ImpAstNode>>() {
              public List<ImpAstNode> visit(Variable var) {
                return new ArrayList<>();
              }

              public List<ImpAstNode> visit(ArrayIndex arrayIndex) {
                ExpressionNode renamedInd = arrayIndex.getIndex();
                List<ImpAstNode> arrayArgs = new ArrayList<>();
                arrayArgs.add(renamedInd);
                return arrayArgs;
              }
            });

        Variable readVar =
            (Variable) performRead(queryEdge.getSource(), new Variable(node.getId()),
                host, renamedArgs, info);

        queryRenameMap.put(queryRead, new ReadNode(readVar));
      }
    }

    ReplaceVisitor queryRenamer = new ReplaceVisitor(queryRenameMap, new HashMap<>());

    // perform computation
    ImpAstNode astNode = node.getAstNode();
    astNode = computeRenamer.run(astNode);
    astNode = queryRenamer.run(astNode);

    StmtBuilder builder = info.getBuilder(host);
    Variable outVar = info.getFreshVar(node.getId());

    if (astNode instanceof ExpressionNode) {
      builder.let(outVar, (ExpressionNode) astNode);

    } else if (astNode instanceof AssignNode) {
      AssignNode assignNode = (AssignNode)astNode;
      builder.let(outVar, assignNode.getRhs());
    }

    // write to storage nodes
    for (PdgEdge<ImpAstNode> outEdge : node.getOutInfoEdges()) {
      // only write to variables, since if computations read from
      // the output of this node then it will call readFrom() anyway
      if (outEdge instanceof PdgWriteEdge<?>) {
        PdgWriteEdge<ImpAstNode> writeEdge = (PdgWriteEdge<ImpAstNode>)outEdge;

        List<ImpAstNode> renamedArgs = new ArrayList<>();
        for (ImpAstNode arg : writeEdge.getCommandArgs()) {
          ImpAstNode renamedArg = computeRenamer.run(arg);
          renamedArg = queryRenamer.run(renamedArg);
          renamedArgs.add(renamedArg);
        }

        PdgStorageNode<ImpAstNode> outNode = (PdgStorageNode<ImpAstNode>) outEdge.getTarget();
        Protocol<ImpAstNode> outProto = info.getProtocol(outNode);
        outProto.writeTo(outNode, host, renamedArgs, info);
      }
    }

    return outVar;
  }

  void instantiateControlNode(
      Set<Host> hosts,
      PdgControlNode<ImpAstNode> node,
      ProtocolInstantiationInfo<ImpAstNode> info) {

    ImpAstNode astNode = node.getAstNode();

    // TODO: this only works for single-level breaks for now
    if (astNode instanceof BreakNode) {
      Set<Host> breakHosts = info.getCurrentLoopControlContext();
      for (Host breakHost : breakHosts) {
        StmtBuilder breakHostBuilder = info.getBuilder(breakHost);
        breakHostBuilder.loopBreak();
      }

      return;
    }

    List<PdgReadEdge<ImpAstNode>> infoEdges = new ArrayList<>(node.getReadEdges());

    // create control structure in all nodes that have a read channel from the control node
    // TODO: this should really compute a transitive closure
    Set<Host> controlStructureHosts = new HashSet<>(hosts);
    for (PdgInfoEdge<ImpAstNode> infoEdge : node.getOutInfoEdges()) {
      controlStructureHosts.addAll(info.getProtocol(infoEdge.getTarget()).getHosts());
    }

    info.pushControlContext(controlStructureHosts);

    if (astNode instanceof IfNode) {
      // conditional node should only have one read input (result of the guard)
      assert infoEdges.size() == 1;

      IfNode ifNode = (IfNode)astNode;
      for (Host controlStructureHost : controlStructureHosts) {
        StmtBuilder controlStructureBuilder = info.getBuilder(controlStructureHost);
        Map<Variable,Variable> guardRenameMap =
            performComputeReads(controlStructureHost, node, info);
        RenameVisitor guardRenamer = new RenameVisitor(guardRenameMap);
        IfNode newIfNode = (IfNode)guardRenamer.run(ifNode);
        controlStructureBuilder.pushIf(newIfNode.getGuard());
      }

    } else if (astNode instanceof LoopNode) {
      info.pushLoopControlContext(controlStructureHosts);
      for (Host controlStructureHost : controlStructureHosts) {
        StmtBuilder controlStructureBuilder = info.getBuilder(controlStructureHost);
        controlStructureBuilder.pushLoop();
      }

    } else {
      throw new ProtocolInstantiationException(
          "control node not associated with control structure");
    }
  }
}
