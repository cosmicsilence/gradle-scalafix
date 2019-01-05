package io.cosmicsilence.scalafix.tasks

import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction

class Scalafix extends SourceTask {

    @TaskAction
    def fix() {
        println(">>>>>>>> Fixing files...")
        source.files.each {
            println("\t\t- ${it}")
        }
    }
}
