package io.github.cosmicsilence.scalafix

import io.github.cosmicsilence.compat.GradleCompat
import org.gradle.api.GradleException
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.*
import scalafix.interfaces.Scalafix
import scalafix.interfaces.ScalafixMainMode
import scalafix.interfaces.ScalafixRule
import scalafix.internal.interfaces.ScalafixInterfacesClassloader

import java.nio.file.Paths

class ScalafixTask extends SourceTask {

    private static final Logger logger = Logging.getLogger(ScalafixTask)

    @InputFile
    @Optional
    final RegularFileProperty configFile = GradleCompat.fileProperty(project)

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
    List<String> classpath

    @Input
    String sourceRoot

    @Input
    Boolean semanticdbConfigured

    @TaskAction
    void run() {
        if (!source.isEmpty()) processSources()
        else logger.warn("No sources to be processed")
    }

    private void processSources() {
        def sourcePaths = source.collect { it.toPath() }
        def configFilePath = java.util.Optional.ofNullable(configFile.getOrNull()).map { it.asFile.toPath() }
        def customRulesConfiguration = project.configurations.getByName(ScalafixPlugin.CUSTOM_RULES_CONFIGURATION)

        logger.debug(
                """Running Scalafix with the following arguments:
                  | - Mode: ${mode}
                  | - Config file: ${configFilePath}
                  | - Custom rules classpath: ${customRulesConfiguration.asPath}
                  | - Scala version: ${scalaVersion}
                  | - Scalac options: ${compileOptions}
                  | - Source root: ${sourceRoot}
                  | - Sources: ${sourcePaths}
                  | - Classpath: ${classpath}
                  |""".stripMargin())

        def classloader = Scalafix.class.classLoader
        def externalRulesClassloader = classloaderFrom(customRulesConfiguration, classloader)
        def scalafixArgs = Scalafix.classloadInstance(classloader)
                .newArguments()
                .withMode(mode)
                .withConfig(configFilePath)
                .withRules(rules.get())
                .withSourceroot(Paths.get(sourceRoot))
                .withPaths(sourcePaths)
                .withToolClasspath(externalRulesClassloader)
                .withClasspath((classpath ?: []).collect { Paths.get(it)} )
                .withScalaVersion(scalaVersion)
                .withScalacOptions(compileOptions)

        logger.debug(
                """Scalafix rules:
                  | - Available: ${scalafixArgs.availableRules().collect { it.name() }}
                  | - That will run: ${scalafixArgs.rulesThatWillRun().collect { it.name() }}
                  |""".stripMargin())

        if (!scalafixArgs.rulesThatWillRun().empty) {
            assertSemanticdbIsConfigured(scalafixArgs.rulesThatWillRun())

            logger.quiet("Running Scalafix on ${sourcePaths.size} Scala source file(s)")
            def errors = scalafixArgs.run()
            if (errors.size() > 0) throw new ScalafixFailed(errors)
        } else {
            logger.warn("No Scalafix rules to run")
        }
    }

    private void assertSemanticdbIsConfigured(List<ScalafixRule> rulesThatWillRun) {
        def semanticRules = rulesThatWillRun.findAll { it.kind().isSemantic() }

        if (!semanticRules.empty && !semanticdbConfigured) {
            def semanticRuleNames = semanticRules.collect { it.name() }.join(", ")
            throw new GradleException("The semanticdb compiler plugin is required to run semantic rules such as ${semanticRuleNames}. " +
                    "To fix this problem, please enable 'autoConfigureSemanticdb' in the scalafix plugin extension")
        }
    }

    private static URLClassLoader classloaderFrom(Configuration configuration, ClassLoader parent) {
        def jars = configuration.collect { it.toURI().toURL() }.toArray(new URL[0])
        new URLClassLoader(jars, parent)
    }
}
