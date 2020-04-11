package io.github.cosmicsilence.scalafix

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.scala.ScalaPlugin
import org.gradle.api.tasks.ScalaRuntime
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.scala.ScalaCompile
import scalafix.interfaces.ScalafixMainMode

import static scalafix.interfaces.ScalafixMainMode.*

/**
 * Gradle plugin for running Scalafix.
 */
class ScalafixPlugin implements Plugin<Project> {

    private static final Logger logger = Logging.getLogger(ScalafixPlugin)

    private static final String EXTENSION = "scalafix"
    private static final String CUSTOM_RULES_CONFIGURATION = "scalafix"
    private static final String TASK_GROUP = "scalafix"
    private static final String FIX_TASK = "scalafix"
    private static final String CHECK_TASK = "checkScalafix"
    private static final String RULES_PROPERTY = "scalafix.rules"

    @Override
    void apply(Project project) {
        def extension = project.extensions.create(EXTENSION, ScalafixExtension, project)
        def customRulesConfiguration = project.configurations.create(CUSTOM_RULES_CONFIGURATION)
        customRulesConfiguration.description = "Dependencies containing custom Scalafix rules"

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
            if (!extension.ignoreSourceSets.get().contains(sourceSet.name)) {
                configureTaskForSourceSet(sourceSet, IN_PLACE, fixTask, project, extension)
                configureTaskForSourceSet(sourceSet, CHECK, checkTask, project, extension)
            }
        }
    }

    private void configureTaskForSourceSet(SourceSet sourceSet,
                                           ScalafixMainMode taskMode,
                                           Task mainTask,
                                           Project project,
                                           ScalafixExtension extension) {
        def taskName = mainTask.name + sourceSet.name.capitalize()
        def taskProvider = project.tasks.register(taskName, ScalafixTask, { scalafixTask ->
            ScalaCompile scalaCompileTask = project.tasks.getByName(sourceSet.getCompileTaskName('scala'))

            if (extension.autoConfigureSemanticdb) {
                configureSemanticdbCompilerPlugin(project, scalaCompileTask, sourceSet)
            }

            scalafixTask.description = "${mainTask.description} in '${sourceSet.getName()}'"
            scalafixTask.group = mainTask.group
            scalafixTask.sourceRoot = project.projectDir.path
            scalafixTask.source = sourceSet.allScala.matching {
                include(extension.includes.get())
                exclude(extension.excludes.get())
            }
            scalafixTask.configFile = extension.configFile
            scalafixTask.rules.set(project.provider({
                String prop = project.findProperty(RULES_PROPERTY) ?: ''
                prop.split('\\s*,\\s*').findAll { !it.isEmpty() }.toList()
            }))
            scalafixTask.mode = taskMode
            scalafixTask.scalaVersion = getScalaVersion(project, scalaCompileTask)
            scalafixTask.classpath = sourceSet.output.classesDirs.toList().collect { it.path }
            scalafixTask.compileOptions = scalaCompileTask.scalaCompileOptions.additionalParameters ?: []

            if (extension.autoConfigureSemanticdb) {
                scalafixTask.dependsOn scalaCompileTask
            }
        })

        mainTask.dependsOn taskProvider
    }

    private String getScalaVersion(Project project, ScalaCompile scalaCompileTask) {
        def scalaRuntime = project.extensions.findByType(ScalaRuntime)
        def scalaJar = scalaRuntime.findScalaJar(scalaCompileTask.classpath, 'library')
        scalaJar ? scalaRuntime.getScalaVersion(scalaJar) : ''
    }

    private void configureSemanticdbCompilerPlugin(Project project, ScalaCompile scalaCompileTask, SourceSet sourceSet) {
        def scalaVersion = getScalaVersion(project, scalaCompileTask)
        def coordinates = SemanticDB.getMavenCoordinates(scalaVersion)

        if (coordinates.isPresent()) {
            def dependency = project.dependencies.create(coordinates.get())
            def configuration = project.configurations.detachedConfiguration(dependency).setTransitive(false)
            def compilerParameters = [
                    '-Xplugin:' + configuration.asPath,
                    '-P:semanticdb:sourceroot:' + project.projectDir,
                    '-Yrangepos'
            ]

            // intentionally mutating the Scala compile task here to avoid that the SemanticDB compiler plugin
            // always gets configured and runs even when the Scalafix task is not run (which can be costly).
            scalaCompileTask.scalaCompileOptions.additionalParameters =
                        (scalaCompileTask.scalaCompileOptions.additionalParameters ?: []) + compilerParameters
        } else {
            logger.warn("WARNING: The SemanticDB compiler plugin could not be auto-configured because the Scala version " +
                    "used in source set '${sourceSet.name}' is unsupported or could not be determined (value=$scalaVersion)")
        }
    }
}
