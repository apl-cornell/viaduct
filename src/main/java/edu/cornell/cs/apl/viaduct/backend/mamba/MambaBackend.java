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
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import org.fusesource.jansi.AnsiConsole;
import org.fusesource.jansi.AnsiPrintStream;

/** translate Viaduct process configuration to a MAMBA program. */
public final class MambaBackend {
  private static class MambaCompilationInfo {
    final Set<MambaVariable> secretVariables;
    final Optional<ProcessName> publicProcess;
    final Optional<ProcessName> secretProcess;
    final Optional<MambaStatementNode> mambaProcess;

    MambaCompilationInfo(
        Set<MambaVariable> secretVariables,
        Optional<ProcessName> publicProcess,
        Optional<ProcessName> secretProcess,
        Optional<MambaStatementNode> mambaProcess) {
      this.secretVariables = secretVariables;
      this.publicProcess = publicProcess;
      this.secretProcess = secretProcess;
      this.mambaProcess = mambaProcess;
    }

    public Set<ProcessName> getMambaProcesses() {
      Set<ProcessName> mambaProcesses = HashSet.empty();
      if (this.publicProcess.isPresent()) {
        mambaProcesses = mambaProcesses.add(this.publicProcess.get());
      }
      if (this.secretProcess.isPresent()) {
        mambaProcesses = mambaProcesses.add(this.secretProcess.get());
      }

      return mambaProcesses;
    }
  }

  /** compile generated IMP program into MAMBA backend. */
  public void compile(String mambaCompilationTemplateFile, ProgramNode program, String outputDir) {
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

    try {
      String mambaCompilationTemplate = Files.readString(Paths.get(mambaCompilationTemplateFile));

      if (outputDir == null) {
        printToStdout(mambaCompilationTemplate, hostNameMap, processes, mambaInfo);

      } else {
        printToOutputDir(mambaCompilationTemplate, outputDir, hostNameMap, processes, mambaInfo);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void printToOutputDir(
      String mambaCompilationTemplate,
      String outputDirname,
      ImmutableMap<ProcessName, Integer> hostNameMap,
      ImmutableMap<ProcessName, ProcessDeclarationNode> processes,
      MambaCompilationInfo mambaInfo)
      throws IOException {

    File outputDir = new File(outputDirname);
    if (!outputDir.exists()) {
      if (!outputDir.mkdirs()) {
        throw new IOException(String.format("failed to create output directory %s", outputDirname));
      }
    }

    for (Map.Entry<ProcessName, Integer> kv : hostNameMap.entrySet()) {
      ProcessName hostProcessName = kv.getKey();
      ProcessDeclarationNode hostProcess = processes.get(hostProcessName);
      String pythonProcessStr =
          ImpPythonPrintVisitor.run(
              hostProcess.getBody(), hostProcessName, mambaInfo.getMambaProcesses());

      String filename = String.format("player_%d.py", kv.getValue());
      File file = new File(outputDir, filename);
      PrintStream out = new AnsiPrintStream(new PrintStream(file, StandardCharsets.UTF_8));
      out.print(pythonProcessStr);
      out.close();
    }

    if (mambaInfo.mambaProcess.isPresent()) {
      String mambaProcessStr =
          MambaPrintVisitor.run(mambaInfo.secretVariables, mambaInfo.mambaProcess.get());

      String filename = String.format("%s.mpc", outputDir.getName());
      PrintStream out =
          new AnsiPrintStream(
              new PrintStream(new File(outputDir, filename), StandardCharsets.UTF_8));

      out.print(mambaCompilationTemplate);
      out.print(mambaProcessStr);
      out.close();
    }
  }

  private void printToStdout(
      String mambaCompilationTemplate,
      ImmutableMap<ProcessName, Integer> hostNameMap,
      ImmutableMap<ProcessName, ProcessDeclarationNode> processes,
      MambaCompilationInfo mambaInfo)
      throws IOException {

    PrintStream stdout = AnsiConsole.out();

    for (Map.Entry<ProcessName, Integer> kv : hostNameMap.entrySet()) {
      ProcessName hostProcessName = kv.getKey();
      ProcessDeclarationNode hostProcess = processes.get(hostProcessName);
      String pythonProcess =
          ImpPythonPrintVisitor.run(
              hostProcess.getBody(), hostProcessName, mambaInfo.getMambaProcesses());

      stdout.println(String.format("process %s:", kv.getKey().getName()));
      stdout.println(pythonProcess);
      stdout.println("");
    }

    if (mambaInfo.mambaProcess.isPresent()) {
      stdout.println("mamba process:");
      stdout.println(mambaCompilationTemplate);
      stdout.println(
          MambaPrintVisitor.run(mambaInfo.secretVariables, mambaInfo.mambaProcess.get()));
    }
  }

  private MambaCompilationInfo generateMambaProcess(
      ProgramNode program, ImmutableMap<ProcessName, Integer> hostNameMap) {
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
              MambaPublicSecretProcessMerger.run(
                  program, hostNameMap, publicProcessName.get(), secretProcessName.get()));

    } else if (publicProcessName.isPresent()) {
      mambaProcess =
          Optional.of(
              ImpToMambaTranslator.run(
                  false, hostNameMap, processes.get(publicProcessName.get()).getBody()));

    } else if (secretProcessName.isPresent()) {
      mambaProcess =
          Optional.of(
              ImpToMambaTranslator.run(
                  false, hostNameMap, processes.get(secretProcessName.get()).getBody()));
    }

    // mux secret conditionals
    Set<MambaVariable> secretVariables = HashSet.empty();
    if (mambaProcess.isPresent()) {
      secretVariables = secretVariables.addAll(MambaSecretVariablesVisitor.run(mambaProcess.get()));

      MambaSecretInputChecker secretChecker = new MambaSecretInputChecker(secretVariables);
      mambaProcess =
          Optional.of(MambaSecretConditionalConverter.run(secretChecker, mambaProcess.get()));
    }

    return new MambaCompilationInfo(
        secretVariables, publicProcessName, secretProcessName, mambaProcess);
  }
}
