package io.github.cosmicsilence.scalafix

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.junit.Rule
import org.junit.rules.TemporaryFolder

import spock.lang.Specification

abstract class ScalafixPluginFunctionalTest extends Specification {

    @Rule
    public final TemporaryFolder testProjectDir = new TemporaryFolder()
    private File settingsFile
    private File buildFile
    private File scalafixConfFile

    def setup() {
        settingsFile = testProjectDir.newFile("settings.gradle")
        settingsFile.write "rootProject.name = 'hello-world'"

        buildFile = testProjectDir.newFile("build.gradle")
        buildFile.write '''
plugins {
    id 'scala\'
    id 'io.github.cosmicsilence.scalafix\'
}

repositories {
    mavenCentral()
}

dependencies {
    implementation "org.scala-lang:scala-library:2.12.10"
}

tasks.withType(ScalaCompile) {
    // needed for RemoveUnused rule
    scalaCompileOptions.additionalParameters = [ "-Ywarn-unused" ]
}
'''

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
        final File src = createSourceFile '''
import scala.util.Random

object HelloWorld
'''

        when:
        runGradleTask('scalafix', [ ])

        then:
        src.getText() == '''

object HelloWorld
'''
    }

    def 'checkScalafix should run semantic rewrite rule and fail the build without fixing input source files'() {
        given:
        scalafixConfFile.write "rules = [ RemoveUnused ]"
        final File src = createSourceFile '''
import scala.util.Random

object HelloWorld
'''

        when:
        runGradleTask('checkScalafix', [ ])

        then:
        // code is not changed on disk
        src.getText() == '''
import scala.util.Random

object HelloWorld
'''
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
        final File src = createSourceFile '''
import scala.util.Random

object HelloWorld
'''

        when:
        runGradleTask('scalafix', [ ])

        then:
        src.getText() == '''
import scala.util.Random

object HelloWorld
'''
    }


    def 'checkScalafix should skip excluded source files'() {
        given:
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
        final File src = createSourceFile '''
import scala.util.Random

object HelloWorld
'''
        when:
        final BuildResult buildResult = runGradleTask('checkScalafix', [ ])

        then:
        buildResult.output.contains("BUILD SUCCESSFUL")
        src.getText() == '''
import scala.util.Random

object HelloWorld
'''
    }

    def 'scalafix should run syntactic linter rule and fail the build if any violation is reported'() {
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
        final File src = createSourceFile '''
object HelloWorld {
  var msg: String = "hello, world!"
}
'''

        when:
        runGradleTask('scalafix', [ ])

        then:
        src.getText() == '''
object HelloWorld {
  var msg: String = "hello, world!"
}
'''
        final UnexpectedBuildFailure err = thrown()
        err.message.contains("A linter error was reported")
        err.message.contains("error: [DisableSyntax.var] mutable state should be avoided")
    }


    def 'checkScalafix should run syntactic linter rule and fail the build if any violation is reported'() {
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
        final File src = createSourceFile '''
object HelloWorld {
  var msg: String = "hello, world!"
}
'''

        when:
        runGradleTask('checkScalafix', [ ])

        then:
        src.getText() == '''
object HelloWorld {
  var msg: String = "hello, world!"
}
'''
        final UnexpectedBuildFailure err = thrown()
        err.message.contains("A linter error was reported")
        err.message.contains("error: [DisableSyntax.var] mutable state should be avoided")
    }



    def 'scalafix should run semantic rewrite rule and leave wart-free code unchanged'() {
        given:
        scalafixConfFile.write "rules = [ RemoveUnused ]"
        final File src = createSourceFile '''
import scala.util.Random

object HelloWorld {
  val i: Int = Random.nextInt()
}
'''
        when:
        final BuildResult buildResult = runGradleTask('scalafix', [ ])

        then:
        buildResult.output.contains("BUILD SUCCESSFUL")
        src.getText() == '''
import scala.util.Random

object HelloWorld {
  val i: Int = Random.nextInt()
}
'''
    }


    def 'checkScalafix should run syntactic linter rule and succeed if there are no violations'() {
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
        final File src = createSourceFile '''
object HelloWorld {
  val i: Int = 3
}
'''
        when:
        final BuildResult buildResult = runGradleTask('checkScalafix', [ ])

        then:
        buildResult.output.contains("BUILD SUCCESSFUL")
        src.getText() == '''
object HelloWorld {
  val i: Int = 3
}
'''
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
                .withGradleVersion(getGradleVersion())
                .withPluginClasspath()
                .build()
    }


    /**
     * Writes the given content to
     * @param content
     * @return
     */
    private File createSourceFile(String content) {
        // write a minimal scala source file with an unused import
        final File scalaSrcDir = testProjectDir.newFolder("src", "main", "scala", "io", "github", "cosmicsilence", "scalafix")
        final File scalaSrcFile = new File(scalaSrcDir.absolutePath +  "/package.scala")
        scalaSrcFile.write content
        scalaSrcFile
    }

    abstract String getGradleVersion();
}

class ScalafixPluginFunctionalTestGradle6 extends ScalafixPluginFunctionalTest {
    @Override
    String getGradleVersion() {
        return '6.0'
    }
}

class ScalafixPluginFunctionalTestGradle5 extends ScalafixPluginFunctionalTest {
    @Override
    String getGradleVersion() {
        return '5.1'
    }
}