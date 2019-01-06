package io.cosmicsilence.scalafix

import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction

class ScalafixTask extends SourceTask {

    @TaskAction
    def run() {
        println(">>>> Config file: " + project.extensions.scalafix.configFile)
        println(">>>>> Checking files...")
        source.files.each {
            println("\t\t- ${it}")
        }
    }
}
