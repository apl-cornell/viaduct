package edu.cornell.cs.apl.viaduct.imp.visitors;

import edu.cornell.cs.apl.viaduct.imp.ast.ProcessConfigurationNode;

/** Process configuration visitor. */
public interface ProcessConfigurationVisitor<R> {
  R visit(ProcessConfigurationNode processConfigurationNode);
}
