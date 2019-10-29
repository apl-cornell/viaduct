package edu.cornell.cs.apl.viaduct.backend.mamba;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaBlockNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaExpressionNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaIfNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaRevealNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaStatementNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaVariable;
import edu.cornell.cs.apl.viaduct.backend.mamba.visitors.ImpToMambaTranslator;
import edu.cornell.cs.apl.viaduct.backend.mamba.visitors.MambaInliner;
import edu.cornell.cs.apl.viaduct.imp.ast.IfNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LoopNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ProcessDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ProcessName;
import edu.cornell.cs.apl.viaduct.imp.ast.ProgramNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReceiveNode;
import edu.cornell.cs.apl.viaduct.imp.ast.SendNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StatementNode;
import edu.cornell.cs.apl.viaduct.imp.parsing.SourcePosition;

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
      ProcessName publicProcessName,
      ProcessName secretProcessName)
  {
    return
        (new MambaPublicSecretProcessMerger())
        .mergePublicSecret(program, HashMap.empty(), publicProcessName, secretProcessName);
  }

  /** merge mamba public and secret processes from a generated program. */
  private MambaStatementNode mergePublicSecret(
      ProgramNode program,
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
            inlineMap,
            publicProcessName, publicStmtList,
            secretProcessName, secretStmtList);

    return MambaBlockNode.builder().addAll(mambaStmtList).build();
  }

  private Iterable<MambaStatementNode> mergePublicSecretProcesses(
      Map<MambaVariable, MambaExpressionNode> inlineMap,
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

      SourcePosition publicStmtLocation =
          publicStmt != null ? publicStmt.getSourceLocation().getStart() : null;
      SourcePosition secretStmtLocation =
          secretStmt != null ? secretStmt.getSourceLocation().getStart() : null;

      int compareLocations =
          publicStmtLocation != null && secretStmtLocation != null
          ? publicStmtLocation.compareTo(secretStmtLocation) : 0;

      // public stmt is ordered before secret stmt
      if (compareLocations < 0 || secretIndex >= secretProcessSize) {
        mambaStmtList.add(
            MambaInliner.run(
                inlineMap,
                ImpToMambaTranslator.run(false, publicStmt)));

        publicIndex++;

      // secret stmt is ordered before public stmt
      } else if (compareLocations > 0 || publicIndex >= publicProcessSize) {
        mambaStmtList.add(
            MambaInliner.run(
              inlineMap,
              ImpToMambaTranslator.run(true, secretStmt)));

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
            MambaVariable mambaVar = ImpToMambaTranslator.run(false, publicRecv.getVariable());
            MambaExpressionNode mambaRhs =
                MambaRevealNode.builder()
                .setRevealedExpr(
                    ImpToMambaTranslator
                    .run(true, secretSend.getSentExpression()))
                .build();

            inlineMap = inlineMap.put(mambaVar, mambaRhs);
            /*
            MambaStatementNode newStmt =
                MambaAssignNode.builder()
                .setVariable(mambaVar)
                .setRhs(mambaRhs)
                .build();
            mambaStmtList.add(newStmt);
            */

          } else {
            // order arbitrarily
            mambaStmtList.add(
                MambaInliner.run(
                    inlineMap,
                    ImpToMambaTranslator.run(false, publicRecv)));

            mambaStmtList.add(
                MambaInliner.run(
                    inlineMap,
                    ImpToMambaTranslator.run(true, secretSend)));
          }

        // public send, secret recv
        } else if (publicStmt instanceof SendNode && secretStmt instanceof ReceiveNode) {
          SendNode publicSend = (SendNode) publicStmt;
          ReceiveNode secretRecv = (ReceiveNode) secretStmt;

          if (secretRecv.getSender().equals(publicProcessName)
              && publicSend.getRecipient().equals(secretProcessName))
          {
            MambaVariable mambaVar =
                ImpToMambaTranslator.run(true, secretRecv.getVariable());

            MambaExpressionNode mambaExpr =
                MambaInliner.run(
                    inlineMap,
                    ImpToMambaTranslator.run(false, publicSend.getSentExpression()));

            inlineMap = inlineMap.put(mambaVar, mambaExpr);
            /*
            MambaStatementNode newStmt =
                MambaAssignNode.builder()
                .setVariable(mambaVar)
                .setRhs(mambaExpr)
                .build();
            mambaStmtList.add(newStmt);
            */

          } else {
            // order arbitrarily
            mambaStmtList.add(
                MambaInliner.run(inlineMap,
                    ImpToMambaTranslator.run(true, secretRecv)));

            mambaStmtList.add(
                MambaInliner.run(inlineMap,
                    ImpToMambaTranslator.run(false, publicSend)));
          }

        // conditional
        } else if (publicStmt instanceof IfNode && secretStmt instanceof IfNode) {
          IfNode publicIf = (IfNode) publicStmt;
          IfNode secretIf = (IfNode) secretStmt;

          MambaExpressionNode newGuard =
              MambaInliner.run(
                inlineMap,
                ImpToMambaTranslator.run(false, publicIf.getGuard()));

          Iterable<MambaStatementNode> newThenBranch =
              mergePublicSecretProcesses(
                  inlineMap,
                  publicProcessName, publicIf.getThenBranch().getStatements(),
                  secretProcessName, secretIf.getElseBranch().getStatements());

          Iterable<MambaStatementNode> newElseBranch =
              mergePublicSecretProcesses(
                  inlineMap,
                  publicProcessName, publicIf.getElseBranch().getStatements(),
                  secretProcessName, secretIf.getElseBranch().getStatements());

          MambaIfNode newIf =
              MambaIfNode.builder()
              .setGuard(newGuard)
              .setThenBranch(MambaBlockNode.builder().addAll(newThenBranch).build())
              .setElseBranch(MambaBlockNode.builder().addAll(newElseBranch).build())
              .build();
          mambaStmtList.add(newIf);

        // loop
        } else if (publicStmt instanceof LoopNode && secretStmt instanceof LoopNode) {
          LoopNode publicLoop = (LoopNode) publicStmt;
          LoopNode secretLoop = (LoopNode) secretStmt;

          Iterable<MambaStatementNode> newBody =
              mergePublicSecretProcesses(
                  inlineMap,
                  publicProcessName, publicLoop.getBody().getStatements(),
                  secretProcessName, secretLoop.getBody().getStatements());

          /*
          LoopNode newLoop =
              LoopNode.builder()
              .setBody(BlockNode.builder().addAll(newBody).build())
              .build();
          mambaStmtList.add(newLoop);
          */

        } else {
          throw new Error("merge error!");
        }

        publicIndex++;
        secretIndex++;
      }
    }

    return mambaStmtList;
  }
}
