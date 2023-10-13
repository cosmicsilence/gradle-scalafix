package io.github.cosmicsilence.scalafix

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.scala.ScalaCompile
import org.gradle.testfixtures.ProjectBuilder
import scalafix.interfaces.ScalafixMainMode
import spock.lang.Specification
import spock.lang.Unroll

class ScalafixPluginTest extends Specification {

    private static final String SCALA_VERSION = '2.13.12'
    private static final List<String> DEFAULT_COMPILER_OPTS = ['-Ywarn-unused']

    private Project scalaProject

    def setup() {
        scalaProject = buildScalaProject()
    }

    def 'The plugin adds the scalafix configuration, tasks and extension to the project'() {
        given:
        applyScalafixPlugin(scalaProject)

        when:
        scalaProject.evaluate()

        then:
        scalaProject.tasks.scalafix
        scalaProject.tasks.scalafixMain
        scalaProject.tasks.scalafixTest
        scalaProject.tasks.checkScalafix
        scalaProject.tasks.checkScalafixMain
        scalaProject.tasks.checkScalafixTest
        scalaProject.extensions.scalafix
        scalaProject.configurations.scalafix
    }

    def 'The plugin throws an exception if the scala plugin has not been applied to the project'() {
        given:
        def project = ProjectBuilder.builder().build()
        applyScalafixPlugin(project)

        when:
        project.evaluate()

        then:
        thrown GradleException
    }

    def 'The plugin should not throw any exception if the scala plugin is applied after it'() {
        given:
        def project = ProjectBuilder.builder().build()
        applyScalafixPlugin(project)
        applyScalaPlugin(project)
        project.scalafix.semanticdb.autoConfigure = false

        when:
        project.evaluate()
        project.tasks.scalafix

        then:
        noExceptionThrown()
    }

    def 'The plugin should not trigger dependency resolution during the configuration phase'() {
        given:
        applyScalafixPlugin(scalaProject)

        when:
        scalaProject.evaluate()
        scalaProject.tasks.scalafixMain // forces plugin configuration
        scalaProject.tasks.scalafixTest // forces plugin configuration

        then:
        scalaProject.configurations.compileClasspath.state == Configuration.State.UNRESOLVED
        scalaProject.configurations.testCompileClasspath.state == Configuration.State.UNRESOLVED
    }

    def 'checkScalafix task configuration validation'() {
        given:
        applyScalafixPlugin(scalaProject)

        when:
        scalaProject.evaluate()

        then:
        Task task = scalaProject.tasks.checkScalafix
        task.dependsOn.find { it.name == 'checkScalafixMain' }
        task.dependsOn.find { it.name == 'checkScalafixTest' }
        scalaProject.tasks.check.dependsOn.find { it.name == 'checkScalafix' }
    }

    def 'checkScalafixMain task configuration validation'() {
        given:
        applyScalafixPlugin(scalaProject)
        scalaProject.scalafix.configFile = scalaProject.file('.custom.conf')
        scalaProject.ext.'scalafix.rules' = 'Foo,Bar'

        when:
        scalaProject.evaluate()

        then:
        ScalafixTask task = scalaProject.tasks.checkScalafixMain
        task.dependsOn.find{ it.name == 'compileScala' }
        task.mode == ScalafixMainMode.CHECK
        task.configFile.get().asFile.path == "${scalaProject.projectDir}/.custom.conf"
        task.sourceRoot == scalaProject.projectDir.path
        task.source.files == [
                new File(scalaProject.projectDir, "/src/main/scala/Cat.scala"),
                new File(scalaProject.projectDir, "/src/main/scala/Dog.scala"),
                new File(scalaProject.projectDir, "/src/main/scala/Duck.scala")
        ].toSet()
        task.classpath.get().contains(scalaProject.projectDir.path + "/build/classes/scala/main")
        task.classpath.get().contains(scalaProject.projectDir.path + "/build/classes/java/main")
        task.classpath.get().find { it.endsWith("scala-library-${SCALA_VERSION}.jar") }
        !task.classpath.get().find { it.endsWith("scala-library-${SCALA_VERSION}.pom") }
        task.compileOptions.get() == DEFAULT_COMPILER_OPTS
        task.scalaVersion.get() == SCALA_VERSION
        task.rules.get().containsAll(['Foo', 'Bar'])
        task.semanticdbConfigured
    }

    def 'checkScalafixTest task configuration validation'() {
        given:
        applyScalafixPlugin(scalaProject)
        scalaProject.scalafix.configFile = scalaProject.file('.custom.conf')
        scalaProject.ext.'scalafix.rules' = 'Foo,Bar'

        when:
        scalaProject.evaluate()

        then:
        ScalafixTask task = scalaProject.tasks.checkScalafixTest
        task.dependsOn.find{ it.name == 'compileTestScala' }
        task.mode == ScalafixMainMode.CHECK
        task.configFile.get().asFile.path == "${scalaProject.projectDir}/.custom.conf"
        task.sourceRoot == scalaProject.projectDir.path
        task.source.files == [
                new File(scalaProject.projectDir, "/src/test/scala/Cat.scala"),
                new File(scalaProject.projectDir, "/src/test/scala/Dog.scala"),
                new File(scalaProject.projectDir, "/src/test/scala/Duck.scala")
        ].toSet()
        task.classpath.get().contains(scalaProject.projectDir.path + "/build/classes/scala/test")
        task.classpath.get().contains(scalaProject.projectDir.path + "/build/classes/java/test")
        task.classpath.get().find { it.endsWith("scala-library-${SCALA_VERSION}.jar") }
        !task.classpath.get().find { it.endsWith("scala-library-${SCALA_VERSION}.pom") }
        task.compileOptions.get() == DEFAULT_COMPILER_OPTS
        task.scalaVersion.get() == SCALA_VERSION
        task.rules.get().containsAll(['Foo', 'Bar'])
        task.semanticdbConfigured
    }

    def 'scalafix task configuration validation'() {
        given:
        applyScalafixPlugin(scalaProject)

        when:
        scalaProject.evaluate()

        then:
        Task task = scalaProject.tasks.scalafix
        task.dependsOn.find { it.name == 'scalafixMain' }
        task.dependsOn.find { it.name == 'scalafixTest' }
        !scalaProject.tasks.check.dependsOn.find { it.name == 'scalafix' }
    }

    def 'scalafixMain task configuration validation'() {
        given:
        applyScalafixPlugin(scalaProject)
        scalaProject.scalafix.configFile = scalaProject.file('.custom.conf')
        scalaProject.ext.'scalafix.rules' = 'Foo,Bar'

        when:
        scalaProject.evaluate()

        then:
        ScalafixTask task = scalaProject.tasks.scalafixMain
        task.dependsOn.find{ it.name == 'compileScala' }
        task.mode == ScalafixMainMode.IN_PLACE
        task.configFile.get().asFile.path == "${scalaProject.projectDir}/.custom.conf"
        task.sourceRoot == scalaProject.projectDir.path
        task.source.files == [
                new File(scalaProject.projectDir, "/src/main/scala/Cat.scala"),
                new File(scalaProject.projectDir, "/src/main/scala/Dog.scala"),
                new File(scalaProject.projectDir, "/src/main/scala/Duck.scala")
        ].toSet()
        task.classpath.get().contains(scalaProject.projectDir.path + "/build/classes/scala/main")
        task.classpath.get().contains(scalaProject.projectDir.path + "/build/classes/java/main")
        task.classpath.get().find { it.endsWith("scala-library-${SCALA_VERSION}.jar") }
        !task.classpath.get().find { it.endsWith("scala-library-${SCALA_VERSION}.pom") }
        task.compileOptions.get() == DEFAULT_COMPILER_OPTS
        task.scalaVersion.get() == SCALA_VERSION
        task.rules.get().containsAll(['Foo', 'Bar'])
        task.semanticdbConfigured
    }

    def 'scalafixTest task configuration validation'() {
        given:
        applyScalafixPlugin(scalaProject)
        scalaProject.scalafix.configFile = scalaProject.file('.custom.conf')
        scalaProject.ext.'scalafix.rules' = 'Foo,Bar'

        when:
        scalaProject.evaluate()

        then:
        ScalafixTask task = scalaProject.tasks.scalafixTest
        task.dependsOn.find{ it.name == 'compileTestScala' }
        task.mode == ScalafixMainMode.IN_PLACE
        task.configFile.get().asFile.path == "${scalaProject.projectDir}/.custom.conf"
        task.sourceRoot == scalaProject.projectDir.path
        task.source.files == [
                new File(scalaProject.projectDir, "/src/test/scala/Cat.scala"),
                new File(scalaProject.projectDir, "/src/test/scala/Dog.scala"),
                new File(scalaProject.projectDir, "/src/test/scala/Duck.scala")
        ].toSet()
        task.classpath.get().contains(scalaProject.projectDir.path + "/build/classes/scala/test")
        task.classpath.get().contains(scalaProject.projectDir.path + "/build/classes/java/test")
        task.classpath.get().find { it.endsWith("scala-library-${SCALA_VERSION}.jar") }
        !task.classpath.get().find { it.endsWith("scala-library-${SCALA_VERSION}.pom") }
        task.compileOptions.get() == DEFAULT_COMPILER_OPTS
        task.scalaVersion.get() == SCALA_VERSION
        task.rules.get().containsAll(['Foo', 'Bar'])
        task.semanticdbConfigured
    }

    def 'scalafix<SourceSet> task configuration validation when additional source set is present'() {
        given:
        def scalaProject = buildScalaProject(null, ["foo"])
        applyScalafixPlugin(scalaProject)
        scalaProject.scalafix.configFile = scalaProject.file('.custom.conf')
        scalaProject.ext.'scalafix.rules' = 'Foo,Bar'

        when:
        scalaProject.evaluate()

        then:
        ScalafixTask task = scalaProject.tasks.scalafixFoo
        task.dependsOn.find{ it.name == 'compileFooScala' }
        task.mode == ScalafixMainMode.IN_PLACE
        task.configFile.get().asFile.path == "${scalaProject.projectDir}/.custom.conf"
        task.sourceRoot == scalaProject.projectDir.path
        task.source.files == [
                new File(scalaProject.projectDir, "/src/foo/scala/Cat.scala"),
                new File(scalaProject.projectDir, "/src/foo/scala/Dog.scala"),
                new File(scalaProject.projectDir, "/src/foo/scala/Duck.scala")
        ].toSet()
        task.classpath.get().contains(scalaProject.projectDir.path + "/build/classes/scala/foo")
        task.classpath.get().contains(scalaProject.projectDir.path + "/build/classes/java/foo")
        task.classpath.get().find { it.endsWith("scala-library-${SCALA_VERSION}.jar") }
        !task.classpath.get().find { it.endsWith("scala-library-${SCALA_VERSION}.pom") }
        task.compileOptions.get() == DEFAULT_COMPILER_OPTS
        task.scalaVersion.get() == SCALA_VERSION
        task.rules.get().containsAll(['Foo', 'Bar'])
        task.semanticdbConfigured
    }

    def 'checkScalafix<SourceSet> task configuration validation when additional source set is present'() {
        given:
        def scalaProject = buildScalaProject(null, ["foo"])
        applyScalafixPlugin(scalaProject)
        scalaProject.scalafix.configFile = scalaProject.file('.custom.conf')
        scalaProject.ext.'scalafix.rules' = 'Foo,Bar'

        when:
        scalaProject.evaluate()

        then:
        ScalafixTask task = scalaProject.tasks.checkScalafixFoo
        task.dependsOn.find{ it.name == 'compileFooScala' }
        task.mode == ScalafixMainMode.CHECK
        task.configFile.get().asFile.path == "${scalaProject.projectDir}/.custom.conf"
        task.sourceRoot == scalaProject.projectDir.path
        task.source.files == [
                new File(scalaProject.projectDir, "/src/foo/scala/Cat.scala"),
                new File(scalaProject.projectDir, "/src/foo/scala/Dog.scala"),
                new File(scalaProject.projectDir, "/src/foo/scala/Duck.scala")
        ].toSet()
        task.classpath.get().contains(scalaProject.projectDir.path + "/build/classes/scala/foo")
        task.classpath.get().contains(scalaProject.projectDir.path + "/build/classes/java/foo")
        task.classpath.get().find { it.endsWith("scala-library-${SCALA_VERSION}.jar") }
        !task.classpath.get().find { it.endsWith("scala-library-${SCALA_VERSION}.pom") }
        task.compileOptions.get() == DEFAULT_COMPILER_OPTS
        task.scalaVersion.get() == SCALA_VERSION
        task.rules.get().containsAll(['Foo', 'Bar'])
        task.semanticdbConfigured
    }

    def 'scalafix* and checkScalafix* tasks configuration when semanticdb.autoconfigure is false'() {
        given:
        applyScalafixPlugin(scalaProject)
        scalaProject.scalafix.semanticdb.autoConfigure = false

        when:
        scalaProject.evaluate()

        then:
        TaskContainer tasks = scalaProject.tasks
        [tasks.scalafixMain, tasks.scalafixTest, tasks.checkScalafixMain, tasks.checkScalafixTest].each { ScalafixTask task ->
            assert !task.semanticdbConfigured
            assert task.dependsOn.empty
        }
    }

    def 'the semanticdb compiler plugin is not configured before the execution phase'() {
        given:
        applyScalafixPlugin(scalaProject)

        when:
        scalaProject.evaluate()
        TaskContainer tasks = scalaProject.tasks
        ScalafixTask scalafixMainTask = tasks.scalafixMain // forces plugin configuration
        ScalafixTask scalafixTestTask = tasks.scalafixTest // forces plugin configuration

        then:
        tasks.compileScala.scalaCompileOptions.additionalParameters == DEFAULT_COMPILER_OPTS
        tasks.compileTestScala.scalaCompileOptions.additionalParameters == DEFAULT_COMPILER_OPTS
        scalafixMainTask.compileOptions.get() == DEFAULT_COMPILER_OPTS
        scalafixTestTask.compileOptions.get() == DEFAULT_COMPILER_OPTS
    }

    def 'the semanticdb compiler plugin is configured during the execution phase'() {
        given:
        applyScalafixPlugin(scalaProject)

        when:
        scalaProject.evaluate()
        TaskContainer tasks = scalaProject.tasks
        ScalafixTask scalafixMainTask = tasks.scalafixMain // forces plugin configuration
        ScalafixTask scalafixTestTask = tasks.scalafixTest // forces plugin configuration
        ScalaCompile compileMainTask = tasks.compileScala
        ScalaCompile compileTestTask = tasks.compileTestScala
        [compileMainTask, compileTestTask].each { task -> // emulates the task execution
            task.actions.find { it.displayName.contains('doFirst') }.execute(task)
        }

        then:
        [compileMainTask, compileTestTask].each { task ->
            def compileOpts = task.scalaCompileOptions.additionalParameters
            assert compileOpts.containsAll(DEFAULT_COMPILER_OPTS)
            assert compileOpts.containsAll(['-Yrangepos', "-P:semanticdb:sourceroot:${scalaProject.projectDir}".toString()])
            assert compileOpts.find {
                it.startsWith('-Xplugin:') &&
                        it.endsWith("semanticdb-scalac_${SCALA_VERSION}-${ScalafixProps.scalametaVersion}.jar") &&
                        !it.contains("scala-library")
            }
        }
        scalafixMainTask.compileOptions.get() == compileMainTask.scalaCompileOptions.additionalParameters
        scalafixTestTask.compileOptions.get() == compileTestTask.scalaCompileOptions.additionalParameters
    }

    @Unroll
    def 'The plugin uses semanticdb version #semanticdbVersion when provided using the scalafix extension'() {
        given:
        applyScalafixPlugin(scalaProject)
        scalaProject.scalafix.semanticdb.version = semanticdbVersion

        when:
        scalaProject.evaluate()
        TaskContainer tasks = scalaProject.tasks
        ScalafixTask scalafixMainTask = tasks.scalafixMain // forces plugin configuration
        ScalafixTask scalafixTestTask = tasks.scalafixTest // forces plugin configuration
        ScalaCompile compileMainTask = tasks.compileScala
        ScalaCompile compileTestTask = tasks.compileTestScala
        [compileMainTask, compileTestTask].each { task -> // emulates the task execution
            task.actions.find { it.displayName.contains('doFirst') }.execute(task)
        }

        then:
        [compileMainTask, compileTestTask].each { task ->
            assert task.scalaCompileOptions.additionalParameters.find {
                it.startsWith('-Xplugin:') && it.endsWith(expectedSemanticdbJar) && !it.contains("scala-library")
            }
        }
        scalafixMainTask.compileOptions.get() == compileMainTask.scalaCompileOptions.additionalParameters
        scalafixTestTask.compileOptions.get() == compileTestTask.scalaCompileOptions.additionalParameters

        where:
        semanticdbVersion   || expectedSemanticdbJar
        '4.8.3'            || "semanticdb-scalac_${SCALA_VERSION}-${semanticdbVersion}.jar"
        '4.8.4'            || "semanticdb-scalac_${SCALA_VERSION}-${semanticdbVersion}.jar"
    }

    def 'the semanticdb compiler plugin is not configured if semanticdb.autoConfigure is set to false'() {
        given:
        applyScalafixPlugin(scalaProject)
        scalaProject.scalafix.semanticdb.autoConfigure = false

        when:
        scalaProject.evaluate()
        TaskContainer tasks = scalaProject.tasks
        ScalafixTask scalafixMainTask = tasks.scalafixMain // forces plugin configuration
        ScalafixTask scalafixTestTask = tasks.scalafixTest // forces plugin configuration
        ScalaCompile compileMainTask = tasks.compileScala
        ScalaCompile compileTestTask = tasks.compileTestScala
        [compileMainTask, compileTestTask].each { task -> // emulates the task execution
            task.actions.find { it.displayName.contains('doFirst') }?.execute(task)
        }

        then:
        compileMainTask.scalaCompileOptions.additionalParameters == DEFAULT_COMPILER_OPTS
        compileTestTask.scalaCompileOptions.additionalParameters == DEFAULT_COMPILER_OPTS
        scalafixMainTask.compileOptions.get() == DEFAULT_COMPILER_OPTS
        scalafixTestTask.compileOptions.get() == DEFAULT_COMPILER_OPTS
    }

    def 'scalafix uses the config file provided via extension'() {
        given:
        Project rootProject = ProjectBuilder.builder().withName("root").build()
        File rootProjectConfig = new File(rootProject.projectDir, '.scalafix.conf')
        rootProjectConfig.write 'rules = [Foo, Bar]'

        Project subProject = buildScalaProject(rootProject)
        File subProjectConfig = new File(subProject.projectDir, '.scalafix.conf')
        subProjectConfig.write 'rules = [Foo, Bar]'

        File extensionConfig = new File(subProject.projectDir, '.custom.conf')
        extensionConfig.write 'rules = [Foo, Bar]'

        applyScalafixPlugin(subProject)
        subProject.scalafix.configFile = extensionConfig
        subProject.scalafix.semanticdb.autoConfigure = false

        when:
        subProject.evaluate()

        then:
        ScalafixTask task = subProject.tasks.checkScalafixMain
        task.configFile.get().asFile.path == extensionConfig.path
    }

    def 'scalafix uses the config file from the subproject if it has not been provided via extension'() {
        given:
        Project rootProject = ProjectBuilder.builder().withName("root").build()
        File rootProjectConfig = new File(rootProject.projectDir, '.scalafix.conf')
        rootProjectConfig.write 'rules = [Foo, Bar]'

        Project subProject = buildScalaProject(rootProject)
        File subProjectConfig = new File(subProject.projectDir, '.scalafix.conf')
        subProjectConfig.write 'rules = [Foo, Bar]'

        applyScalafixPlugin(subProject)

        when:
        subProject.evaluate()

        then:
        ScalafixTask task = subProject.tasks.checkScalafixMain
        task.configFile.get().asFile.path == subProjectConfig.path
    }

    def 'scalafix uses the config file from the root project as the file is not present in the subproject and it is not specified in the extension'() {
        given:
        Project rootProject = ProjectBuilder.builder().withName("root").build()
        File rootProjectConfig = new File(rootProject.projectDir, '.scalafix.conf')
        rootProjectConfig.write 'rules = [Foo, Bar]'

        Project subProject = buildScalaProject(rootProject)
        applyScalafixPlugin(subProject)

        when:
        subProject.evaluate()

        then:
        ScalafixTask task = subProject.tasks.checkScalafixMain
        task.configFile.get().asFile.path == rootProjectConfig.path
    }

    def 'scalafix does not use any config file as it is not provided'() {
        given:
        Project rootProject = ProjectBuilder.builder().withName("root").build()
        Project subProject = buildScalaProject(rootProject)
        applyScalafixPlugin(subProject)

        when:
        subProject.evaluate()

        then:
        ScalafixTask task = subProject.tasks.checkScalafixMain
        !task.configFile.isPresent()
    }

    def 'scalafix should only select sources matching include filter'() {
        given:
        applyScalafixPlugin(scalaProject)
        scalaProject.scalafix.includes = [ "**/Cat.scala", "**/Duck.scala" ]

        when:
        scalaProject.evaluate()

        then:
        ScalafixTask task = scalaProject.tasks.scalafixMain
        task.source.files == [
                new File(scalaProject.projectDir, "/src/main/scala/Cat.scala"),
                new File(scalaProject.projectDir, "/src/main/scala/Duck.scala")
        ].toSet()
    }

    def 'scalafix should not select sources matching exclude filter'() {
        given:
        applyScalafixPlugin(scalaProject)
        scalaProject.scalafix.excludes = [ "**/Cat.scala", "**/Duck.scala" ]

        when:
        scalaProject.evaluate()

        then:
        ScalafixTask task = scalaProject.tasks.scalafixMain
        task.source.asPath == "${scalaProject.projectDir}/src/main/scala/Dog.scala"
    }

    def 'scalafix should select sources matching include filter and not matching exclude filter'() {
        given:
        applyScalafixPlugin(scalaProject)
        scalaProject.scalafix.includes = [ "**/D*.scala" ]
        scalaProject.scalafix.excludes = [ "**/*g.scala" ]

        when:
        scalaProject.evaluate()

        then:
        ScalafixTask task = scalaProject.tasks.scalafixMain
        task.source.asPath == "${scalaProject.projectDir}/src/main/scala/Duck.scala"
    }

    def 'tasks should not be configured for ignored source sets'() {
        given:
        def scalaProject = buildScalaProject(null, ["bar"])
        applyScalafixPlugin(scalaProject)
        scalaProject.scalafix.ignoreSourceSets = [ "main", "bar" ]

        when:
        scalaProject.evaluate()

        then:
        !scalaProject.tasks.findByName('scalafixMain')
        !scalaProject.tasks.findByName('checkScalafixMain')
        !scalaProject.tasks.findByName('scalafixBar')
        !scalaProject.tasks.findByName('checkScalafixBar')
        scalaProject.tasks.findByName('scalafixTest')
        scalaProject.tasks.findByName('checkScalafixTest')
    }

    private applyScalaPlugin(Project project) {
        project.pluginManager.apply 'scala'
    }

    private applyScalafixPlugin(Project project) {
        project.pluginManager.apply 'io.github.cosmicsilence.scalafix'
    }

    private Project buildScalaProject(Project parent = null, List<String> extraSourceSets = []) {
        def project = ProjectBuilder.builder().withParent(parent).build()
        def standardSourceSets = ["main", "test"]

        (standardSourceSets + extraSourceSets).forEach {
            def folder = new File(project.projectDir, "/src/$it/scala/")
            folder.mkdirs()
            ["Dog", "Cat", "Duck"].forEach {
                def sourceFile = new File(folder, "${it}.scala")
                sourceFile.write "class $it"
            }
        }

        project.with {
            apply plugin: 'scala'

            repositories {
                mavenCentral()
            }

            dependencies {
                compile group: 'org.scala-lang', name: 'scala-library', version: SCALA_VERSION
                compile group: 'org.scala-lang', name: 'scala-library', version: SCALA_VERSION, ext: 'pom'
            }

            sourceSets {
                extraSourceSets.collect {
                    "${it}" {
                        scala {
                            srcDir "src/$it/scala"
                        }
                        compileClasspath += main.compileClasspath
                    }
                }
            }

            tasks.withType(ScalaCompile) {
                scalaCompileOptions.additionalParameters = DEFAULT_COMPILER_OPTS
            }
        }

        return project
    }
}
