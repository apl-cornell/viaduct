package edu.cornell.cs.apl.viaduct;

import edu.cornell.cs.apl.viaduct.imp.ImpProtocolCostEstimator;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StmtNode;
import edu.cornell.cs.apl.viaduct.imp.builders.ExpressionBuilder;
import edu.cornell.cs.apl.viaduct.imp.builders.StmtBuilder;
import edu.cornell.cs.apl.viaduct.imp.visitors.ImpPdgBuilderVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.PrintVisitor;

import java.util.HashSet;
import java.util.Map;

public class Main {
  /** create shell game. */
  public static StmtNode shellGame() {
    Label aLabel = Label.and("A");
    Label cLabel = Label.and("C");
    Label acAndLabel = Label.and("A","C");
    Label acOrLabel = Label.or("A","C");
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
                        e.declassify(e.equals(e.var("shell"), e.var("guess")),
                            acOrConf_acAndIntegLabel)),
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
        .assign("b_richer",
            e.declassify(e.lt(e.var("a"), e.var("b")), abOrConf_abAndIntegLabel))
        .build();

    return prog;
  }


  /** entry method. */
  public static void main(String[] args) {
    // PrintVisitor print = new PrintVisitor();
    StmtNode program = shellGame();

    // print out the AST
    // String shellGameStr = shellGame.accept(print);
    // System.out.println(shellGameStr);

    // build PDG for the shell game
    ImpPdgBuilderVisitor pdgBuilder = new ImpPdgBuilderVisitor();
    program.accept(pdgBuilder);
    ProgramDependencyGraph<ImpAstNode> pdg = pdgBuilder.getPdg();

    // run dataflow analysis to compute labels for all PDG nodes
    PdgLabelDataflow<ImpAstNode> labelDataflow = new PdgLabelDataflow<>();
    labelDataflow.dataflow(pdg);

    System.out.println("PDG:");
    System.out.println(PdgDotPrinter.pdgDotGraphWithLabels(pdg));

    // host configration
    HashSet<Host> hostConfig = new HashSet<>();
    // hostConfig.add(new Host("God", new Label(Label.top(),Label.bottom())));
    hostConfig.add(new Host("Alice", Label.and("A")));
    // hostConfig.add(new Host("Bob", Label.and("B")));
    hostConfig.add(new Host("Chuck", Label.and("C")));

    // run protocol selection given a PDG and host config
    ImpProtocolCostEstimator costEstimator = new ImpProtocolCostEstimator();
    ProtocolSelection<ImpAstNode> protoSelection = new ProtocolSelection<>(costEstimator);
    Map<PdgNode<ImpAstNode>,Protocol<ImpAstNode>> protocolMap =
        protoSelection.selectProtocols(hostConfig, pdg);

    if (protocolMap != null) {
      System.out.println("synthesized protocol:");
      System.out.println(PdgDotPrinter.pdgDotGraphWithProtocols(pdg, protocolMap));
      System.out.println("total cost: " + costEstimator.estimatePdgCost(protocolMap, pdg));
    } else {
      System.out.println("Could not synthesize protocol!");
    }
  }
}
