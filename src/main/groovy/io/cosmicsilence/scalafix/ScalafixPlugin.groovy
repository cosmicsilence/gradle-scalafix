package io.cosmicsilence.scalafix

import io.cosmicsilence.scalafix.tasks.ScalafixTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.scala.ScalaPlugin
import org.gradle.api.tasks.SourceSet

/**
 * Gradle plugin for running Scalafix.
 */
class ScalafixPlugin implements Plugin<Project> {

    static final String EXTENSION = "scalafix"
    static final String CONFIGURATION = "scalafix"
    private static final String TASK_GROUP = "scalafix"
    private static final String SCALAFIX_TASK = "scalafix"
    private static final String CHECK_SCALAFIX_TASK = "checkScalafix"

    @Override
    void apply(Project project) {
        def extension = project.extensions.create(EXTENSION, ScalafixPluginExtension, project)
        def configuration = project.configurations.create(CONFIGURATION)
        configuration.description = "Dependencies containing custom Scalafix rules"
        configuration.visible = false

        project.plugins.withType(ScalaPlugin) {
            configureTasks(project, extension)
        }
    }

    private void configureTasks(Project project, ScalafixPluginExtension extension) {
        def scalafixTask = project.tasks.create(SCALAFIX_TASK)
        scalafixTask.group = TASK_GROUP
        scalafixTask.description = 'Runs Scalafix on Scala sources'

        def checkScalafixTask = project.tasks.create(CHECK_SCALAFIX_TASK)
        checkScalafixTask.group = TASK_GROUP
        checkScalafixTask.description = 'Fails if running Scalafix produces a diff or a linter error message'
        project.tasks.check.dependsOn(checkScalafixTask)

        project.sourceSets.each { SourceSet sourceSet ->
            configureTaskForSourceSet(sourceSet, false, scalafixTask, project, extension)
            configureTaskForSourceSet(sourceSet, true, checkScalafixTask, project, extension)
        }
    }

    private void configureTaskForSourceSet(SourceSet sourceSet,
                                           boolean checkOnly,
                                           Task mainTask,
                                           Project project,
                                           ScalafixPluginExtension extension) {
        def name = mainTask.name + sourceSet.name.capitalize()
        def task = project.tasks.create(name, ScalafixTask)
        task.description = "${mainTask.description} in '${sourceSet.getName()}'"
        task.group = mainTask.group
        task.source = sourceSet.allScala.matching {
            include(extension.includes.get())
            exclude(extension.excludes.get())
        }
        task.configFile = extension.configFile
        task.checkOnly = checkOnly
        mainTask.dependsOn += task
    }
}
