package io.github.aplcornell.viaduct.syntax.precircuit

import io.github.aplcornell.viaduct.attributes.TreeNode
import io.github.aplcornell.viaduct.prettyprinting.PrettyPrintable
import io.github.aplcornell.viaduct.syntax.HasSourceLocation

sealed class Node : TreeNode<Node>, HasSourceLocation, PrettyPrintable
