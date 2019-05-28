package edu.cornell.cs.apl.viaduct;

import edu.cornell.cs.apl.viaduct.imp.ImpProtocolCostEstimator;
import edu.cornell.cs.apl.viaduct.imp.ast.BlockNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpValue;
import edu.cornell.cs.apl.viaduct.imp.ast.StmtNode;
import edu.cornell.cs.apl.viaduct.imp.ast.VarDeclNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;
import edu.cornell.cs.apl.viaduct.imp.parser.Parser;
import edu.cornell.cs.apl.viaduct.imp.visitors.ImpAnnotationVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.ImpPdgBuilderVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.InterpVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.PrintVisitor;
import edu.cornell.cs.apl.viaduct.security.Label;
import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

public class Main {
  private static Set<Host> buildHostConfig(StmtNode hostProg) throws Exception {
    HashSet<Host> hostConfig = new HashSet<>();
    if (hostProg instanceof BlockNode) {
      BlockNode hostBlock = (BlockNode) hostProg;
      for (StmtNode stmt : hostBlock) {
        if (stmt instanceof VarDeclNode) {
          VarDeclNode hostDecl = (VarDeclNode) stmt;
          String hostName = hostDecl.getVariable().toString();
          Label hostLabel = hostDecl.getLabel();
          hostConfig.add(new Host(hostName, hostLabel));
        } else {
          throw new Exception("Invalid host configuration");
        }
      }
    } else {
      throw new Exception("Invalid host configuration");
    }

    return hostConfig;
  }

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

    Set<Host> hostConfig;
    try {
      String hostFile = ns.getString("host");
      StmtNode hostProg = Parser.parse(new File(hostFile));
      hostConfig = buildHostConfig(hostProg);
    } catch (Exception e) {
      System.out.println(e.getMessage());
      return;
    }

    StmtNode program;
    try {
      String sourceFile = ns.getString("file");
      program = Parser.parse(new File(sourceFile));
    } catch (Exception e) {
      System.out.println(e.getMessage());
      return;
    }

    // process annotations
    ImpAnnotationVisitor annotator = new ImpAnnotationVisitor();
    program.accept(annotator);

    // interpret source
    if (ns.getBoolean("interpret")) {
      InterpVisitor interpreter = new InterpVisitor();
      try {
        Map<Host, Map<Variable, ImpValue>> storeMap = interpreter.interpret(program);

        for (Map.Entry<Host, Map<Variable, ImpValue>> kv : storeMap.entrySet()) {
          Map<Variable, ImpValue> store = kv.getValue();

          System.out.println("store: " + kv.getKey());
          for (Map.Entry<Variable, ImpValue> kvStore : store.entrySet()) {
            String str = String.format("%s => %s", kvStore.getKey().toString(), kvStore.getValue());
            System.out.println(str);
          }
        }
        return;

      } catch (Exception e) {
        System.out.println(e.getMessage());
        System.exit(0);
      }
    }

    // pretty print source
    if (ns.getBoolean("source")) {
      PrintVisitor printer = new PrintVisitor();
      String progStr = program.accept(printer);
      System.out.println(progStr);
      return;
    }

    ImpPdgBuilderVisitor pdgBuilder = new ImpPdgBuilderVisitor();
    ProgramDependencyGraph<ImpAstNode> pdg = pdgBuilder.generatePDG(program);

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

    // otherwise, print out protocol selection info
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
