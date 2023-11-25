package io.github.cosmicsilence.scalafix

import groovy.transform.Memoized
import org.gradle.api.Project
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.tasks.ScalaRuntime
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.scala.ScalaCompile

class ScalaSourceSet {

    private final Project project
    private final SourceSet sourceSet
    private final ScalaCompile compileTask

    ScalaSourceSet(Project project, SourceSet sourceSet) {
        if (!isScalaSourceSet(project, sourceSet)) {
            throw new IllegalArgumentException(sourceSet.name + "is not a Scala source set")
        }

        this.project = project
        this.sourceSet = sourceSet
        this.compileTask = project.tasks.getByName(getCompileTaskName(sourceSet))
    }

    String getName() {
        return sourceSet.name
    }

    File getOutputDir() {
        // Supported in Gradle >= 6.1
        if (compileTask.hasProperty('destinationDirectory')) {
            return compileTask.destinationDirectory.asFile.get()
        }
        return compileTask.destinationDir
    }

    List<File> getFullClasspath() {
        return getClassesDirs() + getJarDependencies()
    }

    private List<File> getClassesDirs() {
        return sourceSet.output.classesDirs.toList()
    }

    private List<File> getJarDependencies() {
        return sourceSet.compileClasspath.toList().findAll { it.name.toLowerCase().endsWith(".jar") }
    }

    SourceDirectorySet getScalaSources() {
        return sourceSet.allScala
    }

    ScalaCompile getCompileTask() {
        return compileTask
    }

    @Memoized
    Optional<String> getScalaVersion() {
        def scalaRuntime = project.extensions.findByType(ScalaRuntime)
        def scala3Jar = scalaRuntime?.findScalaJar(compileTask.classpath, 'library_3')
        if (scala3Jar) return Optional.of(scalaRuntime.getScalaVersion(scala3Jar))

        def scala2Jar = scalaRuntime?.findScalaJar(compileTask.classpath, 'library')
        return Optional.ofNullable(scala2Jar ? scalaRuntime.getScalaVersion(scala2Jar) : null)
    }

    List<String> getCompilerOptions() {
        return compileTask.scalaCompileOptions.additionalParameters ?: []
    }

    void addCompilerOptions(List<String> opts) {
        compileTask.scalaCompileOptions.additionalParameters = getCompilerOptions() + opts
    }

    void addCompilerPlugin(String pluginCoordinates) {
        def dependency = project.dependencies.create(pluginCoordinates)
        def configuration = project.configurations.detachedConfiguration(dependency).setTransitive(false)

        // Supported in Gradle >= 6.4
        if (compileTask.hasProperty('scalaCompilerPlugins')) {
            compileTask.scalaCompilerPlugins = (compileTask.scalaCompilerPlugins ?: project.files()) + configuration
        } else {
            addCompilerOptions(['-Xplugin:' + configuration.asPath])
        }
    }

    static boolean isScalaSourceSet(Project project, SourceSet sourceSet) {
        def taskName = getCompileTaskName(sourceSet)
        return taskName ? project.tasks.findByName(taskName) : false
    }

    private static String getCompileTaskName(SourceSet sourceSet) {
        return sourceSet.getCompileTaskName('scala')
    }
}
