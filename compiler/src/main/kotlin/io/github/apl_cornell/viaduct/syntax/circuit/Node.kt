package io.github.apl_cornell.viaduct.syntax.circuit

import io.github.apl_cornell.viaduct.prettyprinting.PrettyPrintable
import io.github.apl_cornell.viaduct.syntax.HasSourceLocation

sealed class Node : HasSourceLocation, PrettyPrintable
