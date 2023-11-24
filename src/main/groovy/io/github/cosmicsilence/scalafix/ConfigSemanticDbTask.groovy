package io.github.cosmicsilence.scalafix

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction

/** Configures the SemanticDB compiler plugin during the execution phase to avoid resolving dependencies too early. */
class ConfigSemanticDbTask extends DefaultTask {

    @Input
    ScalaSourceSet sourceSet

    @Input
    final Property<String> scalaVersion = project.objects.property(String)

    @Input
    @Optional
    String semanticDbVersion

    @TaskAction
    void run() {
        def maybeSemanticDbVersion = java.util.Optional.ofNullable(semanticDbVersion)
        def relSourceRoot = sourceSet.getOutputDir().toPath().relativize(project.projectDir.toPath())
        sourceSet.addCompilerPlugin(ScalafixProps.getSemanticDbArtifactCoordinates(scalaVersion.get(), maybeSemanticDbVersion))
        sourceSet.addCompilerOptions([
                // Setting `sourceroot` to the project's absolute path is problematic for large code bases that require
                // aggressive caching: any difference in compiler options between machines forces Gradle to recompile,
                // rather than to download existing compiled artifacts. For that reason, `sourceroot` is set relative
                // to `targetroot`. For more context, see: https://github.com/scalameta/scalameta/issues/2515
                '-P:semanticdb:sourceroot:targetroot:' + relSourceRoot,
                '-Yrangepos'
        ])
    }
}
