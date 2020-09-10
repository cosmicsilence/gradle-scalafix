package io.github.cosmicsilence.utils

import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty

class GradleCompat {
    private static boolean isVersion4(Project project) {
        project.gradle.gradleVersion.startsWith("4.")
    }

    static RegularFileProperty fileProperty(Project project, RegularFile defaultFile = null) {
        final boolean isVersion4 = isVersion4(project)
        def fileProp = isVersion4 ? project.layout.fileProperty(): project.objects.fileProperty()
        if (defaultFile) {
            if (isVersion4) {
                fileProp.set(defaultFile)
            } else {
                fileProp.convention(defaultFile)
            }
        }
        fileProp
    }
}
