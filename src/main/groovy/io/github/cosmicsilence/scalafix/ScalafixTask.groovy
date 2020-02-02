package io.github.cosmicsilence.scalafix

import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.*
import scalafix.interfaces.Scalafix
import scalafix.interfaces.ScalafixMainMode

class ScalafixTask extends SourceTask {

    private static final Logger logger = Logging.getLogger(ScalafixTask)

    @InputFile
    @Optional
    final RegularFileProperty configFile = project.objects.fileProperty()

    @Input
    @Optional
    final ListProperty<String> rules = project.objects.listProperty(String)

    @Input
    ScalafixMainMode mode

    @Input
    @Optional
    String scalaVersion

    @Input
    @Optional
    List<String> compileOptions

    @Input
    @Optional
    FileCollection classpath

    @Input
    File sourceRoot

    @TaskAction
    void run() {
        if (!source.isEmpty()) processSources()
        else logger.warn("No sources to be processed")
    }

    private void processSources() {
        def sourcePaths = source.collect { it.toPath() }
        def configPath = java.util.Optional.ofNullable(configFile.getOrNull()).map { it.asFile.toPath() }
        def projectClasspath = classpath.collect { it.toPath() }
        def cliDependency = project.dependencies.create(BuildInfo.scalafixCliArtifact)
        def cliClasspath = project.configurations.detachedConfiguration(cliDependency)
        def customRulesClasspath = project.configurations.getByName(ScalafixPlugin.CUSTOM_RULES_CONFIGURATION)

        logger.debug(
                """Running Scalafix with the following arguments:
                  | - Mode: ${mode}
                  | - Config file: ${configFile}
                  | - Custom rules classpath: ${customRulesClasspath.asPath}
                  | - Scala version: ${scalaVersion}
                  | - Scalac options: ${compileOptions}
                  | - Source root: ${sourceRoot}
                  | - Sources: ${sourcePaths}
                  | - Classpath: ${projectClasspath}
                  |""".stripMargin())

        def interfacesClassloader = new InterfacesClassloader(getClass().classLoader)
        def cliClassloader = classloaderFrom(cliClasspath, interfacesClassloader)
        def customRulesClassloader = classloaderFrom(customRulesClasspath, cliClassloader)

        def args = Scalafix.classloadInstance(cliClassloader)
                .newArguments()
                .withMode(mode)
                .withConfig(configPath)
                .withRules(rules.get())
                .withSourceroot(sourceRoot.toPath())
                .withPaths(sourcePaths)
                .withToolClasspath(customRulesClassloader)
                .withClasspath(projectClasspath)
                .withScalaVersion(scalaVersion)
                .withScalacOptions(compileOptions)

        logger.debug(
                """Scalafix rules:
                  | - Available: ${args.availableRules().collect { it.name() }}
                  | - That will run: ${args.rulesThatWillRun().collect { it.name() }}
                  |""".stripMargin())

        if (!args.rulesThatWillRun().empty) {
            logger.quiet("Running Scalafix on ${sourcePaths.size} Scala source files")
            def errors = args.run()
            if (errors.size() > 0) throw new ScalafixFailed(errors)
        } else {
            logger.warn("No Scalafix rules to run")
        }
    }

    private static URLClassLoader classloaderFrom(Configuration configuration, ClassLoader parent) {
        def jars = configuration.collect { it.toURI().toURL() }.toArray(new URL[0])
        new URLClassLoader(jars, parent)
    }
}
