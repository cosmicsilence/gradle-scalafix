package io.github.cosmicsilence.scalafix

import org.gradle.api.GradleException
import spock.lang.Specification
import spock.lang.Unroll

class ScalafixPropsTest extends Specification {

    def 'it should return the Scalafix version'() {
        when:
        def version = ScalafixProps.scalafixVersion

        then:
        version == "0.9.27"
    }

    def 'it should return the Scalameta version'() {
        when:
        def version = ScalafixProps.scalametaVersion

        then:
        version == "4.4.10"
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
        version == "2.12.13"
    }

    def 'it should return the supported Scala 2.13.x version'() {
        when:
        def version = ScalafixProps.supportedScala213Version

        then:
        version == "2.13.5"
    }

    @Unroll
    def 'it should return the supported Scala version for a #projectScalaVersion project'() {
        when:
        def version = ScalafixProps.getSupportedScalaVersion(projectScalaVersion)

        then:
        version == expectedScalaVersion

        where:
        projectScalaVersion || expectedScalaVersion
        '2.11.11'           || ScalafixProps.supportedScala211Version
        '2.11.12'           || ScalafixProps.supportedScala211Version
        '2.12.11'           || ScalafixProps.supportedScala212Version
        '2.12.12'           || ScalafixProps.supportedScala212Version
        '2.13.3'            || ScalafixProps.supportedScala213Version
        '2.13.4'            || ScalafixProps.supportedScala213Version
    }

    @Unroll
    def 'it should fail to return a supported Scala version if project uses Scala #projectScalaVersion'() {
        when:
        ScalafixProps.getSupportedScalaVersion(projectScalaVersion)

        then:
        thrown GradleException

        where:
        projectScalaVersion || _
        '2.9.3'             || _
        '2.10.7'            || _
    }

    @Unroll
    def 'it should return the scalafix-cli artifact coordinates for a #projectScalaVersion project'() {
        when:
        def coordinates = ScalafixProps.getScalafixCliArtifactCoordinates(projectScalaVersion)

        then:
        coordinates == expectedCoordinates

        where:
        projectScalaVersion || expectedCoordinates
        '2.11.11'           || "ch.epfl.scala:scalafix-cli_${ScalafixProps.supportedScala211Version}:${ScalafixProps.scalafixVersion}"
        '2.12.10'           || "ch.epfl.scala:scalafix-cli_${ScalafixProps.supportedScala212Version}:${ScalafixProps.scalafixVersion}"
        '2.13.0'            || "ch.epfl.scala:scalafix-cli_${ScalafixProps.supportedScala213Version}:${ScalafixProps.scalafixVersion}"
    }

    @Unroll
    def 'it should return the semanticdb artifact coordinates for a #projectScalaVersion project'() {
        when:
        def coordinates = ScalafixProps.getSemanticDbArtifactCoordinates(projectScalaVersion, Optional.empty())

        then:
        coordinates == expectedCoordinates

        where:
        projectScalaVersion || expectedCoordinates
        '2.11.11'           || "org.scalameta:semanticdb-scalac_${projectScalaVersion}:${ScalafixProps.scalametaVersion}"
        '2.12.10'           || "org.scalameta:semanticdb-scalac_${projectScalaVersion}:${ScalafixProps.scalametaVersion}"
        '2.13.0'            || "org.scalameta:semanticdb-scalac_${projectScalaVersion}:${ScalafixProps.scalametaVersion}"
    }

    def 'it should return the semanticdb artifact coordinates using a version override'() {
        when:
        def coordinates = ScalafixProps.getSemanticDbArtifactCoordinates('2.12.10', Optional.of('1.2.3'))

        then:
        coordinates == "org.scalameta:semanticdb-scalac_2.12.10:1.2.3"
    }
}
