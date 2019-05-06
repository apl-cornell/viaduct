package edu.cornell.cs.apl.viaduct;

import edu.cornell.cs.apl.viaduct.imp.ast.ImpAstNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StmtNode;
import edu.cornell.cs.apl.viaduct.imp.builders.ExpressionBuilder;
import edu.cornell.cs.apl.viaduct.imp.builders.StmtBuilder;
import edu.cornell.cs.apl.viaduct.imp.visitors.ImpPdgBuilderVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.PrintVisitor;
import org.junit.Test;

public class ShellGameTest {
  @Test
  public void testShellGame() {
    ExpressionBuilder e = new ExpressionBuilder();
    StmtNode shellGame =
        (new StmtBuilder())
            .varDecl("cinput", Label.bottom())
            .varDecl("ainput", Label.bottom())
            .varDecl("shell", Label.bottom())
            .varDecl("guess", Label.bottom())
            .varDecl("win", Label.bottom())
            .assign("shell", e.endorse(e.var("cinput"), Label.bottom()))
            .assign("guess", e.endorse(e.var("ainput"), Label.bottom()))
            .cond(
                e.declassify(
                    e.and(e.leq(e.intLit(1), e.var("shell")), e.leq(e.var("shell"), e.intLit(3))),
                    Label.bottom()),
                (new StmtBuilder())
                    .assign(
                        "win",
                        e.declassify(e.equals(e.var("shell"), e.var("guess")), Label.bottom())),
                (new StmtBuilder()).skip())
            .build();

    // print out the AST
    PrintVisitor print = new PrintVisitor();
    String shellGameStr = shellGame.accept(print);
    System.out.println(shellGameStr);

    // build PDG for the shell game
    ImpPdgBuilderVisitor pdgBuilder = new ImpPdgBuilderVisitor();
    shellGame.accept(pdgBuilder);
    ProgramDependencyGraph<ImpAstNode> pdg = pdgBuilder.getPdg();
    System.out.println(pdg.toString());
  }
}
