package io.github.cosmicsilence.scalafix

import org.gradle.api.Action
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.scala.ScalaCompile

/**
 * Appends SemanticDB-related compiler options to a {@link ScalaCompile} task at execution time.
 * Registered unconditionally via {@code doFirst} so that the task's identity is stable across
 * invocations; the action no-ops when {@code configureSemanticDb} is not set, which is the case
 * when no scalafix task is being run.
 *
 * The mutated {@code additionalParameters} list itself is not part of the task's input snapshot,
 * but the gating {@code Property<Boolean>} is captured via {@code scalaCompilerPlugins} (resolved
 * to either the semanticdb plugin classpath or empty depending on the same Property), so cache
 * keys still vary correctly between invocations that wire SemanticDB and those that do not.
 */
class AppendSemanticDbOptionsAction implements Action<Task> {

    private final Property<Boolean> configureSemanticDb
    private final Property<String> scalaVersion
    private final Property<String> semanticDbVersion
    private final String projectDirPath
    private final FileCollection compilerPluginFilesFallback

    AppendSemanticDbOptionsAction(Property<Boolean> configureSemanticDb,
                                  Property<String> scalaVersion,
                                  Property<String> semanticDbVersion,
                                  String projectDirPath,
                                  FileCollection compilerPluginFilesFallback) {
        this.configureSemanticDb = configureSemanticDb
        this.scalaVersion = scalaVersion
        this.semanticDbVersion = semanticDbVersion
        this.projectDirPath = projectDirPath
        this.compilerPluginFilesFallback = compilerPluginFilesFallback
    }

    @Override
    void execute(Task task) {
        if (!configureSemanticDb.getOrElse(false)) return

        def compile = (ScalaCompile) task
        def existing = compile.scalaCompileOptions.additionalParameters ?: []
        def v = scalaVersion.get()
        def additions

        if (v.startsWith('3.')) {
            // -sourceroot is set to the project's absolute path because Scala 3's SemanticDB does
            // not yet support targetroot-relative sourceroots (see gradle/gradle#27161).
            additions = ['-Xsemanticdb', '-sourceroot', projectDirPath]
        } else {
            // ScalaCompile.destinationDirectory (DirectoryProperty) was added in Gradle 6.1; the
            // older API exposes destinationDir as a plain File.
            def outputDir = compile.hasProperty('destinationDirectory') ?
                    compile.destinationDirectory.get().asFile.toPath() :
                    compile.destinationDir.toPath()
            def relSourceRoot = outputDir.relativize(new File(projectDirPath).toPath())
            additions = ['-Yrangepos', '-P:semanticdb:sourceroot:targetroot:' + relSourceRoot]
            if (compilerPluginFilesFallback != null) {
                // Gradle < 6.4 has no ScalaCompile.scalaCompilerPlugins property; fall back to
                // injecting the semanticdb-scalac jar via -Xplugin:<paths>.
                additions += ['-Xplugin:' + compilerPluginFilesFallback.asPath]
            }
        }

        compile.scalaCompileOptions.additionalParameters = existing + additions
    }
}
