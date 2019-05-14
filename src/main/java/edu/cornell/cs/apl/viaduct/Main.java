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
import edu.cornell.cs.apl.viaduct.security.Label;
import edu.cornell.cs.apl.viaduct.security.Principal;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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
  private static final Label Alice = new Label(new Principal("A"));
  private static final Label Bob = new Label(new Principal("B"));
  private static final Label Chuck = new Label(new Principal("C"));

  /** The cups and balls game. */
  public static StmtNode shellGame() {
    final Label aliceAndChuck = Alice.and(Chuck);
    final Label aliceOrChuck = Alice.or(Chuck);
    final Label shellLabel = Chuck.confidentiality().and(aliceAndChuck.integrity());
    final Label guessLabel = Alice.confidentiality().and(aliceAndChuck.integrity());
    final Label winLabel = aliceOrChuck.confidentiality().and(aliceAndChuck.integrity());
    ExpressionBuilder e = new ExpressionBuilder();
    return new StmtBuilder()
        .varDecl("cinput", Chuck)
        .varDecl("ainput", Alice)
        .varDecl("shell", shellLabel)
        .varDecl("guess", guessLabel)
        .varDecl("win", winLabel)
        .assign("shell", e.endorse(e.var("cinput"), shellLabel))
        .assign("guess", e.endorse(e.var("ainput"), guessLabel))
        .cond(
            e.declassify(
                e.and(e.leq(e.intLit(1), e.var("shell")), e.leq(e.var("shell"), e.intLit(3))),
                winLabel),
            (new StmtBuilder())
                .assign("win", e.declassify(e.equals(e.var("shell"), e.var("guess")), winLabel)),
            (new StmtBuilder()).skip())
        .build();
  }

  /** The millionaire's problem. */
  public static StmtNode millionaire() {
    ExpressionBuilder e = new ExpressionBuilder();
    final Label aliceAndBob = Alice.and(Bob);
    final Label aliceOrBob = Alice.or(Bob);
    final Label resultLabel = aliceOrBob.confidentiality().and(aliceAndBob.integrity());
    return new StmtBuilder()
        .varDecl("a", Alice)
        .varDecl("b", Bob)
        .varDecl("b_richer", resultLabel)
        .assign("b_richer", e.declassify(e.lt(e.var("a"), e.var("b")), resultLabel))
        .build();
  }

  private static Set<Host> buildHostConfig(StmtNode hostProg) throws Exception {
    HashSet<Host> hostConfig = new HashSet<>();
    if (hostProg instanceof BlockNode) {
      BlockNode hostBlock = (BlockNode) hostProg;
      List<StmtNode> stmts = hostBlock.getStatements();
      for (StmtNode stmt : stmts) {
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
    argp.addArgument("-s", "--source")
        .nargs("?")
        .setConst(true)
        .setDefault(false)
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
      InputStreamReader reader =
          new InputStreamReader(new FileInputStream(hostfile), StandardCharsets.UTF_8);
      Scanner hostLexer = new ImpLexer(reader, symbolFactory);
      ImpParser hostParser = new ImpParser(hostLexer, symbolFactory);
      StmtNode hostProg = (StmtNode) (hostParser.parse().value);
      hostConfig = buildHostConfig(hostProg);

    } catch (Exception e) {
      System.out.println(e.getMessage());
      System.exit(0);
    }

    StmtNode program = null;
    try {
      String filename = ns.getString("file");
      InputStreamReader reader =
          new InputStreamReader(new FileInputStream(filename), StandardCharsets.UTF_8);
      Scanner progLexer = new ImpLexer(reader, symbolFactory);
      ImpParser progParser = new ImpParser(progLexer, symbolFactory);
      program = (StmtNode) (progParser.parse().value);

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
