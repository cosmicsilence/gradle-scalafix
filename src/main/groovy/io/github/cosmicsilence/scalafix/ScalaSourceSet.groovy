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

    ScalaSourceSet(Project project, SourceSet sourceSet) {
        this.project = project
        this.sourceSet = sourceSet
    }

    String getName() {
        return sourceSet.name
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
        return project.tasks.getByName(getCompileTaskName(sourceSet))
    }

    @Memoized
    Optional<String> getScalaVersion() {
        def scalaRuntime = project.extensions.findByType(ScalaRuntime)
        def scalaJar = scalaRuntime?.findScalaJar(getCompileTask().classpath, 'library')
        return Optional.ofNullable(scalaJar ? scalaRuntime.getScalaVersion(scalaJar) : null)
    }

    List<String> getCompilerOptions() {
        return getCompileTask().scalaCompileOptions.additionalParameters ?: []
    }

    void addCompilerOptions(List<String> opts) {
        getCompileTask().scalaCompileOptions.additionalParameters = getCompilerOptions() + opts
    }

    static boolean isScalaSourceSet(Project project, SourceSet sourceSet) {
        return project.tasks.named(getCompileTaskName(sourceSet)).present
    }

    private static String getCompileTaskName(SourceSet sourceSet) {
        return sourceSet.getCompileTaskName('scala')
    }
}
