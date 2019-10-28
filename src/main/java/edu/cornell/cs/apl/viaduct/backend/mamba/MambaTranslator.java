package edu.cornell.cs.apl.viaduct.backend.mamba;

import com.google.common.collect.ImmutableMap;

import edu.cornell.cs.apl.viaduct.AstNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.protocols.MambaPublic;
import edu.cornell.cs.apl.viaduct.backend.mamba.protocols.MambaSecret;
import edu.cornell.cs.apl.viaduct.imp.ast.ProcessDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ProcessName;
import edu.cornell.cs.apl.viaduct.imp.ast.ProgramNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StatementNode;
import edu.cornell.cs.apl.viaduct.imp.parsing.Printer;
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

    // public and secret mamba processes exist; merge them
    if (publicProcessName != null && secretProcessName != null) {
      StatementNode mergedProcess =
          MambaPublicSecretProcessMerger.run(program, publicProcessName, secretProcessName);

      System.out.println(Printer.run(mergedProcess));
    }
  }
}
