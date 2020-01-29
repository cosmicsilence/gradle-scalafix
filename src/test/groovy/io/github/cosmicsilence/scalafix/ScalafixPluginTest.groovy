package io.github.cosmicsilence.scalafix

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class ScalafixPluginTest extends Specification {

    Project project
    File scalafixConf

    def setup() {
        project = ProjectBuilder.builder().build()
        scalafixConf = new File(project.projectDir, 'scalafix.conf')
        scalafixConf.write 'rules = [RemoveUnusedImports, RemoveUnusedTerms]'
    }

    def 'The plugin adds the scalafix configuration, tasks and extension to the project'() {
        given:
        project.pluginManager.apply 'scala'

        when:
        project.pluginManager.apply ScalafixPlugin

        then:
        project.tasks.scalafix
        project.tasks.scalafixMain
        project.tasks.scalafixTest
        project.tasks.checkScalafix
        project.tasks.checkScalafixMain
        project.tasks.checkScalafixTest
        project.extensions.scalafix
        project.configurations.scalafix

        project.tasks.check.taskDependencies.getDependencies().contains(project.tasks.checkScalafix)
        project.tasks.checkScalafix.taskDependencies.getDependencies()
                .containsAll([project.tasks.checkScalafixMain, project.tasks.checkScalafixTest])
    }

    def 'The plugin throws an exception if when applied, the scala plugin has not been applied to the project'() {
        when:
        project.pluginManager.apply 'io.github.cosmicsilence.scalafix'

        then:
        thrown GradleException
    }

    def 'The plugin adds the semanticdb plugin config to the compiler options when autoConfigureSemanticdb is set to true'() {
        setup:
        applyScalafixPlugin(project, true)

        when:
        project.evaluate()

        then:
        def compileScalaParameters = project.tasks.getByName('compileScala').scalaCompileOptions.additionalParameters
        compileScalaParameters.contains('-Yrangepos')
        compileScalaParameters.find { it.contains('-P:semanticdb:sourceroot:') }
        compileScalaParameters.contains('-Yrangepos')
        compileScalaParameters.find {
            it.contains('-Xplugin:') && it.contains('semanticdb-scalac_2.12.10-4.3.0.jar') && it.contains('scala-library-2.12.10.jar')
        }

        def compileTestScalaParameters = project.tasks.getByName('compileTestScala').scalaCompileOptions.additionalParameters
        compileTestScalaParameters.contains('-Yrangepos')
        compileTestScalaParameters.find { it.contains('-P:semanticdb:sourceroot:') }
        compileTestScalaParameters.contains('-Yrangepos')
        compileTestScalaParameters.find {
            it.contains('-Xplugin:') && it.contains('semanticdb-scalac_2.12.10-4.3.0.jar') && it.contains('scala-library-2.12.10.jar')
        }
    }

    def 'Semanticdb configuration is not added if autoConfigureSemanticdb is set to false'() {
        setup:
        applyScalafixPlugin(project, false)

        when:
        project.evaluate()

        then:
        !project.tasks.getByName('compileScala').scalaCompileOptions.additionalParameters
        !project.tasks.getByName('compileTestScala').scalaCompileOptions.additionalParameters
    }

    def 'checkScalafix task configuration validation'() {
        when:
        applyScalafixPlugin(project, false, 'ExplicitResultTypes,DisableSyntax',
                project.file('.scalafix.conf'))

        then:
        Task task = project.tasks.getByName('checkScalafix')
        task.dependsOn.find { it.name == 'checkScalafixMain' }
        task.dependsOn.find { it.name == 'checkScalafixTest' }
        project.tasks.getByName('check').dependsOn.find { it.name == 'checkScalafix' }
    }

    def 'checkScalafixMain task configuration validation'() {
        setup:
        applyScalafixPlugin(project, false, 'ExplicitResultTypes,DisableSyntax',
                project.file('.scalafix.conf'))

        when:
        project.evaluate()

        then:
        ScalafixTask task = project.tasks.getByName('checkScalafixMain')
        task.dependsOn.isEmpty()
        task.configFile.get().asFile.path == "${project.projectDir}/.scalafix.conf"
        task.rules.get().contains('ExplicitResultTypes')
        task.rules.get().contains('DisableSyntax')
    }

    def 'checkScalafixMain task configuration validation when autoConfigureSemanticDb is enabled'() {
        setup:
        applyScalafixPlugin(project, true, 'ExplicitResultTypes,DisableSyntax',
                project.file('.scalafix.conf'))

        when:
        project.evaluate()

        then:
        ScalafixTask task = project.tasks.getByName('checkScalafixMain')
        task.dependsOn.find{ it.name == 'compileScala' }
        task.configFile.get().asFile.path == "${project.projectDir}/.scalafix.conf"
        task.rules.get().contains('ExplicitResultTypes')
        task.rules.get().contains('DisableSyntax')
    }

    def 'checkScalafixTest task configuration validation'() {
        setup:
        applyScalafixPlugin(project, false, 'ExplicitResultTypes,DisableSyntax',
                project.file('.scalafix.conf'))

        when:
        project.evaluate()

        then:
        ScalafixTask task = project.tasks.getByName('checkScalafixTest')
        task.dependsOn.isEmpty()
        task.configFile.get().asFile.path == "${project.projectDir}/.scalafix.conf"
        task.rules.get().contains('ExplicitResultTypes')
        task.rules.get().contains('DisableSyntax')
    }

    def 'checkScalafixTest task configuration validation when autoConfigureSemanticDb is enabled'() {
        setup:
        applyScalafixPlugin(project, true, 'ExplicitResultTypes,DisableSyntax',
                project.file('.scalafix.conf'))

        when:
        project.evaluate()

        then:
        ScalafixTask task = project.tasks.getByName('checkScalafixTest')
        task.dependsOn.find{ it.name == 'compileTestScala' }
        task.configFile.get().asFile.path == "${project.projectDir}/.scalafix.conf"
        task.rules.get().contains('ExplicitResultTypes')
        task.rules.get().contains('DisableSyntax')
    }

    def 'scalafix task configuration validation'() {
        when:
        applyScalafixPlugin(project, false, 'ExplicitResultTypes,DisableSyntax',
                project.file('.scalafix.conf'))

        then:
        Task task = project.tasks.getByName('scalafix')
        task.dependsOn.find { it.name == 'scalafixMain' }
        task.dependsOn.find { it.name == 'scalafixTest' }
        !project.tasks.getByName('check').dependsOn.find { it.name == 'checkScalaFix' }
    }

    def 'scalafixMain task configuration validation'() {
        setup:
        applyScalafixPlugin(project, false, 'ExplicitResultTypes,DisableSyntax',
                project.file('.scalafix.conf'))

        when:
        project.evaluate()

        then:
        ScalafixTask task = project.tasks.getByName('scalafixMain')
        task.dependsOn.isEmpty()
        task.configFile.get().asFile.path == "${project.projectDir}/.scalafix.conf"
        task.rules.get().contains('ExplicitResultTypes')
        task.rules.get().contains('DisableSyntax')
    }

    def 'scalafixMain task configuration validation when autoConfigureSemanticDb is enabled'() {
        setup:
        applyScalafixPlugin(project, true, 'ExplicitResultTypes,DisableSyntax',
                project.file('.scalafix.conf'))

        when:
        project.evaluate()

        then:
        ScalafixTask task = project.tasks.getByName('checkScalafixMain')
        task.dependsOn.find { it.name == 'compileScala' }
        task.configFile.get().asFile.path == "${project.projectDir}/.scalafix.conf"
        task.rules.get().contains('ExplicitResultTypes')
        task.rules.get().contains('DisableSyntax')
    }

    def 'scalafixTest task configuration validation'() {
        setup:
        applyScalafixPlugin(project, false, 'ExplicitResultTypes,DisableSyntax',
                project.file('.scalafix.conf'))

        when:
        project.evaluate()

        then:
        ScalafixTask task = project.tasks.getByName('checkScalafixTest')
        task.dependsOn.isEmpty()
        task.configFile.get().asFile.path == "${project.projectDir}/.scalafix.conf"
        task.rules.get().contains('ExplicitResultTypes')
        task.rules.get().contains('DisableSyntax')

    }

    def 'scalafixTest task configuration validation when autoConfigureSemanticDb is enabled'() {
        setup:
        applyScalafixPlugin(project, true, 'ExplicitResultTypes,DisableSyntax',
                project.file('.scalafix.conf'))

        when:
        project.evaluate()

        then:
        ScalafixTask task = project.tasks.getByName('checkScalafixTest')
        task.dependsOn.find { it.name == 'compileTestScala' }
        task.configFile.get().asFile.path == "${project.projectDir}/.scalafix.conf"
        task.rules.get().contains('ExplicitResultTypes')
        task.rules.get().contains('DisableSyntax')
    }

//    def 'scalafix uses the .scalafix config file from the subproject by default'() {
//        setup:
//        Project subproject = ProjectBuilder.builder().withName('the-subproject')
//                .withParent(project).build()
//
//        when:
//        applyScalafixPlugin(subproject)
//
//        then:
//        ScalafixTask task = subproject.tasks.getByName('checkScalafixMain')
//        task.configFile.get().asFile.path == "${subproject.projectDir}/.scalafix.conf"
//    }
//
//    def 'scalafix uses the .scalafix config file from the root project as the file is not present in the subproject'() {
//        setup:
//        Project subproject = ProjectBuilder.builder().withName('the-subproject')
//                .withParent(project).build()
//
//        when:
//        applyScalafixPlugin(subproject)
//
//        then:
//        ScalafixTask task = subproject.tasks.getByName('checkScalafixMain')
//        task.configFile.get().asFile.path == "${project.projectDir}/.scalafix.conf"
//    }

    private applyScalafixPlugin(Project project, Boolean autoConfigureSemanticDb = true,
                                String rules = '', File configFile = project.file('.scalafix.conf')) {
        project.with {
            apply plugin: 'scala'
            apply plugin: ScalafixPlugin

            repositories {
                mavenCentral()
            }
            scalafix.autoConfigureSemanticdb = autoConfigureSemanticDb
            scalafix.configFile = configFile
            ext.'scalafix.rules' = rules
        }
    }
}