package io.github.cosmicsilence.scalafix

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction

/** Configures the SemanticDB compiler plugin during the execution phase to avoid resolving dependencies too early. */
class ConfigSemanticDbTask extends DefaultTask {

    @Input
    final Property<String> sourceSetName = project.objects.property(String)

    @Input
    final Property<String> scalaVersion = project.objects.property(String)

    @Input
    @Optional
    String semanticDbVersion

    @TaskAction
    void run() {
        final ScalaSourceSet sourceSet = new ScalaSourceSet(project, project.sourceSets.findByName(sourceSetName.get()))
        if (isScala3()) {
            // It's currently not possible to set `-sourceroot` in a fully cache-friendly way (see comment below):
            // https://github.com/gradle/gradle/issues/27161
            sourceSet.addCompilerOptions(['-Xsemanticdb', '-sourceroot', project.projectDir.absolutePath])
        } else {
            def maybeSemanticDbVersion = java.util.Optional.ofNullable(semanticDbVersion)
            sourceSet.addCompilerPlugin(ScalafixProps.getSemanticDbArtifactCoordinates(scalaVersion.get(), maybeSemanticDbVersion))
            // Setting `sourceroot` to the project's absolute path is problematic for large code bases that require
            // aggressive caching: any difference in compiler options between machines forces Gradle to recompile,
            // rather than to download existing compiled artifacts. For that reason, `sourceroot` is set relative
            // to `targetroot`. For more context, see: https://github.com/scalameta/scalameta/issues/2515
            def relSourceRoot = sourceSet.getOutputDir().toPath().relativize(project.projectDir.toPath())
            sourceSet.addCompilerOptions(['-Yrangepos', '-P:semanticdb:sourceroot:targetroot:' + relSourceRoot])
        }
    }

    private boolean isScala3() {
        return scalaVersion.getOrNull()?.startsWith('3.')
    }
}
