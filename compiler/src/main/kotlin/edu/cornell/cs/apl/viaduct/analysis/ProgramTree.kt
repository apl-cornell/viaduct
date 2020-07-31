package edu.cornell.cs.apl.viaduct.analysis

import edu.cornell.cs.apl.attributes.Tree
import edu.cornell.cs.apl.attributes.attribute
import edu.cornell.cs.apl.viaduct.syntax.intermediate.Node
import edu.cornell.cs.apl.viaduct.syntax.intermediate.ProgramNode

/** Returns the [Tree] instance for the program. */
val ProgramNode.tree: Tree<Node, ProgramNode> by attribute { Tree(this) }
