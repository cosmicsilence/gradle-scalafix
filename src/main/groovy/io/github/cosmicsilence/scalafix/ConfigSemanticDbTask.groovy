package io.github.cosmicsilence.scalafix

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.scala.ScalaCompile
import java.nio.file.Path

/** Configures the SemanticDB compiler plugin during the execution phase to avoid resolving dependencies too early. */
class ConfigSemanticDbTask extends DefaultTask {
    @Input
    final Property<String> scalaVersion = project.objects.property(String)

    @Input
    @Optional
    final Property<String> semanticDbVersion = project.objects.property(String)

    @Input
    final Property<String> sourceSetName = project.objects.property(String)

    @Input
    final Property<String> projectDirPath = project.objects.property(String)

    @Internal
    final DirectoryProperty outputDir = project.objects.directoryProperty()

    @TaskAction
    void run() {
        def compileTaskName = sourceSetName.get() == 'main' ? 'compileScala' : "compile${sourceSetName.get().capitalize()}Scala"
        final ScalaCompile task = project.tasks.getByName(compileTaskName) as ScalaCompile

        if (isScala3()) {
            // It's currently not possible to set `-sourceroot` in a fully cache-friendly way (see comment below):
            // https://github.com/gradle/gradle/issues/27161
            addCompilerOptions(task, ['-Xsemanticdb', '-sourceroot', projectDirPath.get()])
        } else {
            def maybeSemanticDbVersion = java.util.Optional.ofNullable(semanticDbVersion.getOrNull())
            addCompilerPlugin(task, ScalafixProps.getSemanticDbArtifactCoordinates(scalaVersion.get(), maybeSemanticDbVersion))

            // Setting `sourceroot` to the project's absolute path is problematic for large code bases that require
            // aggressive caching: any difference in compiler options between machines forces Gradle to recompile,
            // rather than to download existing compiled artifacts. For that reason, `sourceroot` is set relative
            // to `targetroot`. For more context, see: https://github.com/scalameta/scalameta/issues/2515
            Path relSourceRoot = outputDir.get().asFile.toPath().relativize(new File(projectDirPath.get()).toPath())
            addCompilerOptions(task, ['-Yrangepos', '-P:semanticdb:sourceroot:targetroot:' + relSourceRoot])
        }
    }

    private void addCompilerOptions(ScalaCompile task, List<String> opts) {
        def currentOptions = task.scalaCompileOptions.additionalParameters ?: []
        task.scalaCompileOptions.additionalParameters = currentOptions + opts
    }

    private void addCompilerPlugin(ScalaCompile task, String pluginCoordinates) {
        def dependency = project.dependencies.create(pluginCoordinates)
        def configuration = project.configurations.detachedConfiguration(dependency).setTransitive(false)

        // Supported in Gradle >= 6.4
        if (task.hasProperty('scalaCompilerPlugins')) {
            task.scalaCompilerPlugins = (task.scalaCompilerPlugins ?: project.files()) + configuration
        } else {
            addCompilerOptions(task, ['-Xplugin:' + configuration.asPath])
        }
    }

    private boolean isScala3() {
        return scalaVersion.getOrNull()?.startsWith('3.')
    }
}
