package io.cosmicsilence.scalafix.tasks

import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import scalafix.interfaces.ScalafixMainMode

class CheckScalafixTask extends BaseScalafixTask {

    private static final Logger LOGGER = Logging.getLogger(CheckScalafixTask)

    @Override
    ScalafixMainMode getScalafixMode() {
        ScalafixMainMode.CHECK
    }

    @Override
    Logger getLogger() {
        LOGGER
    }
}
