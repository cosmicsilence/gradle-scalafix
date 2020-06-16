package io.github.cosmicsilence.scalafix

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import spock.lang.Unroll

class ScalafixPluginFunctionalTest extends Specification {

    def 'scalafixMain task should run compileScala by default'() {
        given:
        TemporaryFolder projectDir = createScalaProject()

        when:
        BuildResult buildResult = runGradle(projectDir, 'scalafix', '-m')

        then:
        buildResult.output.contains(':compileScala SKIPPED')
        buildResult.output.contains(':compileTestScala SKIPPED')
        buildResult.output.contains(':scalafixMain SKIPPED')
        buildResult.output.contains(':scalafixTest SKIPPED')
    }

    def 'scalafixMain task should run compileScala when autoConfigureSemanticdb is enabled in the scalafix extension'() {
        given:
        TemporaryFolder projectDir = createScalaProject('scalafix { autoConfigureSemanticdb = true }')

        when:
        BuildResult buildResult = runGradle(projectDir, 'scalafix', '-m')

        then:
        buildResult.output.contains(':compileScala SKIPPED')
        buildResult.output.contains(':compileTestScala SKIPPED')
        buildResult.output.contains(':scalafixMain SKIPPED')
        buildResult.output.contains(':scalafixTest SKIPPED')
    }

    def 'scalafixMain task should not run compileScala when autoConfigureSemanticdb is disabled in the scalafix extension'() {
        given:
        TemporaryFolder projectDir = createScalaProject('scalafix { autoConfigureSemanticdb = false }')

        when:
        BuildResult buildResult = runGradle(projectDir, 'scalafix', '-m')

        then:
        !buildResult.output.contains(':compileScala SKIPPED')
        !buildResult.output.contains(':compileTestScala SKIPPED')
        buildResult.output.contains(':scalafixMain SKIPPED')
        buildResult.output.contains(':scalafixTest SKIPPED')
    }

    def 'checkScalafix task should run compileScala by default'() {
        given:
        TemporaryFolder projectDir = createScalaProject()

        when:
        BuildResult buildResult = runGradle(projectDir, 'checkScalafix', '-m')

        then:
        buildResult.output.contains(':compileScala SKIPPED')
        buildResult.output.contains(':compileTestScala SKIPPED')
        buildResult.output.contains(':checkScalafixMain SKIPPED')
        buildResult.output.contains(':checkScalafixTest SKIPPED')
    }

    def 'checkScalafix task should run compileScala when autoConfigureSemanticdb is enabled in the scalafix extension'() {
        given:
        TemporaryFolder projectDir = createScalaProject('scalafix { autoConfigureSemanticdb = true }')

        when:
        BuildResult buildResult = runGradle(projectDir, 'checkScalafix', '-m')

        then:
        buildResult.output.contains(':compileScala SKIPPED')
        buildResult.output.contains(':compileTestScala SKIPPED')
        buildResult.output.contains(':checkScalafixMain SKIPPED')
        buildResult.output.contains(':checkScalafixTest SKIPPED')
    }

    def 'checkScalafix task should not run compileScala when autoConfigureSemanticdb is disabled in the scalafix extension'() {
        given:
        TemporaryFolder projectDir = createScalaProject('scalafix { autoConfigureSemanticdb = false }')

        when:
        BuildResult buildResult = runGradle(projectDir, 'checkScalafix', '-m')

        then:
        !buildResult.output.contains(':compileScala SKIPPED')
        !buildResult.output.contains(':compileTestScala SKIPPED')
        buildResult.output.contains(':checkScalafixMain SKIPPED')
        buildResult.output.contains(':checkScalafixTest SKIPPED')
    }

    def 'scalafix<SourceSet> task should be created and run compile<SourceSet>Scala by default when additional source set exists in the build script'() {
        given:
        TemporaryFolder projectDir = createScalaProject('sourceSets { integTest { } }')

        when:
        BuildResult buildResult = runGradle(projectDir, 'scalafix', '-m')

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
        TemporaryFolder projectDir = createScalaProject('sourceSets { integTest { } }')

        when:
        BuildResult buildResult = runGradle(projectDir, 'checkScalafix', '-m')

        then:
        buildResult.output.contains(':compileScala SKIPPED')
        buildResult.output.contains(':compileTestScala SKIPPED')
        buildResult.output.contains(':compileIntegTestScala SKIPPED')
        buildResult.output.contains(':checkScalafixMain SKIPPED')
        buildResult.output.contains(':checkScalafixTest SKIPPED')
        buildResult.output.contains(':checkScalafixIntegTest SKIPPED')
    }

    def 'check task should run checkScalafix tasks but not scalafix'() {
        given:
        TemporaryFolder projectDir = createScalaProject()

        when:
        BuildResult buildResult = runGradle(projectDir, 'check', '-m')

        then:
        buildResult.output.contains(':checkScalafixMain SKIPPED')
        buildResult.output.contains(':checkScalafixTest SKIPPED')
        buildResult.output.contains(':checkScalafix SKIPPED')
        buildResult.output.contains(':check SKIPPED')
        !buildResult.output.contains(':scalafix')
        !buildResult.output.contains(':scalafixMain')
        !buildResult.output.contains(':scalafixTest')
    }

    def 'all scalafix tasks should be grouped'() {
        given:
        TemporaryFolder projectDir = createScalaProject('sourceSets { foo { } }')

        when:
        BuildResult buildResult = runGradle(projectDir, 'tasks')

        then:
        buildResult.output.contains '''
Scalafix tasks
--------------
checkScalafix - Fails if running Scalafix produces a diff or a linter error message. Won't write to files
checkScalafixFoo - Fails if running Scalafix produces a diff or a linter error message. Won't write to files in 'foo'
checkScalafixMain - Fails if running Scalafix produces a diff or a linter error message. Won't write to files in 'main'
checkScalafixTest - Fails if running Scalafix produces a diff or a linter error message. Won't write to files in 'test'
scalafix - Runs Scalafix on Scala sources
scalafixFoo - Runs Scalafix on Scala sources in 'foo'
scalafixMain - Runs Scalafix on Scala sources in 'main'
scalafixTest - Runs Scalafix on Scala sources in 'test'
'''
    }

    def 'A warning message should inform when the SemanticDB compiler plugin is not auto-configured because the Scala version is not supported'() {
        given:
        TemporaryFolder projectDir = createScalaProject('scalafix { autoConfigureSemanticdb = true }', '2.10.7')

        when:
        BuildResult buildResult = runGradle(projectDir, 'scalafix')

        then:
        buildResult.output.contains "WARNING: The SemanticDB compiler plugin could not be auto-configured because the " +
                "Scala version used in source set 'main' is unsupported or could not be determined (value=2.10.7)"
        buildResult.output.contains "WARNING: The SemanticDB compiler plugin could not be auto-configured because the " +
                "Scala version used in source set 'test' is unsupported or could not be determined (value=2.10.7)"
    }

    def '*.semanticdb files should be created during compilation when autoConfigureSemanticdb is true and scalafix task is run'() {
        given:
        TemporaryFolder projectDir = createScalaProject()
        File mainSrc = createSourceFile(projectDir, 'object Foo', 'main')
        File testSrc = createSourceFile(projectDir, 'object FooTest', 'test')
        File buildDir = new File(projectDir.root, 'build')

        when:
        runGradle(projectDir, 'scalafix')

        then:
        new File(buildDir, "classes/scala/main/META-INF/semanticdb/src/main/scala/dummy/${mainSrc.name}.semanticdb").exists()
        new File(buildDir, "classes/scala/test/META-INF/semanticdb/src/test/scala/dummy/${testSrc.name}.semanticdb").exists()
    }

    def '*.semanticdb files should not be created during compilation when autoConfigureSemanticdb is false and scalafix task is run'() {
        given:
        TemporaryFolder projectDir = createScalaProject('scalafix { autoConfigureSemanticdb = false }')
        createSourceFile(projectDir, 'object Foo', 'main')
        createSourceFile(projectDir, 'object FooTest', 'test')
        File buildDir = new File(projectDir.root, 'build')

        when:
        runGradle(projectDir, 'scalafix')

        then:
        !buildDir.exists()
    }

    def '*.semanticdb files should not be created during compilation when scalafix task is not run'() {
        given:
        TemporaryFolder projectDir = createScalaProject()
        createSourceFile(projectDir, 'object Foo', 'main')
        createSourceFile(projectDir, 'object FooTest', 'test')
        File buildDir = new File(projectDir.root, 'build')

        when:
        runGradle(projectDir, 'compileScala', 'compileTestScala')

        then:
        buildDir.eachFileRecurse {
            assert !it.name.endsWith('.semanticdb')
        }
    }

    def 'scalafix task should not fail when no rules are informed'() {
        given:
        TemporaryFolder projectDir = createScalaProject()
        createSourceFile(projectDir, 'object Foo')

        when:
        BuildResult buildResult = runGradle(projectDir, 'scalafix')

        then:
        buildResult.output.contains '''
> Task :scalafixMain
No Scalafix rules to run'''
        buildResult.output.contains 'BUILD SUCCESSFUL'
    }

    def 'checkScalafix task should not fail when no rules are informed'() {
        given:
        TemporaryFolder projectDir = createScalaProject()
        createSourceFile(projectDir, 'object Foo')

        when:
        BuildResult buildResult = runGradle(projectDir, 'checkScalafix')

        then:
        buildResult.output.contains '''
> Task :checkScalafixMain
No Scalafix rules to run'''
        buildResult.output.contains 'BUILD SUCCESSFUL'
    }

    def 'scalafix task should not fail when there is no source file to be processed'() {
        given:
        TemporaryFolder projectDir = createScalaProject()
        createScalafixConfig(projectDir, 'rules = [ RemoveUnused ]')

        when:
        BuildResult buildResult = runGradle(projectDir, 'scalafix')

        then:
        buildResult.output.contains ':scalafixMain NO-SOURCE'
        buildResult.output.contains ':scalafixTest NO-SOURCE'
        buildResult.output.contains ':scalafix UP-TO-DATE'
        buildResult.output.contains 'BUILD SUCCESSFUL'
    }

    def 'checkScalafix task should not fail when there is no source file to be processed'() {
        given:
        TemporaryFolder projectDir = createScalaProject()
        createScalafixConfig(projectDir, 'rules = [ RemoveUnused ]')

        when:
        BuildResult buildResult = runGradle(projectDir, 'checkScalafix')

        then:
        buildResult.output.contains ':checkScalafixMain NO-SOURCE'
        buildResult.output.contains ':checkScalafixTest NO-SOURCE'
        buildResult.output.contains ':checkScalafix UP-TO-DATE'
        buildResult.output.contains 'BUILD SUCCESSFUL'
    }

    def 'scalafix should run semantic rewrite rule and fix input source files'() {
        given:
        TemporaryFolder projectDir = createScalaProject()
        createScalafixConfig(projectDir, 'rules = [ RemoveUnused ]')
        File src = createSourceFile(projectDir, '''
import scala.util.Random
object HelloWorld
''')

        when:
        runGradle(projectDir, 'scalafix')

        then:
        src.getText() == '''
object HelloWorld
'''
    }

    def 'checkScalafix should run semantic rewrite rule and fail the build without fixing input source files'() {
        given:
        TemporaryFolder projectDir = createScalaProject()
        createScalafixConfig(projectDir, 'rules = [ RemoveUnused ]')
        String originalSrcContent = '''
import scala.util.Random
object HelloWorld
'''
        File srcFile = createSourceFile(projectDir, originalSrcContent)

        when:
        runGradle(projectDir, 'checkScalafix')

        then:
        UnexpectedBuildFailure err = thrown()
        err.message.contains 'A file on disk does not match the file contents if it was fixed with Scalafix'
        srcFile.getText() == originalSrcContent
    }

    def 'scalafix should fail to run semantic rules if the SemanticDB compiler plugin is not configured'() {
        given:
        TemporaryFolder projectDir = createScalaProject('scalafix { autoConfigureSemanticdb = false }')
        createScalafixConfig(projectDir, 'rules = [ RemoveUnused ]')
        createSourceFile(projectDir, 'object Foo')

        when:
        runGradle(projectDir, 'scalafix')

        then:
        UnexpectedBuildFailure err = thrown()
        err.message.contains 'A semantic rule was run on a source file that has no associated *.semanticdb file'
    }

    def 'checkScalafix should fail to run semantic rules if the SemanticDB compiler plugin is not configured'() {
        given:
        TemporaryFolder projectDir = createScalaProject('scalafix { autoConfigureSemanticdb = false }')
        createScalafixConfig(projectDir, 'rules = [ RemoveUnused ]')
        createSourceFile(projectDir, 'object Foo')

        when:
        runGradle(projectDir, 'checkScalafix')

        then:
        UnexpectedBuildFailure err = thrown()
        err.message.contains 'A semantic rule was run on a source file that has no associated *.semanticdb file'
    }

    def 'scalafix should skip non-included source files'() {
        given:
        TemporaryFolder projectDir = createScalaProject('scalafix { includes = ["**/animals/**"] }')
        createScalafixConfig(projectDir, 'rules = [ RemoveUnused ]')
        String dogSrcContent = '''
import scala.util.Random
object Dog
'''
        String dogTestSrcContent = '''
import scala.util.Random
object DogTest
'''
        String appleSrcContent = '''
import scala.util.Random
object Apple
'''
        String appleTestSrcContent = '''
import scala.util.Random
object AppleTest
'''
        File dogSrcFile = createSourceFile(projectDir, dogSrcContent, 'main', 'animals')
        File dogTestSrcFile = createSourceFile(projectDir, dogTestSrcContent, 'test', 'animals')
        File appleSrcFile = createSourceFile(projectDir, appleSrcContent, 'main', 'fruits')
        File appleTestSrcFile = createSourceFile(projectDir, appleTestSrcContent, 'test', 'fruits')

        when:
        BuildResult buildResult = runGradle(projectDir, 'scalafix')

        then:
        buildResult.output.contains 'BUILD SUCCESSFUL'
        buildResult.output.contains '''
> Task :scalafixMain
Running Scalafix on 1 Scala source file(s)
'''
        buildResult.output.contains '''
> Task :scalafixTest
Running Scalafix on 1 Scala source file(s)
'''
        dogSrcFile.getText() != dogSrcContent
        dogTestSrcFile.getText() != dogTestSrcContent
        appleSrcFile.getText() == appleSrcContent
        appleTestSrcFile.getText() == appleTestSrcContent
    }

    def 'checkScalafix should skip non-included source files'() {
        given:
        TemporaryFolder projectDir = createScalaProject('scalafix { includes = ["**/fruits/**"] }')
        createScalafixConfig(projectDir, 'rules = [ RemoveUnused ]')
        createSourceFile(projectDir, '''
import scala.util.Random
object Dog
''', 'main', 'animals')
        createSourceFile(projectDir, '''
import scala.util.Random
object DogTest
''', 'test', 'animals')
        createSourceFile(projectDir, 'object Apple', 'main', 'fruits')
        createSourceFile(projectDir, 'object AppleTest', 'test', 'fruits')

        when:
        BuildResult buildResult = runGradle(projectDir, 'checkScalafix')

        then:
        buildResult.output.contains 'BUILD SUCCESSFUL' // offending files in 'animals' are skipped
        buildResult.output.contains '''
> Task :checkScalafixMain
Running Scalafix on 1 Scala source file(s)
'''
        buildResult.output.contains '''
> Task :checkScalafixTest
Running Scalafix on 1 Scala source file(s)
'''
    }

    def 'scalafix should skip excluded source files'() {
        given:
        TemporaryFolder projectDir = createScalaProject('scalafix { excludes = ["**/dummy/**"] }')
        createScalafixConfig(projectDir, 'rules = [ RemoveUnused ]')
        String originalSrcContent = '''
import scala.util.Random
object HelloWorld
'''
        File srcFile = createSourceFile(projectDir, originalSrcContent)

        when:
        BuildResult buildResult = runGradle(projectDir, 'scalafix')

        then:
        buildResult.output.contains 'BUILD SUCCESSFUL'
        srcFile.getText() == originalSrcContent
    }

    def 'checkScalafix should skip excluded source files'() {
        given:
        TemporaryFolder projectDir = createScalaProject('''
scalafix {
  excludes = ["**/dummy/**"]
  autoConfigureSemanticdb = false
}
''')
        createScalafixConfig(projectDir, '''
rules = [ DisableSyntax ]
DisableSyntax.noVars = true
''')
        String originalSrcContent = '''
object HelloWorld {
  var i: Int = 5
}
'''
        File srcFile = createSourceFile(projectDir, originalSrcContent)

        when:
        BuildResult buildResult = runGradle(projectDir, 'checkScalafix')

        then:
        buildResult.output.contains 'BUILD SUCCESSFUL'
        srcFile.getText() == originalSrcContent
    }

    def 'scalafix should run syntactic linter rule and fail the build if any violation is reported'() {
        given:
        TemporaryFolder projectDir = createScalaProject('scalafix { autoConfigureSemanticdb = false }')
        createScalafixConfig(projectDir, '''
rules = [ DisableSyntax ]
DisableSyntax.noVars = true
''')
        String originalSrcContent = '''
object HelloWorld {
  var msg: String = "hello, world!"
}
'''
        File srcFile = createSourceFile(projectDir, originalSrcContent)

        when:
        runGradle(projectDir, 'scalafix')

        then:
        UnexpectedBuildFailure err = thrown()
        err.message.contains 'A linter error was reported'
        err.message.contains 'error: [DisableSyntax.var] mutable state should be avoided'
        srcFile.getText() == originalSrcContent
    }

    def 'checkScalafix should run syntactic linter rule and fail the build if any violation is reported'() {
        given:
        TemporaryFolder projectDir = createScalaProject('scalafix { autoConfigureSemanticdb = false }')
        createScalafixConfig(projectDir, '''
rules = [ DisableSyntax ]
DisableSyntax.noVars = true
''')
        String originalSrcContent = '''
object HelloWorld {
  var msg: String = "hello, world!"
}
'''
        File srcFile = createSourceFile(projectDir, originalSrcContent)

        when:
        runGradle(projectDir, 'checkScalafix')

        then:
        UnexpectedBuildFailure err = thrown()
        err.message.contains 'A linter error was reported'
        err.message.contains 'error: [DisableSyntax.var] mutable state should be avoided'
        srcFile.getText() == originalSrcContent
    }

    def 'scalafix should run semantic rewrite rule and leave wart-free code unchanged'() {
        given:
        TemporaryFolder projectDir = createScalaProject()
        createScalafixConfig(projectDir, 'rules = [ RemoveUnused ]')
        String originalSrcContent = '''
import scala.util.Random
object HelloWorld {
  val i: Int = Random.nextInt()
}
'''
        File srcFile = createSourceFile(projectDir, originalSrcContent)

        when:
        BuildResult buildResult = runGradle(projectDir, 'scalafix')

        then:
        buildResult.output.contains 'BUILD SUCCESSFUL'
        srcFile.getText() == originalSrcContent
    }

    def 'checkScalafix should run syntactic linter rule and succeed if there are no violations'() {
        given:
        TemporaryFolder projectDir = createScalaProject('scalafix { autoConfigureSemanticdb = false }')
        createScalafixConfig(projectDir, '''
rules = [ DisableSyntax ]
DisableSyntax.noVars = true
''')
        String originalSrcContent = '''
object HelloWorld {
  val i: Int = 3
}
'''
        File srcFile = createSourceFile(projectDir, originalSrcContent)

        when:
        BuildResult buildResult = runGradle(projectDir, 'checkScalafix')

        then:
        buildResult.output.contains 'BUILD SUCCESSFUL'
        srcFile.getText() == originalSrcContent
    }

    def 'scalafix should run single rule informed via command line even when it is not defined in the config file'() {
        given:
        TemporaryFolder projectDir = createScalaProject()
        File src = createSourceFile(projectDir, '''
import scala.util.Random
object Foo {
  def proc { }
}
''')

        when:
        BuildResult buildResult = runGradle(projectDir, 'scalafix', '-Pscalafix.rules=ProcedureSyntax')

        then:
        buildResult.output.contains 'BUILD SUCCESSFUL'
        src.getText() == '''
import scala.util.Random
object Foo {
  def proc: Unit = { }
}
'''
    }

    def 'scalafix should run multiple rules informed via command line even when they are not defined in the config file'() {
        given:
        TemporaryFolder projectDir = createScalaProject()
        File src = createSourceFile(projectDir, '''
import scala.util.Random
object Foo {
  def proc { }
}
''')

        when:
        BuildResult buildResult = runGradle(projectDir, 'scalafix', '-Pscalafix.rules=ProcedureSyntax,RemoveUnused')

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
        TemporaryFolder projectDir = createScalaProject()
        createScalafixConfig(projectDir, '''
rules = [
  DisableSyntax
  RemoveUnused
  ProcedureSyntax
]

DisableSyntax.noVars = true
''')
        File src = createSourceFile(projectDir, '''
import scala.util.Random
object Foo {
  var foo = "foo"
  def proc { }
}
''')

        when:
        BuildResult buildResult = runGradle(projectDir, 'scalafix', '-Pscalafix.rules=ProcedureSyntax')

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

    def 'scalafix should run rules defined in the config file if blank value is informed via command line'() {
        given:
        TemporaryFolder projectDir = createScalaProject()
        createScalafixConfig(projectDir, 'rules = [ RemoveUnused, ProcedureSyntax ]')
        File src = createSourceFile(projectDir, '''
import scala.util.Random
object Foo {
  def proc { }
}
''')

        when:
        BuildResult buildResult = runGradle(projectDir, 'scalafix', '-Pscalafix.rules=')

        then:
        buildResult.output.contains 'BUILD SUCCESSFUL'
        src.getText() == '''
object Foo {
  def proc: Unit = { }
}
'''
    }

    def 'scalafix should fail if invalid rule is informed via command line'() {
        given:
        TemporaryFolder projectDir = createScalaProject()
        createSourceFile(projectDir, 'object Bar')

        when:
        runGradle(projectDir, 'scalafix', '-Pscalafix.rules=Foo')

        then:
        UnexpectedBuildFailure err = thrown()
        err.message.contains "scalafix.internal.interfaces.ScalafixMainArgsException: Unknown rule 'Foo'"
    }

    def 'scalafix should fail if invalid rule is informed via config file'() {
        given:
        TemporaryFolder projectDir = createScalaProject()
        createScalafixConfig(projectDir, 'rules = [ Foo ]')
        createSourceFile(projectDir, 'object Bar')

        when:
        runGradle(projectDir, 'scalafix')

        then:
        UnexpectedBuildFailure err = thrown()
        err.message.contains "scalafix.internal.interfaces.ScalafixMainArgsException: Unknown rule 'Foo'"
    }

    @Unroll
    def 'scalafix should run built-in semantic and syntactic rules in projects using Scala #scalaVersion'() {
        given:
        TemporaryFolder projectDir = createScalaProject('', scalaVersion)
        createScalafixConfig(projectDir, '''
rules = [
  RemoveUnused
  NoAutoTupling
  DisableSyntax
  LeakingImplicitClassVal
  NoValInForComprehension
  ProcedureSyntax
]

DisableSyntax.noVars = true
''')
        createSourceFile(projectDir, 'object Dog ')
        createSourceFile(projectDir, 'object Cat')
        createSourceFile(projectDir, 'class Bird')
        createSourceFile(projectDir, 'class Fish')

        when:
        BuildResult buildResult = runGradle(projectDir, 'scalafix', '--stacktrace')

        then:
        buildResult.output.contains 'Running Scalafix on 4 Scala source file(s)'
        buildResult.output.contains 'BUILD SUCCESSFUL'

        where:
        scalaVersion || _
        '2.11.8'     || _
        '2.11.9'     || _
        '2.11.10'    || _
        '2.11.11'    || _
        '2.11.12'    || _
        '2.12.4'     || _
        '2.12.5'     || _
        '2.12.6'     || _
        '2.12.7'     || _
        '2.12.8'     || _
        '2.12.9'     || _
        '2.12.10'    || _
        '2.12.11'    || _
        '2.13.0'     || _
        '2.13.1'     || _
        '2.13.2'     || _
    }

    @Unroll
    def 'scalafix should run custom rules in projects using Scala #scalaVersion'() {
        given:
        TemporaryFolder projectDir = createScalaProject('''
dependencies {
    scalafix 'com.github.vovapolu:scaluzzi_2.12:0.1.4'
    scalafix 'com.nequissimus:sort-imports_2.12:0.3.2'
}
''', scalaVersion)
        createScalafixConfig(projectDir, 'rules = [ MissingFinal, SortImports ]')
        File srcFile = createSourceFile(projectDir, '''
import scala.concurrent.duration._
import java.lang.String
import scala.collection.immutable.List

sealed trait Animal
case class Dog(breed: String) extends Animal
case class Cat(breed: String) extends Animal
''')

        when:
        BuildResult buildResult = runGradle(projectDir, 'scalafix', '--stacktrace')

        then:
        buildResult.output.contains 'BUILD SUCCESSFUL'
        srcFile.getText() == '''
import java.lang.String
import scala.collection.immutable.List
import scala.concurrent.duration._

sealed trait Animal
final case class Dog(breed: String) extends Animal
final case class Cat(breed: String) extends Animal
'''

        where:
        scalaVersion || _
        '2.11.12'    || _
        '2.12.11'    || _
        '2.13.2'     || _
    }

    private BuildResult runGradle(TemporaryFolder projectDir, String... arguments) {
        return GradleRunner.create()
                .withProjectDir(projectDir.getRoot())
                .withArguments(arguments)
                .withGradleVersion(System.getProperty('compat.gradle.version'))
                .withPluginClasspath()
                .forwardOutput()
                .build()
    }

    private TemporaryFolder createScalaProject(String additionalBuildInstructions = '', String scalaVersion = '2.12.11') {
        TemporaryFolder projectDir = new TemporaryFolder()
        projectDir.create()
        projectDir.newFile("build.gradle").write """
plugins {
    id 'scala'
    id 'io.github.cosmicsilence.scalafix'
}

repositories {
    mavenCentral()
}

dependencies {
    implementation "org.scala-lang:scala-library:${scalaVersion}"
}

tasks.withType(ScalaCompile) {
    // needed for RemoveUnused rule
    scalaCompileOptions.additionalParameters = [ '-Ywarn-unused' ]
}

$additionalBuildInstructions
"""
        projectDir.newFile("settings.gradle").write "rootProject.name = 'hello-world'"
        projectDir
    }

    private File createScalafixConfig(TemporaryFolder projectDir, String content) {
        projectDir.newFile(".scalafix.conf").write content
    }

    private File createSourceFile(TemporaryFolder projectDir, String content, String sourceSet = 'main', String pkgName = "dummy") {
        File pkgFolder = new File(projectDir.root, "src/$sourceSet/scala/$pkgName")

        if (!pkgFolder.exists()) pkgFolder.mkdirs()

        File scalaSrcFile = new File(pkgFolder, "source_${new Random().nextInt(1000)}.scala")
        scalaSrcFile.write content
        scalaSrcFile
    }
}
