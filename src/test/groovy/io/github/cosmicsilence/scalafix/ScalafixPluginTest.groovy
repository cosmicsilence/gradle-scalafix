package io.github.cosmicsilence.scalafix

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.testfixtures.ProjectBuilder
import scalafix.interfaces.ScalafixMainMode
import spock.lang.Specification

class ScalafixPluginTest extends Specification {

    private Project project
    private File scalafixConf

    def setup() {
        project = ProjectBuilder.builder().build()
        project.repositories {
            mavenCentral()
        }

        scalafixConf = new File(project.projectDir, 'scalafix.conf')
        scalafixConf.write 'rules = [Foo, Bar]'
    }

    def 'The plugin adds the scalafix configuration, tasks and extension to the project'() {
        given:
        applyScalafixPlugin(project)

        when:
        project.evaluate()

        then:
        project.tasks.scalafix
        project.tasks.scalafixMain
        project.tasks.scalafixTest
        project.tasks.checkScalafix
        project.tasks.checkScalafixMain
        project.tasks.checkScalafixTest
        project.extensions.scalafix
        project.configurations.scalafix
    }

    def 'The plugin throws an exception if the scala plugin has not been applied to the project'() {
        given:
        project.pluginManager.apply 'io.github.cosmicsilence.scalafix'

        when:
        project.evaluate()

        then:
        thrown GradleException
    }

    def 'The plugin should not throw any exception if the scala plugin is applied after it'() {
        given:
        project.pluginManager.apply 'io.github.cosmicsilence.scalafix'
        project.pluginManager.apply 'scala'

        when:
        project.evaluate()

        then:
        project.tasks.scalafix
    }

    def 'The plugin adds the semanticdb plugin config to the compiler options when autoConfigureSemanticdb is set to true'() {
        given:
        applyScalafixPlugin(project, true)

        when:
        project.evaluate()

        then:
        def compileScalaParameters = project.tasks.getByName('compileScala').scalaCompileOptions.additionalParameters
        compileScalaParameters.contains('-Yrangepos')
        compileScalaParameters.find { it.contains('-P:semanticdb:sourceroot:') }
        compileScalaParameters.find {
            it.contains('-Xplugin:') && it.contains('semanticdb-scalac_2.12.10-4.3.0.jar') && it.contains('scala-library-2.12.10.jar')
        }

        def compileTestScalaParameters = project.tasks.getByName('compileTestScala').scalaCompileOptions.additionalParameters
        compileTestScalaParameters.contains('-Yrangepos')
        compileTestScalaParameters.find { it.contains('-P:semanticdb:sourceroot:') }
        compileTestScalaParameters.find {
            it.contains('-Xplugin:') && it.contains('semanticdb-scalac_2.12.10-4.3.0.jar') && it.contains('scala-library-2.12.10.jar')
        }
    }

    def 'Semanticdb configuration is not added if autoConfigureSemanticdb is set to false'() {
        given:
        applyScalafixPlugin(project, false)

        when:
        project.evaluate()

        then:
        !project.tasks.getByName('compileScala').scalaCompileOptions.additionalParameters
        !project.tasks.getByName('compileTestScala').scalaCompileOptions.additionalParameters
    }

    def 'checkScalafix task configuration validation'() {
        given:
        applyScalafixPlugin(project, false, 'Foo,Bar', project.file('.scalafix.conf'))

        when:
        project.evaluate()

        then:
        Task task = project.tasks.getByName('checkScalafix')
        task.dependsOn.find { it.name == 'checkScalafixMain' }
        task.dependsOn.find { it.name == 'checkScalafixTest' }
        project.tasks.getByName('check').dependsOn.find { it.name == 'checkScalafix' }
    }

    def 'checkScalafixMain task configuration validation'() {
        given:
        applyScalafixPlugin(project, false, 'Foo,Bar', project.file('.scalafix.conf'))

        when:
        project.evaluate()

        then:
        ScalafixTask task = project.tasks.getByName('checkScalafixMain')
        task.dependsOn.isEmpty()
        task.configFile.get().asFile.path == "${project.projectDir}/.scalafix.conf"
        task.rules.get().contains('Foo')
        task.rules.get().contains('Bar')
        task.mode == ScalafixMainMode.CHECK
    }

    def 'checkScalafixMain task configuration validation when autoConfigureSemanticDb is enabled'() {
        given:
        applyScalafixPlugin(project, true, 'Foo,Bar', project.file('.scalafix.conf'))

        when:
        project.evaluate()

        then:
        ScalafixTask task = project.tasks.getByName('checkScalafixMain')
        task.dependsOn.find{ it.name == 'compileScala' }
        task.configFile.get().asFile.path == "${project.projectDir}/.scalafix.conf"
        task.rules.get().contains('Foo')
        task.rules.get().contains('Bar')
        task.mode == ScalafixMainMode.CHECK
    }

    def 'checkScalafixTest task configuration validation'() {
        given:
        applyScalafixPlugin(project, false, 'Foo,Bar', project.file('.scalafix.conf'))

        when:
        project.evaluate()

        then:
        ScalafixTask task = project.tasks.getByName('checkScalafixTest')
        task.dependsOn.isEmpty()
        task.configFile.get().asFile.path == "${project.projectDir}/.scalafix.conf"
        task.rules.get().contains('Foo')
        task.rules.get().contains('Bar')
        task.mode == ScalafixMainMode.CHECK
    }

    def 'checkScalafixTest task configuration validation when autoConfigureSemanticDb is enabled'() {
        given:
        applyScalafixPlugin(project, true, 'Foo,Bar', project.file('.scalafix.conf'))

        when:
        project.evaluate()

        then:
        ScalafixTask task = project.tasks.getByName('checkScalafixTest')
        task.dependsOn.find{ it.name == 'compileTestScala' }
        task.configFile.get().asFile.path == "${project.projectDir}/.scalafix.conf"
        task.rules.get().contains('Foo')
        task.rules.get().contains('Bar')
        task.mode == ScalafixMainMode.CHECK
    }

    def 'scalafix task configuration validation'() {
        given:
        applyScalafixPlugin(project, false, 'Foo,Bar', project.file('.scalafix.conf'))

        when:
        project.evaluate()

        then:
        Task task = project.tasks.getByName('scalafix')
        task.dependsOn.find { it.name == 'scalafixMain' }
        task.dependsOn.find { it.name == 'scalafixTest' }
        !project.tasks.getByName('check').dependsOn.find { it.name == 'scalafix' }
    }

    def 'scalafixMain task configuration validation'() {
        given:
        applyScalafixPlugin(project, false, 'Foo,Bar', project.file('.scalafix.conf'))

        when:
        project.evaluate()

        then:
        ScalafixTask task = project.tasks.getByName('scalafixMain')
        task.dependsOn.isEmpty()
        task.configFile.get().asFile.path == "${project.projectDir}/.scalafix.conf"
        task.rules.get().contains('Foo')
        task.rules.get().contains('Bar')
        task.mode == ScalafixMainMode.IN_PLACE
    }

    def 'scalafixMain task configuration validation when autoConfigureSemanticDb is enabled'() {
        given:
        applyScalafixPlugin(project, true, 'Foo,Bar', project.file('.scalafix.conf'))

        when:
        project.evaluate()

        then:
        ScalafixTask task = project.tasks.getByName('scalafixMain')
        task.dependsOn.find { it.name == 'compileScala' }
        task.configFile.get().asFile.path == "${project.projectDir}/.scalafix.conf"
        task.rules.get().contains('Foo')
        task.rules.get().contains('Bar')
        task.mode == ScalafixMainMode.IN_PLACE
    }

    def 'scalafixTest task configuration validation'() {
        given:
        applyScalafixPlugin(project, false, 'Foo,Bar', project.file('.scalafix.conf'))

        when:
        project.evaluate()

        then:
        ScalafixTask task = project.tasks.getByName('scalafixTest')
        task.dependsOn.isEmpty()
        task.configFile.get().asFile.path == "${project.projectDir}/.scalafix.conf"
        task.rules.get().contains('Foo')
        task.rules.get().contains('Bar')
        task.mode == ScalafixMainMode.IN_PLACE
    }

    def 'scalafixTest task configuration validation when autoConfigureSemanticDb is enabled'() {
        given:
        applyScalafixPlugin(project, true, 'Foo,Bar', project.file('.scalafix.conf'))

        when:
        project.evaluate()

        then:
        ScalafixTask task = project.tasks.getByName('scalafixTest')
        task.dependsOn.find { it.name == 'compileTestScala' }
        task.configFile.get().asFile.path == "${project.projectDir}/.scalafix.conf"
        task.rules.get().contains('Foo')
        task.rules.get().contains('Bar')
        task.mode == ScalafixMainMode.IN_PLACE
    }

    def 'scalafix<SourceSet> task configuration validation when additional source set is present'() {
        given:
        applyScalafixPlugin(project, false, 'Foo,Bar', project.file('.scalafix.conf'))
        project.with {
            sourceSets {
                foo { }
            }
        }

        when:
        project.evaluate()

        then:
        ScalafixTask task = project.tasks.getByName('scalafixFoo')
        task.dependsOn.isEmpty()
        task.configFile.get().asFile.path == "${project.projectDir}/.scalafix.conf"
        task.rules.get().contains('Foo')
        task.rules.get().contains('Bar')
        project.tasks.getByName('scalafix').dependsOn(task)
    }

    def 'scalafix<SourceSet> task configuration validation when additional source set is present and autoConfigureSemanticDb is enabled'() {
        given:
        applyScalafixPlugin(project, true, 'Foo,Bar', project.file('.scalafix.conf'))
        project.with {
            sourceSets {
                bar { }
            }
        }

        when:
        project.evaluate()

        then:
        ScalafixTask task = project.tasks.getByName('scalafixBar')
        task.dependsOn.find { it.name == 'compileBarScala' }
        task.configFile.get().asFile.path == "${project.projectDir}/.scalafix.conf"
        task.rules.get().contains('Foo')
        task.rules.get().contains('Bar')
        project.tasks.getByName('scalafix').dependsOn(task)
    }

    def 'checkScalafix<SourceSet> task configuration validation when additional source set is present'() {
        given:
        applyScalafixPlugin(project, false, 'Foo,Bar', project.file('.scalafix.conf'))
        project.with {
            sourceSets {
                foo { }
            }
        }

        when:
        project.evaluate()

        then:
        ScalafixTask task = project.tasks.getByName('checkScalafixFoo')
        task.dependsOn.isEmpty()
        task.configFile.get().asFile.path == "${project.projectDir}/.scalafix.conf"
        task.rules.get().contains('Foo')
        task.rules.get().contains('Bar')
        project.tasks.getByName('checkScalafix').dependsOn(task)
    }

    def 'checkScalafix<SourceSet> task configuration validation when additional source set is present and autoConfigureSemanticDb is enabled'() {
        given:
        applyScalafixPlugin(project, true, 'Foo,Bar', project.file('.scalafix.conf'))
        project.with {
            sourceSets {
                bar { }
            }
        }

        when:
        project.evaluate()

        then:
        ScalafixTask task = project.tasks.getByName('checkScalafixBar')
        task.dependsOn.find { it.name == 'compileBarScala' }
        task.configFile.get().asFile.path == "${project.projectDir}/.scalafix.conf"
        task.rules.get().contains('Foo')
        task.rules.get().contains('Bar')
        project.tasks.getByName('checkScalafix').dependsOn(task)
    }

    def 'scalafix uses the .scalafix config file provided via extension'() {
        given:
        Project subproject = ProjectBuilder.builder().withName('the-subproject')
                .withParent(project).build()
        subproject.projectDir.mkdir()
        File subprojectScalafixConf = new File(subproject.projectDir, '.scalafix.conf')
        subprojectScalafixConf.write 'rules = [Foo, Bar]'
        File extensionScalafixConf = new File(subproject.projectDir, '.test-scalafix.conf')
        extensionScalafixConf.write 'rules = [Foo, Bar]'
        applyScalafixPlugin(subproject, false, '', extensionScalafixConf)

        when:
        subproject.evaluate()

        then:
        println subproject.tasks
        ScalafixTask task = subproject.tasks.getByName('checkScalafixMain')
        task.configFile.get().asFile.path == "${subproject.projectDir}/.test-scalafix.conf"
    }

    def 'scalafix uses the .scalafix config file from the subproject if it has not been provided via extension'() {
        given:
        Project subproject = ProjectBuilder.builder().withName('the-subproject')
                .withParent(project).build()
        subproject.projectDir.mkdir()
        File subprojectScalafixConf = new File(subproject.projectDir, '.scalafix.conf')
        subprojectScalafixConf.write 'rules = [Foo, Bar]'
        applyScalafixPlugin(subproject, false, '', null)

        when:
        subproject.evaluate()

        then:
        ScalafixTask task = subproject.tasks.getByName('checkScalafixMain')
        task.configFile.get().asFile.path == "${subproject.projectDir}/.scalafix.conf"
    }

    def 'scalafix uses the .scalafix config file from the root project as the file is not present in the subproject and\
 it is not specified in the extension'() {
        given:
        Project subproject = ProjectBuilder.builder().withParent(project).withName('the-subproject').build()
        scalafixConf.delete()
        applyScalafixPlugin(subproject, false, '', null)

        when:
        subproject.evaluate()

        then:
        ScalafixTask task = subproject.tasks.getByName('checkScalafixMain')
    }

    def 'scalafix does not use any scalafix.conf file as it is not provided'() {
        given:
        Project subproject = ProjectBuilder.builder().withParent(project).withName('the-subproject').build()
        scalafixConf.delete()
        applyScalafixPlugin(subproject, false, '', null)

        when:
        subproject.evaluate()

        then:
        ScalafixTask task = subproject.tasks.getByName('checkScalafixMain')
        !task.configFile.get()
    }

    private applyScalafixPlugin(Project project, Boolean autoConfigureSemanticDb = false,
                                String rules = '', File configFile = project.file('.scalafix.conf')) {
        project.with {
            apply plugin: 'scala'
            apply plugin: 'io.github.cosmicsilence.scalafix'
            scalafix.autoConfigureSemanticdb = autoConfigureSemanticDb
            scalafix.configFile = configFile
            ext.'scalafix.rules' = rules
        }
    }
}