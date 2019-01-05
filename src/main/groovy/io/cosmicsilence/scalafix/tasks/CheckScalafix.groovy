package io.cosmicsilence.scalafix.tasks

import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction

class CheckScalafix extends SourceTask {

    @TaskAction
    def check() {
        println(">>>> Config file: " + project.extensions.scalafix.configFile)
        println(">>>>> Checking files...")
        source.files.each {
            println("\t\t- ${it}")
        }
    }
}
