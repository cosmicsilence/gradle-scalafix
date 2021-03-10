package io.github.cosmicsilence.scalafix

import org.gradle.api.GradleException

abstract class ScalafixProps {

    private static final String PROPS_FILE_PATH = "scalafix-interfaces.properties"
    private static final Properties PROPS = new Properties()

    static {
        def file = ScalafixProps.classLoader.getResourceAsStream(PROPS_FILE_PATH)
        assert file != null, "File ${PROPS_FILE_PATH} not found in the classpath"
        PROPS.load(file)
    }

    private ScalafixProps() {}

    static String getScalafixVersion() {
        return PROPS.getProperty("scalafixVersion")
    }

    static String getScalametaVersion() {
        return PROPS.getProperty("scalametaVersion")
    }

    static String getSupportedScala211Version() {
        return PROPS.getProperty("scala211")
    }

    static String getSupportedScala212Version() {
        return PROPS.getProperty("scala212")
    }

    static String getSupportedScala213Version() {
        return PROPS.getProperty("scala213")
    }

    static String getSupportedScalaVersion(String projectScalaVersion) {
        switch (projectScalaVersion) {
            case ~/^2\.11\..+$/:
                return supportedScala211Version
            case ~/^2\.12\..+$/:
                return supportedScala212Version
            case ~/^2\.13\..+$/:
                return supportedScala213Version
            default:
                throw new GradleException("Scala version '${projectScalaVersion}' is not supported")
        }
    }

    static String getScalafixCliArtifactCoordinates(String projectScalaVersion) {
        return "ch.epfl.scala:scalafix-cli_${getSupportedScalaVersion(projectScalaVersion)}:${scalafixVersion}"
    }

    static String getSemanticDbArtifactCoordinates(String projectScalaVersion, String customScalametaVersion = null) {
        return "org.scalameta:semanticdb-scalac_${projectScalaVersion}:${customScalametaVersion?: scalametaVersion}"
    }
}