package io.github.aplcornell.viaduct.runtime.commitment

import io.github.aplcornell.viaduct.runtime.ViaductRuntimeException

/** Thrown when opening a commitment fails. */
class InvalidCommitmentException(value: Any?) : ViaductRuntimeException("Invalid commitment for $value.")
