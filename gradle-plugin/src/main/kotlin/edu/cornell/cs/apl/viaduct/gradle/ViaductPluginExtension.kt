package edu.cornell.cs.apl.viaduct.gradle

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input

abstract class ViaductPluginExtension {
    @get:Input
    abstract val backends: Property<Int>
}
