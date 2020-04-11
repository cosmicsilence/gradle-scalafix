package io.github.cosmicsilence.scalafix

import spock.lang.Specification
import spock.lang.Unroll

class SemanticDBTest extends Specification {

    def 'it should return Scalameta version'() {
        when:
        def version = SemanticDB.scalametaVersion

        then:
        version == "4.3.0"
    }

    def 'it should return supported Scala 2.11.x version'() {
        when:
        def version = SemanticDB.supportedScala211Version

        then:
        version == "2.11.12"
    }

    def 'it should return supported Scala 2.12.x version'() {
        when:
        def version = SemanticDB.supportedScala212Version

        then:
        version == "2.12.10"
    }

    @Unroll
    def 'it should return the Maven coordinates of SemanticDB for Scala #scalaVersion'() {
        when:
        def coordinates = SemanticDB.getMavenCoordinates(scalaVersion)

        then:
        coordinates.get() == "org.scalameta:semanticdb-scalac_${expectedScalaVersion}:${SemanticDB.scalametaVersion}"

        where:
        scalaVersion || expectedScalaVersion
        '2.11.8'     || SemanticDB.supportedScala211Version
        '2.11.9'     || SemanticDB.supportedScala211Version
        '2.11.10'    || SemanticDB.supportedScala211Version
        '2.11.11'    || SemanticDB.supportedScala211Version
        '2.11.12'    || SemanticDB.supportedScala211Version
        '2.12.4'     || SemanticDB.supportedScala212Version
        '2.12.5'     || SemanticDB.supportedScala212Version
        '2.12.6'     || SemanticDB.supportedScala212Version
        '2.12.7'     || SemanticDB.supportedScala212Version
        '2.12.8'     || SemanticDB.supportedScala212Version
        '2.12.9'     || SemanticDB.supportedScala212Version
        '2.12.10'    || SemanticDB.supportedScala212Version
        '2.12.11'    || SemanticDB.supportedScala212Version
        '2.13.0'     || '2.13.0'
        '2.13.1'     || '2.13.1'
    }

    def 'it should not return the Maven coordinates of SemanticDB if informed Scala version is blank'() {
        when:
        def coordinates1 = SemanticDB.getMavenCoordinates(null)
        def coordinates2 = SemanticDB.getMavenCoordinates('')
        def coordinates3 = SemanticDB.getMavenCoordinates('    ')

        then:
        !coordinates1.isPresent()
        !coordinates2.isPresent()
        !coordinates3.isPresent()
    }

    def 'it should not return the Maven coordinates of SemanticDB if informed Scala version is not supported'() {
        when:
        def coordinates1 = SemanticDB.getMavenCoordinates('2.10.7')
        def coordinates2 = SemanticDB.getMavenCoordinates('3.0.0')

        then:
        !coordinates1.isPresent()
        !coordinates2.isPresent()
    }
}
