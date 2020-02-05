package io.github.cosmicsilence.scalafix

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
        def cliDependency = project.dependencies.create(BuildInfo.scalafixCliArtifact)
        def cliClasspath = project.configurations.detachedConfiguration(cliDependency)
        def customRulesClasspath = project.configurations.getByName(ScalafixPlugin.CUSTOM_RULES_CONFIGURATION)
        def scalacVersion = getScalaVersion()
        def scalacOptions = getScalacOptions()
        def projectClasspath = getProjectClasspath()

        logger.debug(
                """Initialising Scalafix with the following parameters:
                  | - Mode: ${mode}
                  | - Config file: ${configFile}
                  | - Custom rules classpath: ${customRulesClasspath.asPath}
                  | - Scala version: ${scalacVersion}
                  | - Scalac options: ${scalacOptions}
                  | - Sources: ${sourcePaths}
                  | - Classpath: ${projectClasspath}
                  |""".stripMargin())

        def interfacesClassloader = new InterfacesClassloader(getClass().classLoader)
        def cliClassloader = classloaderFrom(cliClasspath, interfacesClassloader)
        def toolsClassloader = classloaderFrom(customRulesClasspath, cliClassloader)

        def args = Scalafix.classloadInstance(cliClassloader)
                .newArguments()
                .withMode(mode)
                .withConfig(configFile)
                .withRules(rules.get())
                .withSourceroot(project.projectDir.toPath())
                .withPaths(sourcePaths)
                .withToolClasspath(toolsClassloader)
                .withClasspath(projectClasspath)
                .withScalaVersion(getScalaVersion())
                .withScalacOptions(getScalacOptions())

        logger.debug(
                """Scalafix initialised!:
                  | - Rules available: ${args.availableRules().collect { it.name() }}
                  | - Rules that will run: ${args.rulesThatWillRun().collect { it.name() }}
                  |""".stripMargin())

        if (!args.rulesThatWillRun().empty) {
            logger.quiet("Running Scalafix on ${sourcePaths.size} Scala source files")
            def errors = args.run()
            if (errors.size() > 0) throw new ScalafixFailed(errors)
        } else {
            logger.warn("No Scalafix rules to run")
        }
    }

    private List<String> getScalacOptions() {
        getCompileTask().scalaCompileOptions.additionalParameters ?: []
    }

    private String getScalaVersion() {
        def scalaRuntime = project.extensions.findByType(ScalaRuntime.class)
        def scalaJar = scalaRuntime.findScalaJar(getCompileTask().classpath, "library")
        scalaRuntime.getScalaVersion(scalaJar)
    }

    private ScalaCompile getCompileTask() {
        project.tasks.withType(ScalaCompile).first()
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
}
