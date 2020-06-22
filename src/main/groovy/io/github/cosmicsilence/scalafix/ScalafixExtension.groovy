package io.github.cosmicsilence.scalafix

import org.gradle.api.Project
import org.gradle.api.file.RegularFile
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
     * Name of source sets to which the Scalafix plugin should not be applied.
     */
    final SetProperty<String> ignoreSourceSets

    /**
     * Auto configures the SemanticDB Scala compiler. This is required to run
     * semantic rules.
     */
    Boolean autoConfigureSemanticdb = true

    private final Project project

    ScalafixExtension(Project project) {
        this.project = project
        final boolean isGradle4 = project.gradle.gradleVersion.startsWith("4")
        configFile = isGradle4? project.layout.fileProperty(): project.objects.fileProperty()

        RegularFile defaultRegularFile = locateConfigFile(project)?: locateConfigFile(project.rootProject)
        if (defaultRegularFile) {
            if (isGradle4) {
                configFile.set(defaultRegularFile)
            } else {
                configFile.convention(defaultRegularFile)
            }
        }
        includes = project.objects.setProperty(String)
        excludes = project.objects.setProperty(String)
        ignoreSourceSets = project.objects.setProperty(String)
    }

    private RegularFile locateConfigFile(Project project) {
        RegularFile configFile = project.layout.projectDirectory.file(DEFAULT_CONFIG_FILE)
        return (configFile.asFile.exists() && configFile.asFile.isFile()) ? configFile : null
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
