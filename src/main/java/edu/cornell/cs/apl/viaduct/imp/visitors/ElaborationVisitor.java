package edu.cornell.cs.apl.viaduct.imp.visitors;

import edu.cornell.cs.apl.viaduct.imp.ast.BlockNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ForNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ProgramNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StmtNode;
import edu.cornell.cs.apl.viaduct.imp.ast.WhileNode;
import java.util.ArrayList;
import java.util.List;

/** elaborate derived forms. */
public class ElaborationVisitor extends IdentityVisitor {
  public ExpressionNode run(ExpressionNode expr) {
    return expr.accept(this);
  }

  public StmtNode run(StmtNode stmt) {
    return stmt.accept(this);
  }

  public ProgramNode run(ProgramNode prog) {
    return prog.accept(this);
  }

  /** elaborate for loop into a while loop. */
  @Override
  public StmtNode visit(ForNode forNode) {
    StmtNode newInit = forNode.getInitialize().accept(this);

    List<StmtNode> whileBody = new ArrayList<>();
    whileBody.add(forNode.getBody());
    whileBody.add(forNode.getUpdate());
    StmtNode whileLoop = new WhileNode(forNode.getGuard(), new BlockNode(whileBody));

    List<StmtNode> block = new ArrayList<>();
    block.add(newInit);
    block.add(whileLoop);

    return new BlockNode(block);
  }
}
