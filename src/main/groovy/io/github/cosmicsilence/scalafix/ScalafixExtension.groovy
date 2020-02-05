package io.github.cosmicsilence.scalafix

import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.SetProperty

class ScalafixExtension {

    private static final String DEFAULT_CONFIG_FILE = ".scalafix.conf"

    /**
     * Scalafix configuration file. If not specified, the plugin will try to find
     * a file named '.scalafix.conf' in the project's directory and then in the
     * root project's directory, in this order.
     */
    final RegularFileProperty configFile

    /**
     * Adds ANT style include patterns. If includes are not provided, then all files
     * in the Scala source set will be included. If includes are provided, then a
     * file must match at least one of the include patterns to be processed.
     */
    final SetProperty<String> includes

    /**
     * Adds ANT style exclude pattern. If excludes are not provided, then no files
     * will be excluded. If excludes are provided, then files must not match any
     * exclude pattern to be processed.
     */
    final SetProperty<String> excludes

    /**
     * Auto configures the SemanticDB Scala compiler. This is required to run
     * semantic rules.
     */
    Boolean autoConfigureSemanticdb = true

    private final Project project

    ScalafixExtension(Project project) {
        this.project = project
        configFile = project.objects.fileProperty()

        def projectConfigFile = project.getLayout().getProjectDirectory().file(DEFAULT_CONFIG_FILE)
        def rootProjectConfigFile = project.rootProject.getLayout().getProjectDirectory().file(DEFAULT_CONFIG_FILE)

        if (projectConfigFile.asFile.exists()) {
            configFile = project.objects.fileProperty().convention(projectConfigFile)
        } else if (rootProjectConfigFile.asFile.exists()) {
            configFile = project.objects.fileProperty().convention(rootProjectConfigFile)
        }

        includes = project.objects.setProperty(String)
        excludes = project.objects.setProperty(String)
    }

    /**
     * Path to the Scalafix configuration file. This path must be relative to the
     * project's directory. If not specified, the plugin will try to find a file
     * named '.scalafix.conf' in the project's directory and then in the root
     * project's directory, in this order.
     */
    void setConfigFile(String path) {
        configFile.set(project.file(path))
    }
}
