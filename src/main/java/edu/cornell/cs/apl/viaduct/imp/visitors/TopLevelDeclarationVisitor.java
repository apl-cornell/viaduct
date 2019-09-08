package edu.cornell.cs.apl.viaduct.imp.visitors;

import edu.cornell.cs.apl.viaduct.imp.ast.HostDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ProcessDeclarationNode;

/** Top level declaration visitor. */
public interface TopLevelDeclarationVisitor<R> {
  R visit(ProcessDeclarationNode processDeclarationNode);

  R visit(HostDeclarationNode hostDeclarationNode);
}
