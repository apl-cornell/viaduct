package edu.cornell.cs.apl.viaduct.imp.visitors;

import edu.cornell.cs.apl.viaduct.imp.ast.HostDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ProcessDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.TopLevelDeclarationNode;

/**
 * Skeletal implementation of the {@link TopLevelDeclarationVisitor} interface.
 *
 * <p>See {@link AbstractReferenceVisitor} for a detailed explanation of available methods.
 *
 * @param <SelfT> concrete implementation subclass
 * @param <StmtResultT> return type for statement nodes
 * @param <DeclarationResultT> return type for declaration nodes
 */
public abstract class AbstractTopLevelDeclarationVisitor<
        SelfT extends AbstractTopLevelDeclarationVisitor<SelfT, StmtResultT, DeclarationResultT>,
        StmtResultT,
        DeclarationResultT>
    implements TopLevelDeclarationVisitor<DeclarationResultT> {

  /** Return the visitor that will be used for statement sub-nodes. */
  protected abstract StmtVisitor<StmtResultT> getStatementVisitor();

  /* ENTER  */

  protected abstract SelfT enter(TopLevelDeclarationNode node);

  protected SelfT enter(ProcessDeclarationNode node) {
    return enter((TopLevelDeclarationNode) node);
  }

  protected SelfT enter(HostDeclarationNode node) {
    return enter((TopLevelDeclarationNode) node);
  }

  /* LEAVE  */

  protected DeclarationResultT leave(TopLevelDeclarationNode node, SelfT visitor) {
    throw new MissingCaseError(node);
  }

  protected DeclarationResultT leave(ProcessDeclarationNode node, SelfT visitor, StmtResultT body) {
    return leave((TopLevelDeclarationNode) node, visitor);
  }

  protected DeclarationResultT leave(HostDeclarationNode node, SelfT visitor) {
    return leave((TopLevelDeclarationNode) node, visitor);
  }

  /* VISIT  */

  @Override
  public DeclarationResultT visit(ProcessDeclarationNode node) {
    final SelfT visitor = enter(node);
    final StmtResultT body = node.getBody().accept(visitor.getStatementVisitor());
    return leave(node, visitor, body);
  }

  @Override
  public DeclarationResultT visit(HostDeclarationNode node) {
    final SelfT visitor = enter(node);
    return leave(node, visitor);
  }
}
