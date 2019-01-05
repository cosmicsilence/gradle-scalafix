package io.cosmicsilence.scalafix

import io.cosmicsilence.scalafix.tasks.CheckScalafix
import io.cosmicsilence.scalafix.tasks.Scalafix
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.scala.ScalaPlugin
import org.gradle.api.tasks.SourceSet

class ScalafixPlugin implements Plugin<Project> {

    private static final String EXTENSION_NAME = "scalafix"
    private static final String TASK_GROUP = "scalafix"
    private static final String FIX_TASK = "scalafix"
    private static final String CHECK_TASK = "checkScalafix"

    @Override
    void apply(Project project) {
        project.pluginManager.apply(ScalaPlugin)

        project.extensions.create(EXTENSION_NAME, ScalafixPluginExtension)
        if (project.rootProject.extensions.findByName(EXTENSION_NAME) == null) {
            project.rootProject.extensions.create(EXTENSION_NAME, ScalafixPluginExtension)
        }

        def mainFixTask = project.tasks.create(FIX_TASK)
        mainFixTask.setGroup(TASK_GROUP)
        mainFixTask.setDescription('Run Scalafix on Scala sources')

        def mainCheckTask = project.tasks.create(CHECK_TASK)
        mainCheckTask.setGroup(TASK_GROUP)
        mainCheckTask.setDescription('Fails the build if running Scalafix produces a diff or a linter error message')
        project.tasks.check.dependsOn(mainCheckTask)

        project.sourceSets.each { SourceSet sourceSet ->
            configureTaskForSourceSet(Scalafix, sourceSet, mainFixTask, project)
            configureTaskForSourceSet(CheckScalafix, sourceSet, mainCheckTask, project)
        }
    }

    private void configureTaskForSourceSet(Class<Task> taskClass, SourceSet sourceSet, Task mainTask, Project project) {
        def name = mainTask.name + sourceSet.name.capitalize()
        def description = "${mainTask.description} in ${sourceSet.getName()}"
        def task = project.tasks.create(name, taskClass)
        task.setDescription(description)
        task.setSource(sourceSet.getAllSource().matching { include '**/*.scala'})
        mainTask.dependsOn += task
    }
}
