package edu.cornell.cs.apl.viaduct.gradle

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input

abstract class ViaductPluginExtension {
    // TODO: this will probably track available back ends.
    @get:Input
    abstract val backends: Property<Int>
}
