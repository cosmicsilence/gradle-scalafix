package io.github.cosmicsilence.scalafix

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.scala.ScalaCompile
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification
import spock.lang.Unroll

class ConfigSemanticDbTaskTest extends Specification {

    private static final List<String> COMPILER_OPTS = ['-Xfoo']

    private Project project

    def setup() {
        project = buildScalaProject()
    }

    def 'should configure the SemanticDB compiler plugin for Scala 2.x'() {
        given:
        ScalaCompile compileTask = project.tasks.compileScala
        SourceSet ss = project.sourceSets.main
        Task task = project.tasks.create('config', ConfigSemanticDbTask, {
            sourceSetName.set(ss.name)
            scalaVersion.set('2.13.15')
            projectDirPath.set(project.projectDir.absolutePath)
            outputDir.set(compileTask.destinationDirectory)
        })

        when:
        task.run()

        then:
        compileTask.scalaCompileOptions.additionalParameters == COMPILER_OPTS + [
                '-Yrangepos', '-P:semanticdb:sourceroot:targetroot:../../../..'
        ]
        compileTask.scalaCompilerPlugins.find {
            it.absolutePath.endsWith("semanticdb-scalac_2.13.15-${ScalafixProps.scalametaVersion}.jar")
        }
        !compileTask.scalaCompilerPlugins.find { it.name.contains("scala-library") }
    }

    @Unroll
    def 'should configure the SemanticDB compiler plugin for Scala 2.x using the provided #semanticdbVersion version'() {
        given:
        ScalaCompile compileTask = project.tasks.compileScala
        SourceSet ss = project.sourceSets.main
        Task task = project.tasks.create('config', ConfigSemanticDbTask, {
            sourceSetName.set(ss.name)
            scalaVersion.set('2.13.15')
            semanticDbVersion.set(semanticdbVersion)
            projectDirPath.set(project.projectDir.absolutePath)
            outputDir.set(compileTask.destinationDirectory)
        })

        when:
        task.run()

        then:
        compileTask.scalaCompilerPlugins.find { it.absolutePath.endsWith(expectedSemanticdbJar) }

        where:
        semanticdbVersion   || expectedSemanticdbJar
        '4.9.9'             || "semanticdb-scalac_2.13.15-${semanticdbVersion}.jar"
        '4.10.0'            || "semanticdb-scalac_2.13.15-${semanticdbVersion}.jar"
    }


    def 'should configure SemanticDB for Scala 3.x'() {
        given:
        ScalaCompile compileTask = project.tasks.compileScala
        SourceSet ss = project.sourceSets.main
        Task task = project.tasks.create('config', ConfigSemanticDbTask, {
            sourceSetName.set(ss.name)
            scalaVersion.set('3.3.1')
            projectDirPath.set(project.projectDir.absolutePath)
            outputDir.set(compileTask.destinationDirectory)
        })

        when:
        task.run()

        then:
        compileTask.scalaCompileOptions.additionalParameters == COMPILER_OPTS + [
                '-Xsemanticdb', '-sourceroot', project.projectDir.toString()
        ]
        compileTask.scalaCompilerPlugins.empty
    }

    private Project buildScalaProject() {
        def project = ProjectBuilder.builder().build()

        project.with {
            apply plugin: 'scala'

            repositories { mavenCentral() }

            tasks.withType(ScalaCompile) {
                scalaCompileOptions.additionalParameters = COMPILER_OPTS
            }
        }

        return project
    }
}
