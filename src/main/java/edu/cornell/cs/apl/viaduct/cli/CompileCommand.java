package edu.cornell.cs.apl.viaduct.cli;

import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.annotations.Option;
import edu.cornell.cs.apl.viaduct.backend.mamba.ImpMambaCommunicationCostEstimator;
import edu.cornell.cs.apl.viaduct.backend.mamba.ImpMambaMpcProtocolSearchStrategy;
import edu.cornell.cs.apl.viaduct.backend.mamba.ImpMambaProtocolSearchStrategy;
import edu.cornell.cs.apl.viaduct.backend.mamba.ImpMambaReplProtocolSearchStrategy;
import edu.cornell.cs.apl.viaduct.backend.mamba.MambaBackend;
import edu.cornell.cs.apl.viaduct.imp.HostTrustConfiguration;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ProcessName;
import edu.cornell.cs.apl.viaduct.imp.ast.ProgramNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StatementNode;
import edu.cornell.cs.apl.viaduct.imp.informationflow.InformationFlowChecker;
import edu.cornell.cs.apl.viaduct.imp.parsing.Printer;
import edu.cornell.cs.apl.viaduct.imp.parsing.SourceFile;
import edu.cornell.cs.apl.viaduct.imp.parsing.TrustConfigurationParser;
import edu.cornell.cs.apl.viaduct.imp.protocols.ImpProtocolCommunicationStrategy;
import edu.cornell.cs.apl.viaduct.imp.protocols.ImpProtocolSearchSelection;
import edu.cornell.cs.apl.viaduct.imp.protocols.ImpZKProtocolSearchStrategy;
import edu.cornell.cs.apl.viaduct.imp.transformers.AnfConverter;
import edu.cornell.cs.apl.viaduct.imp.transformers.Elaborator;
import edu.cornell.cs.apl.viaduct.imp.transformers.ImpPdgBuilderPreprocessor;
import edu.cornell.cs.apl.viaduct.imp.transformers.LogicalPositionInjector;
import edu.cornell.cs.apl.viaduct.imp.typing.TypeChecker;
import edu.cornell.cs.apl.viaduct.imp.visitors.ImpPdgBuilderVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.ImpProtocolInstantiationVisitor;
import edu.cornell.cs.apl.viaduct.pdg.PdgDotPrinter;
import edu.cornell.cs.apl.viaduct.pdg.PdgNode;
import edu.cornell.cs.apl.viaduct.pdg.ProgramDependencyGraph;
import edu.cornell.cs.apl.viaduct.protocol.Protocol;
import edu.cornell.cs.apl.viaduct.protocol.ProtocolCommunicationStrategy;
import edu.cornell.cs.apl.viaduct.protocol.ProtocolCostEstimator;
import edu.cornell.cs.apl.viaduct.protocol.ProtocolSearchStrategy;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.engine.GraphvizCmdLineEngine;
import guru.nidi.graphviz.engine.GraphvizServerEngine;
import guru.nidi.graphviz.engine.GraphvizV8Engine;
import guru.nidi.graphviz.model.MutableGraph;
import io.vavr.collection.Map;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.apache.commons.io.FilenameUtils;
import org.fusesource.jansi.AnsiConsole;

@Command(name = "compile", description = "Compile ideal protocol to secure distributed program")
public class CompileCommand extends BaseCommand {
  /** Stop graphviz-java from using the deprecated Nashorn Javascript engine. */
  static {
    Graphviz.useEngine(
        new GraphvizCmdLineEngine(), new GraphvizV8Engine(), new GraphvizServerEngine());
  }

  @Option(
      name = {"-h", "--hosts"},
      title = "files",
      description =
          "Read host trust configuration from <files>."
              + " If multiple files are given, declarations in all files are combined together."
              + " Declarations in later files overwrite earlier ones when there are multiple"
              + " declarations for the same host.")
  private List<String> hostConfigurationFiles = null;

  @Option(
      name = {"-l", "--label-graph"},
      title = "file.ext",
      description =
          "Write program dependency graph with security labels to <file.ext>."
              + " File extension determines output format."
              + " Supported formats are the same as the ones in Graphviz."
              + " Most useful ones are .dot, .svg, .png, and .pdf.")
  // TODO: enable checks when we learn how to remove them from generated help.
  // @Once
  // @com.github.rvesse.airline.annotations.restrictions.File(readable = false)
  private String labelGraphOutput = null;

  @Option(
      name = {"-p", "--protocol-graph"},
      title = "file.ext",
      description =
          "Write program dependency graph with selected cryptographic protocols to <file.ext>"
              + " File extension determines output format. See --label-graph.")
  // TODO: same.
  // @Once
  // @com.github.rvesse.airline.annotations.restrictions.File(readable = false)
  private String protocolGraphOutput = null;

  @Option(
      name = {"-c", "--constraint-graph"},
      title = "file.ext",
      description = "Write label constraint graph to <file.ext>.")
  private String constraintGraphOutput = null;

  @Option(
      name = {"--skip"},
      description =
          "Compilation will stop after the last debugging information (e.g. graph dump)"
              + " option is generated.")
  private boolean skip;

  @Option(
      name = {"-prof", "--profile"},
      description = "Enable profiling for protocol selection.")
  private boolean enableProfiling;

  @Option(
      name = {"--imp"},
      title = "output.imp",
      description = "Output synthesized protocol in an intermediate representation only.")
  private boolean impOnly;

  @Option(
      name = {"--template"},
      title = "template.py",
      description = "Compilation template for MAMBA programs.")
  private String mambaCompilationTemplate = "template.py";

  @Option(
      name = {"--strategy"},
      description = "Compilation strategy. Current available options: opt, zk, mpc")
  private String strategy = "opt";

  @Option(
      name = {"-v", "--verbose"},
      description = "Print information throughout the compilation process.")
  private boolean verbose;

  /**
   * Write the given graph to the output file if the filename is not {@code null}. Do nothing
   * otherwise. The output format is determined automatically from the file extension.
   *
   * @param graph graph to output
   * @param file name of the file to output to
   */
  private static void dumpGraph(Supplier<MutableGraph> graph, String file) throws IOException {
    if (file == null) {
      return;
    }

    String fileExtension = FilenameUtils.getExtension(file);
    if (fileExtension.equals("dot")) {
      try (Writer writer =
          new BufferedWriter(
              new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
        writer.write(graph.get().toString());
      }
    } else {
      Format format = formatFromExtension(fileExtension);
      Graphviz.fromGraph(graph.get()).render(format).toFile(new File(file));
    }
  }

  private static void dumpConstraints(Consumer<Writer> graph, String file) throws IOException {
    if (file == null) {
      return;
    }

    String fileExtension = FilenameUtils.getExtension(file);
    if (fileExtension.equals("dot")) {
      try (Writer writer =
          new BufferedWriter(
              new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
        graph.accept(writer);
      }

    } else {
      Format format = formatFromExtension(fileExtension);
      Writer writer = new StringWriter();
      graph.accept(writer);
      Graphviz.fromString(writer.toString()).render(format).toFile(new File(file));
    }
  }

  /** Compute graph output format from the file extension. */
  private static Format formatFromExtension(String extension) {
    switch (extension.toLowerCase()) {
      case "json":
        return Format.JSON0;
      case "png":
        return Format.PNG;
      case "svg":
        return Format.SVG;
      case "txt":
        return Format.PLAIN;
      case "xdot":
        return Format.XDOT;
      default:
        throw new Error("Unknown extension: " + extension);
    }
  }

  private ProtocolSearchStrategy<ImpAstNode> getSearchStrategy(
      HostTrustConfiguration hostConfig,
      ProtocolCommunicationStrategy<ImpAstNode> communicationStrategy,
      ProtocolCostEstimator<ImpAstNode> costEstimator) {
    switch (this.strategy) {
      case "opt":
        return new ImpMambaProtocolSearchStrategy(costEstimator);

      case "mpc":
        return new ImpMambaMpcProtocolSearchStrategy(costEstimator);

      case "repl":
        return new ImpMambaReplProtocolSearchStrategy(costEstimator);

      case "zk":
        return new ImpZKProtocolSearchStrategy(costEstimator);

      default:
        // TODO: better exception type
        throw new Error(String.format("unknown compilation strategy: %s", this.strategy));
    }
  }

  private boolean canCompileToMamba() {
    return this.strategy.equals("opt")
        || this.strategy.equals("mpc")
        || this.strategy.equals("repl");
  }

  @Override
  public void run() throws IOException {
    if (this.verbose) {
      System.out.println(String.format("parsing input file %s...", this.input.input));
    }

    final ProgramNode program = this.input.parse();
    final HostTrustConfiguration hostConfig = this.parseHostConfig(program);

    if (this.verbose) {
      System.out.println("type checking...");
    }

    TypeChecker.run(program);

    if (this.verbose) {
      System.out.println("elaborating derived forms...");
      System.out.println("converting to A-normal form...");
      System.out.println("removing uncompilable syntactic forms...");
    }

    final ProgramNode processedProgram =
        ImpPdgBuilderPreprocessor.run(
            LogicalPositionInjector.run(AnfConverter.run(Elaborator.run(program))));

    if (this.verbose) {
      System.out.println("checking information flow constraints...");
      System.out.println("generating minimum authority solution...");
    }

    // Check information flow and inject trust labels into the AST.
    InformationFlowChecker.run(processedProgram);

    // Dump constraint graph to a file if requested.
    dumpConstraints(
        (output) ->
            InformationFlowChecker.exportDotGraph(
                Elaborator.run(program).processes().get(ProcessName.getMain()).getBody(),
                program.hosts(),
                output),
        constraintGraphOutput);

    if (this.skip && labelGraphOutput == null && protocolGraphOutput == null) {
      return;
    }

    final StatementNode main = processedProgram.processes().get(ProcessName.getMain()).getBody();

    if (this.verbose) {
      System.out.println("generating information dependency graph...");
    }

    // Generate program dependency graph.
    final ProgramDependencyGraph<ImpAstNode> pdg = new ImpPdgBuilderVisitor().generatePDG(main);

    // Dump PDG with information flow labels to a file (if requested).
    dumpGraph(() -> PdgDotPrinter.pdgDotGraphWithLabels(pdg, new Printer()), labelGraphOutput);

    if (this.skip && protocolGraphOutput == null) {
      return;
    }

    // Select cryptographic protocols for each node.
    final ImpProtocolCommunicationStrategy communicationStrategy =
        new ImpProtocolCommunicationStrategy();

    /*
    final ProtocolCostEstimator<ImpAstNode> costEstimator =
        new ImpCommunicationCostEstimator(hostConfig, communicationStrategy);

    final ProtocolSearchStrategy<ImpAstNode> strategy =
        new ImpProtocolSearchStrategy(costEstimator);
    */

    final ProtocolCostEstimator<ImpAstNode> costEstimator =
        new ImpMambaCommunicationCostEstimator(hostConfig, communicationStrategy);

    final ProtocolSearchStrategy<ImpAstNode> strategy =
        getSearchStrategy(hostConfig, communicationStrategy, costEstimator);

    if (this.verbose) {
      System.out.println("selecting protocols...");
    }

    final Map<PdgNode<ImpAstNode>, Protocol<ImpAstNode>> protocolMap =
        (new ImpProtocolSearchSelection(this.enableProfiling, strategy))
            .selectProtocols(hostConfig, pdg);

    // Dump PDG with protocol information to a file (if requested).
    dumpGraph(
        () ->
            PdgDotPrinter.pdgDotGraphWithProtocols(pdg, protocolMap, costEstimator, new Printer()),
        protocolGraphOutput);

    if (this.skip) {
      return;
    }

    int protoCost = strategy.estimatePdgCost(protocolMap, pdg);
    if (pdg.getOrderedNodes().size() == protocolMap.size()) {
      if (this.verbose) {
        System.out.println(String.format("protocols selected! estimated cost: %d", protoCost));
      }

      // Found a protocol for every node! Output synthesized distributed program.
      ImpProtocolInstantiationVisitor protocolInstantiator =
          new ImpProtocolInstantiationVisitor(
              hostConfig, communicationStrategy, pdg, protocolMap, main);

      final ProgramNode generatedProgram =
          protocolInstantiator
              .run()
              .toBuilder()
              .addAll(hostConfig.getDeclarations().values())
              .build();

      if (this.impOnly || this.verbose) {
        System.out.println("process configuration:");

        PrintStream writer = AnsiConsole.out();
        Printer.run(generatedProgram, writer);
      }

      if (this.impOnly) {
        return;
      }

      if (this.verbose) {
        System.out.println("compiling to MAMBA backend...");
      }

      if (canCompileToMamba()) {
        (new MambaBackend())
            .compile(this.mambaCompilationTemplate, generatedProgram, output.outputDir);
      }

    } else {
      // TODO: this should be reported better.
      throw new Error("Could not synthesize protocol");
    }
  }

  /**
   * Parse all host configuration files, add host declarations from the main program, and return
   * them in one bundle.
   */
  private HostTrustConfiguration parseHostConfig(ProgramNode program) throws IOException {
    final HostTrustConfiguration.Builder builder = HostTrustConfiguration.builder();

    // Parse and concatenate trust configurations.
    if (hostConfigurationFiles != null) {
      for (String hostConfig : hostConfigurationFiles) {
        // TODO: check that there are no collisions within each file.
        //    Collisions across files are meant to have an overwriting semantic.
        final SourceFile hostConfigFile = SourceFile.from(new File(hostConfig));
        builder.addAll(TrustConfigurationParser.parse(hostConfigFile));
      }
    }

    // Add declarations from the main program.
    builder.addAll(program.hosts().values());

    return builder.build();
  }
}
