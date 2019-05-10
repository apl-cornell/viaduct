package edu.cornell.cs.apl.viaduct;

import edu.cornell.cs.apl.viaduct.imp.ImpProtocolCostEstimator;
import edu.cornell.cs.apl.viaduct.imp.ast.BlockNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StmtNode;
import edu.cornell.cs.apl.viaduct.imp.ast.VarDeclNode;
import edu.cornell.cs.apl.viaduct.imp.builders.ExpressionBuilder;
import edu.cornell.cs.apl.viaduct.imp.builders.StmtBuilder;
import edu.cornell.cs.apl.viaduct.imp.parser.ImpLexer;
import edu.cornell.cs.apl.viaduct.imp.parser.ImpParser;
import edu.cornell.cs.apl.viaduct.imp.visitors.ImpPdgBuilderVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.PrintVisitor;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java_cup.runtime.DefaultSymbolFactory;
import java_cup.runtime.Scanner;
import java_cup.runtime.SymbolFactory;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

public class Main {
  /** create shell game. */
  public static StmtNode shellGame() {
    Label aLabel = Label.and("A");
    Label cLabel = Label.and("C");
    Label acAndLabel = Label.and("A", "C");
    Label acOrLabel = Label.or("A", "C");
    Label cConf_acAndIntegLabel = new Label(cLabel, acAndLabel);
    Label aConf_acAndIntegLabel = new Label(aLabel, acAndLabel);
    Label acOrConf_acAndIntegLabel = new Label(acOrLabel, acAndLabel);
    ExpressionBuilder e = new ExpressionBuilder();
    StmtNode shellGame =
        (new StmtBuilder())
            .varDecl("cinput", cLabel)
            .varDecl("ainput", aLabel)
            .varDecl("shell", cConf_acAndIntegLabel)
            .varDecl("guess", aConf_acAndIntegLabel)
            .varDecl("win", acOrConf_acAndIntegLabel)
            .assign("shell", e.endorse(e.var("cinput"), new Label(cLabel, acAndLabel)))
            .assign("guess", e.endorse(e.var("ainput"), new Label(aLabel, acAndLabel)))
            .cond(
                e.declassify(
                    e.and(e.leq(e.intLit(1), e.var("shell")), e.leq(e.var("shell"), e.intLit(3))),
                    acOrConf_acAndIntegLabel),
                (new StmtBuilder())
                    .assign(
                        "win",
                        e.declassify(
                            e.equals(e.var("shell"), e.var("guess")), acOrConf_acAndIntegLabel)),
                (new StmtBuilder()).skip())
            .build();

    return shellGame;
  }

  /** milionaire's problem. */
  public static StmtNode millionaires() {
    ExpressionBuilder e = new ExpressionBuilder();
    // Label aLabel = new Label(Label.and("A"), Label.and("A","B"));
    // Label bLabel = new Label(Label.and("B"), Label.and("A","B"));
    Label aLabel = Label.and("A");
    Label bLabel = Label.and("B");
    Label abAndLabel = Label.and("A", "B");
    Label abOrLabel = Label.or("A", "B");
    Label abOrConf_abAndIntegLabel = new Label(abOrLabel, abAndLabel);
    StmtNode prog =
        (new StmtBuilder())
            .varDecl("a", aLabel)
            .varDecl("b", bLabel)
            .varDecl("b_richer", abOrConf_abAndIntegLabel)
            .assign(
                "b_richer", e.declassify(e.lt(e.var("a"), e.var("b")), abOrConf_abAndIntegLabel))
            .build();

    return prog;
  }

  private static Set<Host> buildHostConfig(StmtNode hostProg)
      throws Exception
  {
    HashSet<Host> hostConfig = new HashSet<>();
    if (hostProg instanceof BlockNode) {
      BlockNode hostBlock = (BlockNode)hostProg;
      List<StmtNode> stmts = hostBlock.getStatements();
      for (StmtNode stmt : stmts) {
        if (stmt instanceof VarDeclNode) {
          VarDeclNode hostDecl = (VarDeclNode)stmt;
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


  /** main function. */
  public static void main(String[] args) {
    ArgumentParser argp = ArgumentParsers.newFor("viaduct").build()
        .defaultHelp(true)
        .description("Optimizing, extensible MPC compiler.");
    argp.addArgument("file")
        .help("source file to compile");
    argp.addArgument("-hc", "--host").nargs("?").setDefault("hosts.conf")
        .help("host configuration file");
    argp.addArgument("-s", "--source").nargs("?").setConst(true).setDefault(false)
        .help("pretty print source program");
    argp.addArgument("-lpdg", "--labelgraph")
        .nargs("?")
        .setConst(true)
        .setDefault(false)
        .help("output PDG with label information");
    argp.addArgument("-ppdg", "--protograph")
        .nargs("?")
        .setConst(true)
        .setDefault(false)
        .help("output PDG with synthesized protocol information");

    Namespace ns = null;
    try {
      ns = argp.parseArgs(args);
    } catch (ArgumentParserException e) {
      argp.handleError(e);
      System.out.println(e.getMessage());
      System.exit(1);
    }

    SymbolFactory symbolFactory = new DefaultSymbolFactory();

    Set<Host> hostConfig = null;
    try {
      String hostfile = ns.getString("host");
      InputStreamReader reader = new InputStreamReader(new FileInputStream(hostfile), "UTF-8");
      Scanner hostLexer = new ImpLexer(reader, symbolFactory);
      ImpParser hostParser = new ImpParser(hostLexer, symbolFactory);
      StmtNode hostProg = (StmtNode)(hostParser.parse().value);
      hostConfig = buildHostConfig(hostProg);

    } catch (Exception e) {
      System.out.println(e.getMessage());
      System.exit(0);
    }

    StmtNode program = null;
    try {
      String filename = ns.getString("file");
      InputStreamReader reader = new InputStreamReader(new FileInputStream(filename), "UTF-8");
      Scanner progLexer = new ImpLexer(reader, symbolFactory);
      ImpParser progParser = new ImpParser(progLexer, symbolFactory);
      program = (StmtNode)(progParser.parse().value);

    } catch (Exception e) {
      System.out.println(e.getMessage());
      System.exit(0);
    }

    PrintVisitor printer = new PrintVisitor();
    String progStr = program.accept(printer);

    ImpPdgBuilderVisitor pdgBuilder = new ImpPdgBuilderVisitor();
    program.accept(pdgBuilder);
    ProgramDependencyGraph<ImpAstNode> pdg = pdgBuilder.getPdg();

    // run dataflow analysis to compute labels for all PDG nodes
    PdgLabelDataflow<ImpAstNode> labelDataflow = new PdgLabelDataflow<>();
    labelDataflow.dataflow(pdg);

    // run protocol selection given a PDG and host config
    ImpProtocolCostEstimator costEstimator = new ImpProtocolCostEstimator();
    ProtocolSelection<ImpAstNode> protoSelection = new ProtocolSelection<>(costEstimator);
    Map<PdgNode<ImpAstNode>, Protocol<ImpAstNode>> protocolMap =
        protoSelection.selectProtocols(hostConfig, pdg);
    int protocolCost = costEstimator.estimatePdgCost(protocolMap, pdg);

    String labelGraph = PdgDotPrinter.pdgDotGraphWithLabels(pdg);
    String protoGraph = PdgDotPrinter.pdgDotGraphWithProtocols(pdg, protocolMap);

    if (ns.getBoolean("protograph")) {
      System.out.println(protoGraph);
    } else if (ns.getBoolean("labelgraph")) {
      System.out.println(labelGraph);
    } else if (ns.getBoolean("source")) {
      System.out.println(progStr);
    } else {
      System.out.println("source program:");
      System.out.println(progStr);

      System.out.println("PDG information:");
      boolean synthesizedProto = true;
      for (PdgNode<ImpAstNode> node : pdg.getNodes()) {
        String astStr = node.getAstNode().toString();

        Protocol<ImpAstNode> proto = protocolMap.get(node);
        String protoStr = null;
        if (proto == null) {
          synthesizedProto = false;
          protoStr = "NO PROTOCOL";
        } else {
          protoStr = proto.toString();
        }

        String labelStr = "";
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
