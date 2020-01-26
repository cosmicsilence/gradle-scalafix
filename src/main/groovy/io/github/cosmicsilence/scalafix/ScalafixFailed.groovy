package io.github.cosmicsilence.scalafix

import scalafix.interfaces.ScalafixError

import static scalafix.interfaces.ScalafixError.*

class ScalafixFailed extends RuntimeException {

    ScalafixFailed(ScalafixError... errors) {
        super("Errors:\n" + errors.collect { " - ${mapToDescription(it)}" }.join("\n"))
    }

    private static String mapToDescription(error) {
        switch (error) {
            case CommandLineError:
                return "A command-line argument parsed incorrectly"
            case LinterError:
                return "A linter error was reported"
            case MissingSemanticdbError:
                return "A semantic rule was run on a source file that has no associated *.semanticdb file"
            case NoFilesError:
                return "No files were provided to Scalafix"
            case ParseError:
                return "A source file failed to parse"
            case StaleSemanticdbError:
                return "The source file contents on disk have changed since the last compilation with the SemanticDB compiler plugin"
            case TestError:
                return "A file on disk does not match the file contents if it was fixed with Scalafix"
            case UnexpectedError:
                return "Something unexpected happened"
            default:
                return error.toString()
        }
    }
}
