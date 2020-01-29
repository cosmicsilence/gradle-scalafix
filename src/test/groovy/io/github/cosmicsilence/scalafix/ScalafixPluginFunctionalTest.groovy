package io.github.cosmicsilence.scalafix

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder

import spock.lang.Specification

class ScalafixPluginFunctionalTest extends Specification {

    @Rule
    public final TemporaryFolder testProjectDir = new TemporaryFolder();
    private File settingsFile;
    private File buildFile;

    def setup() {
        settingsFile = testProjectDir.newFile("settings.gradle");
        buildFile = testProjectDir.newFile("build.gradle");

        buildFile.write'''
plugins {
    id 'scala'
    id 'io.github.cosmicsilence.scalafix'
}

repositories {
    mavenCentral()
}
'''
        settingsFile.write "rootProject.name = 'hello-world'"
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

    BuildResult runGradleTask(String task, List<String> arguments) {
        arguments.add(task)
        return GradleRunner.create()
                .withProjectDir(testProjectDir.getRoot())
                .withArguments(arguments)
                .withPluginClasspath()
                .build()
    }
}
