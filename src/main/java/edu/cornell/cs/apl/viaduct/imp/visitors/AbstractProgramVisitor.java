package edu.cornell.cs.apl.viaduct.imp.visitors;

import edu.cornell.cs.apl.viaduct.imp.ast.ProcessName;
import edu.cornell.cs.apl.viaduct.imp.ast.ProgramNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StatementNode;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import java.util.LinkedList;
import java.util.List;

/**
 * Skeletal implementation of the {@link ProgramVisitor} interface.
 *
 * <p>See {@link AbstractReferenceVisitor} for a detailed explanation of available methods.
 *
 * @param <SelfT> concrete implementation subclass
 * @param <StmtResultT> return type for statement nodes
 */
public abstract class AbstractProgramVisitor<
        SelfT extends AbstractProgramVisitor<SelfT, StmtResultT, ProgramResultT>,
        StmtResultT,
        ProgramResultT>
    implements ProgramVisitor<ProgramResultT> {

  /** Return the visitor that will be used for statement sub-nodes. */
  protected abstract StmtVisitor<StmtResultT> getStatementVisitor();

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
      processes.add(Tuple.of(process._1(), process._2().accept(visitor.getStatementVisitor())));
    }

    return leave(node, processes);
  }
}
