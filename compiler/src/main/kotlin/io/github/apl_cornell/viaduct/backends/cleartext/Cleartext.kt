package io.github.apl_cornell.viaduct.backends.cleartext

import io.github.apl_cornell.viaduct.syntax.Protocol

abstract class Cleartext : Protocol() {
    companion object {
        const val INPUT = "INPUT"
        const val HASH_COMMITMENT_INPUT = "HASH_COMMITMENT_INPUT"
        const val CLEARTEXT_COMMITMENT_INPUT = "CLEARTEXT_COMMITMENT_INPUT"
        const val OUTPUT = "OUTPUT"
    }
}
