package edu.cornell.cs.apl.viaduct.backend.mamba;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaBlockNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaStatementNode;
import edu.cornell.cs.apl.viaduct.imp.ast.AssignNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BlockNode;
import edu.cornell.cs.apl.viaduct.imp.ast.IfNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LoopNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ProcessDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ProcessName;
import edu.cornell.cs.apl.viaduct.imp.ast.ProgramNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReceiveNode;
import edu.cornell.cs.apl.viaduct.imp.ast.SendNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StatementNode;
import edu.cornell.cs.apl.viaduct.imp.parsing.Printer;
import edu.cornell.cs.apl.viaduct.imp.parsing.SourcePosition;

import java.util.ArrayList;
import java.util.List;

/** merge public and secret mamba processes together. */
public final class MambaPublicSecretProcessMerger {
  /** run merger. */
  public static StatementNode run(
      ProgramNode program,
      ProcessName publicProcessName,
      ProcessName secretProcessName)
  {
    return
        (new MambaPublicSecretProcessMerger())
        .mergePublicSecret(program, publicProcessName, secretProcessName);
  }

  /** merge mamba public and secret processes from a generated program. */
  private StatementNode mergePublicSecret(
      ProgramNode program,
      ProcessName publicProcessName,
      ProcessName secretProcessName)
  {
    ImmutableMap<ProcessName, ProcessDeclarationNode> processes = program.processes();

    ProcessDeclarationNode publicProcess = processes.get(publicProcessName);
    ImmutableList<StatementNode> publicStmtList = publicProcess.getBody().getStatements();

    ProcessDeclarationNode secretProcess = processes.get(secretProcessName);
    ImmutableList<StatementNode> secretStmtList = secretProcess.getBody().getStatements();

    Iterable<StatementNode> mambaStmtList =
        mergePublicSecretProcesses(
            publicProcessName, publicStmtList,
            secretProcessName, secretStmtList);

    return BlockNode.builder().addAll(mambaStmtList).build();
  }

  private Iterable<StatementNode> mergePublicSecretProcesses(
      ProcessName publicProcessName,
      ImmutableList<StatementNode> publicProcess,
      ProcessName secretProcessName,
      ImmutableList<StatementNode> secretProcess)
  {
    List<StatementNode> mambaStmtList = new ArrayList<>();
    int publicIndex = 0;
    int secretIndex = 0;
    int publicProcessSize = publicProcess.size();
    int secretProcessSize = secretProcess.size();

    while (publicIndex < publicProcessSize || secretIndex < secretProcessSize) {
      StatementNode publicStmt =
          publicIndex < publicProcessSize ? publicProcess.get(publicIndex) : null;
      StatementNode secretStmt =
          secretIndex < secretProcessSize ? secretProcess.get(secretIndex) : null;

      SourcePosition publicStmtLocation =
          publicStmt != null ? publicStmt.getSourceLocation().getStart() : null;
      SourcePosition secretStmtLocation =
          secretStmt != null ? secretStmt.getSourceLocation().getStart() : null;

      int compareLocations =
          publicStmtLocation != null && secretStmtLocation != null
          ? publicStmtLocation.compareTo(secretStmtLocation) : 0;

      // public stmt is ordered before secret stmt
      if (compareLocations < 0 || secretIndex >= secretProcessSize) {
        mambaStmtList.add(publicStmt);
        publicIndex++;

      // secret stmt is ordered before public stmt
      } else if (compareLocations > 0 || publicIndex >= publicProcessSize) {
        mambaStmtList.add(secretStmt);
        secretIndex++;

      // statements came from the same source location;
      } else {
        // public recv, secret send (add reveal node here!)
        if (publicStmt instanceof ReceiveNode && secretStmt instanceof SendNode) {
          ReceiveNode publicRecv = (ReceiveNode) publicStmt;
          SendNode secretSend = (SendNode) secretStmt;

          if (publicRecv.getSender().equals(secretProcessName)
              && secretSend.getRecipient().equals(publicProcessName))
          {
            StatementNode newStmt =
                AssignNode.builder()
                .setLhs(publicRecv.getVariable())
                .setRhs(secretSend.getSentExpression())
                .build();

            mambaStmtList.add(newStmt);

          } else {
            // order arbitrarily
            mambaStmtList.add(publicRecv);
            mambaStmtList.add(secretSend);
          }

        // public send, secret recv
        } else if (publicStmt instanceof SendNode && secretStmt instanceof ReceiveNode) {
          SendNode publicSend = (SendNode) publicStmt;
          ReceiveNode secretRecv = (ReceiveNode) secretStmt;

          if (secretRecv.getSender().equals(publicProcessName)
              && publicSend.getRecipient().equals(secretProcessName))
          {
            StatementNode newStmt =
                AssignNode.builder()
                .setLhs(secretRecv.getVariable())
                .setRhs(publicSend.getSentExpression())
                .build();

            mambaStmtList.add(newStmt);

          } else {
            // order arbitrarily
            mambaStmtList.add(secretRecv);
            mambaStmtList.add(publicSend);
          }

        // conditional
        } else if (publicStmt instanceof IfNode && secretStmt instanceof IfNode) {
          IfNode publicIf = (IfNode) publicStmt;
          IfNode secretIf = (IfNode) secretStmt;

          Iterable<StatementNode> newThenBranch =
              mergePublicSecretProcesses(
                  publicProcessName, publicIf.getThenBranch().getStatements(),
                  secretProcessName, secretIf.getElseBranch().getStatements());

          Iterable<StatementNode> newElseBranch =
              mergePublicSecretProcesses(
                  publicProcessName, publicIf.getElseBranch().getStatements(),
                  secretProcessName, secretIf.getElseBranch().getStatements());

          IfNode newIf =
              IfNode.builder()
              .setGuard(publicIf.getGuard())
              .setThenBranch(BlockNode.builder().addAll(newThenBranch).build())
              .setElseBranch(BlockNode.builder().addAll(newElseBranch).build())
              .build();

          mambaStmtList.add(newIf);

        // loop
        } else if (publicStmt instanceof LoopNode && secretStmt instanceof LoopNode) {
          LoopNode publicLoop = (LoopNode) publicStmt;
          LoopNode secretLoop = (LoopNode) secretStmt;

          Iterable<StatementNode> newBody =
              mergePublicSecretProcesses(
                  publicProcessName, publicLoop.getBody().getStatements(),
                  secretProcessName, secretLoop.getBody().getStatements());

          LoopNode newLoop =
              LoopNode.builder()
              .setBody(BlockNode.builder().addAll(newBody).build())
              .build();
          mambaStmtList.add(newLoop);

        } else {
          // TODO: fix error to be more informative
          System.out.println(Printer.run(publicStmt));
          System.out.println(Printer.run(secretStmt));
          throw new Error("merge error!");
        }

        publicIndex++;
        secretIndex++;
      }
    }

    return BlockNode.builder().addAll(mambaStmtList).build();
  }
}
