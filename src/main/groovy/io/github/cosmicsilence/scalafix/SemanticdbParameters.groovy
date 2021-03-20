package io.github.cosmicsilence.scalafix

import io.github.cosmicsilence.compat.GradleCompat
import org.gradle.api.Project
import org.gradle.api.provider.Property

import javax.inject.Inject


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

    @Inject
    SemanticdbParameters(Project project) {
        autoConfigure = GradleCompat.booleanProperty(project, true)
        version = project.objects.property(String)
    }
}
