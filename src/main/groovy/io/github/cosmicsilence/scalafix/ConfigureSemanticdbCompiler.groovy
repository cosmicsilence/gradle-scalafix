package io.github.cosmicsilence.scalafix

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction

class ConfigureSemanticdbCompiler extends DefaultTask {

    @Input
    final Property<ScalaSourceSet> scalaSourceSet = project.objects.property(ScalaSourceSet)

    @Input
    final Property<String> scalaVersion = project.objects.property(String)

    @Input
    @Optional
    final Property<String> semanticdbVersion = project.objects.property(String)

    @TaskAction
    def run() {
        def semanticDbCoordinates = ScalafixProps.getSemanticDbArtifactCoordinates(scalaVersion.get(),
                java.util.Optional.ofNullable(semanticdbVersion.orNull))
        def semanticDbDependency = project.dependencies.create(semanticDbCoordinates)
        def configuration = project.configurations.detachedConfiguration(semanticDbDependency).setTransitive(false)
        def compilerOpts = [
                '-Xplugin:' + configuration.asPath,
                '-P:semanticdb:sourceroot:' + project.projectDir,
                '-Yrangepos'
        ]
        scalaSourceSet.get().addCompilerOptions(compilerOpts)
    }
}
