package edu.cornell.cs.apl.viaduct;

import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StmtNode;
import edu.cornell.cs.apl.viaduct.imp.builders.ExpressionBuilder;
import edu.cornell.cs.apl.viaduct.imp.builders.StmtBuilder;
import edu.cornell.cs.apl.viaduct.imp.visitors.ImpPdgBuilderVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.PrintVisitor;

public class Main {
  /** create shell game. */
  public static StmtNode getShellGame() {
    ExpressionBuilder e = new ExpressionBuilder();
    StmtNode shellGame =
        (new StmtBuilder())
            .varDecl("cinput", Label.BOTTOM)
            .varDecl("ainput", Label.BOTTOM)
            .varDecl("shell", Label.BOTTOM)
            .varDecl("guess", Label.BOTTOM)
            .varDecl("win", Label.BOTTOM)
            .assign("shell", e.endorse(e.var("cinput"), Label.BOTTOM))
            .assign("guess", e.endorse(e.var("ainput"), Label.BOTTOM))
            .cond(
                e.declassify(
                    e.and(e.leq(e.intLit(1), e.var("shell")), e.leq(e.var("shell"), e.intLit(3))),
                    Label.BOTTOM),
                (new StmtBuilder())
                    .assign(
                        "win",
                        e.declassify(e.equals(e.var("shell"), e.var("guess")), Label.BOTTOM)),
                (new StmtBuilder()).skip())
            .build();

    return shellGame;
  }

  /** entry method. */
  public static void main(String[] args) {
    PrintVisitor print = new PrintVisitor();
    StmtNode shellGame = getShellGame();

    // print out the AST
    String shellGameStr = shellGame.accept(print);
    System.out.println(shellGameStr);

    // build PDG for the shell game
    ImpPdgBuilderVisitor pdgBuilder = new ImpPdgBuilderVisitor();
    shellGame.accept(pdgBuilder);
    ProgramDependencyGraph<ImpAstNode> pdg = pdgBuilder.getPdg();
    System.out.println(pdg.toString());
  }
}
