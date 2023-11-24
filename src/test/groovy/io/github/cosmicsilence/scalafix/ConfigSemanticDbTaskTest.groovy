package io.github.cosmicsilence.scalafix

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.scala.ScalaCompile
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification
import spock.lang.Unroll

class ConfigSemanticDbTaskTest extends Specification {

    private static final String SCALA_VERSION = '2.13.12'
    private static final List<String> COMPILER_OPTS = ['-Ywarn-unused']

    private Project project

    def setup() {
        project = buildScalaProject()
    }

    def 'should configure the SemanticDB compiler plugin during the execution phase'() {
        given:
        ScalaCompile compileTask = project.tasks.compileScala
        SourceSet ss = project.sourceSets.main
        Task task = project.tasks.create('config', ConfigSemanticDbTask, {
            sourceSet = new ScalaSourceSet(project, ss)
            scalaVersion = SCALA_VERSION
        })

        when:
        task.run()

        then:
        compileTask.scalaCompileOptions.additionalParameters.containsAll(
            COMPILER_OPTS + ['-Yrangepos', '-P:semanticdb:sourceroot:targetroot:../../../..']
        )
        compileTask.scalaCompilerPlugins.find {
            it.absolutePath.endsWith("semanticdb-scalac_${SCALA_VERSION}-${ScalafixProps.scalametaVersion}.jar")
        }
        !compileTask.scalaCompilerPlugins.find { it.name.contains("scala-library") }
    }

    @Unroll
    def 'should configure the SemanticDB using the provided #semanticdbVersion version'() {
        given:
        ScalaCompile compileTask = project.tasks.compileScala
        SourceSet ss = project.sourceSets.main
        Task task = project.tasks.create('config', ConfigSemanticDbTask, {
            sourceSet = new ScalaSourceSet(project, ss)
            scalaVersion = SCALA_VERSION
            semanticDbVersion = semanticdbVersion
        })

        when:
        task.run()

        then:
        compileTask.scalaCompilerPlugins.find { it.absolutePath.endsWith(expectedSemanticdbJar) }

        where:
        semanticdbVersion   || expectedSemanticdbJar
        '4.8.3'             || "semanticdb-scalac_${SCALA_VERSION}-${semanticdbVersion}.jar"
        '4.8.4'             || "semanticdb-scalac_${SCALA_VERSION}-${semanticdbVersion}.jar"
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
