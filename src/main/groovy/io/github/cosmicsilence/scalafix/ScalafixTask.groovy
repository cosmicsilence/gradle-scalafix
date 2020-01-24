package io.github.cosmicsilence.scalafix

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.*
import org.gradle.api.tasks.scala.ScalaCompile
import scalafix.interfaces.Scalafix
import scalafix.interfaces.ScalafixMainMode

import javax.inject.Inject
import java.nio.file.Path

class ScalafixTask extends SourceTask {

    private static final Logger logger = Logging.getLogger(ScalafixTask)
    private static final String DEFAULT_CONFIG_FILE = ".scalafix.conf"

    private final ScalafixMainMode mode

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    @Optional
    final RegularFileProperty configFile = project.objects.fileProperty()

    @Input
    @Optional
    final ListProperty<String> rules = project.objects.listProperty(String)

    @Inject
    ScalafixTask(ScalafixMainMode mode) {
        this.mode = mode
    }

    @TaskAction
    void run() {
        if (!source.isEmpty()) processSources()
        else logger.warn("No sources to be processed")
    }

    private void processSources() {
        def sourcePaths = source.collect { it.toPath() }
        def configFile = resolveConfigFile()
        def cliDependency = project.dependencies.create(BuildInfo.scalafixCliArtifact)
        def cliClasspath = project.configurations.detachedConfiguration(cliDependency)
        def customRulesClasspath = project.configurations.getByName(ScalafixPlugin.CUSTOM_RULES_CONFIGURATION)
        def scalacOptions = getScalacOptions()
        def projectClasspath = getProjectClasspath()

        logger.debug(
                """Initialising Scalafix with the following parameters:
                  | - Mode: ${mode}
                  | - Config file: ${configFile}
                  | - Custom rules classpath: ${customRulesClasspath.asPath}
                  | - Scalac options: ${scalacOptions}
                  | - Sources: ${sourcePaths}
                  | - Classpath: ${projectClasspath}
                  |""".stripMargin())

        def interfacesClassloader = new InterfacesClassloader(getClass().classLoader)
        def cliClassloader = classloaderFrom(cliClasspath, interfacesClassloader)
        def toolsClassloader = classloaderFrom(customRulesClasspath, cliClassloader)

        def args = Scalafix.classloadInstance(cliClassloader)
                .newArguments()
                .withToolClasspath(toolsClassloader)
                .withConfig(configFile)
                .withPaths(sourcePaths)
                .withMode(mode)
                .withScalacOptions(scalacOptions)
                .withSourceroot(project.projectDir.toPath())
                .withClasspath(projectClasspath)
                .withRules(rules.get())

        logger.debug(
                """Scalafix initialised!:
                  | - Rules available: ${args.availableRules().collect { it.name() }}
                  | - Rules that will run: ${args.rulesThatWillRun().collect { it.name() }}
                  |""".stripMargin())

        if (!args.rulesThatWillRun().empty) {
            logger.quiet("Running Scalafix on ${sourcePaths.size} Scala source files...")
            def errors = args.run()
            if (errors.size() > 0) throw new ScalafixFailed(errors.toList())
        } else {
            logger.warn("No Scalafix rules to run")
        }
    }

    private List<String> getScalacOptions() {
        def maybeCompileTask = project.tasks.withType(ScalaCompile).stream().findFirst()
        maybeCompileTask.map { it.scalaCompileOptions.additionalParameters }.orElse([])
    }

    private List<Path> getProjectClasspath() {
        def classesDirs = project.sourceSets.collect { SourceSet ss ->
            ss.output.classesDirs.toList()
        }.flatten()
        classesDirs.collect { it.toPath() }
    }

    private static URLClassLoader classloaderFrom(Configuration configuration, ClassLoader parent) {
        def jars = configuration.collect { it.toURI().toURL() }.toArray(new URL[0])
        new URLClassLoader(jars, parent)
    }

    private java.util.Optional<Path> resolveConfigFile() {
        def defaultConfig = { Project proj ->
            def file = proj.file(DEFAULT_CONFIG_FILE)
            if (file.exists()) file.toPath() else null
        }

        def file = configFile.map { it.asFile.toPath() }.orNull ?: defaultConfig(project) ?: defaultConfig(project.rootProject)
        java.util.Optional.ofNullable(file)
    }
}
