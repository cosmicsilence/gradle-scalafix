package io.cosmicsilence.scalafix

import org.gradle.api.logging.Logger
import scalafix.interfaces.ScalafixDiagnostic
import scalafix.interfaces.ScalafixLintID
import scalafix.interfaces.ScalafixMainCallback
import scalafix.interfaces.ScalafixSeverity

class ScalafixLogger implements ScalafixMainCallback {

    private final Logger logger

    ScalafixLogger(Logger logger) { this.logger = logger }

    @Override
    void reportDiagnostic(ScalafixDiagnostic diagnostic) {
        def fullStringID = { ScalafixLintID lintID ->
            if (lintID.categoryID().empty) lintID.ruleName()
            else if (lintID.ruleName().empty) lintID.categoryID()
            else "${lintID.ruleName()}.${lintID.categoryID()}"
        }

        def formatMessage = {
            String prefix = diagnostic.lintID().present ? "[${fullStringID(diagnostic.lintID().get())}]" : ""
            String message = prefix + diagnostic.message()

            if (diagnostic.position().present) {
                String severity = diagnostic.severity().toString().toLowerCase()
                diagnostic.position().get().formatMessage(severity, message)
            } else message
        }

        switch (diagnostic.severity()) {
            case ScalafixSeverity.INFO:
                logger.info(formatMessage())
                break

            case ScalafixSeverity.WARNING:
                logger.warn(formatMessage())
                break

            case ScalafixSeverity.ERROR:
                logger.error(formatMessage())
                break
        }
    }
}
