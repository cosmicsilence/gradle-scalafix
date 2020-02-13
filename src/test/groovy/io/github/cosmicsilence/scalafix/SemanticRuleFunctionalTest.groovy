package io.github.cosmicsilence.scalafix

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

/**
 * Created by tomas.mccandless on 2/12/20.
 */
class SemanticRuleFunctionalTest extends Specification {

    @Rule
    public final TemporaryFolder testProjectDir = new TemporaryFolder()
    private File settingsFile
    private File buildFile
    private File scalaSrcFile


    def setup() {
        settingsFile = testProjectDir.newFile("settings.gradle")
        buildFile = testProjectDir.newFile("build.gradle")

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
        settingsFile.write "rootProject.name = 'hello-world'"

        // write a minimal scala source file with an unused import
        final File scalaSrcDir = testProjectDir.newFolder("src", "main", "scala", "io", "github", "cosmicsilence", "scalafix")
        scalaSrcFile = new File(scalaSrcDir.absolutePath +  "/HelloWorld.scala")
        scalaSrcFile.write '''
package io.github.cosmicsilence.scalafix

import scala.util.Random

object HelloWorld extends App {
  println("hello, world!")
}
'''
    }


    def 'checkScalafixMain task runs compileScala by default'() {
        when:
        BuildResult buildResult = runGradleTask('scalafix', [ "-Pscalafix.rules=RemoveUnused", "--stacktrace" ])

        then:
        println("----- start nested gradle build -----")
        println(buildResult.output)
        println("----- finish nested gradle build -----")
        scalaSrcFile.getText().equals('''
package io.github.cosmicsilence.scalafix


object HelloWorld extends App {
  println("hello, world!")
}
''')
    }

    // TODO duplicated code
    BuildResult runGradleTask(String task, List<String> arguments) {
        arguments.add(task)
        return GradleRunner.create()
                .withProjectDir(testProjectDir.getRoot())
                .withArguments(arguments)
                .withPluginClasspath()
                .build()

    }
}