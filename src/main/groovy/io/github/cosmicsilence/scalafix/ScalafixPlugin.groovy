package io.github.cosmicsilence.scalafix

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.scala.ScalaPlugin
import org.gradle.api.tasks.SourceSet
import scalafix.interfaces.ScalafixMainMode

import static scalafix.interfaces.ScalafixMainMode.CHECK
import static scalafix.interfaces.ScalafixMainMode.IN_PLACE

/** Gradle plugin for running Scalafix */
class ScalafixPlugin implements Plugin<Project> {

    private static final String EXTENSION = "scalafix"
    private static final String EXTERNAL_RULES_CONFIGURATION = "scalafix"
    private static final String TASK_GROUP = "scalafix"
    private static final String FIX_TASK = "scalafix"
    private static final String CHECK_TASK = "checkScalafix"
    private static final String RULES_PROPERTY = "scalafix.rules"

    @Override
    void apply(Project project) {
        def extension = project.extensions.create(EXTENSION, ScalafixExtension, project)
        def externalRulesConfiguration = project.configurations.create(EXTERNAL_RULES_CONFIGURATION)
        externalRulesConfiguration.description = "Dependencies containing external Scalafix rules"

        project.afterEvaluate {
            if (!project.plugins.hasPlugin(ScalaPlugin)) {
                throw new GradleException("The 'scala' plugin must be applied")
            }

            configureTasks(project, extension)
        }
    }

    private void configureTasks(Project project, ScalafixExtension extension) {
        def fixTask = project.tasks.create(FIX_TASK)
        fixTask.group = TASK_GROUP
        fixTask.description = 'Runs Scalafix on Scala sources'

        def checkTask = project.tasks.create(CHECK_TASK)
        checkTask.group = TASK_GROUP
        checkTask.description = "Fails if running Scalafix produces a diff or a linter error message. Won't write to files"

        project.tasks.named('check').configure { it.dependsOn checkTask }

        project.sourceSets.each { SourceSet sourceSet ->
            if (ScalaSourceSet.isScalaSourceSet(project, sourceSet) && !extension.ignoreSourceSets.get().contains(sourceSet.name)) {
                def scalaSourceSet = new ScalaSourceSet(project, sourceSet)
                configureTaskForSourceSet(project, scalaSourceSet, IN_PLACE, fixTask, extension)
                configureTaskForSourceSet(project, scalaSourceSet, CHECK, checkTask, extension)
            }
        }
    }

    private void configureTaskForSourceSet(Project project,
                                           ScalaSourceSet sourceSet,
                                           ScalafixMainMode taskMode,
                                           Task mainTask,
                                           ScalafixExtension extension) {
        def taskName = mainTask.name + sourceSet.getName().capitalize()
        def taskProvider = project.tasks.register(taskName, ScalafixTask, { scalafixTask ->
            scalafixTask.description = "${mainTask.description} in '${sourceSet.getName()}'"
            scalafixTask.group = mainTask.group
            scalafixTask.sourceRoot = project.projectDir.path
            scalafixTask.source = sourceSet.getScalaSources().matching {
                include(extension.includes.get())
                exclude(extension.excludes.get())
            }
            scalafixTask.configFile = extension.configFile
            scalafixTask.rules.set(project.provider({
                String prop = project.findProperty(RULES_PROPERTY) ?: ''
                prop.split('\\s*,\\s*').findAll { !it.empty }.toList()
            }))
            scalafixTask.mode = taskMode
            scalafixTask.scalaVersion.set(project.provider({ resolveScalaVersion(sourceSet) }))
            scalafixTask.classpath.set(project.provider({ sourceSet.getFullClasspath().collect { it.path } }))
            scalafixTask.compileOptions.set(project.provider({ sourceSet.getCompilerOptions() }))
            scalafixTask.semanticdbConfigured = extension.semanticdb.autoConfigure.get()

            if (extension.semanticdb.autoConfigure.get()) {
                // configures the semanticdb compiler plugin during the execution phase, but before the
                // compile task is executed. This prevents dependencies from being resolved too early
                sourceSet.getCompileTask().doFirst {
                    configureSemanticdbCompilerPlugin(project, sourceSet, extension)
                }
                scalafixTask.dependsOn sourceSet.getCompileTask()
            }
        })

        mainTask.dependsOn taskProvider
    }

    private void configureSemanticdbCompilerPlugin(Project project, ScalaSourceSet sourceSet, ScalafixExtension extension) {
        def scalaVersion = resolveScalaVersion(sourceSet)
        def semanticDbVersion = Optional.ofNullable(extension.semanticdb.version.orNull)
        def semanticDbCoordinates = ScalafixProps.getSemanticDbArtifactCoordinates(scalaVersion, semanticDbVersion)
        def semanticDbDependency = project.dependencies.create(semanticDbCoordinates)
        def configuration = project.configurations.detachedConfiguration(semanticDbDependency).setTransitive(false)
        def compilerOpts = [
                '-Xplugin:' + configuration.asPath,
                // Gradle does not use the project root as it's working directory. Instead, it has N workers that run
                // under their own directories and point to the `scalac` task's output location (which is under the project).
                // Setting `sourceroot` to `project.projectDir` is problematic for large code bases that require aggressive
                // caching: any difference in compiler options between machines forces Gradle to recompile, rather than
                // to download existing compiled artifacts. For that reason, we set `sourceroot` relative to `targetroot`
                // (e.g. `{project_root}/build/classes/scala/{source_set}/` -> `{project_root}/`).
                // For more context, see: https://github.com/scalameta/scalameta/issues/2515
                '-P:semanticdb:sourceroot:targetroot:../../../../',
                '-Yrangepos'
        ]
        sourceSet.addCompilerOptions(compilerOpts)
    }

    private String resolveScalaVersion(ScalaSourceSet sourceSet) {
        sourceSet.getScalaVersion().orElseThrow {
            new GradleException("Unable to detect the Scala version for the '${sourceSet.getName()}' source set. Please " +
                    "ensure it declares dependency to scala-library or consider adding it to 'ignoreSourceSets'")
        }
    }
}
