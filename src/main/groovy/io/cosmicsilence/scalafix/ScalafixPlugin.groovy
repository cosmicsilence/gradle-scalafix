package io.cosmicsilence.scalafix

import io.cosmicsilence.scalafix.internal.BuildInfo
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.scala.ScalaPlugin
import org.gradle.api.tasks.SourceSet

class ScalafixPlugin implements Plugin<Project> {

    static final String EXTENSION = "scalafix"
    static final String CONFIGURATION = "scalafix"
    private static final String TASK_GROUP = "scalafix"
    private static final String SCALAFIX_TASK = "scalafix"
    private static final String CHECK_SCALAFIX_TASK = "checkScalafix"

    @Override
    void apply(Project project) {
        project.pluginManager.apply(ScalaPlugin)

        project.extensions.create(EXTENSION, ScalafixPluginExtension)
        if (project.rootProject.extensions.findByName(EXTENSION) == null) {
            project.rootProject.extensions.create(EXTENSION, ScalafixPluginExtension)
        }

        if (project.configurations.findByName(CONFIGURATION) == null) {
            project.configurations.create(CONFIGURATION)
            project.dependencies.add(CONFIGURATION, BuildInfo.scalafixCli)
        }

        configureTasks(project)
    }

    private void configureTasks(Project project) {
        def scalafixTask = project.tasks.create(SCALAFIX_TASK)
        scalafixTask.setGroup(TASK_GROUP)
        scalafixTask.setDescription('Runs Scalafix on Scala sources')

        def checkScalafixTask = project.tasks.create(CHECK_SCALAFIX_TASK)
        checkScalafixTask.setGroup(TASK_GROUP)
        checkScalafixTask.setDescription('Fails the build if running Scalafix produces a diff or a linter error message')
        project.tasks.check.dependsOn(checkScalafixTask)

        project.sourceSets.each { SourceSet sourceSet ->
            configureTaskForSourceSet(sourceSet, scalafixTask, project)
            configureTaskForSourceSet(sourceSet, checkScalafixTask, project)
        }
    }

    private void configureTaskForSourceSet(SourceSet sourceSet, Task mainTask, Project project) {
        def task = project.tasks.create(mainTask.name + sourceSet.name.capitalize(), ScalafixTask)
        task.setDescription("${mainTask.description} in ${sourceSet.getName()}")
        task.setGroup(mainTask.group)
        task.setSource(sourceSet.getAllSource().matching { include '**/*.scala'})
        mainTask.dependsOn += task
    }
}
