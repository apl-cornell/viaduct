package edu.cornell.cs.apl.viaduct;

import edu.cornell.cs.apl.viaduct.imp.HostTrustConfiguration;
import edu.cornell.cs.apl.viaduct.imp.ImpProtocolCostEstimator;
import edu.cornell.cs.apl.viaduct.imp.ast.Host;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ProcessConfigurationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StmtNode;
import edu.cornell.cs.apl.viaduct.imp.interpreter.Interpreter;
import edu.cornell.cs.apl.viaduct.imp.interpreter.Store;
import edu.cornell.cs.apl.viaduct.imp.parser.Parser;
import edu.cornell.cs.apl.viaduct.imp.parser.TrustConfigurationParser;
import edu.cornell.cs.apl.viaduct.imp.visitors.ImpPdgBuilderVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.PrintVisitor;
import java.io.File;
import java.util.Map;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

public class Main {
  /** Run the compiler. */
  public static void main(String[] args) {
    ArgumentParser argp =
        ArgumentParsers.newFor("viaduct")
            .build()
            .defaultHelp(true)
            .description("Optimizing, extensible MPC compiler.");
    argp.addArgument("file").help("source file to compile");
    argp.addArgument("-hc", "--host")
        .nargs("?")
        .setDefault("hosts.conf")
        .help("host configuration file");
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

    Namespace ns;
    try {
      ns = argp.parseArgs(args);
    } catch (ArgumentParserException e) {
      argp.handleError(e);
      System.out.println(e.getMessage());
      return;
    }

    HostTrustConfiguration hostConfig;
    try {
      String hostFile = ns.getString("host");
      hostConfig = TrustConfigurationParser.parse(new File(hostFile));
    } catch (Exception e) {
      System.out.println(e.getMessage());
      return;
    }

    ImpAstNode program;
    try {
      String sourceFile = ns.getString("file");
      program = Parser.parse(new File(sourceFile));
    } catch (Exception e) {
      System.out.println(e.getMessage());
      return;
    }

    // Pretty Print
    if (ns.getBoolean("source")) {
      System.out.println(new PrintVisitor().run(program));
      return;
    }

    // Interpret
    if (ns.getBoolean("interpret")) {
      Interpreter interpreter = new Interpreter();
      if (program instanceof StmtNode) {
        Store store = interpreter.run((StmtNode) program);
        System.out.println(store);
      } else if (program instanceof ProcessConfigurationNode) {
        Map<Host, Store> stores = interpreter.run((ProcessConfigurationNode) program);
        for (Map.Entry<Host, Store> entry : stores.entrySet()) {
          System.out.println("host " + entry.getKey() + ":");
          System.out.println(entry.getValue());
          System.out.println();
        }
      }
      return;
    }

    if (!(program instanceof StmtNode)) {
      return;
    }

    ProgramDependencyGraph<ImpAstNode> pdg =
        new ImpPdgBuilderVisitor().generatePDG((StmtNode) program);

    // run data-flow analysis to compute labels for all PDG nodes
    PdgLabelDataflow<ImpAstNode> labelDataFlow = new PdgLabelDataflow<>();
    labelDataFlow.dataflow(pdg);

    // generate DOT graph of PDG with information flow labels
    if (ns.getBoolean("labelgraph")) {
      String labelGraph = PdgDotPrinter.pdgDotGraphWithLabels(pdg);
      System.out.println(labelGraph);
      System.exit(0);
    }

    // run protocol selection given a PDG and host config
    ImpProtocolCostEstimator costEstimator = new ImpProtocolCostEstimator();
    ProtocolSelection<ImpAstNode> protoSelection = new ProtocolSelection<>(costEstimator);
    Map<PdgNode<ImpAstNode>, Protocol<ImpAstNode>> protocolMap =
        protoSelection.selectProtocols(hostConfig, pdg);
    int protocolCost = costEstimator.estimatePdgCost(protocolMap, pdg);

    // generate DOT graph of protocol selection
    if (ns.getBoolean("protograph")) {
      String protoGraph = PdgDotPrinter.pdgDotGraphWithProtocols(pdg, protocolMap);
      System.out.println(protoGraph);
      System.exit(0);
    }

    // protocol synthesized!
    if (pdg.getNodes().size() == protocolMap.size()) {
      ProtocolInstantiation<ImpAstNode> instantiator = new ProtocolInstantiation<>();
      ProcessConfigurationNode targetProg =
          instantiator.instantiateProtocolConfiguration(hostConfig, pdg, protocolMap);
      PrintVisitor printer = new PrintVisitor();
      System.out.println(printer.run(targetProg));

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
      }
    }
  }
}
