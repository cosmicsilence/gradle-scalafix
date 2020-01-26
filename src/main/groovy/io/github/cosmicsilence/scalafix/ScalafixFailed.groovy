package io.github.cosmicsilence.scalafix

import scalafix.interfaces.ScalafixError

class ScalafixFailed extends RuntimeException {

    ScalafixFailed(List<ScalafixError> errors) {
        super(errors.join(" "))
    }
}
