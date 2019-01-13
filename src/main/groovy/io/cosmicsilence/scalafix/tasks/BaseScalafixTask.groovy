package io.cosmicsilence.scalafix.tasks

import io.cosmicsilence.scalafix.ScalafixPlugin
import io.cosmicsilence.scalafix.internal.BuildInfo
import io.cosmicsilence.scalafix.internal.InterfacesClassloader
import io.cosmicsilence.scalafix.internal.ScalafixFailed
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction
import scalafix.interfaces.Scalafix
import scalafix.interfaces.ScalafixMainMode

import java.nio.file.Path

abstract class BaseScalafixTask extends SourceTask {
    private static final String DEFAULT_SCALAFIX_CONF = ".scalafix.conf"
    private final Logger logger = Logging.getLogger(getClass())

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    @Optional
    final RegularFileProperty configFile = newInputFile()

    @TaskAction
    void run() {
        def scalafixConfig = resolveConfigFile()
        logger.debug("Using config file: {}", scalafixConfig)

        def sources = selectSources()
        def cliDependency = project.dependencies.create(BuildInfo.scalafixCli)
        def cliClasspath = project.configurations.detachedConfiguration(cliDependency)
        def toolsClasspath = project.configurations.getByName(ScalafixPlugin.CONFIGURATION)
        logger.debug("Tools classpath: {}", toolsClasspath.asPath)

        def interfacesClassloader = new InterfacesClassloader(getClass().classLoader)
        def cliClassloader = classloaderFrom(cliClasspath, interfacesClassloader)
        def toolsClassloader = classloaderFrom(toolsClasspath, cliClassloader)
        def args = Scalafix.classloadInstance(cliClassloader)
                .newArguments()
                .withToolClasspath(toolsClassloader)
                .withConfig(scalafixConfig)
                .withPaths(sources)
                .withMode(scalafixMode)

        logger.debug("Scalafix rules available: {}", args.availableRules())
        logger.debug("Scalafix rules that will run: {}", args.rulesThatWillRun())
        logger.debug("Source files to be processed: {}", sources)

        if (!sources.empty) {
            if (!args.rulesThatWillRun().empty) {
                logger.quiet("Running Scalafix on ${sources.size} Scala source files")
                def errors = args.run()
                if (errors.size() > 0) throw new ScalafixFailed(errors.toList())
            } else {
                logger.warn("No Scalafix rules to run")
            }
        }
    }

    private static URLClassLoader classloaderFrom(Configuration configuration, ClassLoader parent) {
        def jars = configuration.collect { it.toURI().toURL() }.toArray(new URL[0])
        new URLClassLoader(jars, parent)
    }

    private java.util.Optional<Path> resolveConfigFile() {
        def defaultConfig = { Project proj ->
            def file = proj.file(DEFAULT_SCALAFIX_CONF)
            if (file.exists()) file.toPath() else null
        }

        def file = configFile.map { it.asFile.toPath() }.orNull ?: defaultConfig(project) ?: defaultConfig(project.rootProject)
        java.util.Optional.ofNullable(file)
    }

    private List<Path> selectSources() {
        source.collect { it.toPath() }
    }

    abstract ScalafixMainMode getScalafixMode()
}
