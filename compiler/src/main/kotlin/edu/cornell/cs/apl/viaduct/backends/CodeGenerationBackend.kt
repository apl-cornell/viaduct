package edu.cornell.cs.apl.viaduct.backends

import edu.cornell.cs.apl.viaduct.backends.cleartext.CleartextBackend
import edu.cornell.cs.apl.viaduct.backends.commitment.CommitmentBackend

/** Combines all back ends that support code generation. */
val CodeGenerationBackend: Backend = listOf(CleartextBackend, CommitmentBackend).unions()
