package edu.cornell.cs.apl.viaduct;

import edu.cornell.cs.apl.viaduct.imp.ImpProtocolCostEstimator;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StmtNode;
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
import java.util.Map;
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

  /** Run the compiler. */
  public static void main(String[] args) {
    ArgumentParser argp =
        ArgumentParsers.newFor("viaduct")
            .build()
            .defaultHelp(true)
            .description("Optimizing, extensible MPC compiler.");
    argp.addArgument("file").help("source file to compile");
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

    StmtNode program = null;
    try {
      String filename = ns.getString("file");
      InputStreamReader reader =
          new InputStreamReader(new FileInputStream(filename), StandardCharsets.UTF_8);
      SymbolFactory symbolFactory = new DefaultSymbolFactory();
      Scanner lexer = new ImpLexer(reader, symbolFactory);
      ImpParser parser = new ImpParser(lexer, symbolFactory);
      program = (StmtNode) (parser.parse().value);
    } catch (Exception e) {
      System.out.println(e.getMessage());
      System.exit(0);
    }

    ImpPdgBuilderVisitor pdgBuilder = new ImpPdgBuilderVisitor();
    program.accept(pdgBuilder);
    ProgramDependencyGraph<ImpAstNode> pdg = pdgBuilder.getPdg();

    // run dataflow analysis to compute labels for all PDG nodes
    PdgLabelDataflow<ImpAstNode> labelDataflow = new PdgLabelDataflow<>();
    labelDataflow.dataflow(pdg);

    // host configration
    HashSet<Host> hostConfig = new HashSet<>();
    hostConfig.add(new Host("God", Label.bottom()));
    hostConfig.add(new Host("Alice", Alice));
    // hostConfig.add(new Host("Chuck", Label.and("C")));

    // run protocol selection given a PDG and host config
    ImpProtocolCostEstimator costEstimator = new ImpProtocolCostEstimator();
    ProtocolSelection<ImpAstNode> protoSelection = new ProtocolSelection<>(costEstimator);
    Map<PdgNode<ImpAstNode>, Protocol<ImpAstNode>> protocolMap =
        protoSelection.selectProtocols(hostConfig, pdg);
    int protocolCost = costEstimator.estimatePdgCost(protocolMap, pdg);

    PrintVisitor printer = new PrintVisitor();
    String progStr = program.accept(printer);
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
