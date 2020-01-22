package io.cosmicsilence.scalafix

import io.cosmicsilence.scalafix.internal.BuildInfo
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

    private static final String EXTENSION = "scalafix"
    private static final String CUSTOM_RULES_CONFIGURATION = "scalafix"
    private static final String SEMANTICDB_CONFIGURATION = "semanticdb"
    private static final String TASK_GROUP = "scalafix"
    private static final String SCALAFIX_TASK = "scalafix"
    private static final String CHECK_SCALAFIX_TASK = "checkScalafix"
    private static final String RULES_PROPERTY = "scalafix.rules"

    @Override
    void apply(Project project) {
        def extension = project.extensions.create(EXTENSION, ScalafixPluginExtension, project)
        def customRulesConfiguration = project.configurations.create(CUSTOM_RULES_CONFIGURATION)
        customRulesConfiguration.description = "Dependencies containing custom Scalafix rules"

        def semanticDbConfiguration = project.configurations.create(SEMANTICDB_CONFIGURATION)
        semanticDbConfiguration.description = "SemanticDB compiler plugin"
        project.dependencies.add(SEMANTICDB_CONFIGURATION, "org.scalameta:semanticdb-scalac_${BuildInfo.scala212Version}:${BuildInfo.scalametaVersion}")

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
        def task = project.tasks.create(name, ScalafixTask, checkOnly)
        task.description = "${mainTask.description} in '${sourceSet.getName()}'"
        task.group = mainTask.group
        task.source = sourceSet.allScala.matching {
            include(extension.includes.get())
            exclude(extension.excludes.get())
        }
        task.configFile = extension.configFile
        task.rules.set(project.provider({
            String prop = project.findProperty(RULES_PROPERTY) ?: ""
            prop.split('\\s*,\\s*').findAll { !it.isEmpty() }.toList()
        }))
        mainTask.dependsOn += task
    }
}
