import edu.cornell.cs.apl.viaduct.*;

public class Test {
  public void test() {
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
                        e.declassify(e.equals(e.var("shell"), e.var("guess")), Label.BOTTOM)))
            .build();
  }
}
