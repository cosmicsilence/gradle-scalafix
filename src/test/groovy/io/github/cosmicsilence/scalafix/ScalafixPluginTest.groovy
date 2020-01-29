package io.github.cosmicsilence.scalafix

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

import static org.junit.Assert.*

class ScalafixPluginTest {

    @Test
    void shouldConfigureProjectWhenPluginIsApplied() {
        Project project = ProjectBuilder.builder().build()
        project.pluginManager.apply 'scala'
        project.pluginManager.apply 'io.github.cosmicsilence.scalafix'

        assertNotNull(project.tasks.scalafix)
        assertNotNull(project.tasks.scalafixMain)
        assertNotNull(project.tasks.scalafixTest)
        assertNotNull(project.tasks.checkScalafix)
        assertNotNull(project.tasks.checkScalafixMain)
        assertNotNull(project.tasks.checkScalafixTest)
        assertNotNull(project.extensions.scalafix)
        assertNotNull(project.configurations.scalafix)

        assertTrue(project.tasks.check.taskDependencies.getDependencies().contains(project.tasks.checkScalafix))
        assertTrue(project.tasks.checkScalafix.taskDependencies.getDependencies()
                .containsAll([project.tasks.checkScalafixMain, project.tasks.checkScalafixTest]))
    }

    @Test(expected = GradleException)
    void shouldFailIfScalaPluginIsNotApplied() {
        Project project = ProjectBuilder.builder().build()
        project.pluginManager.apply 'io.github.cosmicsilence.scalafix'
    }
}
