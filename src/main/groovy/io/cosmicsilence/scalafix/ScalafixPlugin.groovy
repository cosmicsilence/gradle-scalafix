package io.cosmicsilence.scalafix

import org.gradle.api.Plugin
import org.gradle.api.Project

class ScalafixPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.task('hello') {
            doLast {
                println 'Hello from the ScalafixPlugin'
            }
        }
    }
}
