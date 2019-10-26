package edu.cornell.cs.apl.viaduct.imp.protocols;

import edu.cornell.cs.apl.viaduct.Binding;
import edu.cornell.cs.apl.viaduct.imp.ast.ArrayDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ArrayIndexingNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.HostName;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ProcessName;
import edu.cornell.cs.apl.viaduct.imp.ast.ReadNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StatementNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;
import edu.cornell.cs.apl.viaduct.imp.ast.VariableDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.builders.ExpressionBuilder;
import edu.cornell.cs.apl.viaduct.imp.builders.StmtBuilder;
import edu.cornell.cs.apl.viaduct.imp.visitors.ReferenceVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.RenameVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.ReplaceVisitor;
import edu.cornell.cs.apl.viaduct.pdg.PdgComputeEdge;
import edu.cornell.cs.apl.viaduct.pdg.PdgComputeNode;
import edu.cornell.cs.apl.viaduct.pdg.PdgEdge;
import edu.cornell.cs.apl.viaduct.pdg.PdgNode;
import edu.cornell.cs.apl.viaduct.pdg.PdgQueryEdge;
import edu.cornell.cs.apl.viaduct.pdg.PdgReadEdge;
import edu.cornell.cs.apl.viaduct.pdg.PdgStorageNode;
import edu.cornell.cs.apl.viaduct.pdg.PdgWriteEdge;
import edu.cornell.cs.apl.viaduct.protocol.AbstractProtocol;
import edu.cornell.cs.apl.viaduct.protocol.Protocol;
import edu.cornell.cs.apl.viaduct.protocol.ProtocolInstantiationError;
import edu.cornell.cs.apl.viaduct.protocol.ProtocolInstantiationInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class Cleartext extends AbstractProtocol<ImpAstNode> {
  protected Cleartext(Set<HostName> hosts) {
    super(hosts);
  }

  protected Cleartext(HostName host) {
    super(host);
  }

  protected ExpressionNode getReadValue(
      PdgNode<ImpAstNode> node, List<Variable> readArgs, Variable outVar,
      ProtocolInstantiationInfo<ImpAstNode> info) {

    if (node.isStorageNode()) {
      StatementNode stmt = (StatementNode) node.getAstNode();
      if (stmt instanceof VariableDeclarationNode) { // variable read
        return ReadNode.builder().setReference(outVar).build();

      } else if (stmt instanceof ArrayDeclarationNode) { // array access
        Variable idx = readArgs.get(0);
        return ReadNode.builder()
            .setReference(
                ArrayIndexingNode.builder()
                    .setArray(outVar)
                    .setIndex(ReadNode.builder().setReference(idx).build())
                    .build())
            .build();

      } else {
        throw new ProtocolInstantiationError(
            "storage node not associated with var or array declaration");
      }
    } else {
      return ReadNode.builder().setReference(outVar).build();
    }
  }

  protected Binding<ImpAstNode> performRead(
      PdgNode<ImpAstNode> node,
      ProcessName readProcess,
      Binding<ImpAstNode> readLabel,
      ProcessName outProcess,
      Binding<ImpAstNode> outVar,
      List<ImpAstNode> args,
      ProtocolInstantiationInfo<ImpAstNode> info) {

    StmtBuilder builder = info.getBuilder(outProcess);
    StmtBuilder readBuilder = info.getBuilder(readProcess);
    List<Variable> readArgs = new ArrayList<>();
    for (ImpAstNode arg : args) {
      readBuilder.send(outProcess, (ExpressionNode) arg);

      Variable readArgVar = info.getFreshVar("arg");
      readArgs.add(readArgVar);
      builder.recv(readProcess, readArgVar);
    }

    ExpressionNode readVal = getReadValue(node, readArgs, (Variable) outVar, info);
    builder.send(readProcess, readVal);

    Variable readVar = info.getFreshVar(readLabel);
    readBuilder.recv(outProcess, readVar);

    return readVar;
  }

  protected void performWrite(
      PdgNode<ImpAstNode> node,
      ProcessName writeProcess,
      ProcessName inProcess,
      Binding<ImpAstNode> storageVar,
      List<ImpAstNode> args,
      ProtocolInstantiationInfo<ImpAstNode> info) {

    ExpressionBuilder e = new ExpressionBuilder();
    StmtBuilder builder = info.getBuilder(inProcess);
    StmtBuilder writerBuilder = info.getBuilder(writeProcess);
    StatementNode stmt = (StatementNode) node.getAstNode();

    if (stmt instanceof VariableDeclarationNode) {
      assert args.size() == 1;

      ExpressionNode val = (ExpressionNode) args.get(0);
      writerBuilder.send(inProcess, val);
      Variable valVar = info.getFreshVar(String.format("%s_val", storageVar));
      builder.recv(writeProcess, valVar);
      builder.assign((Variable) storageVar, e.var(valVar));

    } else if (stmt instanceof ArrayDeclarationNode) {
      assert args.size() == 2;

      ExpressionNode idx = (ExpressionNode) args.get(0);
      ExpressionNode val = (ExpressionNode) args.get(1);
      writerBuilder.send(inProcess, idx);
      writerBuilder.send(inProcess, val);

      Variable arrayVar = ((ArrayDeclarationNode) stmt).getVariable();
      Variable idxVar = info.getFreshVar(String.format("%s_idx", arrayVar));
      Variable valVar = info.getFreshVar(String.format("%s_val", arrayVar));
      builder.recv(writeProcess, idxVar);
      builder.recv(writeProcess, valVar);
      builder.assign((Variable) storageVar, e.var(idxVar), e.var(valVar));

    } else {
      throw new ProtocolInstantiationError(
          "storage node not associated with var or array declaration");
    }
  }

  protected Map<Variable, Variable> performComputeReads(
      ProcessName process, PdgNode<ImpAstNode> node, ProtocolInstantiationInfo<ImpAstNode> info) {
    // read from other compute nodes
    Map<Variable, Variable> computeRenameMap = new HashMap<>();
    for (PdgReadEdge<ImpAstNode> readEdge : node.getReadEdges()) {
      if (readEdge.isComputeEdge()) {
        PdgComputeEdge<ImpAstNode> computeEdge = (PdgComputeEdge<ImpAstNode>) readEdge;
        Variable readLabel = (Variable) computeEdge.getBinding();
        PdgNode<ImpAstNode> readNode = computeEdge.getSource();
        Protocol<ImpAstNode> readProto = info.getProtocol(readNode);
        Variable readVar =
            (Variable) readProto.readFrom(
                readNode, node, process, readLabel, new ArrayList<>(), info);
        computeRenameMap.put(readLabel, readVar);
      }
    }

    return computeRenameMap;
  }

  protected Variable instantiateStorageNode(
      ProcessName process,
      PdgStorageNode<ImpAstNode> node,
      ProtocolInstantiationInfo<ImpAstNode> info)
  {

    // declare new variable
    ImpAstNode astNode = node.getAstNode();
    StmtBuilder builder = info.getBuilder(process);
    Variable newVar = null;

    if (astNode instanceof VariableDeclarationNode) {
      VariableDeclarationNode varDecl = (VariableDeclarationNode) astNode;
      newVar = varDecl.getVariable();
      builder.varDecl(newVar, varDecl.getType(), varDecl.getLabel());

    } else if (astNode instanceof ArrayDeclarationNode) {
      Map<Variable, Variable> computeRenameMap = performComputeReads(process, node, info);
      RenameVisitor computeRenamer = new RenameVisitor(computeRenameMap);
      astNode = computeRenamer.run(astNode);

      ArrayDeclarationNode arrayDecl = (ArrayDeclarationNode) astNode;
      newVar = arrayDecl.getVariable();
      builder.arrayDecl(
          newVar, arrayDecl.getLength(), arrayDecl.getElementType(), arrayDecl.getLabel());

    } else {
      throw new ProtocolInstantiationError(
          "storage node not associated with var or array declaration");
    }

    return newVar;
  }

  protected Variable instantiateComputeNode(
      ProcessName process,
      PdgComputeNode<ImpAstNode> node,
      ProtocolInstantiationInfo<ImpAstNode> info)
  {
    // read from other compute nodes
    Map<Variable, Variable> computeRenameMap = performComputeReads(process, node, info);
    RenameVisitor computeRenamer = new RenameVisitor(computeRenameMap);

    // perform queries (read from storage nodes)
    Map<ExpressionNode, ExpressionNode> queryRenameMap = new HashMap<>();
    for (PdgReadEdge<ImpAstNode> readEdge : node.getReadEdges()) {
      if (readEdge.isQueryEdge()) {
        PdgQueryEdge<ImpAstNode> queryEdge = (PdgQueryEdge<ImpAstNode>) readEdge;

        ReadNode queryRead = (ReadNode) queryEdge.getQuery();
        queryRead = (ReadNode) computeRenamer.run(queryRead);
        List<ImpAstNode> renamedArgs =
            queryRead
                .getReference()
                .accept(
                    new ReferenceVisitor<List<ImpAstNode>>() {
                      @Override
                      public List<ImpAstNode> visit(Variable var) {
                        return new ArrayList<>();
                      }

                      @Override
                      public List<ImpAstNode> visit(ArrayIndexingNode arrayIndex) {
                        ExpressionNode renamedInd = arrayIndex.getIndex();
                        List<ImpAstNode> arrayArgs = new ArrayList<>();
                        arrayArgs.add(renamedInd);
                        return arrayArgs;
                      }
                    });

        PdgNode<ImpAstNode> queryNode = queryEdge.getSource();
        Protocol<ImpAstNode> queryProto = info.getProtocol(queryNode);
        Variable readVar =
            (Variable)
                queryProto.readFrom(
                    queryEdge.getSource(),
                    node,
                    process,
                    Variable.create(node.getId()),
                    renamedArgs,
                    info);

        queryRenameMap.put(queryRead, ReadNode.builder().setReference(readVar).build());
      }
    }

    ReplaceVisitor queryRenamer = new ReplaceVisitor(queryRenameMap, new HashMap<>());

    // perform computation
    ImpAstNode astNode = node.getAstNode();
    astNode = computeRenamer.run(astNode);
    astNode = queryRenamer.run(astNode);

    StmtBuilder builder = info.getBuilder(process);
    Variable outVar = info.getFreshVar(node.getId());

    if (astNode instanceof ExpressionNode) {
      builder.let(outVar, (ExpressionNode) astNode);

    }
    // there's no need to let-bind assignments since the
    // the protocol for the storage node being written to will do it
    // in its writeTo function
    /*
    } else if (astNode instanceof AssignNode) {
      AssignNode assignNode = (AssignNode) astNode;
      builder.let(outVar, assignNode.getRhs());
    }
    */

    // write to storage nodes
    for (PdgEdge<ImpAstNode> outEdge : node.getOutInfoEdges()) {
      // only write to variables, since if computations read from
      // the output of this node then it will call readFrom() anyway
      if (outEdge instanceof PdgWriteEdge<?>) {
        PdgWriteEdge<ImpAstNode> writeEdge = (PdgWriteEdge<ImpAstNode>) outEdge;

        List<ImpAstNode> renamedArgs = new ArrayList<>();
        for (ImpAstNode arg : writeEdge.getCommandArgs()) {
          ImpAstNode renamedArg = computeRenamer.run(arg);
          renamedArg = queryRenamer.run(renamedArg);
          renamedArgs.add(renamedArg);
        }

        PdgStorageNode<ImpAstNode> outNode = (PdgStorageNode<ImpAstNode>) outEdge.getTarget();
        Protocol<ImpAstNode> outProto = info.getProtocol(outNode);
        outProto.writeTo(outNode, node, process, renamedArgs, info);
      }
    }

    return outVar;
  }
}
