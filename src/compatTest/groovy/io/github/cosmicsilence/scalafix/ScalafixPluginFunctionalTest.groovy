package io.github.cosmicsilence.scalafix

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.gradle.testkit.runner.internal.DefaultGradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder

import spock.lang.Specification

class ScalafixPluginFunctionalTest extends Specification {

    @Rule
    public final TemporaryFolder testProjectDir = new TemporaryFolder()
    private File settingsFile
    private File buildFile
    private File scalafixConfFile

    def setup() {
        settingsFile = testProjectDir.newFile('settings.gradle')
        settingsFile.write "rootProject.name = 'hello-world'"

        buildFile = testProjectDir.newFile('build.gradle')
        buildFile.write '''
plugins {
    id 'scala'
    id 'io.github.cosmicsilence.scalafix'
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

        scalafixConfFile = testProjectDir.newFile('.scalafix.conf')
    }

    def 'scalafixMain task should run compileScala by default'() {
        when:
        BuildResult buildResult = runGradle('scalafix', '-m')

        then:
        buildResult.output.contains(':compileScala SKIPPED')
        buildResult.output.contains(':compileTestScala SKIPPED')
        buildResult.output.contains(':scalafixMain SKIPPED')
        buildResult.output.contains(':scalafixTest SKIPPED')
    }

    def 'scalafixMain task should run compileScala when autoConfigureSemanticdb is enabled in the scalafix extension'() {
        given:
        buildFile.append '''
scalafix { autoConfigureSemanticdb = true }'''

        when:
        BuildResult buildResult = runGradle('scalafix', '-m')

        then:
        buildResult.output.contains(':compileScala SKIPPED')
        buildResult.output.contains(':compileTestScala SKIPPED')
        buildResult.output.contains(':scalafixMain SKIPPED')
        buildResult.output.contains(':scalafixTest SKIPPED')
    }

    def 'scalafixMain task should not run compileScala when autoConfigureSemanticdb is disabled in the scalafix extension'() {
        given:
        buildFile.append '''
scalafix { autoConfigureSemanticdb = false }'''

        when:
        BuildResult buildResult = runGradle('scalafix', '-m')

        then:
        !buildResult.output.contains(':compileScala SKIPPED')
        !buildResult.output.contains(':compileTestScala SKIPPED')
        buildResult.output.contains(':scalafixMain SKIPPED')
        buildResult.output.contains(':scalafixTest SKIPPED')
    }

    def 'checkScalafix task should run compileScala by default'() {
        when:
        BuildResult buildResult = runGradle('checkScalafix', '-m')

        then:
        buildResult.output.contains(':compileScala SKIPPED')
        buildResult.output.contains(':compileTestScala SKIPPED')
        buildResult.output.contains(':checkScalafixMain SKIPPED')
        buildResult.output.contains(':checkScalafixTest SKIPPED')
    }

    def 'checkScalafix task should run compileScala when autoConfigureSemanticdb is enabled in the scalafix extension'() {
        given:
        buildFile.append '''
scalafix { autoConfigureSemanticdb = true }'''

        when:
        BuildResult buildResult = runGradle('checkScalafix', '-m')

        then:
        buildResult.output.contains(':compileScala SKIPPED')
        buildResult.output.contains(':compileTestScala SKIPPED')
        buildResult.output.contains(':checkScalafixMain SKIPPED')
        buildResult.output.contains(':checkScalafixTest SKIPPED')
    }

    def 'checkScalafix task should not run compileScala when autoConfigureSemanticdb is disabled in the scalafix extension'() {
        given:
        buildFile.append '''
scalafix { autoConfigureSemanticdb = false }'''

        when:
        BuildResult buildResult = runGradle('checkScalafix', '-m')

        then:
        !buildResult.output.contains(':compileScala SKIPPED')
        !buildResult.output.contains(':compileTestScala SKIPPED')
        buildResult.output.contains(':checkScalafixMain SKIPPED')
        buildResult.output.contains(':checkScalafixTest SKIPPED')
    }

    def 'scalafix<SourceSet> task should be created and run compile<SourceSet>Scala by default when additional source set exists in the build script'() {
        given:
        buildFile.append '''
sourceSets {
    integTest { }
}'''

        when:
        BuildResult buildResult = runGradle('scalafix', '-m')

        then:
        buildResult.output.contains(':compileScala SKIPPED')
        buildResult.output.contains(':compileTestScala SKIPPED')
        buildResult.output.contains(':compileIntegTestScala SKIPPED')
        buildResult.output.contains(':scalafixMain SKIPPED')
        buildResult.output.contains(':scalafixTest SKIPPED')
        buildResult.output.contains(':scalafixIntegTest SKIPPED')
    }

    def 'checkScalafix<SourceSet> task should be created and run compile<SourceSet>Scala by default when additional source set exists in the build script'() {
        given:
        buildFile.append '''
sourceSets {
    integTest { }
}'''

        when:
        BuildResult buildResult = runGradle('checkScalafix', '-m')

        then:
        buildResult.output.contains(':compileScala SKIPPED')
        buildResult.output.contains(':compileTestScala SKIPPED')
        buildResult.output.contains(':compileIntegTestScala SKIPPED')
        buildResult.output.contains(':checkScalafixMain SKIPPED')
        buildResult.output.contains(':checkScalafixTest SKIPPED')
        buildResult.output.contains(':checkScalafixIntegTest SKIPPED')
    }

    def 'check task should run checkScalafix tasks but not scalafix'() {
        when:
        BuildResult buildResult = runGradle('check', '-m')

        then:
        buildResult.output.contains(':checkScalafixMain SKIPPED')
        buildResult.output.contains(':checkScalafixTest SKIPPED')
        buildResult.output.contains(':checkScalafix SKIPPED')
        buildResult.output.contains(':check SKIPPED')
        !buildResult.output.contains(':scalafix SKIPPED')
        !buildResult.output.contains(':scalafixMain SKIPPED')
        !buildResult.output.contains(':scalafixTest SKIPPED')
    }

    def 'all scalafix tasks should be grouped'() {
        given:
        buildFile.append '''
sourceSets {
    foo { }
}'''

        when:
        BuildResult buildResult = runGradle('tasks')

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

    def '*.semanticdb files should be created during compilation when autoConfigureSemanticdb is true and scalafix task is run'() {
        given:
        File mainSrc = createSourceFile('object Foo', 'main')
        File testSrc = createSourceFile('object FooTest', 'test')
        File buildDir = new File(testProjectDir.root, 'build')

        when:
        runGradle('scalafix')

        then:
        new File(buildDir, "classes/scala/main/META-INF/semanticdb/src/main/scala/dummy/${mainSrc.name}.semanticdb").exists()
        new File(buildDir, "classes/scala/test/META-INF/semanticdb/src/test/scala/dummy/${testSrc.name}.semanticdb").exists()
    }

    def '*.semanticdb files should not be created during compilation when autoConfigureSemanticdb is false and scalafix task is run'() {
        given:
        buildFile.append '''
scalafix { autoConfigureSemanticdb = false }'''
        createSourceFile('object Foo', 'main')
        createSourceFile('object FooTest', 'test')

        when:
        runGradle('scalafix')

        then:
        !new File(testProjectDir.root, 'build').exists()
    }

    def '*.semanticdb files should not be created during compilation when scalafix task is not run'() {
        given:
        createSourceFile('object Foo', 'main')
        createSourceFile('object FooTest', 'test')
        File buildDir = new File(testProjectDir.root, 'build')

        when:
        runGradle('compileScala', 'compileTestScala')

        then:
        buildDir.eachFileRecurse {
            assert !it.name.endsWith('.semanticdb')
        }
    }

    def 'scalafix task should not fail when no rules are informed'() {
        given:
        createSourceFile('object Foo')

        when:
        BuildResult buildResult = runGradle('scalafix')

        then:
        buildResult.output.contains '''
> Task :scalafixMain
No Scalafix rules to run'''
        buildResult.output.contains 'BUILD SUCCESSFUL'
    }

    def 'checkScalafix task should not fail when no rules are informed'() {
        given:
        createSourceFile('object Foo')

        when:
        BuildResult buildResult = runGradle('checkScalafix')

        then:
        buildResult.output.contains '''
> Task :checkScalafixMain
No Scalafix rules to run'''
        buildResult.output.contains 'BUILD SUCCESSFUL'
    }

    def 'scalafix task should not fail when there is no source file to be processed'() {
        given:
        scalafixConfFile.write 'rules = [ RemoveUnused ]'

        when:
        BuildResult buildResult = runGradle('scalafix')

        then:
        buildResult.output.contains ':scalafixMain NO-SOURCE'
        buildResult.output.contains ':scalafixTest NO-SOURCE'
        buildResult.output.contains ':scalafix UP-TO-DATE'
        buildResult.output.contains 'BUILD SUCCESSFUL'
    }

    def 'checkScalafix task should not fail when there is no source file to be processed'() {
        given:
        scalafixConfFile.write 'rules = [ RemoveUnused ]'

        when:
        BuildResult buildResult = runGradle('checkScalafix')

        then:
        buildResult.output.contains ':checkScalafixMain NO-SOURCE'
        buildResult.output.contains ':checkScalafixTest NO-SOURCE'
        buildResult.output.contains ':checkScalafix UP-TO-DATE'
        buildResult.output.contains 'BUILD SUCCESSFUL'
    }

    def 'scalafix should run semantic rewrite rule and fix input source files'() {
        given:
        scalafixConfFile.write 'rules = [ RemoveUnused ]'
        File src = createSourceFile '''
import scala.util.Random

object HelloWorld
'''

        when:
        runGradle('scalafix')

        then:
        src.getText() == '''

object HelloWorld
'''
    }

    def 'checkScalafix should run semantic rewrite rule and fail the build without fixing input source files'() {
        given:
        scalafixConfFile.write 'rules = [ RemoveUnused ]'
        File src = createSourceFile '''
import scala.util.Random

object HelloWorld
'''

        when:
        runGradle('checkScalafix')

        then:
        // code is not changed on disk
        src.getText() == '''
import scala.util.Random

object HelloWorld
'''
        UnexpectedBuildFailure err = thrown()
        err.message.contains('A file on disk does not match the file contents if it was fixed with Scalafix')
    }

    def 'scalafix should skip excluded source files'() {
        given:
        scalafixConfFile.write 'rules = [ RemoveUnused ]'
        buildFile.append '''
scalafix {
  excludes = ["**/dummy/**"]
}
'''
        File src = createSourceFile '''
import scala.util.Random

object HelloWorld
'''

        when:
        runGradle('scalafix')

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
  excludes = ["**/dummy/**"]
  autoConfigureSemanticdb = false
}
'''
        File src = createSourceFile '''
object HelloWorld {
  var i: Int = 5
}
'''
        when:
        BuildResult buildResult = runGradle('checkScalafix')

        then:
        buildResult.output.contains 'BUILD SUCCESSFUL'
        src.getText() == '''
object HelloWorld {
  var i: Int = 5
}
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
        File src = createSourceFile '''
object HelloWorld {
  var msg: String = "hello, world!"
}
'''

        when:
        runGradle('scalafix')

        then:
        src.getText() == '''
object HelloWorld {
  var msg: String = "hello, world!"
}
'''
        UnexpectedBuildFailure err = thrown()
        err.message.contains('A linter error was reported')
        err.message.contains('error: [DisableSyntax.var] mutable state should be avoided')
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
        File src = createSourceFile '''
object HelloWorld {
  var msg: String = "hello, world!"
}
'''

        when:
        runGradle('checkScalafix')

        then:
        src.getText() == '''
object HelloWorld {
  var msg: String = "hello, world!"
}
'''
        UnexpectedBuildFailure err = thrown()
        err.message.contains('A linter error was reported')
        err.message.contains('error: [DisableSyntax.var] mutable state should be avoided')
    }

    def 'scalafix should run semantic rewrite rule and leave wart-free code unchanged'() {
        given:
        scalafixConfFile.write 'rules = [ RemoveUnused ]'
        File src = createSourceFile '''
import scala.util.Random

object HelloWorld {
  val i: Int = Random.nextInt()
}
'''
        when:
        BuildResult buildResult = runGradle('scalafix')

        then:
        buildResult.output.contains 'BUILD SUCCESSFUL'
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
        File src = createSourceFile '''
object HelloWorld {
  val i: Int = 3
}
'''
        when:
        BuildResult buildResult = runGradle('checkScalafix')

        then:
        buildResult.output.contains 'BUILD SUCCESSFUL'
        src.getText() == '''
object HelloWorld {
  val i: Int = 3
}
'''
    }

    def 'scalafix should run single rule informed via command line even when it is not defined in the config file'() {
        given:
        File src = createSourceFile '''
import scala.util.Random
object Foo {
  def proc { }
}
'''
        when:
        BuildResult buildResult = runGradle('scalafix', '-Pscalafix.rules=ProcedureSyntax')

        then:
        buildResult.output.contains 'BUILD SUCCESSFUL'
        src.getText() == '''
import scala.util.Random
object Foo {
  def proc: Unit = { }
}
'''
    }

    def 'scalafix should run multiple rules informed via command line even when it is not defined in the config file'() {
        given:
        File src = createSourceFile '''
import scala.util.Random
object Foo {
  def proc { }
}
'''
        when:
        BuildResult buildResult = runGradle('scalafix', '-Pscalafix.rules=ProcedureSyntax,RemoveUnused')

        then:
        buildResult.output.contains 'BUILD SUCCESSFUL'
        src.getText() == '''
object Foo {
  def proc: Unit = { }
}
'''
    }

    def 'scalafix should only run rule informed via command line when there are other rules defined in the config file'() {
        given:
        scalafixConfFile.write '''
rules = [
  DisableSyntax
  RemoveUnused
  ProcedureSyntax
]

DisableSyntax.noVars = true
'''
        File src = createSourceFile '''
import scala.util.Random
object Foo {
  var foo = "foo"
  def proc { }
}
'''
        when:
        BuildResult buildResult = runGradle('scalafix', '-Pscalafix.rules=ProcedureSyntax')

        then:
        buildResult.output.contains 'BUILD SUCCESSFUL'
        src.getText() == '''
import scala.util.Random
object Foo {
  var foo = "foo"
  def proc: Unit = { }
}
'''
    }

    private BuildResult runGradle(String... arguments) {
        return new DefaultGradleRunner()
                .withJvmArguments('-XX:MaxMetaspaceSize=500m')
                .withProjectDir(testProjectDir.getRoot())
                .withArguments(arguments.toList())
                .withGradleVersion(System.getProperty('compat.gradle.version'))
                .withPluginClasspath()
                .forwardOutput()
                .build()
    }

    private File createSourceFile(String content, String sourceSet = 'main') {
        def pkgFolder = new File(testProjectDir.root, "src/$sourceSet/scala/dummy")

        if (!pkgFolder.exists()) pkgFolder.mkdirs()

        def scalaSrcFile = new File(pkgFolder, "source_${new Random().nextInt(1000)}.scala")
        scalaSrcFile.write content
        scalaSrcFile
    }
}
