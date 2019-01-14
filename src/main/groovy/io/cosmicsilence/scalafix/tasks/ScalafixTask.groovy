package io.cosmicsilence.scalafix.tasks

import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import scalafix.interfaces.ScalafixMainMode

class ScalafixTask extends BaseScalafixTask {

    private static final Logger LOGGER = Logging.getLogger(ScalafixTask)

    @Override
    ScalafixMainMode getScalafixMode() {
        ScalafixMainMode.IN_PLACE
    }

    @Override
    Logger getLogger() {
        LOGGER
    }
}
