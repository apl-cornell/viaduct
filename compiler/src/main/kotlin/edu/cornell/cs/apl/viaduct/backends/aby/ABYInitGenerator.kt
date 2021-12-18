package edu.cornell.cs.apl.viaduct.backends.aby

import com.github.apl_cornell.aby.ABYParty
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import edu.cornell.cs.apl.viaduct.codegeneration.InitCodeGenerator
import edu.cornell.cs.apl.viaduct.codegeneration.getRole
import edu.cornell.cs.apl.viaduct.syntax.Host
import edu.cornell.cs.apl.viaduct.syntax.Protocol

class ABYInitGenerator : InitCodeGenerator {
    override fun setup(protocol: Protocol, host: Host): PropertySpec =
        PropertySpec.builder("ABYParty${protocol.protocolName.name}", ABYParty::class)
            .initializer(
                "ABYParty(%L, %L, %L, %L, %L)",
                getRole(protocol, host),
                "127.0.0.1", // TODO() - implement passing IP address
                "8000", // TODO() - implement passing port
                "Aby.getLT()", // TODO() - how to make this not hard-coded
                32 // TODO() - where is best place to store this value?
            )
            .addModifiers(KModifier.PRIVATE)
            .build()
}
