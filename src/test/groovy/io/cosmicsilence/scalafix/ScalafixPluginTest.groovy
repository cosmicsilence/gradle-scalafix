package io.cosmicsilence.scalafix

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

import static org.junit.Assert.assertTrue

class ScalafixPluginTest {

    @Test
    void scalafixPluginAddsTasksToProject() {
        Project project = ProjectBuilder.builder().build()
        project.pluginManager.apply 'io.cosmicsilence.scalafix'

        assertTrue(project.tasks.scalafix != null)
        assertTrue(project.tasks.scalafixMain != null)
        assertTrue(project.tasks.scalafixTest != null)
        assertTrue(project.tasks.checkScalafix != null)
        assertTrue(project.tasks.checkScalafixMain != null)
        assertTrue(project.tasks.checkScalafixTest != null)
    }
}
