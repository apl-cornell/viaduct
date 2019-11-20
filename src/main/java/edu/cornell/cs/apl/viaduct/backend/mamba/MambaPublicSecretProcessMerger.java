package edu.cornell.cs.apl.viaduct.backend.mamba;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaAssignNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaBlockNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaExpressionNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaIfNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaIntLiteralNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaReadNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaRegIntDeclarationNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaRevealNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaSecurityType;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaStatementNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaVariable;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaWhileNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.visitors.ImpToMambaTranslator;
import edu.cornell.cs.apl.viaduct.backend.mamba.visitors.MambaInliner;
import edu.cornell.cs.apl.viaduct.imp.ast.BreakNode;
import edu.cornell.cs.apl.viaduct.imp.ast.CommunicationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ControlNode;
import edu.cornell.cs.apl.viaduct.imp.ast.IfNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LoopNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ProcessDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ProcessName;
import edu.cornell.cs.apl.viaduct.imp.ast.ProgramNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReceiveNode;
import edu.cornell.cs.apl.viaduct.imp.ast.SendNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StatementNode;
import edu.cornell.cs.apl.viaduct.util.AbstractLineNumber;
import edu.cornell.cs.apl.viaduct.util.AbstractLineNumber.RelativePosition;
import io.vavr.collection.HashMap;
import io.vavr.collection.Map;

import java.util.ArrayList;
import java.util.List;

/** merge public and secret mamba processes together. */
public final class MambaPublicSecretProcessMerger {
  private MambaPublicSecretProcessMerger() {}

  /** run merger. */
  public static MambaStatementNode run(
      ProgramNode program,
      ImmutableMap<ProcessName, Integer> hostNameMap,
      ProcessName publicProcessName,
      ProcessName secretProcessName)
  {
    return
        (new MambaPublicSecretProcessMerger())
            .mergePublicSecret(
                program, hostNameMap, HashMap.empty(),
                publicProcessName, secretProcessName);
  }

  /** merge mamba public and secret processes from a generated program. */
  private MambaStatementNode mergePublicSecret(
      ProgramNode program,
      ImmutableMap<ProcessName, Integer> hostNameMap,
      Map<MambaVariable, MambaExpressionNode> inlineMap,
      ProcessName publicProcessName,
      ProcessName secretProcessName)
  {
    ImmutableMap<ProcessName, ProcessDeclarationNode> processes = program.processes();

    ProcessDeclarationNode publicProcess = processes.get(publicProcessName);
    ImmutableList<StatementNode> publicStmtList = publicProcess.getBody().getStatements();

    ProcessDeclarationNode secretProcess = processes.get(secretProcessName);
    ImmutableList<StatementNode> secretStmtList = secretProcess.getBody().getStatements();

    Iterable<MambaStatementNode> mambaStmtList =
        mergePublicSecretProcesses(
            inlineMap, hostNameMap,
            null,
            publicProcessName, publicStmtList,
            secretProcessName, secretStmtList);

    return MambaBlockNode.builder().addAll(mambaStmtList).build();
  }

  private Iterable<MambaStatementNode> mergePublicSecretProcesses(
      Map<MambaVariable, MambaExpressionNode> inlineMap,
      ImmutableMap<ProcessName, Integer> hostNameMap,
      MambaVariable loopVar,
      ProcessName publicProcessName,
      ImmutableList<StatementNode> publicProcess,
      ProcessName secretProcessName,
      ImmutableList<StatementNode> secretProcess)
  {
    List<MambaStatementNode> mambaStmtList = new ArrayList<>();

    int publicIndex = 0;
    int secretIndex = 0;
    int publicProcessSize = publicProcess.size();
    int secretProcessSize = secretProcess.size();

    while (publicIndex < publicProcessSize || secretIndex < secretProcessSize) {
      StatementNode publicStmt =
          publicIndex < publicProcessSize ? publicProcess.get(publicIndex) : null;
      StatementNode secretStmt =
          secretIndex < secretProcessSize ? secretProcess.get(secretIndex) : null;

      AbstractLineNumber publicStmtLocation =
          publicStmt != null ? publicStmt.getLogicalPosition() : null;
      AbstractLineNumber secretStmtLocation =
          secretStmt != null ? secretStmt.getLogicalPosition() : null;

      RelativePosition compareLocations =
          publicStmtLocation != null && secretStmtLocation != null
          ? publicStmtLocation.comparePositionTo(secretStmtLocation) : RelativePosition.UNORDERED;

      // public stmt is ordered before secret stmt
      if (compareLocations == RelativePosition.BEFORE || secretIndex >= secretProcessSize) {
        mambaStmtList.add(
            MambaInliner.run(
                inlineMap,
                ImpToMambaTranslator.run(false, hostNameMap, loopVar, publicStmt)));

        publicIndex++;

      // secret stmt is ordered before public stmt
      } else if (compareLocations == RelativePosition.AFTER || publicIndex >= publicProcessSize) {
        mambaStmtList.add(
            MambaInliner.run(
              inlineMap,
              ImpToMambaTranslator.run(true, hostNameMap, loopVar, secretStmt)));

        secretIndex++;

      // statements came from the same source location;
      } else if (compareLocations == RelativePosition.EQUAL) {
        // public recv, secret send (add reveal node here!)
        if (publicStmt instanceof ReceiveNode && secretStmt instanceof SendNode) {
          ReceiveNode publicRecv = (ReceiveNode) publicStmt;
          SendNode secretSend = (SendNode) secretStmt;

          if (publicRecv.getSender().equals(secretProcessName)
              && secretSend.getRecipient().equals(publicProcessName))
          {
            MambaVariable mambaVar =
                ImpToMambaTranslator.visitVariable(publicRecv.getVariable());

            MambaExpressionNode mambaRhs =
                MambaRevealNode.builder()
                .setRevealedExpr(
                      MambaInliner.run(
                          inlineMap,
                          ImpToMambaTranslator
                          .run(true, hostNameMap, secretSend.getSentExpression())))
                .build();

            inlineMap = inlineMap.put(mambaVar, mambaRhs);
            publicIndex++;
            secretIndex++;

          } else {
            if (publicRecv.getSender().equals(secretProcessName)) {
              mambaStmtList.add(
                  MambaInliner.run(
                      inlineMap,
                      ImpToMambaTranslator
                      .run(true, hostNameMap, loopVar, secretSend)));

              secretIndex++;

            } else {
              mambaStmtList.add(
                  MambaInliner.run(
                      inlineMap,
                      ImpToMambaTranslator
                      .run(false, hostNameMap, loopVar, publicRecv)));

              publicIndex++;
            }
          }

        // public send, secret recv
        } else if (publicStmt instanceof SendNode && secretStmt instanceof ReceiveNode) {
          SendNode publicSend = (SendNode) publicStmt;
          ReceiveNode secretRecv = (ReceiveNode) secretStmt;

          if (secretRecv.getSender().equals(publicProcessName)
              && publicSend.getRecipient().equals(secretProcessName))
          {
            MambaVariable mambaVar = ImpToMambaTranslator.visitVariable(secretRecv.getVariable());
            MambaExpressionNode mambaExpr =
                MambaInliner.run(
                    inlineMap,
                    ImpToMambaTranslator.run(true, hostNameMap, publicSend.getSentExpression()));

            inlineMap = inlineMap.put(mambaVar, mambaExpr);
            publicIndex++;
            secretIndex++;

          } else {
            if (secretRecv.getSender().equals(publicProcessName)) {
              mambaStmtList.add(
                  MambaInliner.run(inlineMap,
                      ImpToMambaTranslator.run(false, hostNameMap, loopVar, publicSend)));

              publicIndex++;

            } else {
              mambaStmtList.add(
                  MambaInliner.run(inlineMap,
                      ImpToMambaTranslator.run(true, hostNameMap, loopVar, secretRecv)));

              secretIndex++;
            }
          }

        // conditional
        } else if (publicStmt instanceof IfNode && secretStmt instanceof IfNode) {
          IfNode publicIf = (IfNode) publicStmt;
          IfNode secretIf = (IfNode) secretStmt;

          MambaExpressionNode newGuard =
              MambaInliner.run(
                inlineMap,
                ImpToMambaTranslator.run(false, hostNameMap, publicIf.getGuard()));

          Iterable<MambaStatementNode> newThenBranch =
              mergePublicSecretProcesses(
                  inlineMap, hostNameMap, loopVar,
                  publicProcessName, publicIf.getThenBranch().getStatements(),
                  secretProcessName, secretIf.getThenBranch().getStatements());

          Iterable<MambaStatementNode> newElseBranch =
              mergePublicSecretProcesses(
                  inlineMap, hostNameMap, loopVar,
                  publicProcessName, publicIf.getElseBranch().getStatements(),
                  secretProcessName, secretIf.getElseBranch().getStatements());

          MambaIfNode newIf =
              MambaIfNode.builder()
              .setGuard(newGuard)
              .setThenBranch(MambaBlockNode.builder().addAll(newThenBranch).build())
              .setElseBranch(MambaBlockNode.builder().addAll(newElseBranch).build())
              .build();

          mambaStmtList.add(MambaInliner.run(inlineMap, newIf));

          publicIndex++;
          secretIndex++;

        // loop
        } else if (publicStmt instanceof LoopNode && secretStmt instanceof LoopNode) {
          LoopNode publicLoop = (LoopNode) publicStmt;
          LoopNode secretLoop = (LoopNode) secretStmt;
          MambaVariable newLoopVar = ImpToMambaTranslator.getFreshLoopVariable();

          Iterable<MambaStatementNode> newBody =
              mergePublicSecretProcesses(
                  inlineMap, hostNameMap, newLoopVar,
                  publicProcessName, publicLoop.getBody().getStatements(),
                  secretProcessName, secretLoop.getBody().getStatements());

          mambaStmtList.add(
              MambaInliner.run(inlineMap,
                  MambaRegIntDeclarationNode.builder()
                  .setRegisterType(MambaSecurityType.CLEAR)
                  .setVariable(newLoopVar)
                  .build()));
          mambaStmtList.add(
              MambaInliner.run(inlineMap,
                  MambaAssignNode.builder()
                  .setVariable(newLoopVar)
                  .setRhs(
                      MambaIntLiteralNode.builder()
                      .setSecurityType(MambaSecurityType.CLEAR)
                      .setValue(1)
                      .build())
                  .build()));
          mambaStmtList.add(
              MambaInliner.run(inlineMap,
                  MambaWhileNode.builder()
                  .setGuard(MambaReadNode.create(newLoopVar))
                  .setBody(MambaBlockNode.create(newBody))
                  .build()));

          publicIndex++;
          secretIndex++;

        } else if (publicStmt instanceof BreakNode && secretStmt instanceof BreakNode) {
          mambaStmtList.add(
              MambaInliner.run(
                  inlineMap,
                  ImpToMambaTranslator.run(false, hostNameMap, loopVar, publicStmt)));

          publicIndex++;
          secretIndex++;

        } else if (publicStmt instanceof CommunicationNode && secretStmt instanceof ControlNode) {
          mambaStmtList.add(
              MambaInliner.run(inlineMap,
                  ImpToMambaTranslator.run(false, hostNameMap, loopVar, publicStmt)));

          publicIndex++;

        } else if (publicStmt instanceof ControlNode && secretStmt instanceof CommunicationNode) {
          mambaStmtList.add(
              MambaInliner.run(inlineMap,
                  ImpToMambaTranslator.run(true, hostNameMap, loopVar, secretStmt)));

          secretIndex++;

        } else {
          throw new Error("merge error!");
        }

      } else {
        String msg =
            String.format("attempting to merge nodes in different execution paths: %s and %s",
                publicStmtLocation, secretStmtLocation);
        throw new Error(msg);
      }
    }

    return mambaStmtList;
  }
}
