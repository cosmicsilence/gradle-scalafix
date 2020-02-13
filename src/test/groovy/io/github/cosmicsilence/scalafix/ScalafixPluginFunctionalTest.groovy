package io.github.cosmicsilence.scalafix

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.junit.Rule
import org.junit.rules.TemporaryFolder

import spock.lang.Specification

class ScalafixPluginFunctionalTest extends Specification {

    @Rule
    public final TemporaryFolder testProjectDir = new TemporaryFolder()
    private File settingsFile
    private File buildFile
    private File scalaSrcFile
    private File scalafixConfFile

    def setup() {
        settingsFile = testProjectDir.newFile("settings.gradle")
        buildFile = testProjectDir.newFile("build.gradle")

        buildFile.write ScalafixTestConstants.BUILD_SCRIPT
        settingsFile.write "rootProject.name = 'hello-world'"

        // write a minimal scala source file with an unused import
        final File scalaSrcDir = testProjectDir.newFolder("src", "main", "scala", "io", "github", "cosmicsilence", "scalafix")
        scalaSrcFile = new File(scalaSrcDir.absolutePath +  "/HelloWorld.scala")
        scalaSrcFile.write ScalafixTestConstants.FLAWED_SCALA_CODE

        scalafixConfFile = testProjectDir.newFile(ScalafixExtension.DEFAULT_CONFIG_FILE)
    }

    def 'checkScalafixMain task runs compileScala by default'() {
        when:
        BuildResult buildResult = runGradleTask('scalafix', ['-m'])

        then:
        buildResult.output.contains(':compileScala SKIPPED')
        buildResult.output.contains(':compileTestScala SKIPPED')
        buildResult.output.contains(':scalafixMain SKIPPED')
        buildResult.output.contains(':scalafixTest SKIPPED')
    }

    def 'checkScalafixMain task runs compileScala when autoConfigureSemanticdb is enabled in the scalafix extension'() {
        given:
        buildFile.append '''
scalafix { autoConfigureSemanticdb = true }'''

        when:
        BuildResult buildResult = runGradleTask('scalafix', ['-m'])

        then:
        buildResult.output.contains(':compileScala SKIPPED')
        buildResult.output.contains(':compileTestScala SKIPPED')
        buildResult.output.contains(':scalafixMain SKIPPED')
        buildResult.output.contains(':scalafixTest SKIPPED')
    }

    def 'checkScalafixMain task does not run compileScala when autoConfigureSemanticdb is disabled in the scalafix extension'() {
        given:
        buildFile.append '''
scalafix { autoConfigureSemanticdb = false }'''

        when:
        BuildResult buildResult = runGradleTask('scalafix', ['-m'])

        then:
        !buildResult.output.contains(':compileScala SKIPPED')
        !buildResult.output.contains(':compileTestScala SKIPPED')
        buildResult.output.contains(':scalafixMain SKIPPED')
        buildResult.output.contains(':scalafixTest SKIPPED')
    }

    def 'scalafix<SourceSet> task is created and runs compile<SourceSet>Scala by default when additional source set exists in the build script'() {
        given:
        buildFile.append '''
sourceSets {
    integTest { }
}'''

        when:
        BuildResult buildResult = runGradleTask('scalafix', ['-m'])

        then:
        buildResult.output.contains(':compileScala SKIPPED')
        buildResult.output.contains(':compileTestScala SKIPPED')
        buildResult.output.contains(':compileIntegTestScala SKIPPED')
        buildResult.output.contains(':scalafixMain SKIPPED')
        buildResult.output.contains(':scalafixTest SKIPPED')
        buildResult.output.contains(':scalafixIntegTest SKIPPED')
    }

    def 'checkScalafix<SourceSet> task is created and runs compile<SourceSet>Scala by default when additional source set exists in the build script'() {
        given:
        buildFile.append '''
sourceSets {
    integTest { }
}'''

        when:
        BuildResult buildResult = runGradleTask('checkScalafix', ['-m'])

        then:
        buildResult.output.contains(':compileScala SKIPPED')
        buildResult.output.contains(':compileTestScala SKIPPED')
        buildResult.output.contains(':compileIntegTestScala SKIPPED')
        buildResult.output.contains(':checkScalafixMain SKIPPED')
        buildResult.output.contains(':checkScalafixTest SKIPPED')
        buildResult.output.contains(':checkScalafixIntegTest SKIPPED')
    }

    def 'scalafix semantic rule RemoveUnused works'() {
        given:
        scalafixConfFile.write "rules = [ RemoveUnused ]"

        when:
        runGradleTask('scalafix', [ ])

        then:
        scalaSrcFile.getText() == ScalafixTestConstants.FIXED_SCALA_CODE
    }

    def 'scalafix syntactic rule DisableSyntax.noVars works'() {
        given:
        scalafixConfFile.write '''
rules = [ DisableSyntax ]
DisableSyntax.noVars = true
'''

        when:
        BuildResult buildResult = runGradleTask('scalafix', [ ])

        then:
        thrown(UnexpectedBuildFailure)
        // TODO should we check exception message?
        // buildResult.output.contains("error: [DisableSyntax.var] mutable state should be avoided")
    }


    BuildResult runGradleTask(String task, List<String> arguments) {
        arguments.add(task)
        return GradleRunner.create()
                .withProjectDir(testProjectDir.getRoot())
                .withArguments(arguments)
                .withPluginClasspath()
                .build()
    }
}
