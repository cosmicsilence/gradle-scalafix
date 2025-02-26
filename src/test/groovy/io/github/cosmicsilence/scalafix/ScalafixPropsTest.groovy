package io.github.cosmicsilence.scalafix

import org.gradle.api.GradleException
import scalafix.interfaces.Scalafix
import spock.lang.Specification
import spock.lang.Unroll

class ScalafixPropsTest extends Specification {

    def 'it should return the Scalafix version'() {
        when:
        def version = ScalafixProps.scalafixVersion

        then:
        version == Scalafix.package.implementationVersion
    }

    def 'it should return the Scalameta version'() {
        when:
        def version = ScalafixProps.scalametaVersion

        then:
        version ==~ "\\d+.\\d+.\\d+"
    }

    def 'it should return the supported Scala 2.12.x version'() {
        when:
        def version = ScalafixProps.supportedScala212Version

        then:
        version ==~ "2.12.\\d+"
    }

    def 'it should return the supported Scala 2.13.x version'() {
        when:
        def version = ScalafixProps.supportedScala213Version

        then:
        version ==~ "2.13.\\d+"
    }

    def 'it should return the supported Scala 3.x version'() {
        when:
        def version = ScalafixProps.supportedScala3Version

        then:
        version ==~ "3.\\d+.\\d+"
    }

    @Unroll
    def 'it should return the scalafix-cli artifact coordinates for a #projectScalaVersion project'() {
        when:
        def coordinates = ScalafixProps.getScalafixCliArtifactCoordinates(projectScalaVersion)

        then:
        coordinates == expectedCoordinates

        where:
        projectScalaVersion || expectedCoordinates
        '2.12.10'           || "ch.epfl.scala:scalafix-cli_${ScalafixProps.supportedScala212Version}:${ScalafixProps.scalafixVersion}"
        '2.13.0'            || "ch.epfl.scala:scalafix-cli_${ScalafixProps.supportedScala213Version}:${ScalafixProps.scalafixVersion}"
        '3.3.1'             || "ch.epfl.scala:scalafix-cli_${ScalafixProps.supportedScala3Version}:${ScalafixProps.scalafixVersion}"
    }

    @Unroll
    def 'it should fail to return the scalafix-cli artifact coordinates for Scala #projectScalaVersion'() {
        when:
        ScalafixProps.getScalafixCliArtifactCoordinates(projectScalaVersion)

        then:
        thrown GradleException

        where:
        projectScalaVersion || _
        '2.9.3'             || _
        '2.10.7'            || _
    }

    @Unroll
    def 'it should return the semanticdb artifact coordinates for a #projectScalaVersion project'() {
        when:
        def coordinates = ScalafixProps.getSemanticDbArtifactCoordinates(projectScalaVersion, Optional.empty())

        then:
        coordinates == expectedCoordinates

        where:
        projectScalaVersion || expectedCoordinates
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
