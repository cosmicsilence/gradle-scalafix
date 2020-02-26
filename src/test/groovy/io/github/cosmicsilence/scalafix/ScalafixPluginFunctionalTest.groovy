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

    def 'scalafixMain task runs compileScala by default'() {
        when:
        BuildResult buildResult = runGradleTask('scalafix', ['-m'])

        then:
        buildResult.output.contains(':compileScala SKIPPED')
        buildResult.output.contains(':compileTestScala SKIPPED')
        buildResult.output.contains(':scalafixMain SKIPPED')
        buildResult.output.contains(':scalafixTest SKIPPED')
    }

    def 'scalafixMain task runs compileScala when autoConfigureSemanticdb is enabled in the scalafix extension'() {
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

    def 'scalafixMain task does not run compileScala when autoConfigureSemanticdb is disabled in the scalafix extension'() {
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

    def 'checkScalafix task runs compileScala by default'() {
        when:
        BuildResult buildResult = runGradleTask('checkScalafix', ['-m'])

        then:
        buildResult.output.contains(':compileScala SKIPPED')
        buildResult.output.contains(':compileTestScala SKIPPED')
        buildResult.output.contains(':checkScalafixMain SKIPPED')
        buildResult.output.contains(':checkScalafixTest SKIPPED')
    }

    def 'checkScalafix task runs compileScala when autoConfigureSemanticdb is enabled in the scalafix extension'() {
        given:
        buildFile.append '''
scalafix { autoConfigureSemanticdb = true }'''

        when:
        BuildResult buildResult = runGradleTask('checkScalafix', ['-m'])

        then:
        buildResult.output.contains(':compileScala SKIPPED')
        buildResult.output.contains(':compileTestScala SKIPPED')
        buildResult.output.contains(':checkScalafixMain SKIPPED')
        buildResult.output.contains(':checkScalafixTest SKIPPED')
    }

    def 'checkScalafix task does not run compileScala when autoConfigureSemanticdb is disabled in the scalafix extension'() {
        given:
        buildFile.append '''
scalafix { autoConfigureSemanticdb = false }'''

        when:
        BuildResult buildResult = runGradleTask('checkScalafix', ['-m'])

        then:
        !buildResult.output.contains(':compileScala SKIPPED')
        !buildResult.output.contains(':compileTestScala SKIPPED')
        buildResult.output.contains(':checkScalafixMain SKIPPED')
        buildResult.output.contains(':checkScalafixTest SKIPPED')
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

    def 'scalafix should run semantic rewrite rule and fix input source files'() {
        given:
        scalafixConfFile.write "rules = [ RemoveUnused ]"

        when:
        runGradleTask('scalafix', [ ])

        then:
        scalaSrcFile.getText() == ScalafixTestConstants.FIXED_SCALA_CODE
    }

    def 'checkScalafix should run semantic rewrite rule and fail the build without fixing input source files'() {
        given:
        scalafixConfFile.write "rules = [ RemoveUnused ]"

        when:
        runGradleTask('checkScalafix', [ ])

        then:
        // code is not changed on disk
        scalaSrcFile.getText() == ScalafixTestConstants.FLAWED_SCALA_CODE
        final UnexpectedBuildFailure err = thrown()
        err.message.contains("A file on disk does not match the file contents if it was fixed with Scalafix")
    }


    def 'scalafix should skip excluded source files'() {
        given:
        scalafixConfFile.write "rules = [ RemoveUnused ]"
        buildFile.append '''
scalafix {
  excludes = ["**/scalafix/**"]
}
'''
        when:
        runGradleTask('scalafix', [ ])

        then:
        scalaSrcFile.getText() == ScalafixTestConstants.FLAWED_SCALA_CODE
    }


    def 'checkScalafix should skip excluded source files'() {
        scalafixConfFile.write '''
rules = [ DisableSyntax ]
DisableSyntax.noVars = true
'''
        buildFile.append '''
scalafix {
  excludes = ["**/scalafix/**"]
  autoConfigureSemanticdb = false
}
'''
        when:
        final BuildResult buildResult = runGradleTask('checkScalafix', [ ])

        then:
        buildResult.output.contains("BUILD SUCCESSFUL")
        scalaSrcFile.getText() == ScalafixTestConstants.FLAWED_SCALA_CODE
    }

    def 'scalafix should run syntactic linter rule and fails the build if any violation is reported'() {
        given:
        scalafixConfFile.write '''
rules = [ DisableSyntax ]
DisableSyntax.noVars = true
'''
        buildFile.append '''
scalafix {
  autoConfigureSemanticdb = false
}
'''

        when:
        runGradleTask('scalafix', [ ])

        then:
        scalaSrcFile.getText() == ScalafixTestConstants.FLAWED_SCALA_CODE
        final UnexpectedBuildFailure err = thrown()
        err.message.contains("A linter error was reported")
        err.message.contains("error: [DisableSyntax.var] mutable state should be avoided")
    }


    def 'checkScalafix should run syntactic linter rule and fails the build if any violation is reported'() {
        given:
        scalafixConfFile.write '''
rules = [ DisableSyntax ]
DisableSyntax.noVars = true
'''
        buildFile.append '''
scalafix {
  autoConfigureSemanticdb = false
}
'''

        when:
        runGradleTask('checkScalafix', [ ])

        then:
        scalaSrcFile.getText() == ScalafixTestConstants.FLAWED_SCALA_CODE
        final UnexpectedBuildFailure err = thrown()
        err.message.contains("A linter error was reported")
        err.message.contains("error: [DisableSyntax.var] mutable state should be avoided")
    }


    def 'all tasks should be grouped'() {
        given:
        buildFile.append '''
sourceSets {
    foo { }
}'''

        when:
        BuildResult buildResult = runGradleTask('tasks', [])

        then:
        buildResult.output.contains(
                """Scalafix tasks
                  |--------------
                  |checkScalafix - Fails if running Scalafix produces a diff or a linter error message. Won't write to files
                  |checkScalafixFoo - Fails if running Scalafix produces a diff or a linter error message. Won't write to files in 'foo'
                  |checkScalafixMain - Fails if running Scalafix produces a diff or a linter error message. Won't write to files in 'main'
                  |checkScalafixTest - Fails if running Scalafix produces a diff or a linter error message. Won't write to files in 'test'
                  |scalafix - Runs Scalafix on Scala sources
                  |scalafixFoo - Runs Scalafix on Scala sources in 'foo'
                  |scalafixMain - Runs Scalafix on Scala sources in 'main'
                  |scalafixTest - Runs Scalafix on Scala sources in 'test'""".stripMargin())
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
