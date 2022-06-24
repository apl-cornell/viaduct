package edu.cornell.cs.apl.viaduct.backend.zkp

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
