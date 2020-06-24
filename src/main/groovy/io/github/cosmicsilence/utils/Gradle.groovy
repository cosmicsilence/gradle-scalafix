package io.github.cosmicsilence.utils

class Gradle {
    static boolean isVersion4(project) {
        project.gradle.gradleVersion.startsWith("4")
    }
}
