package io.github.cosmicsilence.scalafix

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
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

            if (extension.autoConfigureSemanticdb) {
                configureSemanticdbCompilerPlugin(project)
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
        project.tasks.check.dependsOn checkTask

        project.sourceSets.each { SourceSet sourceSet ->
            configureTaskForSourceSet(sourceSet, IN_PLACE, fixTask, project, extension)
            configureTaskForSourceSet(sourceSet, CHECK, checkTask, project, extension)
        }
    }

    private void configureTaskForSourceSet(SourceSet sourceSet,
                                           ScalafixMainMode taskMode,
                                           Task mainTask,
                                           Project project,
                                           ScalafixExtension extension) {
        ScalaCompile compileTask = project.tasks.getByName(sourceSet.getCompileTaskName('scala'))
        def scalaRuntime = project.extensions.findByType(ScalaRuntime)
        def scalaJar = scalaRuntime.findScalaJar(compileTask.classpath, 'library')

        def name = mainTask.name + sourceSet.name.capitalize()
        def task = project.tasks.create(name, ScalafixTask) {
            description = "${mainTask.description} in '${sourceSet.getName()}'"
            group = mainTask.group
            sourceRoot = project.projectDir
            source = sourceSet.allScala.matching {
                include(extension.includes.get())
                exclude(extension.excludes.get())
            }
            configFile = extension.configFile
            rules.set(project.provider({
                String prop = project.findProperty(RULES_PROPERTY) ?: ''
                prop.split('\\s*,\\s*').findAll { !it.isEmpty() }.toList()
            }))
            mode = taskMode
            classpath = (sourceSet.output.classesDirs + sourceSet.compileClasspath).files.toList()
            scalaVersion = scalaJar ? scalaRuntime.getScalaVersion(scalaJar) : ''
            compileOptions = compileTask.scalaCompileOptions.additionalParameters ?: []
        }
        mainTask.dependsOn task

        if (extension.autoConfigureSemanticdb) {
            task.dependsOn compileTask
        }
    }

    private void configureSemanticdbCompilerPlugin(Project project) {
        def dependency = project.dependencies.create(BuildInfo.semanticdbArtifact)
        def configuration = project.configurations.detachedConfiguration(dependency)
        def compilerParameters = [
                '-Xplugin:' + configuration.asPath,
                '-P:semanticdb:sourceroot:' + project.projectDir,
                '-Yrangepos'
        ]

        project.tasks.withType(ScalaCompile) { ScalaCompile task ->
            task.scalaCompileOptions.additionalParameters =
                    (task.scalaCompileOptions.additionalParameters ?: []) + compilerParameters
        }
    }
}
