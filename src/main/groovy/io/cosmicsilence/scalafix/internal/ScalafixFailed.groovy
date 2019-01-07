package io.cosmicsilence.scalafix.internal

import scalafix.interfaces.ScalafixError

class ScalafixFailed extends RuntimeException {

    ScalafixFailed(List<ScalafixError> errors) {
        super(errors.join(" "))
    }
}
