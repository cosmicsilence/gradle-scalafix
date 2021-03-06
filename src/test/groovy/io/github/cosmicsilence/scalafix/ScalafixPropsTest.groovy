package io.github.cosmicsilence.scalafix

import spock.lang.Specification
import spock.lang.Unroll

class ScalafixPropsTest extends Specification {

    def 'it should return the Scalafix version'() {
        when:
        def version = ScalafixProps.scalafixVersion

        then:
        version == "0.9.24"
    }

    def 'it should return the Scalameta version'() {
        when:
        def version = ScalafixProps.scalametaVersion

        then:
        version == "4.4.0"
    }

    def 'it should return the supported Scala 2.11.x version'() {
        when:
        def version = ScalafixProps.supportedScala211Version

        then:
        version == "2.11.12"
    }

    def 'it should return the supported Scala 2.12.x version'() {
        when:
        def version = ScalafixProps.supportedScala212Version

        then:
        version == "2.12.12"
    }

    def 'it should return the supported Scala 2.13.x version'() {
        when:
        def version = ScalafixProps.supportedScala213Version

        then:
        version == "2.13.4"
    }

    @Unroll
    def 'it should return the semanticdb artifact coordinates for a #projectScalaVersion project'() {
        when:
        def coordinates = ScalafixProps.getSemanticDbArtifactCoordinates(projectScalaVersion)

        then:
        coordinates == expectedCoordinates

        where:
        projectScalaVersion || expectedCoordinates
        '2.11.12'           || "org.scalameta:semanticdb-scalac_${projectScalaVersion}:${ScalafixProps.scalametaVersion}"
        '2.12.12'           || "org.scalameta:semanticdb-scalac_${projectScalaVersion}:${ScalafixProps.scalametaVersion}"
        '2.13.4'            || "org.scalameta:semanticdb-scalac_${projectScalaVersion}:${ScalafixProps.scalametaVersion}"
    }
}
