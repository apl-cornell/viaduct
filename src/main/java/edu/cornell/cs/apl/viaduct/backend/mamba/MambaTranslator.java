package edu.cornell.cs.apl.viaduct.backend.mamba;

import com.google.common.collect.ImmutableMap;

import edu.cornell.cs.apl.viaduct.AstNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaStatementNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.protocols.MambaPublic;
import edu.cornell.cs.apl.viaduct.backend.mamba.protocols.MambaSecret;
import edu.cornell.cs.apl.viaduct.backend.mamba.visitors.ImpToMambaTranslator;
import edu.cornell.cs.apl.viaduct.backend.mamba.visitors.MambaPrintVisitor;
import edu.cornell.cs.apl.viaduct.backend.mamba.visitors.MambaSecretConditionalConverter;
import edu.cornell.cs.apl.viaduct.backend.mamba.visitors.MambaSecretInputChecker;
import edu.cornell.cs.apl.viaduct.backend.mamba.visitors.MambaSecretVariablesVisitor;
import edu.cornell.cs.apl.viaduct.imp.ast.ProcessDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ProcessName;
import edu.cornell.cs.apl.viaduct.imp.ast.ProgramNode;
import edu.cornell.cs.apl.viaduct.protocol.Protocol;

import java.util.Map;

/** translate Viaduct process configuration to a MAMBA program. */
public final class MambaTranslator {
  /**  do translation. */
  public void translate(ProgramNode program) {
    // merge public and secret mamba processes
    ImmutableMap<ProcessName, ProcessDeclarationNode> processes = program.processes();
    ProcessName publicProcessName = null;
    ProcessName secretProcessName = null;

    for (Map.Entry<ProcessName, ProcessDeclarationNode> kv : processes.entrySet()) {
      Protocol<? extends AstNode> protocol = kv.getValue().getProtocol();

      if (protocol != null && protocol.getId().equals(MambaSecret.PROTOCOL_ID)) {
        if (secretProcessName == null) {
          secretProcessName = kv.getKey();

        } else {
          // TODO: add better exception
          throw new Error("multiple MambaSecret processes");
        }

      } else if (protocol != null && protocol.getId().equals(MambaPublic.PROTOCOL_ID)) {
        if (publicProcessName == null) {
          publicProcessName = kv.getKey();

        } else {
          // TODO: add better exception
          throw new Error("multiple MambaPublic processes");
        }
      }
    }

    MambaStatementNode mambaProcess = null;

    // public and secret mamba processes exist; merge them
    if (publicProcessName != null && secretProcessName != null) {
      mambaProcess =
          MambaPublicSecretProcessMerger.run(program, publicProcessName, secretProcessName);

    } else if (publicProcessName != null) {
      mambaProcess =
          ImpToMambaTranslator.run(false, processes.get(publicProcessName).getBody());

    } else if (secretProcessName != null) {
      mambaProcess =
          ImpToMambaTranslator.run(false, processes.get(secretProcessName).getBody());
    }

    if (mambaProcess != null) {
      MambaSecretInputChecker secretChecker =
          new MambaSecretInputChecker(MambaSecretVariablesVisitor.run(mambaProcess));
      mambaProcess = MambaSecretConditionalConverter.run(secretChecker, mambaProcess);
      System.out.println(MambaPrintVisitor.run(secretChecker, mambaProcess));
    }
  }
}
