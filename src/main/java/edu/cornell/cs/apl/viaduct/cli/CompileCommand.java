package edu.cornell.cs.apl.viaduct.cli;

import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.annotations.Option;
import edu.cornell.cs.apl.viaduct.analysis.InformationFlowAnalysis;
import edu.cornell.cs.apl.viaduct.analysis.NameAnalysis;
import edu.cornell.cs.apl.viaduct.parsing.ParsingKt;
import edu.cornell.cs.apl.viaduct.passes.CheckingKt;
import edu.cornell.cs.apl.viaduct.passes.ElaborationKt;
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode;
import edu.cornell.cs.apl.viaduct.syntax.intermediate.attributes.Tree;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.engine.GraphvizCmdLineEngine;
import guru.nidi.graphviz.engine.GraphvizServerEngine;
import guru.nidi.graphviz.engine.GraphvizV8Engine;
import guru.nidi.graphviz.model.MutableGraph;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.apache.commons.io.FilenameUtils;

@Command(name = "compile", description = "Compile ideal protocol to secure distributed program")
public class CompileCommand extends BaseCommand {
  /** Stop graphviz-java from using the deprecated Nashorn Javascript engine. */
  static {
    Graphviz.useEngine(
        new GraphvizCmdLineEngine(), new GraphvizV8Engine(), new GraphvizServerEngine());
  }

  // TODO: option to print inferred label for each variable.
  // TODO: option to print selected protocol for each variable.

  @Option(
      name = {"-c", "--constraint-graph"},
      title = "file.ext",
      description =
          "Write the label constraint graph generated for the program to <file.ext>."
              + " File extension determines output format."
              + " Supported formats are the same as the ones in Graphviz."
              + " Most useful ones are .dot, .svg, .png, and .pdf.")
  // TODO: enable checks when we learn how to remove them from generated help.
  // @Once
  // @com.github.rvesse.airline.annotations.restrictions.File(readable = false)
  private String constraintGraphOutput = null;

  @Option(
      name = {"-v", "--verbose"},
      description = "Print debugging information.")
  private boolean verbose;

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

  /**
   * Write the given graph to the output file if the filename is not {@code null}. Do nothing
   * otherwise. The output format is determined automatically from the file extension.
   *
   * @param graph A computation that generates DOT formatted output.
   * @param file Name of the file to output to.
   */
  private static void dumpGraph(Consumer<Writer> graph, String file) throws IOException {
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
        // TODO: better error (it should at least include the whole file name)
        throw new Error("Unknown extension: " + extension);
    }
  }

  @Override
  public void run() throws IOException {
    final ProgramNode program =
        ElaborationKt.elaborated(ParsingKt.parse(this.input.newSourceFileKotlin()));

    // Dump label constraint graph to a file if requested.
    dumpGraph(
        (output) ->
            new InformationFlowAnalysis(new NameAnalysis(new Tree<>(program)))
                .exportConstraintGraph(output),
        constraintGraphOutput);

    // Perform checks.
    CheckingKt.check(program);

    // TODO: compile!
    try (PrintStream writer = this.output.newOutputStream()) {
      program.getAsDocument().print(writer, 80, true);
    }
  }
}
