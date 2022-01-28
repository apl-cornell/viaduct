package edu.cornell.cs.apl.viaduct.runtime.commitment

import edu.cornell.cs.apl.viaduct.runtime.ViaductRuntimeException

/** Thrown when opening a commitment fails. */
class InvalidCommitmentException(value: Any?) : ViaductRuntimeException("Invalid commitment for $value.")
