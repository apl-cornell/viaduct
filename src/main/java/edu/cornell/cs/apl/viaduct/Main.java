package edu.cornell.cs.apl.viaduct;

import edu.cornell.cs.apl.viaduct.imp.HostTrustConfiguration;
import edu.cornell.cs.apl.viaduct.imp.ImpProtocolCostEstimator;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ProcessName;
import edu.cornell.cs.apl.viaduct.imp.ast.ProgramNode;
import edu.cornell.cs.apl.viaduct.imp.interpreter.Interpreter;
import edu.cornell.cs.apl.viaduct.imp.interpreter.Store;
import edu.cornell.cs.apl.viaduct.imp.parser.Parser;
import edu.cornell.cs.apl.viaduct.imp.parser.TrustConfigurationParser;
import edu.cornell.cs.apl.viaduct.imp.visitors.ImpPdgBuilderVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.PrintVisitor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

public class Main {
  private static void printOrDumpToFile(String filename, String output) {
    Writer writer = null;

    try {
      if (filename != null) {
        File file = new File(filename);
        writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8);
        writer.write(output);

      } else {
        System.out.println(output);
      }

    } catch (Exception e) {
      throw new Error(e);
    } finally {
      try {
        if (writer != null) {
          writer.close();
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  /** Run the compiler. */
  public static void main(String[] args) {
    ArgumentParser argp =
        ArgumentParsers.newFor("viaduct")
            .build()
            .defaultHelp(true)
            .description("Optimizing, extensible MPC compiler.");
    argp.addArgument("file").help("source file to compile");
    argp.addArgument("-hc", "--host").nargs("?").help("host configuration file");
    argp.addArgument("-i", "--interpret")
        .action(Arguments.storeTrue())
        .help("interpret surface program");
    argp.addArgument("-s", "--source")
        .action(Arguments.storeTrue())
        .help("pretty print source program");
    argp.addArgument("-lpdg", "--labelgraph")
        .action(Arguments.storeTrue())
        .help("output PDG with label information");
    argp.addArgument("-ppdg", "--protograph")
        .action(Arguments.storeTrue())
        .help("output PDG with synthesized protocol information");
    argp.addArgument("-f", "--outfile").help("output file");

    final Namespace ns;
    try {
      ns = argp.parseArgs(args);
    } catch (ArgumentParserException e) {
      argp.handleError(e);
      System.out.println(e.getMessage());
      System.exit(-1);
      return;
    }

    final ProgramNode program;
    try {
      final String hostFile = ns.getString("host");
      final HostTrustConfiguration parsedHostConfig;
      if (hostFile != null) {
        parsedHostConfig = TrustConfigurationParser.parse(new File(hostFile));
      } else {
        parsedHostConfig = HostTrustConfiguration.builder().build();
      }

      final String programFile = ns.getString("file");
      final ProgramNode parsedProgram = Parser.parse(new File(programFile));

      program = ProgramNode.builder().addAll(parsedProgram).addHosts(parsedHostConfig).build();
    } catch (Exception e) {
      System.out.println(e.getMessage());
      System.exit(-1);
      return;
    }

    // Pretty Print
    if (ns.getBoolean("source")) {
      printOrDumpToFile(ns.getString("outfile"), new PrintVisitor().run(program));
      return;
    }

    // Interpret
    if (ns.getBoolean("interpret")) {
      Map<ProcessName, Store> stores = Interpreter.run(program);
      boolean first = true;
      for (Map.Entry<ProcessName, Store> entry : stores.entrySet()) {
        if (!first) {
          System.out.println();
        }

        System.out.println("process " + entry.getKey() + ":");
        System.out.println(entry.getValue());

        first = false;
      }
      return;
    }

    ProgramDependencyGraph<ImpAstNode> pdg =
        new ImpPdgBuilderVisitor().generatePDG(program.getProcessCode(ProcessName.getMain()));

    // run data-flow analysis to compute labels for all PDG nodes
    PdgLabelDataflow<ImpAstNode> labelDataFlow = new PdgLabelDataflow<>();
    labelDataFlow.dataflow(pdg.getOrderedNodes());

    // generate DOT graph of PDG with information flow labels
    if (ns.getBoolean("labelgraph")) {
      String labelGraph = PdgDotPrinter.pdgDotGraphWithLabels(pdg);
      printOrDumpToFile(ns.getString("outfile"), labelGraph);
      System.exit(0);
    }

    // run protocol selection given a PDG and host config
    ImpProtocolCostEstimator costEstimator = new ImpProtocolCostEstimator();
    ProtocolSelection<ImpAstNode> protoSelection = new ProtocolSelection<>(costEstimator);
    Map<PdgNode<ImpAstNode>, Protocol<ImpAstNode>> protocolMap =
        protoSelection.selectProtocols(program.getHostTrustConfiguration(), pdg);
    int protocolCost = costEstimator.estimatePdgCost(protocolMap, pdg);

    // generate DOT graph of protocol selection
    if (ns.getBoolean("protograph")) {
      String protoGraph = PdgDotPrinter.pdgDotGraphWithProtocols(pdg, protocolMap);
      printOrDumpToFile(ns.getString("outfile"), protoGraph);
      System.exit(0);
    }

    // protocol synthesized!
    if (pdg.getNodes().size() == protocolMap.size()) {
      ProtocolInstantiation<ImpAstNode> instantiator = new ProtocolInstantiation<>();
      ProgramNode targetProg =
          instantiator.instantiateProtocolConfiguration(
              program.getHostTrustConfiguration(), pdg, protocolMap);
      PrintVisitor printer = new PrintVisitor();
      printOrDumpToFile(ns.getString("outfile"), printer.run(targetProg));

    } else {
      System.out.println("PDG information:");
      boolean synthesizedProto = true;
      for (PdgNode<ImpAstNode> node : pdg.getNodes()) {
        String astStr = node.getAstNode().toString();

        Protocol<ImpAstNode> proto = protocolMap.get(node);
        String protoStr;
        if (proto == null) {
          synthesizedProto = false;
          protoStr = "NO PROTOCOL";
        } else {
          protoStr = proto.toString();
        }

        String labelStr;
        if (node.isDowngradeNode()) {
          labelStr = node.getInLabel().toString() + " / " + node.getOutLabel().toString();
        } else {
          labelStr = node.getOutLabel().toString();
        }

        System.out.println(String.format("%s (label: %s) => %s", astStr, labelStr, protoStr));
      }
      if (synthesizedProto) {
        System.out.println("\nProtocol cost: " + protocolCost);
      } else {
        System.out.println("\nCould not synthesize protocol!");
        System.exit(-1);
      }
    }
  }
}
