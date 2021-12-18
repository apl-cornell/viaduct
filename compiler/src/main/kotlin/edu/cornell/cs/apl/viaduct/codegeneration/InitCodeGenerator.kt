package edu.cornell.cs.apl.viaduct.codegeneration

import com.squareup.kotlinpoet.PropertySpec
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.Protocol

interface InitCodeGenerator {
    fun setup(protocol: Protocol, host: Host): PropertySpec
}
