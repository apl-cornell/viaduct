package io.github.aplcornell.viaduct.backend.zkp

class LibsnarkJNI {
    external fun sayHello()

    companion object {
        init {
            try {
                System.loadLibrary("snarkwrapper")
            } catch (e: java.io.IOException) {
                throw Error(e)
            }
        }
    }
}
