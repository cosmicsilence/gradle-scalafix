package io.github.cosmicsilence.scalafix

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property


class SemanticdbParameters {

    /**
     * Auto configures the SemanticDB Scala compiler. This is required to run
     * semantic rules.
     */
    final Property<Boolean> autoConfigure

    /**
     * Used to override the version of the SemanticDB compiler plugin. By default
     * this plugin uses a version that is guaranteed to be compatible with Scalafix.
     */
    final Property<String> version

    SemanticdbParameters(ObjectFactory objectFactory) {
        autoConfigure = objectFactory.property(Boolean).convention(true)
        version = objectFactory.property(String)
    }
}
