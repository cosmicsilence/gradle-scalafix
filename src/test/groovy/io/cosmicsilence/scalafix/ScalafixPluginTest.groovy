package io.cosmicsilence.scalafix

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

import static org.junit.Assert.assertTrue

class ScalafixPluginTest {

    @Test
    void greeterPluginAddsGreetingTaskToProject() {
        Project project = ProjectBuilder.builder().build()
        project.pluginManager.apply 'io.cosmicsilence.scalafix'

        assertTrue(project.tasks.hello != null)
    }
}
