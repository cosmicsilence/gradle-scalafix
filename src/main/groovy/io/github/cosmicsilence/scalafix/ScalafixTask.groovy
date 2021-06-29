package io.github.cosmicsilence.scalafix

import io.github.cosmicsilence.compat.GradleCompat
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import scalafix.interfaces.Scalafix
import scalafix.interfaces.ScalafixMainMode
import scalafix.interfaces.ScalafixRule

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
    String sourceSetName

    @Input
    ScalafixMainMode mode

    @Input
    @Optional // made optional so that the task can return a more detailed error message
    final Property<String> scalaVersion = project.objects.property(String)

    @Input
    @Optional
    final ListProperty<String> compileOptions = project.objects.listProperty(String)

    @Input
    @Optional
    final ListProperty<String> classpath = project.objects.listProperty(String)

    @Input
    String sourceRoot

    @Input
    Boolean semanticdbConfigured

    @TaskAction
    void run() {
        if (source.empty) {
            logger.warn("No sources to be processed")
            return
        }

        if (scalaVersion.getOrElse("").blank) {
            throw new GradleException("Unable to detect the Scala version for the '$sourceSetName' source set. " +
                    "Please inform it via the 'scalaVersion' property in the scalafix extension or consider adding " +
                    "'$sourceSetName' to 'ignoreSourceSets'")
        }

        processSources()
    }

    private void processSources() {
        def sourcePaths = source.collect { it.toPath() }
        def configFilePath = java.util.Optional.ofNullable(configFile.getOrNull()).map { it.asFile.toPath() }
        def externalRulesConfiguration = project.configurations.getByName(ScalafixPlugin.EXTERNAL_RULES_CONFIGURATION)
        def scalafixCliCoordinates = ScalafixProps.getScalafixCliArtifactCoordinates(scalaVersion.get())

        logger.debug(
                """Running Scalafix with the following arguments:
                  | - Mode: ${mode}
                  | - Config file: ${configFilePath}
                  | - Scalafix cli artifact: ${scalafixCliCoordinates}
                  | - External rules classpath: ${externalRulesConfiguration.asPath}
                  | - Rules: ${rules.getOrNull()}
                  | - Scala version: ${scalaVersion.get()}
                  | - Scalac options: ${compileOptions.getOrNull()}
                  | - Source root: ${sourceRoot}
                  | - Sources: ${sourcePaths}
                  | - Classpath: ${classpath.getOrNull()}
                  |""".stripMargin())

        def scalafixClassloader = CachedClassloaders.forScalafixCli(project, scalafixCliCoordinates)
        def externalRulesClassloader = CachedClassloaders.forExternalRules(externalRulesConfiguration, scalafixClassloader)
        def scalafixArgs = Scalafix.classloadInstance(scalafixClassloader)
                .newArguments()
                .withMode(mode)
                .withConfig(configFilePath)
                .withRules(rules.getOrElse([]))
                .withSourceroot(Paths.get(sourceRoot))
                .withPaths(sourcePaths)
                .withToolClasspath(externalRulesClassloader)
                .withClasspath(classpath.getOrElse([]).collect { Paths.get(it) } )
                .withScalaVersion(scalaVersion.get())
                .withScalacOptions(compileOptions.getOrElse([]))

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
            def ruleNames = semanticRules.collect { it.name() }.join(", ")
            throw new GradleException("The semanticdb compiler plugin is required to run semantic rules such as $ruleNames. " +
                    "To fix this problem, please enable 'semanticdb.autoConfigure' in the plugin extension")
        }
    }
}
