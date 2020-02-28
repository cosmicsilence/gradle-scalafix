package io.github.cosmicsilence.scalafix

import spock.lang.Specification
import spock.lang.Unroll

class SemanticDBTest extends Specification {

    def 'it should return Scalameta version'() {
        when:
        def version = SemanticDB.scalametaVersion

        then:
        version == "4.3.7"
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
        version == "2.12.11"
    }

    @Unroll
    def 'it should return the Maven coordinates of SemanticDB for Scala #scalaVersion'() {
        when:
        def artifact = SemanticDB.getMavenCoordinates(scalaVersion)

        then:
        artifact.get() == "org.scalameta:semanticdb-scalac_${expectedScalaVersion}:${SemanticDB.scalametaVersion}"

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
        def artifact1 = SemanticDB.getMavenCoordinates(null)
        def artifact2 = SemanticDB.getMavenCoordinates('')
        def artifact3 = SemanticDB.getMavenCoordinates('    ')

        then:
        !artifact1.isPresent()
        !artifact2.isPresent()
        !artifact3.isPresent()
    }

    def 'it should not return the Maven coordinates of SemanticDB if informed Scala version is not supported'() {
        when:
        def artifact = SemanticDB.getMavenCoordinates('3.0.0')

        then:
        !artifact.isPresent()
    }
}
