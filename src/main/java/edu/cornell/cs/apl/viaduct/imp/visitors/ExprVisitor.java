package edu.cornell.cs.apl.viaduct.imp.visitors;

import edu.cornell.cs.apl.viaduct.imp.ast.AndNode;
import edu.cornell.cs.apl.viaduct.imp.ast.DowngradeNode;
import edu.cornell.cs.apl.viaduct.imp.ast.EqualToNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LeqNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LessThanNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LiteralNode;
import edu.cornell.cs.apl.viaduct.imp.ast.NotNode;
import edu.cornell.cs.apl.viaduct.imp.ast.OrNode;
import edu.cornell.cs.apl.viaduct.imp.ast.PlusNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReadNode;

/** Expression visitor. */
public interface ExprVisitor<R> {
  R visit(LiteralNode literalNode);

  R visit(ReadNode readNode);

  R visit(NotNode notNode);

  R visit(OrNode orNode);

  R visit(AndNode andNode);

  R visit(EqualToNode eqNode);

  R visit(LessThanNode ltNode);

  R visit(LeqNode leqNode);

  R visit(PlusNode plusNode);

  R visit(DowngradeNode downgradeNode);
}
