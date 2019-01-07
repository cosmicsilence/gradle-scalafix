package io.cosmicsilence.scalafix

import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty

class ScalafixPluginExtension {

    final RegularFileProperty configFile
    private final Project project

    ScalafixPluginExtension(Project project) {
        this.project = project
        configFile = project.layout.fileProperty()
    }

    /**
     * Custom location of the Scalafix configuration file (optional). If not specified,
     * the Scalafix plugin tries to find a '.scalafix.conf' in the current project's
     * directory and then in the root project directory in this order.
     */
    void setConfigFile(String path) {
        configFile.set(project.file(path))
    }

    /**
     * Custom location of the Scalafix configuration file (optional). If not specified,
     * the Scalafix plugin tries to find a '.scalafix.conf' in the current project's
     * directory and then in the root project directory in this order.
     */
    void setConfigFile(File file) {
        configFile.set(file)
    }

    RegularFileProperty getConfigFile() {
        configFile
    }
}
