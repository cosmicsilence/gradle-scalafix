package io.github.cosmicsilence.compat

import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty

abstract class GradleCompat {

    private GradleCompat() {}

    private static boolean isVersion4(Project project) {
        return project.gradle.gradleVersion.startsWith("4.")
    }

    static RegularFileProperty fileProperty(Project project, RegularFile defaultFile = null) {
        def fileProp = isVersion4(project) ? project.layout.fileProperty() : project.objects.fileProperty()

        if (defaultFile) {
            if (isVersion4(project)) {
                fileProp.set(defaultFile)
            } else {
                fileProp.convention(defaultFile)
            }
        }

        return fileProp
    }
}
