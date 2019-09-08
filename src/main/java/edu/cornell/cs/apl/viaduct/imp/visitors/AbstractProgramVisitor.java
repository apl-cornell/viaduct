package edu.cornell.cs.apl.viaduct.imp.visitors;

import edu.cornell.cs.apl.viaduct.imp.ast.ProgramNode;
import edu.cornell.cs.apl.viaduct.imp.ast.TopLevelDeclarationNode;
import java.util.LinkedList;
import java.util.List;

/**
 * Skeletal implementation of the {@link ProgramVisitor} interface.
 *
 * <p>See {@link AbstractReferenceVisitor} for a detailed explanation of available methods.
 *
 * @param <SelfT> concrete implementation subclass
 * @param <DeclarationResultT> return type for top level declaration nodes
 */
public abstract class AbstractProgramVisitor<
        SelfT extends AbstractProgramVisitor<SelfT, DeclarationResultT, ProgramResultT>,
        DeclarationResultT,
        ProgramResultT>
    implements ProgramVisitor<ProgramResultT> {

  /** Return the visitor that will be used for top level declarations. */
  protected abstract TopLevelDeclarationVisitor<DeclarationResultT> getDeclarationVisitor();

  /* ENTER  */

  protected abstract SelfT enter(ProgramNode node);

  /* LEAVE  */

  protected abstract ProgramResultT leave(
      ProgramNode node, SelfT visitor, Iterable<DeclarationResultT> declarations);

  /* VISIT  */

  @Override
  public ProgramResultT visit(ProgramNode node) {
    final SelfT visitor = enter(node);
    final List<DeclarationResultT> declarations = new LinkedList<>();

    for (TopLevelDeclarationNode declaration : node) {
      declarations.add(declaration.accept(visitor.getDeclarationVisitor()));
    }

    return leave(node, visitor, declarations);
  }
}
