package edu.cornell.cs.apl.viaduct.imp.visitors;

import edu.cornell.cs.apl.viaduct.imp.ast.ProcessName;
import edu.cornell.cs.apl.viaduct.imp.ast.ProgramNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StatementNode;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import java.util.LinkedList;
import java.util.List;

public abstract class AbstractProgramVisitor<
        SelfT extends
            AbstractProgramVisitor<
                    SelfT, ReferenceResultT, ExprResultT, StmtResultT, ProgramResultT>,
        ReferenceResultT,
        ExprResultT,
        StmtResultT,
        ProgramResultT>
    extends AbstractStmtVisitor<SelfT, ReferenceResultT, ExprResultT, StmtResultT>
    implements ProgramVisitor<ProgramResultT> {

  public final ProgramResultT traverse(ProgramNode node) {
    return node.accept(this);
  }

  /* ENTER  */

  /** Enter a process declaration. */
  protected abstract SelfT enter(ProcessName process, StatementNode body);

  /* LEAVE  */

  protected abstract ProgramResultT leave(
      ProgramNode node, Iterable<Tuple2<ProcessName, StmtResultT>> processes);

  /* VISIT  */

  @Override
  public ProgramResultT visit(ProgramNode node) {
    final List<Tuple2<ProcessName, StmtResultT>> processes = new LinkedList<>();

    for (Tuple2<ProcessName, StatementNode> process : node) {
      final SelfT visitor = enter(process._1(), process._2());
      processes.add(Tuple.of(process._1(), visitor.traverse(process._2())));
    }

    return leave(node, processes);
  }
}
