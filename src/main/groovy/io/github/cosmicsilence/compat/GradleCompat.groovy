package io.github.cosmicsilence.compat

import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property

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

    static Property<Boolean> booleanProperty(Project project, Boolean defaultBoolean = null) {
        def booleanProp = project.objects.property(Boolean)

        if (defaultBoolean) {
            if (isVersion4(project)) {
                booleanProp.set(project.provider { defaultBoolean })
            } else {
                booleanProp.convention(defaultBoolean)
            }
        }

        return booleanProp
    }
}
