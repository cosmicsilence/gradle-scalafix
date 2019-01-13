package io.cosmicsilence.scalafix.tasks

import scalafix.interfaces.ScalafixMainMode

class CheckScalafixTask extends BaseScalafixTask {

    @Override
    ScalafixMainMode getScalafixMode() {
        ScalafixMainMode.CHECK
    }
}
