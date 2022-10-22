package io.github.apl_cornell.viaduct.runtime.commitment

import io.github.apl_cornell.viaduct.runtime.ViaductRuntimeException

/** Thrown when opening a commitment fails. */
class InvalidCommitmentException(value: Any?) : ViaductRuntimeException("Invalid commitment for $value.")
