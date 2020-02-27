package io.github.cosmicsilence.scalafix

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.gradle.testkit.runner.internal.DefaultGradleRunner
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import spock.lang.Unroll

class ScalafixPluginFunctionalTest extends Specification {

    def 'scalafixMain task should run compileScala by default'() {
        given:
        def projectDir = createScalaProject()

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
        def projectDir = createScalaProject('scalafix { autoConfigureSemanticdb = true }')

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
        def projectDir = createScalaProject('scalafix { autoConfigureSemanticdb = false }')

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
        def projectDir = createScalaProject()

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
        def projectDir = createScalaProject('scalafix { autoConfigureSemanticdb = true }')

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
        def projectDir = createScalaProject('scalafix { autoConfigureSemanticdb = false }')

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
        def projectDir = createScalaProject('sourceSets { integTest { } }')

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
        def projectDir = createScalaProject('sourceSets { integTest { } }')

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
        def projectDir = createScalaProject()

        when:
        BuildResult buildResult = runGradle(projectDir, 'check', '-m')

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
        def projectDir = createScalaProject('sourceSets { foo { } }')

        when:
        BuildResult buildResult = runGradle(projectDir, 'tasks')

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
        def projectDir = createScalaProject()
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
        def projectDir = createScalaProject('scalafix { autoConfigureSemanticdb = false }')
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
        def projectDir = createScalaProject()
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
        def projectDir = createScalaProject()
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
        def projectDir = createScalaProject()
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
        def projectDir = createScalaProject()
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
        def projectDir = createScalaProject()
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
        def projectDir = createScalaProject()
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
        def projectDir = createScalaProject()
        createScalafixConfig(projectDir, 'rules = [ RemoveUnused ]')
        def originalSrcContent = '''
import scala.util.Random
object HelloWorld
'''
        File srcFile = createSourceFile(projectDir, originalSrcContent)

        when:
        runGradle(projectDir, 'checkScalafix')

        then:
        UnexpectedBuildFailure err = thrown()
        err.message.contains('A file on disk does not match the file contents if it was fixed with Scalafix')
        srcFile.getText() == originalSrcContent
    }

    def 'scalafix should skip excluded source files'() {
        given:
        def projectDir = createScalaProject('scalafix { excludes = ["**/dummy/**"] }')
        createScalafixConfig(projectDir, 'rules = [ RemoveUnused ]')
        def originalSrcContent = '''
import scala.util.Random
object HelloWorld
'''
        def srcFile = createSourceFile(projectDir, originalSrcContent)

        when:
        BuildResult buildResult = runGradle(projectDir, 'scalafix')

        then:
        buildResult.output.contains("BUILD SUCCESSFUL")
        srcFile.getText() == originalSrcContent
    }

    def 'checkScalafix should skip excluded source files'() {
        given:
        def projectDir = createScalaProject('''
scalafix {
  excludes = ["**/dummy/**"]
  autoConfigureSemanticdb = false
}
''')
        createScalafixConfig(projectDir, '''
rules = [ DisableSyntax ]
DisableSyntax.noVars = true
''')
        def originalSrcContent = '''
object HelloWorld {
  var i: Int = 5
}
'''
        File srcFile = createSourceFile(projectDir, originalSrcContent)

        when:
        BuildResult buildResult = runGradle(projectDir, 'checkScalafix')

        then:
        buildResult.output.contains("BUILD SUCCESSFUL")
        srcFile.getText() == originalSrcContent
    }

    def 'scalafix should run syntactic linter rule and fail the build if any violation is reported'() {
        given:
        def projectDir = createScalaProject('scalafix { autoConfigureSemanticdb = false }')
        createScalafixConfig(projectDir, '''
rules = [ DisableSyntax ]
DisableSyntax.noVars = true
''')
        def originalSrcContent = '''
object HelloWorld {
  var msg: String = "hello, world!"
}
'''
        File srcFile = createSourceFile(projectDir, originalSrcContent)

        when:
        runGradle(projectDir, 'scalafix')

        then:
        UnexpectedBuildFailure err = thrown()
        err.message.contains('A linter error was reported')
        err.message.contains('error: [DisableSyntax.var] mutable state should be avoided')
        srcFile.getText() == originalSrcContent
    }

    def 'checkScalafix should run syntactic linter rule and fail the build if any violation is reported'() {
        given:
        def projectDir = createScalaProject('scalafix { autoConfigureSemanticdb = false }')
        createScalafixConfig(projectDir, '''
rules = [ DisableSyntax ]
DisableSyntax.noVars = true
''')
        def originalSrcContent = '''
object HelloWorld {
  var msg: String = "hello, world!"
}
'''
        File srcFile = createSourceFile(projectDir, originalSrcContent)

        when:
        runGradle(projectDir, 'checkScalafix')

        then:
        UnexpectedBuildFailure err = thrown()
        err.message.contains('A linter error was reported')
        err.message.contains('error: [DisableSyntax.var] mutable state should be avoided')
        srcFile.getText() == originalSrcContent
    }

    def 'scalafix should run semantic rewrite rule and leave wart-free code unchanged'() {
        given:
        def projectDir = createScalaProject()
        createScalafixConfig(projectDir, 'rules = [ RemoveUnused ]')
        def originalSrcContent = '''
import scala.util.Random
object HelloWorld {
  val i: Int = Random.nextInt()
}
'''
        File srcFile = createSourceFile(projectDir, originalSrcContent)

        when:
        BuildResult buildResult = runGradle(projectDir, 'scalafix')

        then:
        buildResult.output.contains("BUILD SUCCESSFUL")
        srcFile.getText() == originalSrcContent
    }

    def 'checkScalafix should run syntactic linter rule and succeed if there are no violations'() {
        given:
        def projectDir = createScalaProject('scalafix { autoConfigureSemanticdb = false }')
        createScalafixConfig(projectDir, '''
rules = [ DisableSyntax ]
DisableSyntax.noVars = true
''')
        def originalSrcContent = '''
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
        def projectDir = createScalaProject()
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

    def 'scalafix should run multiple rules informed via command line even when it is not defined in the config file'() {
        given:
        def projectDir = createScalaProject()
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
        def projectDir = createScalaProject()
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

    @Unroll
    def 'it should run semantic and syntactic rules in projects using Scala #scalaVersion'() {
        given:
        def projectDir = createScalaProject("", scalaVersion)
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
        createSourceFile(projectDir, 'trait Animal')
        createSourceFile(projectDir, 'object Dog extends Animal')
        createSourceFile(projectDir, 'object Cat extends Animal')
        createSourceFile(projectDir, 'class Bird extends Animal')

        when:
        BuildResult buildResult = runGradle(projectDir, 'scalafix', '--stacktrace')

        then:
        buildResult.output.contains('BUILD SUCCESSFUL')

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
        '2.13.0'     || _
        '2.13.1'     || _
    }

    private BuildResult runGradle(TemporaryFolder projectDir, String... arguments) {
        return new DefaultGradleRunner()
                .withJvmArguments('-XX:MaxMetaspaceSize=500m')
                .withProjectDir(projectDir.getRoot())
                .withArguments(arguments)
                .withGradleVersion(System.getProperty("compat.gradle.version"))
                .withPluginClasspath()
                .forwardOutput()
                .build()
    }

    private TemporaryFolder createScalaProject(String additionalBuildInstructions = "", String scalaVersion = "2.12.10") {
        def projectDir = new TemporaryFolder()
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
    scalaCompileOptions.additionalParameters = [ "-Ywarn-unused" ]
}

$additionalBuildInstructions
"""
        projectDir.newFile("settings.gradle").write "rootProject.name = 'hello-world'"
        projectDir
    }

    private File createScalafixConfig(TemporaryFolder projectDir, String content) {
        projectDir.newFile(".scalafix.conf").write content
    }

    private File createSourceFile(TemporaryFolder projectDir, String content, String sourceSet = 'main') {
        def pkgFolder = new File(projectDir.root, "src/$sourceSet/scala/dummy")

        if (!pkgFolder.exists()) pkgFolder.mkdirs()

        def scalaSrcFile = new File(pkgFolder, "source_${new Random().nextInt(1000)}.scala")
        scalaSrcFile.write content
        scalaSrcFile
    }
}
