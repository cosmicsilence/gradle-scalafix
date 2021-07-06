package io.github.cosmicsilence.scalafix

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.scala.ScalaPlugin
import org.gradle.api.tasks.SourceSet
import scalafix.interfaces.ScalafixMainMode

import static scalafix.interfaces.ScalafixMainMode.*

/** Gradle plugin for running Scalafix */
class ScalafixPlugin implements Plugin<Project> {

    private static final Logger logger = Logging.getLogger(ScalafixPlugin)

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

                if (scalaSourceSet.scalaVersion.present) {
                    configureTaskForSourceSet(project, scalaSourceSet, IN_PLACE, fixTask, extension)
                    configureTaskForSourceSet(project, scalaSourceSet, CHECK, checkTask, extension)
                } else {
                    logger.warn("WARNING: Skipping source set '${sourceSet.name}' as the Scala version could not be detected")
                }
            }
        }
    }

    private void configureTaskForSourceSet(Project project,
                                           ScalaSourceSet sourceSet,
                                           ScalafixMainMode taskMode,
                                           Task mainTask,
                                           ScalafixExtension extension) {
        def taskName = mainTask.name + sourceSet.name.capitalize()
        def taskProvider = project.tasks.register(taskName, ScalafixTask, { scalafixTask ->
            if (extension.semanticdb.autoConfigure.get()) {
                configureSemanticdbCompilerPlugin(project, sourceSet)
            }

            scalafixTask.description = "${mainTask.description} in '${sourceSet.name}'"
            scalafixTask.group = mainTask.group
            scalafixTask.sourceRoot = project.projectDir.path
            scalafixTask.source = sourceSet.scalaSources.matching {
                include(extension.includes.get())
                exclude(extension.excludes.get())
            }
            scalafixTask.configFile = extension.configFile
            scalafixTask.rules.set(project.provider({
                String prop = project.findProperty(RULES_PROPERTY) ?: ''
                prop.split('\\s*,\\s*').findAll { !it.isEmpty() }.toList()
            }))
            scalafixTask.mode = taskMode
            scalafixTask.scalaVersion = sourceSet.scalaVersion.get()
            scalafixTask.classpath = sourceSet.fullClasspath.collect { it.path }
            scalafixTask.compileOptions = sourceSet.compilerOptions
            scalafixTask.semanticdbConfigured = extension.semanticdb.autoConfigure.get()

            if (extension.semanticdb.autoConfigure.get()) {
                scalafixTask.dependsOn sourceSet.compileTask
            }
        })

        mainTask.dependsOn taskProvider
    }

    private void configureSemanticdbCompilerPlugin(Project project, ScalaSourceSet sourceSet) {
        def semanticDbCoordinates = ScalafixProps.getSemanticDbArtifactCoordinates(sourceSet.scalaVersion.get(),
                Optional.ofNullable(project.scalafix.semanticdb.version.getOrNull()))
        def semanticDbDependency = project.dependencies.create(semanticDbCoordinates)
        def configuration = project.configurations.detachedConfiguration(semanticDbDependency).setTransitive(false)
        def compilerOpts = [
                '-Xplugin:' + configuration.asPath,
                '-P:semanticdb:sourceroot:' + project.projectDir,
                '-Yrangepos'
        ]
        // intentionally mutating the Scala compile task here to avoid that the Semanticdb compiler plugin
        // always gets configured and runs even when the Scalafix task is not run (which can be costly).
        sourceSet.addCompilerOptions(compilerOpts)
    }
}
