package io.github.apl_cornell.viaduct.backends

import io.github.apl_cornell.viaduct.backends.aby.ABYBackend
import io.github.apl_cornell.viaduct.backends.cleartext.CleartextBackend

/** Combines all back ends that support circuit code generation. */
object CircuitCodeGenerationBackend : Backend by listOf(CleartextBackend, ABYBackend).unions()
