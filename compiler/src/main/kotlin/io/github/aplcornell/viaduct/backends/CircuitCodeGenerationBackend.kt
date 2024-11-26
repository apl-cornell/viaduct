package io.github.aplcornell.viaduct.backends

import io.github.aplcornell.viaduct.backends.aby.ABYBackend
import io.github.aplcornell.viaduct.backends.cleartext.CleartextBackend
import io.github.aplcornell.viaduct.backends.commitment.CommitmentBackend

/** Combines all back ends that support circuit code generation. */
object CircuitCodeGenerationBackend : Backend by listOf(CleartextBackend, ABYBackend, CommitmentBackend).unions()
