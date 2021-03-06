package edu.cornell.cs.apl.viaduct.protocols

import edu.cornell.cs.apl.viaduct.syntax.Protocol

abstract class Plaintext : Protocol() {
    companion object {
        const val INPUT = "INPUT"
        const val HASH_COMMITMENT_INPUT = "HASH_COMMITMENT_INPUT"
        const val CLEARTEXT_COMMITMENT_INPUT = "CLEARTEXT_COMMITMENT_INPUT"
        const val OUTPUT = "OUTPUT"
    }
}
