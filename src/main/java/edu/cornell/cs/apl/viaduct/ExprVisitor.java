package edu.cornell.cs.apl.viaduct;

import edu.cornell.cs.apl.viaduct.surface.AndNode;
import edu.cornell.cs.apl.viaduct.surface.BooleanLiteralNode;
import edu.cornell.cs.apl.viaduct.surface.DowngradeNode;
import edu.cornell.cs.apl.viaduct.surface.EqualNode;
import edu.cornell.cs.apl.viaduct.surface.IntegerLiteralNode;
import edu.cornell.cs.apl.viaduct.surface.LeqNode;
import edu.cornell.cs.apl.viaduct.surface.LessThanNode;
import edu.cornell.cs.apl.viaduct.surface.NotNode;
import edu.cornell.cs.apl.viaduct.surface.OrNode;
import edu.cornell.cs.apl.viaduct.surface.PlusNode;
import edu.cornell.cs.apl.viaduct.surface.ReadNode;

/** Expression visitor. */
public interface ExprVisitor<R> {
  R visit(ReadNode readNode);

  R visit(IntegerLiteralNode integerLiteralNode);

  R visit(PlusNode plusNode);

  R visit(BooleanLiteralNode booleanLiteralNode);

  R visit(OrNode orNode);

  R visit(AndNode andNode);

  R visit(LessThanNode ltNode);

  R visit(EqualNode eqNode);

  R visit(LeqNode leqNode);

  R visit(NotNode notNode);

  R visit(DowngradeNode downgradeNode);
}
