package edu.cornell.cs.apl.viaduct;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ShellGameTest {
  @Test
  public void testShellGame() {
    ExprBuilder e = new ExprBuilder();
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

    // print out the AST
    PrintVisitor print = new PrintVisitor();
    String shellGameStr = shellGame.accept(print);
    System.out.println(shellGameStr);

    // build PDG for the shell game
    PdgBuilderVisitor pdgBuilder = new PdgBuilderVisitor();
    shellGame.accept(pdgBuilder);
    ProgramDependencyGraph pdg = pdgBuilder.getPdg();
    System.out.println(pdg.toString());
  }
}
