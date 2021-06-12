package io.github.cosmicsilence.scalafix

import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification
import spock.lang.Unroll

class ScalafixExtensionTest extends Specification {

    private ScalafixExtension extension

    def setup() {
        def project = ProjectBuilder.builder().build()
        extension = project.extensions.create('scalafix', ScalafixExtension, project)
    }

    @Unroll
    def 'isSemanticdbEnabled should be #enabled if semanticdb.autoConfigure is #autoConfigure and autoConfigureSemanticdb is #deprecatedAutoConfigure'(){
        when:
        extension.semanticdb.autoConfigure = autoConfigure
        extension.autoConfigureSemanticdb = deprecatedAutoConfigure

        then:
        extension.isSemanticdbEnabled() == enabled

        where:
        autoConfigure | deprecatedAutoConfigure | enabled
        true          | true                    | true
        true          | false                   | false
        false         | true                    | false
        false         | false                   | false
        false         | null                    | false
        true          | null                    | true
    }
}
