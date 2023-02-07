package io.github.aplcornell.viaduct.backends.cleartext

import io.github.aplcornell.viaduct.syntax.Protocol

abstract class Cleartext : Protocol() {
    companion object {
        const val INPUT = "INPUT"
        const val HASH_COMMITMENT_INPUT = "HASH_COMMITMENT_INPUT"
        const val CLEARTEXT_COMMITMENT_INPUT = "CLEARTEXT_COMMITMENT_INPUT"
        const val OUTPUT = "OUTPUT"
    }
}
