package edu.cornell.cs.apl.viaduct.backend.mamba;

import com.google.common.collect.ImmutableMap;

import edu.cornell.cs.apl.viaduct.AstNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaStatementNode;
import edu.cornell.cs.apl.viaduct.backend.mamba.ast.MambaVariable;
import edu.cornell.cs.apl.viaduct.backend.mamba.protocols.MambaPublic;
import edu.cornell.cs.apl.viaduct.backend.mamba.protocols.MambaSecret;
import edu.cornell.cs.apl.viaduct.backend.mamba.visitors.ImpPythonPrintVisitor;
import edu.cornell.cs.apl.viaduct.backend.mamba.visitors.ImpToMambaTranslator;
import edu.cornell.cs.apl.viaduct.backend.mamba.visitors.MambaPrintVisitor;
import edu.cornell.cs.apl.viaduct.backend.mamba.visitors.MambaSecretConditionalConverter;
import edu.cornell.cs.apl.viaduct.backend.mamba.visitors.MambaSecretInputChecker;
import edu.cornell.cs.apl.viaduct.backend.mamba.visitors.MambaSecretVariablesVisitor;
import edu.cornell.cs.apl.viaduct.imp.ast.HostDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.HostName;
import edu.cornell.cs.apl.viaduct.imp.ast.ProcessDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ProcessName;
import edu.cornell.cs.apl.viaduct.imp.ast.ProgramNode;
import edu.cornell.cs.apl.viaduct.protocol.Protocol;

import io.vavr.collection.HashSet;
import io.vavr.collection.Set;

import java.util.Map;
import java.util.Optional;

/** translate Viaduct process configuration to a MAMBA program. */
public final class MambaBackend {
  private static class MambaCompilationInfo {
    final Set<MambaVariable> secretVariables;
    final Optional<MambaStatementNode> mambaProcess;

    MambaCompilationInfo(
        Set<MambaVariable> secretVariables,
        Optional<MambaStatementNode> mambaProcess)
    {
      this.secretVariables = secretVariables;
      this.mambaProcess = mambaProcess;
    }
  }

  /** compile generated IMP program into MAMBA backend. */
  public void compile(ProgramNode program) {
    // number hosts
    ImmutableMap<HostName, HostDeclarationNode> hosts = program.hosts();
    ImmutableMap.Builder<ProcessName, Integer> hostNameMapBuilder = ImmutableMap.builder();

    int i = 0;
    for (Map.Entry<HostName, HostDeclarationNode> kv : hosts.entrySet()) {
      hostNameMapBuilder.put(ProcessName.create(kv.getKey()), i);
      i++;
    }

    ImmutableMap<ProcessName, Integer> hostNameMap = hostNameMapBuilder.build();

    ImmutableMap<ProcessName, ProcessDeclarationNode> processes = program.processes();
    MambaCompilationInfo mambaInfo = generateMambaProcess(program, hostNameMap);

    System.out.println("printing processes...");
    for (Map.Entry<HostName, HostDeclarationNode> kv : hosts.entrySet()) {
      ProcessDeclarationNode hostProcess = processes.get(ProcessName.create(kv.getKey()));
      String pythonProcess = ImpPythonPrintVisitor.run(hostProcess.getBody());

      System.out.println(String.format("process %s:", kv.getKey().getName()));
      System.out.println(pythonProcess);
      System.out.println("");
    }

    if (mambaInfo.mambaProcess.isPresent()) {
      System.out.println("mamba process:");
      System.out.println(
          MambaPrintVisitor.run(mambaInfo.secretVariables, mambaInfo.mambaProcess.get()));
    }
  }

  private MambaCompilationInfo generateMambaProcess(
        ProgramNode program, ImmutableMap<ProcessName, Integer> hostNameMap)
  {
    ImmutableMap<ProcessName, ProcessDeclarationNode> processes = program.processes();
    Optional<ProcessName> publicProcessName = Optional.empty();
    Optional<ProcessName> secretProcessName = Optional.empty();

    // get public and secret mamba processes
    for (Map.Entry<ProcessName, ProcessDeclarationNode> kv : processes.entrySet()) {
      Protocol<? extends AstNode> protocol = kv.getValue().getProtocol();

      if (protocol != null && protocol.getId().equals(MambaSecret.PROTOCOL_ID)) {
        if (secretProcessName.isEmpty()) {
          secretProcessName = Optional.of(kv.getKey());

        } else {
          // TODO: add better exception
          throw new Error("multiple MambaSecret processes");
        }

      } else if (protocol != null && protocol.getId().equals(MambaPublic.PROTOCOL_ID)) {
        if (publicProcessName.isEmpty()) {
          publicProcessName = Optional.of(kv.getKey());

        } else {
          // TODO: add better exception
          throw new Error("multiple MambaPublic processes");
        }
      }
    }

    Optional<MambaStatementNode> mambaProcess = Optional.empty();

    // public and secret mamba processes exist; merge them
    if (publicProcessName.isPresent() && secretProcessName.isPresent()) {
      mambaProcess =
          Optional.of(
              MambaPublicSecretProcessMerger
              .run(program, hostNameMap, publicProcessName.get(), secretProcessName.get()));

    } else if (publicProcessName.isPresent()) {
      mambaProcess =
          Optional.of(
              ImpToMambaTranslator
              .run(false, hostNameMap, processes.get(publicProcessName.get()).getBody()));

    } else if (secretProcessName.isPresent()) {
      mambaProcess =
          Optional.of(
              ImpToMambaTranslator
              .run(false, hostNameMap, processes.get(secretProcessName.get()).getBody()));
    }

    // mux secret conditionals
    Set<MambaVariable> secretVariables = HashSet.empty();
    if (mambaProcess.isPresent()) {
      secretVariables = secretVariables.addAll(
          MambaSecretVariablesVisitor.run(mambaProcess.get()));

      MambaSecretInputChecker secretChecker = new MambaSecretInputChecker(secretVariables);
      mambaProcess =
          Optional.of(
              MambaSecretConditionalConverter.run(secretChecker, mambaProcess.get()));
    }

    return new MambaCompilationInfo(secretVariables, mambaProcess);
  }
}
