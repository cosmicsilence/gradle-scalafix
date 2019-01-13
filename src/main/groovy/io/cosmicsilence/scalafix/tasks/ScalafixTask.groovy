package io.cosmicsilence.scalafix.tasks

import scalafix.interfaces.ScalafixMainMode

class ScalafixTask extends BaseScalafixTask {

    @Override
    ScalafixMainMode getScalafixMode() {
        ScalafixMainMode.IN_PLACE
    }
}
